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
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;

/**
 * Boy is customizing alert dialogs a pain in the booty. Tried the android-styled-dialogs library but it didn't fit the needs of this app: no support for alert
 * dialogs with EditTexts, and not a clean way to manage clicks on the dialog buttons. Started out trying to copy the resources used for dialogs, one-by-one,
 * from the core android framework, but that was more pain than the approach I decided to take in this class.
 */
public class ScrumChatterDialogFragment extends DialogFragment {

    private static final String TAG = Constants.TAG + "/" + ScrumChatterDialogFragment.class.getSimpleName();
    private static final String EXTRA_TITLE_ID = "title_id";
    private static final String EXTRA_MESSAGE_ID = "message_id";
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

    public interface ScrumChatterDialogButtonListener {
        void onOkClicked();
    }

    public interface ScrumChatterDialogItemListener {
        void onItemSelected(int which);
    }


    /**
     * @return a dialog with the given title and message, and just one OK button.
     */
    public static ScrumChatterDialogFragment showInfoDialog(FragmentActivity activity, int titleId, int messageId) {
        Bundle arguments = new Bundle(2);
        arguments.putInt(EXTRA_TITLE_ID, titleId);
        arguments.putInt(EXTRA_MESSAGE_ID, messageId);
        ScrumChatterDialogFragment result = new ScrumChatterDialogFragment();
        result.setArguments(arguments);
        result.show(activity.getSupportFragmentManager(), TAG);
        return result;
    }


    public ScrumChatterDialogFragment() {}


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.v(TAG, "onCreateDialog: savedInstanceState = " + savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        Bundle arguments = getArguments();
        builder.setTitle(arguments.getInt(EXTRA_TITLE_ID)).setMessage(arguments.getInt(EXTRA_MESSAGE_ID)).setNeutralButton(android.R.string.ok, null);
        // Show the dialog (we have to do this before we can modify its views).
        final AlertDialog dialog = builder.create();
        dialog.getContext().setTheme(R.style.dialogStyle);
        dialog.setOnShowListener(new OnShowListener() {

            @Override
            public void onShow(DialogInterface dialogInterface) {
                uglyHackReplaceBlueHoloBackground(getActivity(), (ViewGroup) dialog.getWindow().getDecorView());

            }
        });
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
