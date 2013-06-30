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

import java.lang.reflect.Field;
import java.util.Arrays;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.NinePatch;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;

/**
 * Boy is customizing alert dialogs a pain in the booty. Tried the android-styled-dialogs library but it didn't fit the needs of this app: no support for alert
 * dialogs with EditTexts, and not a clean way to manage clicks on the dialog buttons. Started out trying to copy the resources used for dialogs, one-by-one,
 * from the core android framework, but that was more pain than the approach I decided to take in this class.
 * 
 * Only 3.x+ devices will have customized dialogs.
 */
public class ScrumChatterDialog {

    private static final String TAG = Constants.TAG + "/" + ScrumChatterDialog.class.getSimpleName();
    private static int sHoloBlueLightColorId = -1;
    private static int sHoloBlueDarkColorId = -1;
    private static int sHoloPurpleColorId = -1;
    private static Field sNinePatchSourceField = null;
    private static Field sNinePatchField = null;

    public static AlertDialog showChoiceDialog(Context context, int titleId, int choicesArrayId, DialogInterface.OnClickListener itemListener) {
        return showDialog(context, context.getString(titleId), null, null, context.getResources().getStringArray(choicesArrayId), itemListener);
    }

    public static AlertDialog showDialog(Context context, String title, String message, DialogInterface.OnClickListener positiveListener) {
        return showDialog(context, title, message, null, null, positiveListener);
    }

    public static AlertDialog showDialog(Context context, int titleId, int messageId, DialogInterface.OnClickListener positiveListener) {
        return showDialog(context, titleId, messageId, null, positiveListener);
    }

    public static AlertDialog showDialog(Context context, int titleId, int messageId, View customView, DialogInterface.OnClickListener positiveListener) {
        String title = titleId > 0 ? context.getString(titleId) : null;
        String message = messageId > 0 ? context.getString(messageId) : null;
        return showDialog(context, title, message, customView, null, positiveListener);
    }

    /**
     * @param title Optional. The title of the dialog.
     * @param message Optional. The message of the dialog.
     * @param customView Optional. A custom view for the dialog.
     * @param items Optional. A list of items. If this is provided, the dialog will have no buttons. If the listener is provided, the listener will be notified
     *            when an item is selected.
     * @param listener Optional. If a list of items is provided, the listener will be notified when the user selects an item. Otherwise the listener will be
     *            notified when the user taps on the positive button.
     * @return
     */
    public static AlertDialog showDialog(Context context, String title, String message, View customView, CharSequence[] items,
            DialogInterface.OnClickListener listener) {
        Log.v(TAG, "showDialog: title = " + title + ", message = " + message + ", customView = " + customView + ", items = " + Arrays.toString(items)
                + ", listener = " + listener);

        // For 3.x+, use our custom theme for the dialog. This will impact the title text color and the EditText color.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) context = new ContextThemeWrapper(context, R.style.scrumDialogStyle);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title).setMessage(message);

        // If items are provided, set the items.  Otherwise add a positive and negative button.
        if (items != null && items.length > 0) {
            builder.setItems(items, listener);
        } else {
            builder.setNegativeButton(android.R.string.cancel, null).setPositiveButton(android.R.string.ok, listener);
        }

        // Add a custom view if provided.
        if (customView != null) builder.setView(customView);

        // Show the dialog (we have to do this before we can modify its views).
        AlertDialog dialog = builder.create();
        dialog.show();

        // For 3.x+, update the dialog elements which couldn't be updated cleanly with the theme:
        // The buttons, list items, and horizontal divider.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ListView listView = dialog.getListView();
            if (listView != null) listView.setSelector(R.drawable.selector);
            uglyHackReplaceButtonBackground(dialog, android.R.id.button1);
            uglyHackReplaceButtonBackground(dialog, android.R.id.button2);
            uglyHackReplaceButtonBackground(dialog, android.R.id.button3);
            uglyHackReplaceBlueHoloBackground(context, (ViewGroup) dialog.getWindow().getDecorView());
        }
        return dialog;
    }

    @TargetApi(11)
    private static void uglyHackReplaceButtonBackground(Dialog dialog, int buttonId) {
        View button = dialog.findViewById(buttonId);
        if (button != null) button.setBackgroundResource(R.drawable.selector);
    }

    /**
     * Iterate through the whole view tree and replace the holo blue element(s) with our holo color.
     * For 3.x, the horizontal divider is a nine patch image "divider_strong_holo".
     * For 4.x, the horizontal divider is a holo color.
     */
    @TargetApi(11)
    private static void uglyHackReplaceBlueHoloBackground(Context context, ViewGroup viewGroup) {
        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = viewGroup.getChildAt(i);
            if (child instanceof ViewGroup) {
                uglyHackReplaceBlueHoloBackground(context, (ViewGroup) child);
            }
            // 3.x: replace the nine patch
            else if (child instanceof ImageView) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    ImageView imageView = (ImageView) child;
                    Drawable drawable = imageView.getDrawable();
                    if (drawable instanceof NinePatchDrawable) {
                        if (isHoloBlueNinePatch((NinePatchDrawable) drawable)) {
                            imageView.setImageResource(R.drawable.divider_strong_scrum_chatter);
                        }
                    }
                }
            }
            // 4.x: replace the color
            else {
                Drawable drawable = child.getBackground();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && drawable instanceof ColorDrawable) {
                    if (isHoloBlueColor(context, (ColorDrawable) drawable)) child.setBackgroundColor(sHoloPurpleColorId);
                }
            }
        }
    }

    /**
     * @return true if the given nine patch is the divider_strong_holo nine patch.
     */
    @TargetApi(11)
    private static boolean isHoloBlueNinePatch(NinePatchDrawable n) {
        // horrible, horrible...
        String imageSource = null;
        lazyInitCrazyReflectionCrap();
        try {
            NinePatch ninePatch = (NinePatch) sNinePatchField.get(n);
            imageSource = (String) sNinePatchSourceField.get(ninePatch);
        } catch (IllegalAccessException e) {
            Log.v(TAG, "Oops: " + e.getMessage(), e);
        }
        return imageSource != null && imageSource.contains("divider_strong_holo");
    }

    /**
     * @return true if the given color is holo blue light or dark
     */
    @TargetApi(14)
    private static boolean isHoloBlueColor(Context context, ColorDrawable c) {
        lazyInitHoloColors(context);
        int viewColorId = c.getColor();
        return (viewColorId == sHoloBlueLightColorId || viewColorId == sHoloBlueDarkColorId);
    }

    @TargetApi(14)
    private static void lazyInitHoloColors(Context context) {
        if (sHoloBlueLightColorId == -1) {
            sHoloBlueLightColorId = context.getResources().getColor(android.R.color.holo_blue_light);
            sHoloBlueDarkColorId = context.getResources().getColor(android.R.color.holo_blue_dark);
            sHoloPurpleColorId = context.getResources().getColor(R.color.scrum_chatter_holo_purple);
        }
    }

    private static void lazyInitCrazyReflectionCrap() {
        try {
            if (sNinePatchSourceField == null) {
                sNinePatchField = NinePatchDrawable.class.getDeclaredField("mNinePatch");
                sNinePatchField.setAccessible(true);
                sNinePatchSourceField = NinePatch.class.getDeclaredField("mSrcName");
                sNinePatchSourceField.setAccessible(true);
            }
        } catch (NoSuchFieldException e) {
            Log.v(TAG, "An exception is what we deserve doing code like this: " + e.getMessage(), e);
        }

    }
}
