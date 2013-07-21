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

import java.util.ArrayList;

import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.provider.MeetingColumns;
import ca.rmen.android.scrumchatter.provider.MeetingColumns.State;
import ca.rmen.android.scrumchatter.provider.MeetingCursorWrapper;
import ca.rmen.android.scrumchatter.provider.MeetingMemberColumns;
import ca.rmen.android.scrumchatter.provider.MeetingMemberCursorWrapper;
import ca.rmen.android.scrumchatter.provider.ScrumChatterProvider;

public class Meeting {
    private static final String TAG = Constants.TAG + "/" + Meeting.class.getSimpleName();
    private final Context mContext;
    public final long id;
    public final Uri uri;
    public long startDate;
    public State state;
    public long duration;

    private Meeting(Context context, long id, long startDate, State state, long duration) {
        mContext = context;
        this.id = id;
        this.startDate = startDate;
        this.state = state;
        this.duration = duration;
        this.uri = Uri.withAppendedPath(MeetingColumns.CONTENT_URI, String.valueOf(id));
    }

    /**
     * Read an existing meeting from the DB.
     */
    static Meeting read(Context context, long id) {
        // Read the meeting attributes from the DB 
        Uri uri = Uri.withAppendedPath(MeetingColumns.CONTENT_URI, String.valueOf(id));
        Cursor meetingCursor = context.getContentResolver().query(uri, null, null, null, null);
        MeetingCursorWrapper cursorWrapper = new MeetingCursorWrapper(meetingCursor);
        cursorWrapper.moveToFirst();
        long duration = cursorWrapper.getTotalDuration();
        long startDate = cursorWrapper.getMeetingDate();
        State state = cursorWrapper.getState();
        cursorWrapper.close();
        return new Meeting(context, id, startDate, state, duration);
    }

    public static Meeting read(Context context, MeetingCursorWrapper cursorWrapper) {
        long id = cursorWrapper.getId();
        long startDate = cursorWrapper.getMeetingDate();
        long duration = cursorWrapper.getTotalDuration();
        MeetingColumns.State state = cursorWrapper.getState();
        return new Meeting(context, id, startDate, state, duration);
    }

    /**
     * Create a new Meeting. This persists the new meeting to the DB.
     */
    static Meeting createNewMeeting(Context context) {
        Log.v(TAG, "create new meeting");
        int teamId = PreferenceManager.getDefaultSharedPreferences(context).getInt(Constants.PREF_TEAM_ID, Constants.DEFAULT_TEAM_ID);
        ContentValues values = new ContentValues();
        long startDate = System.currentTimeMillis();
        values.put(MeetingColumns.MEETING_DATE, System.currentTimeMillis());
        values.put(MeetingColumns.TEAM_ID, teamId);
        Uri newMeetingUri = context.getContentResolver().insert(MeetingColumns.CONTENT_URI, values);
        long meetingId = Long.parseLong(newMeetingUri.getLastPathSegment());
        return new Meeting(context, meetingId, startDate, State.NOT_STARTED, 0);
    }

    /**
     * Updates the start time to now, sets the state to in_progress, and persists the changes.
     */
    void start() {
        /**
         * Change the date of the meeting to now. We do this when the
         * meeting goes from not-started to in-progress. This way it is
         * easier to track the duration of the meeting.
         */
        startDate = System.currentTimeMillis();
        state = State.IN_PROGRESS;
        save();
    }

    /**
     * Updates the meeting duration to time elapsed since startDate, sets the state to finished, and persists the changes.
     */
    void stop() {
        state = State.FINISHED;
        long meetingDuration = System.currentTimeMillis() - startDate;
        duration = meetingDuration / 1000;
        shutEverybodyUp();
        save();
    }

    /**
     * Stop the chronometers of all team members who are still talking. Update
     * the duration for these team members.
     */
    private void shutEverybodyUp() {
        // Query all team members who are still talking in this meeting.
        Uri uri = Uri.withAppendedPath(MeetingMemberColumns.CONTENT_URI, String.valueOf(id));
        Cursor cursor = mContext.getContentResolver().query(uri,
                new String[] { MeetingMemberColumns._ID, MeetingMemberColumns.DURATION, MeetingMemberColumns.TALK_START_TIME },
                MeetingMemberColumns.TALK_START_TIME + ">0", null, null);
        if (cursor != null) {
            // Prepare some update statements to set the duration and reset the
            // talk_start_time, for these members.
            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
            MeetingMemberCursorWrapper cursorWrapper = new MeetingMemberCursorWrapper(cursor);
            if (cursorWrapper.moveToFirst()) {
                do {
                    // Prepare an update operation for one of these members.
                    Builder builder = ContentProviderOperation.newUpdate(MeetingMemberColumns.CONTENT_URI);
                    long memberId = cursorWrapper.getMemberId();
                    // Calculate the total duration the team member talked
                    // during this meeting.
                    long duration = cursorWrapper.getDuration();
                    long talkStartTime = cursorWrapper.getTalkStartTime();
                    long newDuration = duration + (System.currentTimeMillis() - talkStartTime) / 1000;
                    builder.withValue(MeetingMemberColumns.DURATION, newDuration);
                    builder.withValue(MeetingMemberColumns.TALK_START_TIME, 0);
                    builder.withSelection(MeetingMemberColumns.MEMBER_ID + "=? AND " + MeetingMemberColumns.MEETING_ID + "=?",
                            new String[] { String.valueOf(memberId), String.valueOf(id) });
                    operations.add(builder.build());
                } while (cursorWrapper.moveToNext());
            }
            cursorWrapper.close();
            try {
                // Batch update these team members.
                mContext.getContentResolver().applyBatch(ScrumChatterProvider.AUTHORITY, operations);
            } catch (Exception e) {
                Log.v(TAG, "Couldn't close off meeting: " + e.getMessage(), e);
            }
        }
    }

    /**
     * If the team member is talking, stop them. Otherwise, stop all other team members who may be talking, and start this one.
     */
    void toggleTalkingMember(final long memberId) {
        Log.v(TAG, "toggleTalkingMember " + memberId);

        // Find out if this member is currently talking:
        // read its talk_start_time and duration fields.
        Uri meetingMemberUri = Uri.withAppendedPath(MeetingMemberColumns.CONTENT_URI, String.valueOf(id));
        Cursor cursor = mContext.getContentResolver().query(meetingMemberUri,
                new String[] { MeetingMemberColumns.TALK_START_TIME, MeetingMemberColumns.DURATION }, MeetingMemberColumns.MEMBER_ID + "=?",
                new String[] { String.valueOf(memberId) }, null);
        long talkStartTime = 0;
        long duration = 0;
        if (cursor != null) {
            MeetingMemberCursorWrapper cursorWrapper = new MeetingMemberCursorWrapper(cursor);
            if (cursorWrapper.moveToFirst()) {
                talkStartTime = cursorWrapper.getTalkStartTime();
                duration = cursorWrapper.getDuration();
            }
            cursorWrapper.close();
        }
        Log.v(TAG, "Talking member: duration = " + duration + ", talkStartTime = " + talkStartTime);
        ContentValues values = new ContentValues(2);
        // The member is currently talking if talkStartTime > 0.
        if (talkStartTime > 0) {
            long justTalkedFor = (System.currentTimeMillis() - talkStartTime) / 1000;
            long newDuration = duration + justTalkedFor;
            values.put(MeetingMemberColumns.DURATION, newDuration);
            values.put(MeetingMemberColumns.TALK_START_TIME, 0);
        } else {
            // shut up any other talking member before this one starts.
            shutEverybodyUp();
            values.put(MeetingMemberColumns.TALK_START_TIME, System.currentTimeMillis());
        }

        mContext.getContentResolver().update(MeetingMemberColumns.CONTENT_URI, values,
                MeetingMemberColumns.MEMBER_ID + "=? AND " + MeetingMemberColumns.MEETING_ID + "=?",
                new String[] { String.valueOf(memberId), String.valueOf(id) });
    }

    public void delete() {
        mContext.getContentResolver().delete(uri, null, null);
    }

    void save() {
        ContentValues values = new ContentValues(3);
        values.put(MeetingColumns.STATE, state.ordinal());
        values.put(MeetingColumns.MEETING_DATE, startDate);
        values.put(MeetingColumns.TOTAL_DURATION, duration);
        mContext.getContentResolver().update(uri, values, null, null);
    }

}