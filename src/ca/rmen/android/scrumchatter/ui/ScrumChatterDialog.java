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

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import ca.rmen.android.scrumchatter.R;

/**
 * Boy is customizing alert dialogs a pain in the booty. Tried the android-styled-dialogs library but it didn't fit the needs of this app: no support for alert
 * dialogs with EditTexts, and not a clean way to manage clicks on the dialog buttons. Started out trying to copy the resources used for dialogs, one-by-one,
 * from the core android framework, but that was more pain than the approach I decided to take in this class.
 * 
 * Only 3.x+ devices will have customized dialogs.
 */
public class ScrumChatterDialog {

    public static AlertDialog showChoiceDialog(Context context, int titleId, int choicesArrayId, DialogInterface.OnClickListener itemListener) {
        return showDialog(context, context.getString(titleId), null, null, context.getResources().getStringArray(choicesArrayId), itemListener);

    }

    public static AlertDialog showDialog(Context context, String title, String message, DialogInterface.OnClickListener positiveListener) {
        return showDialog(context, title, message, null, null, positiveListener);

    }

    public static AlertDialog showDialog(Context context, int titleId, int messageId, DialogInterface.OnClickListener positiveListener) {
        return showDialog(context, titleId, messageId, positiveListener);
    }

    public static AlertDialog showDialog(Context context, int titleId, int messageId, View customView, DialogInterface.OnClickListener positiveListener) {
        String title = titleId > 0 ? context.getString(titleId) : null;
        String message = messageId > 0 ? context.getString(messageId) : null;
        return showDialog(context, title, message, customView, null, positiveListener);
    }

    public static AlertDialog showDialog(Context context, String title, String message, View customView, CharSequence[] items,
            DialogInterface.OnClickListener listener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) context = new ContextThemeWrapper(context, R.style.scrumDialogStyle);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title).setMessage(message);
        if (items != null && items.length > 0) {
            builder.setItems(items, listener);
        } else {
            builder.setNegativeButton(android.R.string.cancel, null).setPositiveButton(android.R.string.ok, listener);
        }
        if (customView != null) builder.setView(customView);
        AlertDialog dialog = builder.create();
        dialog.show();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            int color = context.getResources().getColor(R.color.scrum_chatter_holo_purple);
            ListView listView = dialog.getListView();
            if (listView != null) listView.setSelector(R.drawable.selector);
            uglyHackReplaceButtonBackground(dialog, android.R.id.button1);
            uglyHackReplaceButtonBackground(dialog, android.R.id.button2);
            uglyHackReplaceButtonBackground(dialog, android.R.id.button3);
            uglyHackReplaceBlueHoloBackground(context, (ViewGroup) dialog.getWindow().getDecorView(), color);
        }
        return dialog;
    }

    @TargetApi(11)
    private static void uglyHackReplaceButtonBackground(Dialog dialog, int buttonId) {
        View button = dialog.findViewById(buttonId);
        if (button != null) button.setBackgroundResource(R.drawable.selector);
    }

    @TargetApi(11)
    private static void uglyHackReplaceBlueHoloBackground(Context context, ViewGroup viewGroup, int replacementColor) {
        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = viewGroup.getChildAt(i);
            if (child instanceof ViewGroup) {
                uglyHackReplaceBlueHoloBackground(context, (ViewGroup) child, replacementColor);
            } else {
                Drawable background = child.getBackground();
                if (background instanceof ColorDrawable) {
                    ColorDrawable c = (ColorDrawable) background;
                    //if (c.getColor() == android.R.color.holo_blue_light) {
                    child.setBackgroundColor(replacementColor);
                    //}
                }
            }
        }
    }
}
