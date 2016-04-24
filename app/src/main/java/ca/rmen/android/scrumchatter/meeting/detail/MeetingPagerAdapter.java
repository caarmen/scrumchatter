/**
 * Copyright 2013 Carmen Alvarez
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import ca.rmen.android.scrumchatter.util.Log;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.meeting.Meetings;
import ca.rmen.android.scrumchatter.provider.MeetingColumns;
import ca.rmen.android.scrumchatter.provider.MeetingCursorWrapper;

/**
 * Adapter for the list of meetings
 */
class MeetingPagerAdapter extends FragmentStatePagerAdapter {
    private static final String TAG = Constants.TAG + "/" + MeetingPagerAdapter.class.getSimpleName();

    private MeetingCursorWrapper mCursor;
    private final Context mContext;
    private final MeetingObserver mMeetingObserver;
    private final int mTeamId;

    public MeetingPagerAdapter(Context context, int teamId, FragmentManager fm) {
        super(fm);
        Log.v(TAG, "Constructor: teamId = " + teamId);
        mContext = context;
        mTeamId = teamId;
        // Closing the cursor wrapper also closes the cursor
        @SuppressLint("Recycle")
        Cursor cursor = context.getContentResolver().query(MeetingColumns.CONTENT_URI, null, MeetingColumns.TEAM_ID + "=?",
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

        public MeetingObserver(Handler handler) {
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
            new AsyncTask<Void, Void, MeetingCursorWrapper>() {

                @Override
                protected MeetingCursorWrapper doInBackground(Void... params) {
                    // Closing the cursorWrapper also closes the cursor
                    @SuppressLint("Recycle")
                    Cursor cursor = mContext.getContentResolver().query(MeetingColumns.CONTENT_URI, null, MeetingColumns.TEAM_ID + "=?",
                            new String[] { String.valueOf(mTeamId) }, MeetingColumns.MEETING_DATE + " DESC");
                    MeetingCursorWrapper cursorWrapper = new MeetingCursorWrapper(cursor);
                    cursorWrapper.getCount();
                    return cursorWrapper;
                }

                @Override
                protected void onPostExecute(MeetingCursorWrapper result) {
                    mCursor.unregisterContentObserver(mMeetingObserver);
                    mCursor.close();
                    mCursor = result;
                    notifyDataSetChanged();
                    mCursor.registerContentObserver(mMeetingObserver);
                }

            }.execute();
        }
    }

}
