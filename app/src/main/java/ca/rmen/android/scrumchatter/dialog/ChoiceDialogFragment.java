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
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import ca.rmen.android.scrumchatter.util.Log;
import ca.rmen.android.scrumchatter.Constants;

/**
 * A dialog fragment with a list of choices.
 */
public class ChoiceDialogFragment extends DialogFragment { // NO_UCD (use default)

    private static final String TAG = Constants.TAG + "/" + ChoiceDialogFragment.class.getSimpleName();

    /**
     * An activity which contains a choice dialog fragment should implement this interface.
     */
    public interface DialogItemListener {
        void onItemSelected(int actionId, CharSequence[] choices, int which);
    }

    public ChoiceDialogFragment() {
        super();
    }

    /**
     * @return an AlertDialog with a list of items, one of them possibly pre-selected.
     */
    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.v(TAG, "onCreateDialog: savedInstanceState = " + savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        Bundle arguments = getArguments();
        builder.setTitle(arguments.getString(DialogFragmentFactory.EXTRA_TITLE));
        final int actionId = arguments.getInt(DialogFragmentFactory.EXTRA_ACTION_ID);
        int selectedItem = arguments.getInt(DialogFragmentFactory.EXTRA_SELECTED_ITEM);
        final CharSequence[] choices = arguments.getCharSequenceArray(DialogFragmentFactory.EXTRA_CHOICES);
        OnClickListener listener = null;
        if (getActivity() instanceof DialogItemListener) {
            listener = new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    FragmentActivity activity = getActivity();
                    if (activity == null) Log.w(TAG, "User clicked on dialog after it was detached from activity. Monkey?");
                    else
                        ((DialogItemListener) activity).onItemSelected(actionId, choices, which);
                }
            };
        }
        // If one item is to be pre-selected, use the single choice items layout.
        if (selectedItem >= 0) builder.setSingleChoiceItems(choices, selectedItem, listener);
        // If no particular item is to be pre-selected, use the default list item layout.
        else
            builder.setItems(choices, listener);

        return builder.create();
    }
}
