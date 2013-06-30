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
import ca.rmen.android.scrumchatter.R;

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
