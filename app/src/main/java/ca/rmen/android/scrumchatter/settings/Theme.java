/**
 * Copyright 2016 Carmen Alvarez
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
package ca.rmen.android.scrumchatter.settings;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import ca.rmen.android.scrumchatter.Constants;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class Theme {
    private static final String TAG = Constants.TAG + Theme.class.getSimpleName();

    static final String PREF_THEME = "PREF_THEME";
    private static final String THEME_DARK = "Dark";
    private static final String THEME_LIGHT = "Light";

    /**
     * If the app isn't using the theme in the shared preferences, this
     * will restart the activity and set the global flag to use the right theme.
     * Logically, this might make more sense in an application class.
     */
    public static void checkTheme(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return;
        }

        String theme = PreferenceManager.getDefaultSharedPreferences(activity).getString(PREF_THEME, THEME_LIGHT);
        if (THEME_DARK.equals(theme)) {
            if (AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_YES) {
                Log.v(TAG, "Restarting in dark mode");
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                activity.recreate();
            }
        } else {
            if (AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_NO) {
                Log.v(TAG, "Restarting in light mode");
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                activity.recreate();
            }
        }
    }
}
