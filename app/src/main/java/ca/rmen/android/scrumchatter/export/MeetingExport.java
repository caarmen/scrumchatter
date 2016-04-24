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
package ca.rmen.android.scrumchatter.export;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.format.DateUtils;
import ca.rmen.android.scrumchatter.util.Log;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.provider.MeetingColumns;
import ca.rmen.android.scrumchatter.provider.MeetingCursorWrapper;
import ca.rmen.android.scrumchatter.provider.MeetingMemberColumns;
import ca.rmen.android.scrumchatter.provider.MeetingMemberCursorWrapper;
import ca.rmen.android.scrumchatter.provider.MemberColumns;
import ca.rmen.android.scrumchatter.util.TextUtils;

public class MeetingExport {

    private static final String TAG = MeetingExport.class.getSimpleName();
    private final Context mContext;

    public MeetingExport(Context context) {
        mContext = context;
    }

    /**
     * Generate a text report for a single meeting and bring up a chooser to
     * send the report (by mail, etc).
     * 
     * @param meetingId the id of the meeting to export
     * @return true if we were able to generate the report and bring up the
     *         chooser to send it.
     */
    public boolean exportMeeting(long meetingId) {
        StringBuilder sb = new StringBuilder();
        // Export info about the meeting (date, total duration)
        // Closing the meetingCursorWrapper also closes the meetingCursor
        @SuppressLint("Recycle")
        Cursor meetingCursor = mContext.getContentResolver().query(Uri.withAppendedPath(MeetingColumns.CONTENT_URI, String.valueOf(meetingId)),
                new String[] { MeetingColumns.MEETING_DATE, MeetingColumns.TOTAL_DURATION }, null, null, null);
        MeetingCursorWrapper meetingCursorWrapper = new MeetingCursorWrapper(meetingCursor);
        //noinspection TryFinallyCanBeTryWithResources
        try {
            if (!meetingCursorWrapper.moveToFirst()) {
                Log.w(TAG, "Trying to export a meeting which doesn't exist.  Surely a monkey must be involved.");
                return false;
            }
            String subject = mContext.getString(R.string.export_meeting_date, TextUtils.formatDateTime(mContext, meetingCursorWrapper.getMeetingDate()));
            sb.append(subject);
            sb.append("\n");
            sb.append(mContext.getString(R.string.export_meeting_duration, DateUtils.formatElapsedTime(meetingCursorWrapper.getTotalDuration())));
            sb.append("\n");

            // Export the member times:
            // Closing the meetingMemberCursorWrapper also closes the meetingMemberCursor
            @SuppressLint("Recycle")
            Cursor meetingMemberCursor = mContext.getContentResolver().query(Uri.withAppendedPath(MeetingMemberColumns.CONTENT_URI, String.valueOf(meetingId)),
                    new String[] { MemberColumns.NAME, MeetingMemberColumns.DURATION },

                    MeetingMemberColumns.DURATION + ">0", null, MeetingMemberColumns.DURATION + " DESC ");
            MeetingMemberCursorWrapper meetingMemberCursorWrapper = new MeetingMemberCursorWrapper(meetingMemberCursor);
            if (meetingMemberCursorWrapper.moveToFirst()) {
                do {
                    sb.append(meetingMemberCursorWrapper.getMemberName());
                    sb.append(": ");
                    sb.append(DateUtils.formatElapsedTime(meetingMemberCursorWrapper.getDuration()));
                    sb.append("\n");
                } while (meetingMemberCursorWrapper.moveToNext());
            }
            meetingMemberCursorWrapper.close();

            // Show the chooser
            showChooser(subject, sb.toString());
            return true;
        } finally {
            meetingCursorWrapper.close();
        }
    }

    /**
     * Bring up the chooser to share the meeting report.
     */
    private void showChooser(String subject, String body) {
        Log.v(TAG, "showChooser");
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        sendIntent.putExtra(Intent.EXTRA_TEXT, body);
        mContext.startActivity(Intent.createChooser(sendIntent, mContext.getResources().getText(R.string.action_share)));
    }
}
