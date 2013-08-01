/**
 * Copyright 2013 Carmen Alvarez
 *
 * This file is part of Scrum Chatter.
 *
 * Scrum Chatter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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
package ca.rmen.android.scrumchatter.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnShowListener;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;

/**
 * Boy is customizing alert dialogs a pain in the booty. Tried the android-styled-dialogs library but it didn't fit the needs of this app: no support for alert
 * dialogs with EditTexts, and not a clean way to manage clicks on the dialog buttons. Started out trying to copy the resources used for dialogs, one-by-one,
 * from the core android framework, but that was more pain than the approach I decided to take in this class.
 */
/**
 * @author calvarez
 * 
 */
public class ScrumChatterDialogFragment extends DialogFragment {

    private static final String TAG = Constants.TAG + "/" + ScrumChatterDialogFragment.class.getSimpleName();
    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_MESSAGE = "message";
    private static final String EXTRA_DIALOG_TYPE = "dialog_type";
    private static final String EXTRA_ACTION_ID = "action_id";
    private static final String EXTRA_CHOICES = "choices";
    private static final String EXTRA_SELECTED_ITEM = "selected_item";
    private static final String EXTRA_INPUT_HINT = "input_hint";
    private static final String EXTRA_INPUT_VALIDATOR_CLASS = "input_validator_class";
    private static final String EXTRA_EXTRAS = "extras";

    private static final String EXTRA_ENTERED_TEXT = "entered_text";

    private String mEnteredText;

    private static enum DialogType {
        INFO, INPUT, CHOICE, CONFIRM
    };

    public interface InputValidator {
        /**
         * @param input the text entered by the user.
         * @return an error string if the input has a problem, null if the input is valid.
         */
        String getError(Context context, int actionId, CharSequence input, Bundle extras);
    };

    public interface ScrumChatterDialogButtonListener {
        void onOkClicked(int actionId, Bundle extras);
    }

    public interface ScrumChatterDialogItemListener {
        void onItemSelected(int actionId, CharSequence[] choices, int which);
    }

    public interface ScrumChatterDialogInputListener {
        void onInputEntered(int actionId, String input, Bundle extras);
    }


    /**
     * @return a dialog with the given title and message, and just one OK button.
     */
    public static ScrumChatterDialogFragment showInfoDialog(FragmentActivity activity, int titleId, int messageId) {
        Bundle arguments = new Bundle(3);
        arguments.putString(EXTRA_TITLE, activity.getString(titleId));
        arguments.putString(EXTRA_MESSAGE, activity.getString(messageId));
        arguments.putSerializable(EXTRA_DIALOG_TYPE, DialogType.INFO);
        ScrumChatterDialogFragment result = new ScrumChatterDialogFragment();
        result.setArguments(arguments);
        result.show(activity.getSupportFragmentManager(), TAG);
        return result;
    }

    public static ScrumChatterDialogFragment showConfirmDialog(FragmentActivity activity, String title, String message, int actionId, Bundle extras) {
        Bundle arguments = new Bundle(4);
        arguments.putString(EXTRA_TITLE, title);
        arguments.putString(EXTRA_MESSAGE, message);
        arguments.putSerializable(EXTRA_DIALOG_TYPE, DialogType.CONFIRM);
        arguments.putInt(EXTRA_ACTION_ID, actionId);
        if (extras != null) arguments.putBundle(EXTRA_EXTRAS, extras);
        ScrumChatterDialogFragment result = new ScrumChatterDialogFragment();
        result.setArguments(arguments);
        result.show(activity.getSupportFragmentManager(), TAG);
        return result;
    }

    public static ScrumChatterDialogFragment showChoiceDialog(FragmentActivity activity, String title, CharSequence[] items, int selectedItem, int actionId) {
        Bundle arguments = new Bundle(5);
        arguments.putString(EXTRA_TITLE, title);
        arguments.putSerializable(EXTRA_DIALOG_TYPE, DialogType.CHOICE);
        arguments.putInt(EXTRA_ACTION_ID, actionId);
        arguments.putCharSequenceArray(EXTRA_CHOICES, items);
        arguments.putInt(EXTRA_SELECTED_ITEM, selectedItem);
        ScrumChatterDialogFragment result = new ScrumChatterDialogFragment();
        result.setArguments(arguments);
        result.show(activity.getSupportFragmentManager(), TAG);
        return result;
    }

    /**
     * @param validatorClass will be called with each text event on the edit text, to validate the user's input.
     */
    public static ScrumChatterDialogFragment showInputDialog(FragmentActivity activity, String title, String inputHint, String prefilledText,
            Class<?> inputValidatorClass, int actionId, Bundle extras) {
        Bundle arguments = new Bundle(6);
        arguments.putString(EXTRA_TITLE, title);
        arguments.putSerializable(EXTRA_DIALOG_TYPE, DialogType.INPUT);
        arguments.putString(EXTRA_INPUT_HINT, inputHint);
        arguments.putInt(EXTRA_ACTION_ID, actionId);
        arguments.putString(EXTRA_ENTERED_TEXT, prefilledText);
        if (inputValidatorClass != null) arguments.putSerializable(EXTRA_INPUT_VALIDATOR_CLASS, inputValidatorClass);
        arguments.putBundle(EXTRA_EXTRAS, extras);
        ScrumChatterDialogFragment result = new ScrumChatterDialogFragment();
        result.setArguments(arguments);
        result.show(activity.getSupportFragmentManager(), TAG);
        return result;
    }


    public ScrumChatterDialogFragment() {}


    @Override
    public void onActivityCreated(Bundle bundle) {
        Log.v(TAG, "onActivityCreated: bundle = " + bundle);
        super.onActivityCreated(bundle);
        if (bundle != null) mEnteredText = bundle.getString(EXTRA_ENTERED_TEXT);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate: savedInstanceState = " + savedInstanceState);
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) mEnteredText = savedInstanceState.getString(EXTRA_ENTERED_TEXT);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        Log.v(TAG, "onSaveInstanceState: bundle = " + bundle);
        super.onSaveInstanceState(bundle);
        bundle.putString(EXTRA_ENTERED_TEXT, mEnteredText);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView: savedInstanceState = " + savedInstanceState);
        if (savedInstanceState != null) mEnteredText = savedInstanceState.getString(EXTRA_ENTERED_TEXT);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.v(TAG, "onCreateDialog: savedInstanceState = " + savedInstanceState);
        if (savedInstanceState != null) mEnteredText = savedInstanceState.getString(EXTRA_ENTERED_TEXT);
        DialogType dialogType = (DialogType) getArguments().getSerializable(EXTRA_DIALOG_TYPE);
        switch (dialogType) {
            case INFO:
                return createInfoDialog();
            case CONFIRM:
                return createConfirmDialog();
            case CHOICE:
                return createChoiceDialog();
            case INPUT:
                return createInputDialog();
            default:
                throw new IllegalArgumentException("Dialog type not specified or unkown: " + dialogType);
        }
    }

    private Dialog createInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        Bundle arguments = getArguments();
        builder.setTitle(arguments.getString(EXTRA_TITLE)).setMessage(arguments.getString(EXTRA_MESSAGE)).setNeutralButton(android.R.string.ok, null);
        final AlertDialog dialog = builder.create();
        styleDialog(dialog);
        return dialog;
    }

    private Dialog createConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        Bundle arguments = getArguments();
        builder.setTitle(arguments.getString(EXTRA_TITLE)).setMessage(arguments.getString(EXTRA_MESSAGE));
        final int actionId = arguments.getInt(EXTRA_ACTION_ID);
        final Bundle extras = arguments.getBundle(EXTRA_EXTRAS);
        OnClickListener positiveListener = null;
        if (getActivity() instanceof ScrumChatterDialogButtonListener) {
            positiveListener = new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ((ScrumChatterDialogButtonListener) getActivity()).onOkClicked(actionId, extras);
                }
            };
        }
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok, positiveListener);
        final AlertDialog dialog = builder.create();
        styleDialog(dialog);
        return dialog;

    }

    private Dialog createChoiceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        Bundle arguments = getArguments();
        builder.setTitle(arguments.getString(EXTRA_TITLE));
        final int actionId = arguments.getInt(EXTRA_ACTION_ID);
        int selectedItem = arguments.getInt(EXTRA_SELECTED_ITEM);
        final CharSequence[] choices = arguments.getCharSequenceArray(EXTRA_CHOICES);
        OnClickListener listener = null;
        if (getActivity() instanceof ScrumChatterDialogItemListener) {
            listener = new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    ((ScrumChatterDialogItemListener) getActivity()).onItemSelected(actionId, choices, which);
                }
            };
        }
        if (selectedItem >= 0) builder.setSingleChoiceItems(choices, selectedItem, listener);
        else
            builder.setItems(choices, listener);

        final AlertDialog dialog = builder.create();
        styleDialog(dialog);
        return dialog;

    }

    private Dialog createInputDialog() {
        Log.v(TAG, "createInputDialog");
        Bundle arguments = getArguments();
        final int actionId = arguments.getInt(EXTRA_ACTION_ID);
        final EditText input = new EditText(getActivity());
        final Bundle extras = arguments.getBundle(EXTRA_EXTRAS);
        final Class<?> inputValidatorClass = (Class<?>) arguments.getSerializable(EXTRA_INPUT_VALIDATOR_CLASS);
        final String prefilledText = arguments.getString(EXTRA_ENTERED_TEXT);
        final Context context = getActivity().getApplicationContext();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(arguments.getString(EXTRA_TITLE));
        builder.setView(input);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setHint(arguments.getString(EXTRA_INPUT_HINT));
        input.setText(prefilledText);
        if (!TextUtils.isEmpty(mEnteredText)) input.setText(mEnteredText);

        OnClickListener listener = null;
        if ((getActivity() instanceof ScrumChatterDialogInputListener)) {
            listener = new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ((ScrumChatterDialogInputListener) getActivity()).onInputEntered(actionId, input.getText().toString(), extras);
                }
            };
        }
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok, listener);

        final AlertDialog dialog = builder.create();
        // Show the keyboard when the EditText gains focus.
        input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
        try {
            final InputValidator validator = inputValidatorClass == null ? null : (InputValidator) inputValidatorClass.newInstance();
            Log.v(TAG, "input validator = " + validator);
            // Validate the text as the user types.
            input.addTextChangedListener(new TextWatcher() {

                @Override
                public void afterTextChanged(Editable s) {}

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    mEnteredText = input.getText().toString();
                    if (validator != null) validateText(context, dialog, input, validator, actionId, extras);
                }
            });
            dialog.getContext().setTheme(R.style.dialogStyle);
            dialog.setOnShowListener(new OnShowListener() {

                @Override
                public void onShow(DialogInterface dialogInterface) {
                    Log.v(TAG, "onShow");
                    DialogStyleHacks.uglyHackReplaceBlueHoloBackground(getActivity(), (ViewGroup) dialog.getWindow().getDecorView());
                    validateText(context, dialog, input, validator, actionId, extras);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Could not instantiate validator " + inputValidatorClass + ": " + e.getMessage(), e);
        }

        return dialog;
    }

    private static void validateText(final Context context, AlertDialog dialog, final EditText input, final InputValidator validator, final int actionId,
            final Bundle extras) {
        Log.v(TAG, "validateText: input = " + input.getText().toString() + ", actionId = " + actionId + ", extras = " + extras);
        // Start off with everything a-ok.
        input.setError(null);
        final Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        okButton.setEnabled(true);

        // Search for an error in background thread, update the dialog in the UI thread.
        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {

            /**
             * @return an error String if the input is invalid.
             */
            @Override
            protected String doInBackground(Void... params) {
                return validator.getError(context, actionId, input.getText().toString().trim(), extras);
            }

            @Override
            protected void onPostExecute(String error) {
                // If the input is invalid, highlight the error
                // and disable the OK button.
                if (!TextUtils.isEmpty(error)) {
                    input.setError(error);
                    okButton.setEnabled(false);
                }
            }
        };
        task.execute();

    }

    private void styleDialog(final AlertDialog dialog) {
        dialog.getContext().setTheme(R.style.dialogStyle);
        dialog.setOnShowListener(new OnShowListener() {

            @Override
            public void onShow(DialogInterface dialogInterface) {
                // For 3.x+, update the dialog elements which couldn't be updated cleanly with the theme:
                // The list items.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    ListView listView = dialog.getListView();
                    if (listView != null) listView.setSelector(R.drawable.selector);
                }
                DialogStyleHacks.uglyHackReplaceBlueHoloBackground(getActivity(), (ViewGroup) dialog.getWindow().getDecorView());

            }
        });

    }


}
