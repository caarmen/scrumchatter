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

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.ui.ScrumChatterDialogFragment.DialogType;
import ca.rmen.android.scrumchatter.ui.ScrumChatterDialogFragment.ScrumChatterDialogButtonListener;
import ca.rmen.android.scrumchatter.ui.ScrumChatterDialogFragment.ScrumChatterDialogItemListener;

/**
 * Create different types of dialog fragments (information, choice, confirmation, progress).
 * The dialogs created by this class are not only created but also shown in the activity given to the creation methods.
 */
public class ScrumChatterDialogFragmentFactory extends DialogFragment {

    private static final String TAG = Constants.TAG + "/" + ScrumChatterDialogFragmentFactory.class.getSimpleName();

    /**
     * @param validatorClass will be called with each text event on the edit text, to validate the user's input.
     */
    public static ScrumChatterInputDialogFragment showInputDialog(FragmentActivity activity, String title, String inputHint, String prefilledText,
            Class<?> inputValidatorClass, int actionId, Bundle extras) {
        Log.v(TAG, "showInputDialog: title = " + title + ", prefilledText =  " + prefilledText + ", actionId = " + actionId + ", extras = " + extras);
        Bundle arguments = new Bundle(6);
        arguments.putString(ScrumChatterDialogFragment.EXTRA_TITLE, title);
        arguments.putString(ScrumChatterInputDialogFragment.EXTRA_INPUT_HINT, inputHint);
        arguments.putInt(ScrumChatterDialogFragment.EXTRA_ACTION_ID, actionId);
        arguments.putString(ScrumChatterInputDialogFragment.EXTRA_ENTERED_TEXT, prefilledText);
        if (inputValidatorClass != null) arguments.putSerializable(ScrumChatterInputDialogFragment.EXTRA_INPUT_VALIDATOR_CLASS, inputValidatorClass);
        arguments.putBundle(ScrumChatterDialogFragment.EXTRA_EXTRAS, extras);
        ScrumChatterInputDialogFragment result = new ScrumChatterInputDialogFragment();
        result.setArguments(arguments);
        result.show(activity.getSupportFragmentManager(), ScrumChatterInputDialogFragment.class.getSimpleName());
        return result;
    }

    /**
     * @return a visible dialog fragment with the given title and message, and just one OK button.
     */
    public static ScrumChatterDialogFragment showInfoDialog(FragmentActivity activity, int titleId, int messageId) {
        Log.v(TAG, "showInfoDialog");
        Bundle arguments = new Bundle(3);
        arguments.putString(ScrumChatterDialogFragment.EXTRA_TITLE, activity.getString(titleId));
        arguments.putString(ScrumChatterDialogFragment.EXTRA_MESSAGE, activity.getString(messageId));
        return showDialog(activity, arguments, DialogType.INFO);
    }

    /**
     * @return a visible dialog fragment with the given title and message, and an ok and cancel button. If the given activity implements
     *         {@link ScrumChatterDialogButtonListener}, the actionId and extras parameter will be provided in
     *         the {@link ScrumChatterDialogButtonListener#onOkClicked(int, Bundle)} callback on the activity, when the user clicks on the ok button.
     */
    public static ScrumChatterDialogFragment showConfirmDialog(FragmentActivity activity, String title, String message, int actionId, Bundle extras) {
        Log.v(TAG, "showConfirmDialog: title = " + title + ", message = " + message + ", actionId = " + actionId + ", extras = " + extras);
        Bundle arguments = new Bundle(4);
        arguments.putString(ScrumChatterDialogFragment.EXTRA_TITLE, title);
        arguments.putString(ScrumChatterDialogFragment.EXTRA_MESSAGE, message);
        arguments.putInt(ScrumChatterDialogFragment.EXTRA_ACTION_ID, actionId);
        if (extras != null) arguments.putBundle(ScrumChatterDialogFragment.EXTRA_EXTRAS, extras);
        return showDialog(activity, arguments, DialogType.CONFIRM);
    }

    /**
     * @return a visible dialog fragment with the given title and list of items. If the given activity implements {@link ScrumChatterDialogItemListener}, the
     *         actionId, list of items, and item selected by the user, will be provided in the
     *         {@link ScrumChatterDialogItemListener#onItemSelected(int, CharSequence[], int)} callback on the activity, when the user selects an item.
     * @param selectedItem if greater than zero, then the given item at that index will be pre-selected in the list.
     */
    public static ScrumChatterDialogFragment showChoiceDialog(FragmentActivity activity, String title, CharSequence[] items, int selectedItem, int actionId) {
        Log.v(TAG, "showChoiceDialog: title = " + title + ", actionId = " + actionId + ", items =" + Arrays.toString(items) + ", selectedItem = "
                + selectedItem);
        Bundle arguments = new Bundle(5);
        arguments.putString(ScrumChatterDialogFragment.EXTRA_TITLE, title);
        arguments.putInt(ScrumChatterDialogFragment.EXTRA_ACTION_ID, actionId);
        arguments.putCharSequenceArray(ScrumChatterDialogFragment.EXTRA_CHOICES, items);
        arguments.putInt(ScrumChatterDialogFragment.EXTRA_SELECTED_ITEM, selectedItem);
        return showDialog(activity, arguments, DialogType.CHOICE);
    }

    /**
     * @return a visible dialog fragment with the given message.
     * @param tag should be used by the calling activity, when the background task is complete, to find the fragment and dismiss it.
     */
    public static ScrumChatterDialogFragment showProgressDialog(FragmentActivity activity, String message, String tag) {
        Log.v(TAG, "showProgressDialog: message = " + message);
        Bundle arguments = new Bundle(2);
        arguments.putString(ScrumChatterDialogFragment.EXTRA_MESSAGE, message);
        arguments.putSerializable(ScrumChatterDialogFragment.EXTRA_DIALOG_TYPE, DialogType.PROGRESS);
        ScrumChatterDialogFragment result = new ScrumChatterDialogFragment();
        result.setArguments(arguments);
        result.show(activity.getSupportFragmentManager(), tag);
        return result;
    }

    /**
     * @return a visible dialog fragment.
     * @param arguments will be set on the dialog fragment and will be retrieved in onCreateDialog to create the actual dialog.
     * @param dialogType will be used on onCreateDialog to determine what time of AlertDialog to create.
     */
    private static ScrumChatterDialogFragment showDialog(FragmentActivity activity, Bundle arguments, DialogType dialogType) {
        ScrumChatterDialogFragment result = new ScrumChatterDialogFragment();
        arguments.putSerializable(ScrumChatterDialogFragment.EXTRA_DIALOG_TYPE, dialogType);
        result.setArguments(arguments);
        result.show(activity.getSupportFragmentManager(), ScrumChatterDialogFragment.class.getSimpleName());
        return result;
    }

}
