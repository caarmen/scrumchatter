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
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import ca.rmen.android.scrumchatter.util.Log;
import android.widget.ProgressBar;

import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;

/**
 * Implements a dialog fragment with a ProgressDialog with a message.
 */
public class ProgressDialogFragment extends DialogFragment {

    private static final String TAG = Constants.TAG + "/" + ProgressDialogFragment.class.getSimpleName();

    public ProgressDialogFragment() {
        super();
    }

    /**
     * @return an indeterminate, non-cancelable, ProgressDialog with a message.
     */
    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.v(TAG, "onCreateDialog: savedInstanceState = " + savedInstanceState);
        ProgressDialog dialog = new ProgressDialog(getActivity());
        Bundle arguments = getArguments();
        dialog.setMessage(arguments.getString(DialogFragmentFactory.EXTRA_MESSAGE));
        dialog.setIndeterminate(true);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface shownDialog) {
                ProgressBar progressBar = (ProgressBar) ((ProgressDialog)shownDialog).findViewById(android.R.id.progress);
                if (progressBar != null) {
                    Drawable drawable = progressBar.getIndeterminateDrawable();
                    if (drawable != null) {
                        drawable.setColorFilter(ContextCompat.getColor(getActivity(), R.color.scrum_chatter_accent_color),
                                android.graphics.PorterDuff.Mode.SRC_IN);
                    }
                }
            }
        });
        dialog.setCancelable(false);
        return dialog;
    }
}
