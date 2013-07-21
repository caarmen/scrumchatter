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
import android.content.DialogInterface.OnShowListener;
import android.graphics.NinePatch;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;

/**
 * Boy is customizing alert dialogs a pain in the booty. Tried the android-styled-dialogs library but it didn't fit the needs of this app: no support for alert
 * dialogs with EditTexts, and not a clean way to manage clicks on the dialog buttons. Started out trying to copy the resources used for dialogs, one-by-one,
 * from the core android framework, but that was more pain than the approach I decided to take in this class.
 */
public class ScrumChatterDialog {

    private static final String TAG = Constants.TAG + "/" + ScrumChatterDialog.class.getSimpleName();
    private static int sHoloBlueLightColorId = -1;
    private static int sHoloBlueDarkColorId = -1;
    private static int sHoloPurpleColorId = -1;
    private static Field sNinePatchSourceField = null;
    private static Field sNinePatchField = null;

    public interface InputValidator {
        /**
         * @param input the text entered by the user.
         * @return an error string if the input has a problem, null if the input is valid.
         */
        String getError(CharSequence input);
    };

    /**
     * @param input an EditText for user input
     * @param validator will be called with each text event on the edit text, to validate the user's input.
     */
    public static AlertDialog showEditTextDialog(Context context, int titleId, int messageId, final EditText input,
            DialogInterface.OnClickListener positiveListener, final InputValidator validator) {

        final AlertDialog dialog = showDialog(context, titleId, messageId, input, positiveListener);
        input.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateMemberName();
            }

            private void validateMemberName() {
                // Start off with everything a-ok.
                input.setError(null);
                final Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                okButton.setEnabled(true);

                // Search for an error in background thread, update the dialog in the UI thread.
                AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {

                    /**
                     * @return an error String if the input is invalid.
                     */
                    @Override
                    protected String doInBackground(Void... params) {
                        return validator.getError(input.getText().toString().trim());
                    }

                    @Override
                    protected void onPostExecute(String error) {
                        // If the input is invalid, highlight the error
                        // and disable the OK button.
                        if (!TextUtils.isEmpty(error)) {
                            input.setError(error);
                            okButton.setEnabled(false);
                        }
                    }
                };
                task.execute();
            }
        });
        return dialog;
    }

    public static AlertDialog showChoiceDialog(Context context, int titleId, int choicesArrayId, int selectedItem, DialogInterface.OnClickListener itemListener) {
        return showDialog(context, context.getString(titleId), null, null, context.getResources().getStringArray(choicesArrayId), selectedItem, itemListener);
    }

    public static AlertDialog showChoiceDialog(Context context, int titleId, CharSequence[] choices, int selectedItem,
            DialogInterface.OnClickListener itemListener) {
        return showDialog(context, context.getString(titleId), null, null, choices, selectedItem, itemListener);
    }

    public static AlertDialog showDialog(Context context, String title, String message, DialogInterface.OnClickListener positiveListener) {
        return showDialog(context, title, message, null, null, -1, positiveListener);
    }

    public static AlertDialog showDialog(Context context, int titleId, int messageId, DialogInterface.OnClickListener positiveListener) {
        return showDialog(context, titleId, messageId, null, positiveListener);
    }

    private static AlertDialog showDialog(Context context, int titleId, int messageId, View customView, DialogInterface.OnClickListener positiveListener) {
        String title = titleId > 0 ? context.getString(titleId) : null;
        String message = messageId > 0 ? context.getString(messageId) : null;
        return showDialog(context, title, message, customView, null, -1, positiveListener);
    }

    /**
     * @return a dialog with the given title and message, and just one OK button.
     */
    public static AlertDialog showInfoDialog(Context context, int titleId, int messageId) {
        context = new ContextThemeWrapper(context, R.style.dialogStyle);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(titleId).setMessage(messageId).setNeutralButton(android.R.string.ok, null);
        // Show the dialog (we have to do this before we can modify its views).
        AlertDialog dialog = builder.create();
        dialog.getContext().setTheme(R.style.dialogStyle);
        dialog.show();

        uglyHackReplaceBlueHoloBackground(context, (ViewGroup) dialog.getWindow().getDecorView());
        return dialog;
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

        // For 3.x+, update the dialog elements which couldn't be updated cleanly with the theme:
        // The list items.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ListView listView = dialog.getListView();
            if (listView != null) listView.setSelector(R.drawable.selector);
        }
        OnShowListener showListener = new OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {
                uglyHackReplaceBlueHoloBackground(contextWrapper, (ViewGroup) ((Dialog) dialog).getWindow().getDecorView());
            }
        };
        dialog.setOnShowListener(showListener);
        dialog.show();
        return dialog;
    }

    /**
     * Iterate through the whole view tree and replace the holo blue element(s) with our holo color.
     * For 2.x, the horizontal divider is a nine patch image "divider_horizontal_dark".
     * For 3.x, the horizontal divider is a nine patch image "divider_strong_holo".
     * For 4.x, the horizontal divider is a holo color.
     */
    private static void uglyHackReplaceBlueHoloBackground(Context context, ViewGroup viewGroup) {
        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = viewGroup.getChildAt(i);
            if (child instanceof ViewGroup) {
                uglyHackReplaceBlueHoloBackground(context, (ViewGroup) child);
            }
            // 2.x and 3.x: replace the nine patch
            else if (child instanceof ImageView) {
                ImageView imageView = (ImageView) child;
                Drawable drawable = imageView.getDrawable();
                if (drawable instanceof NinePatchDrawable) {
                    if (isHoloBlueNinePatch((NinePatchDrawable) drawable)) {
                        imageView.setImageResource(R.drawable.divider_strong_scrum_chatter);
                        // On 2.x, in a dialog with a list, the divider is hidden.  Let's show it.
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) imageView.setVisibility(View.VISIBLE);
                    }
                }
            }
            // 2.x: replace the radio button
            else if (child instanceof CheckedTextView) {
                ((CheckedTextView) child).setCheckMarkDrawable(R.drawable.btn_radio_holo_light);
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
        return imageSource != null && (imageSource.contains("divider_strong_holo") || imageSource.contains("divider_horizontal_dark"));
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
