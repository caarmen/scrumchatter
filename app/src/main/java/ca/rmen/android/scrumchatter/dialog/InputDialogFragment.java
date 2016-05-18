/**
 * Copyright 2013 Carmen Alvarez
 *
 * This file is part of Scrum Chatter.
 *
 * Scrum Chatter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Scrum Chatter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Scrum Chatter. If not, see <http://www.gnu.org/licenses/>.
 */
package ca.rmen.android.scrumchatter.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnShowListener;
import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;

import ca.rmen.android.scrumchatter.databinding.InputDialogEditTextBinding;
import ca.rmen.android.scrumchatter.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;

/**
 * A dialog fragment with an EditText for user text input.
 */
public class InputDialogFragment extends DialogFragment { // NO_UCD (use default)

    private static final String TAG = Constants.TAG + "/" + InputDialogFragment.class.getSimpleName();

    private String mEnteredText;

    public interface InputValidator {
        /**
         * @param input the text entered by the user.
         * @return an error string if the input has a problem, null if the input is valid.
         */
        String getError(Context context, CharSequence input, Bundle extras);
    }

    /**
     * The activity owning this dialog fragment should implement this interface to be notified when the user submits entered text.
     */
    public interface DialogInputListener {
        void onInputEntered(int actionId, String input, Bundle extras);
    }


    public InputDialogFragment() {
        super();
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.v(TAG, "onCreateDialog: savedInstanceState = " + savedInstanceState);
        if (savedInstanceState != null) mEnteredText = savedInstanceState.getString(DialogFragmentFactory.EXTRA_ENTERED_TEXT);
        Bundle arguments = getArguments();
        final int actionId = arguments.getInt(DialogFragmentFactory.EXTRA_ACTION_ID);

        final InputDialogEditTextBinding binding = DataBindingUtil.inflate(
                LayoutInflater.from(getActivity()),
                R.layout.input_dialog_edit_text,
                null,
                false);

        final Bundle extras = arguments.getBundle(DialogFragmentFactory.EXTRA_EXTRAS);
        final Class<?> inputValidatorClass = (Class<?>) arguments.getSerializable(DialogFragmentFactory.EXTRA_INPUT_VALIDATOR_CLASS);
        final String prefilledText = arguments.getString(DialogFragmentFactory.EXTRA_ENTERED_TEXT);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(arguments.getString(DialogFragmentFactory.EXTRA_TITLE));
        builder.setView(binding.getRoot());
        binding.edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        binding.edit.setHint(arguments.getString(DialogFragmentFactory.EXTRA_INPUT_HINT));
        binding.edit.setText(prefilledText);
        if (!TextUtils.isEmpty(mEnteredText)) binding.edit.setText(mEnteredText);

        // Notify the activity of the click on the OK button.
        OnClickListener listener = null;
        if ((getActivity() instanceof DialogInputListener)) {
            listener = new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    FragmentActivity activity = getActivity();
                    if (activity == null) Log.w(TAG, "User clicked on dialog after it was detached from activity. Monkey?");
                    else
                        ((DialogInputListener) activity).onInputEntered(actionId, binding.edit.getText().toString(), extras);
                }
            };
        }
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok, listener);

        final AlertDialog dialog = builder.create();
        // Show the keyboard when the EditText gains focus.
        binding.edit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });

        final Context context = getActivity();
        try {
            final InputValidator validator = inputValidatorClass == null ? null : (InputValidator) inputValidatorClass.newInstance();
            Log.v(TAG, "input validator = " + validator);
            // Validate the text as the user types.
            binding.edit.addTextChangedListener(new TextWatcher() {

                @Override
                public void afterTextChanged(Editable s) {}

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    mEnteredText = binding.edit.getText().toString();
                    if (validator != null) validateText(context, dialog, binding.edit, validator, actionId, extras);
                }
            });
            dialog.setOnShowListener(new OnShowListener() {

                @Override
                public void onShow(DialogInterface dialogInterface) {
                    Log.v(TAG, "onShow");
                    validateText(context, dialog, binding.edit, validator, actionId, extras);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Could not instantiate validator " + inputValidatorClass + ": " + e.getMessage(), e);
        }

        return dialog;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        Log.v(TAG, "onSaveInstanceState: bundle = " + bundle);
        bundle.putString(DialogFragmentFactory.EXTRA_ENTERED_TEXT, mEnteredText);
        super.onSaveInstanceState(bundle);
    }

    /**
     * Invoke the input validator in a background thread. If the validator returns an error, the given edit text will be updated with the error in a tooltip.
     */
    private static void validateText(final Context context, AlertDialog dialog, final EditText editText, final InputValidator validator, final int actionId,
            final Bundle extras) {
        Log.v(TAG, "validateText: input = " + editText.getText().toString() + ", actionId = " + actionId + ", extras = " + extras);
        // Start off with everything a-ok.
        editText.setError(null);
        final Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        okButton.setEnabled(!TextUtils.isEmpty(editText.getText()));

        // Search for an error in background thread, update the dialog in the UI thread.
        AsyncTask<String, Void, String> task = new AsyncTask<String, Void, String>() {

            /**
             * @return an error String if the input is invalid.
             */
            @Override
            protected String doInBackground(String... text) {
                return validator.getError(context, text[0], extras);
            }

            @Override
            protected void onPostExecute(String error) {
                // If the input is invalid, highlight the error
                // and disable the OK button.
                if (!TextUtils.isEmpty(error)) {
                    editText.setError(error);
                    okButton.setEnabled(false);
                }
            }
        };
        task.execute(editText.getText().toString().trim());
    }
}
