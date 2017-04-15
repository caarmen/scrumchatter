/*
 * Copyright 2013, 2017 Carmen Alvarez
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
package ca.rmen.android.scrumchatter.meeting.detail;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.WorkerThread;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v7.preference.PreferenceManager;

import ca.rmen.android.scrumchatter.util.Log;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.meeting.Meetings;
import ca.rmen.android.scrumchatter.provider.MeetingColumns;
import ca.rmen.android.scrumchatter.provider.MeetingCursorWrapper;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Adapter for the list of meetings
 */
class MeetingPagerAdapter extends FragmentStatePagerAdapter {
    private static final String TAG = Constants.TAG + "/" + MeetingPagerAdapter.class.getSimpleName();

    private MeetingCursorWrapper mCursor;
    private final Context mContext;
    private final MeetingObserver mMeetingObserver;
    private final int mTeamId;

    @WorkerThread
    MeetingPagerAdapter(FragmentActivity activity) {
        super(activity.getSupportFragmentManager());
        Log.v(TAG, "Constructor");
        mContext = activity;
        mTeamId = PreferenceManager.getDefaultSharedPreferences(activity).getInt(Constants.PREF_TEAM_ID, Constants.DEFAULT_TEAM_ID);
        // Closing the cursor wrapper also closes the cursor
        @SuppressLint("Recycle")
        Cursor cursor = activity.getContentResolver().query(MeetingColumns.CONTENT_URI, null, MeetingColumns.TEAM_ID + "=?",
                new String[] { String.valueOf(mTeamId) }, MeetingColumns.MEETING_DATE + " DESC");
        mCursor = new MeetingCursorWrapper(cursor);
        mCursor.getCount();
        mMeetingObserver = new MeetingObserver(new Handler(Looper.getMainLooper()));
        mCursor.registerContentObserver(mMeetingObserver);
    }


    @Override
    public Fragment getItem(int position) {
        Log.v(TAG, "getItem at position " + position);
        MeetingFragment fragment = new MeetingFragment();
        Bundle args = new Bundle(1);
        mCursor.moveToPosition(position);
        args.putLong(Meetings.EXTRA_MEETING_ID, mCursor.getId());
        args.putSerializable(Meetings.EXTRA_MEETING_STATE, mCursor.getState());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getCount() {
        return mCursor.getCount();
    }

    int getPositionForMeetingId(long meetingId) {
        Log.v(TAG, "getPositionForMeetingId " + meetingId);

        if (mCursor.moveToFirst()) {
            do {
                if (mCursor.getId() == meetingId) return mCursor.getPosition();
            } while (mCursor.moveToNext());
        }
        return -1;
    }

    Meeting getMeetingAt(int position) {
        mCursor.moveToPosition(position);
        return Meeting.read(mContext, mCursor);
    }

    void destroy() {
        Log.v(TAG, "destroy");
        mCursor.unregisterContentObserver(mMeetingObserver);
        mCursor.close();
    }

    private class MeetingObserver extends ContentObserver {

        private final String TAG = MeetingPagerAdapter.TAG + "/" + MeetingObserver.class.getSimpleName();

        MeetingObserver(Handler handler) {
            super(handler);
            Log.v(TAG, "Constructor");
        }

        /**
         * The Meeting table changed. We need to update our cursor and notify about the change.
         */
        @Override
        public void onChange(boolean selfChange) {
            Log.v(TAG, "MeetingObserver onChange, selfChange: " + selfChange);
            super.onChange(selfChange);
            Single.fromCallable(() -> read(mTeamId))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(meetingCursorWrapper -> {
                        mCursor.unregisterContentObserver(mMeetingObserver);
                        mCursor.close();
                        mCursor = meetingCursorWrapper;
                        notifyDataSetChanged();
                        mCursor.registerContentObserver(mMeetingObserver);
                    });
        }

        @WorkerThread
        private MeetingCursorWrapper read(int teamId) {
            // Closing the cursorWrapper also closes the cursor
            @SuppressLint("Recycle")
            Cursor cursor = mContext.getContentResolver().query(MeetingColumns.CONTENT_URI, null, MeetingColumns.TEAM_ID + "=?",
                    new String[] { String.valueOf(teamId) }, MeetingColumns.MEETING_DATE + " DESC");
            MeetingCursorWrapper cursorWrapper = new MeetingCursorWrapper(cursor);
            cursorWrapper.getCount();
            return cursorWrapper;
        }
    }

}
