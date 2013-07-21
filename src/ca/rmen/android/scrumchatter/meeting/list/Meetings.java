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
package ca.rmen.android.scrumchatter.meeting.list;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.meeting.detail.Meeting;
import ca.rmen.android.scrumchatter.meeting.detail.MeetingActivity;
import ca.rmen.android.scrumchatter.provider.MemberColumns;
import ca.rmen.android.scrumchatter.ui.ScrumChatterDialog;
import ca.rmen.android.scrumchatter.util.TextUtils;

/**
 * Provides both UI and DB logic regarding the management of meetings: creating, starting, stopping, etc.
 */
public class Meetings {
    private static final String TAG = Constants.TAG + "/" + Meetings.class.getSimpleName();
    private final Context mContext;

    public Meetings(Context context) {
        mContext = context;
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
                Cursor c = mContext.getContentResolver().query(MemberColumns.CONTENT_URI, new String[] { "count(*)" }, MemberColumns.TEAM_ID + "=?",
                        new String[] { String.valueOf(teamId) }, null);
                try {
                    c.moveToFirst();
                    int memberCount = c.getInt(0);
                    return memberCount > 0;
                } finally {
                    c.close();
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    Intent intent = new Intent(mContext, MeetingActivity.class);
                    mContext.startActivity(intent);
                } else {
                    ScrumChatterDialog.showInfoDialog(mContext, R.string.dialog_error_title_one_member_required,
                            R.string.dialog_error_message_one_member_required);
                }
            }

        };
        task.execute();


    }

    /**
     * Shows a confirmation dialog, then deletes the given meeting if the user presses OK.
     */
    public void delete(final Meeting meeting) {
        Log.v(TAG, "delete meeting");
        // Let's ask him if he's sure first.
        ScrumChatterDialog.showDialog(mContext, mContext.getString(R.string.action_delete_meeting),
                mContext.getString(R.string.dialog_message_delete_meeting_confirm, TextUtils.formatDateTime(mContext, meeting.startDate)),
                new DialogInterface.OnClickListener() {
                    // The user clicked ok. Let's delete the
                    // meeting.
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            // Delete the meeting in a background
                            // thread.
                            AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

                                @Override
                                protected Void doInBackground(Void... params) {
                                    meeting.delete();
                                    return null;
                                }
                            };
                            task.execute();
                        }
                    }
                });
    }

}
