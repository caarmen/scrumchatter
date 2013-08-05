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
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import ca.rmen.android.scrumchatter.Constants;

/**
 * Implements different types of dialog fragments (information, choice, confirmation, progress).
 */
public class ScrumChatterDialogFragment extends DialogFragment {

    private static final String TAG = Constants.TAG + "/" + ScrumChatterDialogFragment.class.getSimpleName();
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_MESSAGE = "message";
    static final String EXTRA_DIALOG_TYPE = "dialog_type";
    static final String EXTRA_ACTION_ID = "action_id";
    static final String EXTRA_CHOICES = "choices";
    static final String EXTRA_SELECTED_ITEM = "selected_item";
    static final String EXTRA_EXTRAS = "extras";

    static enum DialogType {
        INFO, CHOICE, CONFIRM, PROGRESS
    };

    /**
     * An activity which contains a confirmation dialog fragment should implement this interface.
     */
    public interface ScrumChatterDialogButtonListener {
        void onOkClicked(int actionId, Bundle extras);
    }

    /**
     * An activity which contains a choice dialog fragment should implement this interface.
     */
    public interface ScrumChatterDialogItemListener {
        void onItemSelected(int actionId, CharSequence[] choices, int which);
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

    /**
     * @return an AlertDialog with a title, message, and single button to dismiss the dialog.
     */
    private Dialog createInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        Bundle arguments = getArguments();
        builder.setTitle(arguments.getString(EXTRA_TITLE)).setMessage(arguments.getString(EXTRA_MESSAGE)).setNeutralButton(android.R.string.ok, null);
        final AlertDialog dialog = builder.create();
        DialogStyleHacks.styleDialog(getActivity(), dialog);
        return dialog;
    }

    /**
     * @return an AlertDialog with a title, message, ok, and cancel buttons.
     */
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
        DialogStyleHacks.styleDialog(getActivity(), dialog);
        return dialog;

    }

    /**
     * @return an AlertDialog with a list of items, one of them possibly pre-selected.
     */
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
        // If one item is to be pre-selected, use the single choice items layout.
        if (selectedItem >= 0) builder.setSingleChoiceItems(choices, selectedItem, listener);
        // If no particular item is to be pre-selected, use the default list item layout.
        else
            builder.setItems(choices, listener);

        final AlertDialog dialog = builder.create();
        DialogStyleHacks.styleDialog(getActivity(), dialog);
        return dialog;

    }

    /**
     * @return an indeterminate, non-cancelable, ProgressDialog with a message.
     */
    private Dialog createProgressDialog() {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        Bundle arguments = getArguments();
        dialog.setMessage(arguments.getString(EXTRA_MESSAGE));
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        DialogStyleHacks.styleDialog(getActivity(), dialog);
        return dialog;
    }

}
