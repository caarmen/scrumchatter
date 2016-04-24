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

import java.util.Arrays;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import ca.rmen.android.scrumchatter.util.Log;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.dialog.ChoiceDialogFragment.DialogItemListener;
import ca.rmen.android.scrumchatter.dialog.ConfirmDialogFragment.DialogButtonListener;
import permissions.dispatcher.PermissionRequest;

/**
 * Create different types of dialog fragments (edit text input, information, choice, confirmation, progress).
 * The dialogs created by this class are not only created but also shown in the activity given to the creation methods.
 */
public class DialogFragmentFactory extends DialogFragment {

    private static final String TAG = Constants.TAG + "/" + DialogFragmentFactory.class.getSimpleName();
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_MESSAGE = "message";
    static final String EXTRA_ACTION_ID = "action_id";
    static final String EXTRA_CHOICES = "choices";
    static final String EXTRA_SELECTED_ITEM = "selected_item";
    static final String EXTRA_EXTRAS = "extras";
    static final String EXTRA_INPUT_HINT = "input_hint";
    static final String EXTRA_INPUT_VALIDATOR_CLASS = "input_validator_class";
    static final String EXTRA_ENTERED_TEXT = "entered_text";

    /**
     * @param inputValidatorClass will be called with each text event on the edit text, to validate the user's input.
     */
    public static void showInputDialog(FragmentActivity activity, String title, String inputHint, String prefilledText,
            Class<?> inputValidatorClass, int actionId, Bundle extras) {
        Log.v(TAG, "showInputDialog: title = " + title + ", prefilledText =  " + prefilledText + ", actionId = " + actionId + ", extras = " + extras);
        Bundle arguments = new Bundle(6);
        arguments.putString(EXTRA_TITLE, title);
        arguments.putString(EXTRA_INPUT_HINT, inputHint);
        arguments.putInt(EXTRA_ACTION_ID, actionId);
        arguments.putString(EXTRA_ENTERED_TEXT, prefilledText);
        if (inputValidatorClass != null) arguments.putSerializable(EXTRA_INPUT_VALIDATOR_CLASS, inputValidatorClass);
        arguments.putBundle(EXTRA_EXTRAS, extras);
        InputDialogFragment result = new InputDialogFragment();
        result.setArguments(arguments);
        result.show(activity.getSupportFragmentManager(), InputDialogFragment.class.getSimpleName());
    }

    /**
     * Show a visible dialog fragment with the given title and message, and just one OK button.
     */
    public static void showInfoDialog(FragmentActivity activity, int titleId, int messageId) {
        Log.v(TAG, "showInfoDialog");
        Bundle arguments = new Bundle(3);
        arguments.putString(EXTRA_TITLE, activity.getString(titleId));
        arguments.putString(EXTRA_MESSAGE, activity.getString(messageId));
        InfoDialogFragment result = new InfoDialogFragment();
        result.setArguments(arguments);
        result.show(activity.getSupportFragmentManager(), InfoDialogFragment.class.getSimpleName());
    }

    /**
     * Show a visible dialog fragment with the given title and message, and an ok and cancel button. If the given activity implements
     *         {@link DialogButtonListener}, the actionId and extras parameter will be provided in
     *         the {@link DialogButtonListener#onOkClicked(int, Bundle)} callback on the activity, when the user clicks on the ok button.
     */
    public static void showConfirmDialog(FragmentActivity activity, String title, String message, int actionId, Bundle extras) {
        Log.v(TAG, "showConfirmDialog: title = " + title + ", message = " + message + ", actionId = " + actionId + ", extras = " + extras);
        ConfirmDialogFragment result = new ConfirmDialogFragment();
        Bundle arguments = new Bundle(4);
        arguments.putString(EXTRA_TITLE, title);
        arguments.putString(EXTRA_MESSAGE, message);
        arguments.putInt(EXTRA_ACTION_ID, actionId);
        if (extras != null) arguments.putBundle(EXTRA_EXTRAS, extras);
        result.setArguments(arguments);
        result.show(activity.getSupportFragmentManager(), ConfirmDialogFragment.class.getSimpleName());
    }

    /**
     * Show a visible dialog fragment with the given title and list of items. If the given activity implements {@link DialogItemListener}, the
     *         actionId, list of items, and item selected by the user, will be provided in the
     *         {@link DialogItemListener#onItemSelected(int, CharSequence[], int)} callback on the activity, when the user selects an item.
     * @param selectedItem if greater than zero, then the given item at that index will be pre-selected in the list.
     */
    public static void showChoiceDialog(FragmentActivity activity, String title, CharSequence[] items, int selectedItem, @SuppressWarnings("SameParameterValue") int actionId) {
        Log.v(TAG, "showChoiceDialog: title = " + title + ", actionId = " + actionId + ", items =" + Arrays.toString(items) + ", selectedItem = "
                + selectedItem);
        ChoiceDialogFragment result = new ChoiceDialogFragment();
        Bundle arguments = new Bundle(5);
        arguments.putString(EXTRA_TITLE, title);
        arguments.putInt(EXTRA_ACTION_ID, actionId);
        arguments.putCharSequenceArray(EXTRA_CHOICES, items);
        arguments.putInt(EXTRA_SELECTED_ITEM, selectedItem);
        result.setArguments(arguments);
        result.show(activity.getSupportFragmentManager(), ChoiceDialogFragment.class.getSimpleName());
    }

    /**
     * Show a visible dialog fragment with the given message.
     * @param tag should be used by the calling activity, when the background task is complete, to find the fragment and dismiss it.
     */
    public static void showProgressDialog(FragmentActivity activity, String message, @SuppressWarnings("SameParameterValue") String tag) {
        Log.v(TAG, "showProgressDialog: message = " + message);
        Bundle arguments = new Bundle(2);
        arguments.putString(EXTRA_MESSAGE, message);
        ProgressDialogFragment result = new ProgressDialogFragment();
        result.setArguments(arguments);
        result.show(activity.getSupportFragmentManager(), tag);
    }

    public static void showPermissionRationaleDialog(Context context, String rationale, final PermissionRequest request) {
        new AlertDialog.Builder(context)
                .setMessage(rationale)
                .setPositiveButton(R.string.allow, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request.proceed();
                    }
                }).setNegativeButton(R.string.deny, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                request.cancel();
            }
        }).show();
    }

}
