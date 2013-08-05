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
package ca.rmen.android.scrumchatter.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import ca.rmen.android.scrumchatter.Constants;

/**
 * A dialog fragment with a list of choices.
 */
public class ScrumChatterChoiceDialogFragment extends DialogFragment { // NO_UCD (use default)

    private static final String TAG = Constants.TAG + "/" + ScrumChatterChoiceDialogFragment.class.getSimpleName();

    /**
     * An activity which contains a choice dialog fragment should implement this interface.
     */
    public interface ScrumChatterDialogItemListener {
        void onItemSelected(int actionId, CharSequence[] choices, int which);
    }

    public ScrumChatterChoiceDialogFragment() {
        super();
    }

    /**
     * @return an AlertDialog with a list of items, one of them possibly pre-selected.
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.v(TAG, "onCreateDialog: savedInstanceState = " + savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        Bundle arguments = getArguments();
        builder.setTitle(arguments.getString(ScrumChatterDialogFragmentFactory.EXTRA_TITLE));
        final int actionId = arguments.getInt(ScrumChatterDialogFragmentFactory.EXTRA_ACTION_ID);
        int selectedItem = arguments.getInt(ScrumChatterDialogFragmentFactory.EXTRA_SELECTED_ITEM);
        final CharSequence[] choices = arguments.getCharSequenceArray(ScrumChatterDialogFragmentFactory.EXTRA_CHOICES);
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
}
