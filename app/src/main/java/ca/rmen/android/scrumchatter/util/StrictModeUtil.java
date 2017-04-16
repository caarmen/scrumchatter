/*
 * Copyright 2017 Carmen Alvarez
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
package ca.rmen.android.scrumchatter.util;

import android.app.ActivityManager;
import android.os.StrictMode;

import ca.rmen.android.scrumchatter.Constants;

public class StrictModeUtil {
    private static final String TAG = Constants.TAG + "/" + StrictModeUtil.class.getName();
    private StrictModeUtil() {
        // prevent instantiation
    }

    public static void enable() {
        Log.v(TAG, "enable");
        // Use strict mode for monkey tests.  We can't enable strict mode for normal use
        // because, when sharing (exporting), the mail app may read the attachment in
        // the main thread.
        if (ActivityManager.isUserAMonkey()) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().penaltyLog().penaltyDeath().build());
        }
    }

    public static void disable() {
        Log.v(TAG, "disable");
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
    }

}
