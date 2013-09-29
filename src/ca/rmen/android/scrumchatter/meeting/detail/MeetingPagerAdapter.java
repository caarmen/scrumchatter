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
package ca.rmen.android.scrumchatter.meeting.detail;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;
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

    public MeetingPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        Log.v(TAG, "Constructor");
        Cursor cursor = context.getContentResolver().query(MeetingColumns.CONTENT_URI, new String[] { MeetingColumns._ID, MeetingColumns.STATE }, null, null,
                MeetingColumns.MEETING_DATE + " DESC");
        mCursor = new MeetingCursorWrapper(cursor);
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

    int getPositionForMeetingId(long meetingId) {
        Log.v(TAG, "getPositionForMeetingId " + meetingId);

        int position = 0;
        for (mCursor.moveToFirst(); mCursor.moveToNext(); position++) {
            if (mCursor.getId() == meetingId) return position;
        }
        return -1;
    }

    long getMeetingIdAt(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getId();
    }

    @Override
    public int getCount() {
        return mCursor.getCount();
    }

}
