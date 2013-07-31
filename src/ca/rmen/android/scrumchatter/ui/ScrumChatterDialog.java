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

import java.util.Arrays;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;

/**
 * Boy is customizing alert dialogs a pain in the booty. Tried the android-styled-dialogs library but it didn't fit the needs of this app: no support for alert
 * dialogs with EditTexts, and not a clean way to manage clicks on the dialog buttons. Started out trying to copy the resources used for dialogs, one-by-one,
 * from the core android framework, but that was more pain than the approach I decided to take in this class.
 */
public class ScrumChatterDialog {

    private static final String TAG = Constants.TAG + "/" + ScrumChatterDialog.class.getSimpleName();

    public interface InputValidator {
        /**
         * @param input the text entered by the user.
         * @return an error string if the input has a problem, null if the input is valid.
         */
        String getError(CharSequence input);
    };

    /**
     * @param input an EditText for user input
     * @param validator will be called with each text event on the edit text, to validate the user's input.
     */
    public static AlertDialog showEditTextDialog(Context context, int titleId, int hintId, final EditText input,
            DialogInterface.OnClickListener positiveListener, final InputValidator validator) {
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        final AlertDialog dialog = showDialog(context, titleId, 0, input, positiveListener);
        input.setHint(hintId);
        // Show the keyboard when the EditText gains focus.
        input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
        // Validate the text as the user types.
        input.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateMemberName();
            }

            private void validateMemberName() {
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
                        return validator.getError(input.getText().toString().trim());
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
        });
        return dialog;
    }

    public static AlertDialog showChoiceDialog(Context context, int titleId, int choicesArrayId, int selectedItem, DialogInterface.OnClickListener itemListener) {
        return showDialog(context, context.getString(titleId), null, null, context.getResources().getStringArray(choicesArrayId), selectedItem, itemListener);
    }

    public static AlertDialog showDialog(Context context, String title, String message, DialogInterface.OnClickListener positiveListener) {
        return showDialog(context, title, message, null, null, -1, positiveListener);
    }

    private static AlertDialog showDialog(Context context, int titleId, int messageId, View customView, DialogInterface.OnClickListener positiveListener) {
        String title = titleId > 0 ? context.getString(titleId) : null;
        String message = messageId > 0 ? context.getString(messageId) : null;
        return showDialog(context, title, message, customView, null, -1, positiveListener);
    }

    /**
     * @param title Optional. The title of the dialog.
     * @param message Optional. The message of the dialog.
     * @param customView Optional. A custom view for the dialog.
     * @param items Optional. A list of items. If this is provided, the dialog will have no buttons. If the listener is provided, the listener will be notified
     *            when an item is selected.
     * @param selectedItem Optional. If items is provided, you may optionally select one of the items to be selected by default.
     * @param listener Optional. If a list of items is provided, the listener will be notified when the user selects an item. Otherwise the listener will be
     *            notified when the user taps on the positive or negative button.
     * @return
     */
    private static AlertDialog showDialog(Context context, String title, String message, View customView, CharSequence[] items, int selectedItem,
            DialogInterface.OnClickListener listener) {
        Log.v(TAG, "showDialog: title = " + title + ", message = " + message + ", customView = " + customView + ", items = " + Arrays.toString(items)
                + ", listener = " + listener);

        final Context contextWrapper = new ContextThemeWrapper(context, R.style.dialogStyle);

        AlertDialog.Builder builder = new AlertDialog.Builder(contextWrapper);
        builder.setTitle(title).setMessage(message);

        // If items are provided, set the items.  Otherwise add a positive and negative button.
        if (items != null && items.length > 0) {
            if (selectedItem >= 0) builder.setSingleChoiceItems(items, selectedItem, listener);
            else
                builder.setItems(items, listener);
        } else {
            builder.setNegativeButton(android.R.string.cancel, listener).setPositiveButton(android.R.string.ok, listener);
        }

        // Add a custom view if provided.
        if (customView != null) builder.setView(customView);

        // Show the dialog (we have to do this before we can modify its views).
        AlertDialog dialog = builder.create();
        dialog.getContext().setTheme(R.style.dialogStyle);

        OnShowListener showListener = new OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {
                DialogStyleHacks.uglyHackReplaceBlueHoloBackground(contextWrapper, (ViewGroup) ((Dialog) dialog).getWindow().getDecorView());
            }
        };
        dialog.setOnShowListener(showListener);
        dialog.show();
        return dialog;
    }
}
