/*
 * Copyright 2017 Carmen Alvarez
 * <p/>
 * This file is part of Scrum Chatter.
 * <p/>
 * Scrum Chatter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * Scrum Chatter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with Scrum Chatter. If not, see <http://www.gnu.org/licenses/>.
 */
package ca.rmen.android.scrumchatter.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import java.util.Map;

import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.util.Log;
import ca.rmen.android.scrumchatter.util.StrictModeUtil;

public final class Prefs {
    enum Theme {
        Dark,
        Light,
        Auto,
        System
    }

    static final String PREF_THEME = "PREF_THEME";
    private static final String TAG = Constants.TAG + "/" + Prefs.class.getSimpleName();

    private static Prefs INSTANCE;
    private final SharedPreferences mPrefs;

    private Prefs(Context context) {
        // We try to behave by not reading from the disk on the ui thread.  But one exception
        // we can allow is to read the shared prefs one time when this singleton is initialized
        boolean cheatIgnoreStrictModeViolations = Looper.getMainLooper().getThread() == Thread.currentThread();
        if (cheatIgnoreStrictModeViolations) StrictModeUtil.disable();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        Map<String, ?> allPrefs = mPrefs.getAll();
        Log.v(TAG, "Read prefs " + allPrefs);
        if (cheatIgnoreStrictModeViolations) StrictModeUtil.enable();
    }

    public static synchronized Prefs getInstance(Context context) {
        if (INSTANCE == null) INSTANCE = new Prefs(context);
        return INSTANCE;
    }

    public void register(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        mPrefs.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregister(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        mPrefs.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public int getTeamId() {
        return mPrefs.getInt(Constants.PREF_TEAM_ID, Constants.DEFAULT_TEAM_ID);
    }

    public void setTeamId(int teamId) {
        mPrefs.edit().putInt(Constants.PREF_TEAM_ID, teamId).apply();
    }

    @NonNull
    public Theme getTheme() {
        String themeName = mPrefs.getString(PREF_THEME, Theme.Light.name());
        for (Theme theme : Theme.values()) {
            if (theme.name().equals(themeName)) return theme;
        }
        return Theme.Light;
    }
}
