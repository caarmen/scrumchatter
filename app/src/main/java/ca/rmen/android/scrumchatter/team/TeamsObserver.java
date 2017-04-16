/*
 * Copyright 2016 Carmen Alvarez
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
package ca.rmen.android.scrumchatter.team;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;

import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.provider.TeamColumns;
import ca.rmen.android.scrumchatter.settings.Prefs;
import ca.rmen.android.scrumchatter.util.Log;


/**
 * Notifies listeners of changes to teams: teams added, removed, renamed, or the current team selection changed.
 */
public class TeamsObserver {
    private static final String TAG = Constants.TAG + "/" + TeamsObserver.class.getSimpleName();

    public interface OnTeamsChangedListener {
        void onTeamsChanged();
    }

    private final Context mContext;
    private final OnTeamsChangedListener mListener;

    public TeamsObserver(Context context, OnTeamsChangedListener listener) {
        mContext = context;
        mListener = listener;
    }

    public void register() {
        Prefs.getInstance(mContext).register(mSharedPrefsListener);
        mContext.getContentResolver().registerContentObserver(TeamColumns.CONTENT_URI, true, mContentObserver);

    }

    public void destroy() {
        Prefs.getInstance(mContext).unregister(mSharedPrefsListener);
        mContext.getContentResolver().unregisterContentObserver(mContentObserver);

    }

    private final SharedPreferences.OnSharedPreferenceChangeListener mSharedPrefsListener = new SharedPreferences.OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (Constants.PREF_TEAM_ID.equals(key)) {
                Log.v(TAG, "Team changed");
                mListener.onTeamsChanged();
            }
        }
    };

    private final ContentObserver mContentObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean selfChange) {
            mListener.onTeamsChanged();
        }
    };

}
