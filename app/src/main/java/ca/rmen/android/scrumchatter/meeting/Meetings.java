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
package ca.rmen.android.scrumchatter.meeting;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import ca.rmen.android.scrumchatter.util.Log;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.dialog.DialogFragmentFactory;
import ca.rmen.android.scrumchatter.export.MeetingExport;
import ca.rmen.android.scrumchatter.meeting.detail.Meeting;
import ca.rmen.android.scrumchatter.meeting.detail.MeetingActivity;
import ca.rmen.android.scrumchatter.provider.MemberColumns;
import ca.rmen.android.scrumchatter.util.TextUtils;

/**
 * Provides UI and DB logic regarding the management of meetings: creating and deleting meetings.
 */
public class Meetings {
    private static final String TAG = Constants.TAG + "/" + Meetings.class.getSimpleName();
    public static final String EXTRA_MEETING_ID = "meeting_id";
    public static final String EXTRA_MEETING_STATE = "meeting_state";
    private final FragmentActivity mActivity;

    public Meetings(FragmentActivity activity) {
        mActivity = activity;
    }

    /**
     * Checks if there are any team members in the given team id. If not, an error dialog is shown. If the team does have members, then we start
     * the MeetingActivity class for a new meeting.
     */
    public void createMeeting(final int teamId) {
        Log.v(TAG, "createMeeting in team " + teamId);
        AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {
                Cursor c = mActivity.getContentResolver().query(MemberColumns.CONTENT_URI, new String[] { "count(*)" },
                        MemberColumns.TEAM_ID + "=? AND " + MemberColumns.DELETED + "= 0", new String[] { String.valueOf(teamId) }, null);
                if (c != null) {
                    try {
                        c.moveToFirst();
                        int memberCount = c.getInt(0);
                        return memberCount > 0;
                    } finally {
                        c.close();
                    }
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    MeetingActivity.startNewMeeting(mActivity);
                } else {
                    DialogFragmentFactory.showInfoDialog(mActivity, R.string.dialog_error_title_one_member_required,
                            R.string.dialog_error_message_one_member_required);

                }
            }

        };
        task.execute();


    }

    /**
     * Shows a confirmation dialog to delete the given meeting.
     */
    public void confirmDelete(final Meeting meeting) {
        Log.v(TAG, "confirm delete meeting: " + meeting);
        // Let's ask him if he's sure first.
        Bundle extras = new Bundle(1);
        extras.putLong(EXTRA_MEETING_ID, meeting.getId());
        DialogFragmentFactory.showConfirmDialog(mActivity, mActivity.getString(R.string.action_delete_meeting),
                mActivity.getString(R.string.dialog_message_delete_meeting_confirm, TextUtils.formatDateTime(mActivity, meeting.getStartDate())),
                R.id.action_delete_meeting, extras);
    }

    /**
     * Deletes the given meeting from the DB.
     */
    public void delete(final long meetingId) {
        Log.v(TAG, "delete meeting " + meetingId);
        // Delete the meeting in a background thread.
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                Meeting meeting = Meeting.read(mActivity, meetingId);
                if (meeting == null) {
                    Log.v(TAG, "Tried to delete non-existing meeting " + meetingId);
                    return null;
                }
                meeting.delete();
                return null;
            }
        };
        task.execute();

    }

    /**
     * Read the data for the given meeting, then show an intent chooser to export this data as text.
     */
    public void export(long meetingId) {
        Log.v(TAG, "export meeting " + meetingId);
        // Export the meeting in a background thread.
        AsyncTask<Long, Void, Boolean> asyncTask = new AsyncTask<Long, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Long... meetingId) {
                MeetingExport export = new MeetingExport(mActivity);
                return export.exportMeeting(meetingId[0]);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (!result) Snackbar.make(mActivity.getWindow().getDecorView().getRootView(), R.string.error_sharing_meeting, Snackbar.LENGTH_LONG).show();
            }

        };
        asyncTask.execute(meetingId);
    }

}
