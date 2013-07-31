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
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;

/**
 * Boy is customizing alert dialogs a pain in the booty. Tried the android-styled-dialogs library but it didn't fit the needs of this app: no support for alert
 * dialogs with EditTexts, and not a clean way to manage clicks on the dialog buttons. Started out trying to copy the resources used for dialogs, one-by-one,
 * from the core android framework, but that was more pain than the approach I decided to take in this class.
 */
public class ScrumChatterDialog {

    private static final String TAG = Constants.TAG + "/" + ScrumChatterDialog.class.getSimpleName();

    public static AlertDialog showDialog(Context context, String title, String message, DialogInterface.OnClickListener positiveListener) {
        return showDialog(context, title, message, null, null, -1, positiveListener);
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
