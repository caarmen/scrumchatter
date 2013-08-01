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
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnShowListener;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.ViewGroup;
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
    private static final String EXTRA_EXTRAS = "extras";

    private static enum DialogType {
        INFO, CHOICE, CONFIRM, PROGRESS
    };

    public interface ScrumChatterDialogButtonListener {
        void onOkClicked(int actionId, Bundle extras);
    }

    public interface ScrumChatterDialogItemListener {
        void onItemSelected(int actionId, CharSequence[] choices, int which);
    }

    /**
     * @return a dialog with the given title and message, and just one OK button.
     */
    public static ScrumChatterDialogFragment showInfoDialog(FragmentActivity activity, int titleId, int messageId) {
        Log.v(TAG, "showInfoDialog");
        Bundle arguments = new Bundle(3);
        arguments.putString(EXTRA_TITLE, activity.getString(titleId));
        arguments.putString(EXTRA_MESSAGE, activity.getString(messageId));
        return showDialog(activity, arguments, DialogType.INFO, null);
    }

    public static ScrumChatterDialogFragment showConfirmDialog(FragmentActivity activity, String title, String message, int actionId, Bundle extras) {
        Log.v(TAG, "showConfirmDialog: title = " + title + ", message = " + message + ", actionId = " + actionId + ", extras = " + extras);
        Bundle arguments = new Bundle(4);
        arguments.putString(EXTRA_TITLE, title);
        arguments.putString(EXTRA_MESSAGE, message);
        arguments.putInt(EXTRA_ACTION_ID, actionId);
        if (extras != null) arguments.putBundle(EXTRA_EXTRAS, extras);
        return showDialog(activity, arguments, DialogType.CONFIRM, String.valueOf(actionId));
    }

    public static ScrumChatterDialogFragment showChoiceDialog(FragmentActivity activity, String title, CharSequence[] items, int selectedItem, int actionId) {
        Log.v(TAG, "showChoiceDialog: title = " + title + ", actionId = " + actionId + ", items =" + Arrays.toString(items) + ", selectedItem = "
                + selectedItem);
        Bundle arguments = new Bundle(5);
        arguments.putString(EXTRA_TITLE, title);
        arguments.putInt(EXTRA_ACTION_ID, actionId);
        arguments.putCharSequenceArray(EXTRA_CHOICES, items);
        arguments.putInt(EXTRA_SELECTED_ITEM, selectedItem);
        return showDialog(activity, arguments, DialogType.CHOICE, String.valueOf(actionId));
    }

    public static ScrumChatterDialogFragment showProgressDialog(FragmentActivity activity, String message, String tag) {
        Log.v(TAG, "showProgressDialog: message = " + message);
        Bundle arguments = new Bundle(2);
        arguments.putString(EXTRA_MESSAGE, message);
        arguments.putSerializable(EXTRA_DIALOG_TYPE, DialogType.PROGRESS);
        ScrumChatterDialogFragment result = new ScrumChatterDialogFragment();
        result.setArguments(arguments);
        result.show(activity.getSupportFragmentManager(), tag);
        return result;
    }

    private static ScrumChatterDialogFragment showDialog(FragmentActivity activity, Bundle arguments, DialogType dialogType, String tagAppend) {
        ScrumChatterDialogFragment result = new ScrumChatterDialogFragment();
        arguments.putSerializable(EXTRA_DIALOG_TYPE, dialogType);
        result.setArguments(arguments);
        result.show(activity.getSupportFragmentManager(), ScrumChatterDialogFragment.class.getSimpleName() + dialogType + tagAppend);
        return result;
    }

    public ScrumChatterDialogFragment() {
        super();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.v(TAG, "onCreateDialog: savedInstanceState = " + savedInstanceState);
        DialogType dialogType = (DialogType) getArguments().getSerializable(EXTRA_DIALOG_TYPE);
        Log.v(TAG, "onCreateDialog: dialogType  " + dialogType);
        switch (dialogType) {
            case INFO:
                return createInfoDialog();
            case CONFIRM:
                return createConfirmDialog();
            case CHOICE:
                return createChoiceDialog();
            case PROGRESS:
                return createProgressDialog();
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

    private Dialog createProgressDialog() {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        Bundle arguments = getArguments();
        dialog.setMessage(arguments.getString(EXTRA_MESSAGE));
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        styleDialog(dialog);
        return dialog;
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
