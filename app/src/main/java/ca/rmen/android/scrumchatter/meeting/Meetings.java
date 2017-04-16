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
package ca.rmen.android.scrumchatter.meeting;

import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;

import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.dialog.DialogFragmentFactory;
import ca.rmen.android.scrumchatter.export.MeetingExport;
import ca.rmen.android.scrumchatter.meeting.detail.Meeting;
import ca.rmen.android.scrumchatter.provider.MemberColumns;
import ca.rmen.android.scrumchatter.util.Log;
import ca.rmen.android.scrumchatter.util.TextUtils;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

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
    public Single<Meeting> createMeeting(final int teamId) {
        Log.v(TAG, "createMeeting in team " + teamId);
        return Single.fromCallable(() -> {
            Cursor c = mActivity.getContentResolver().query(MemberColumns.CONTENT_URI, new String[]{"count(*)"},
                    MemberColumns.TEAM_ID + "=? AND " + MemberColumns.DELETED + "= 0", new String[]{String.valueOf(teamId)}, null);
            if (c != null) {
                try {
                    c.moveToFirst();
                    int memberCount = c.getInt(0);
                    if (memberCount > 0) return Meeting.createNewMeeting(mActivity);
                } finally {
                    c.close();
                }
            }
            throw new IllegalArgumentException("Can't create meeting for team " + teamId + " because it doesn't exist or has no members");
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<Meeting> readMeeting(long meetingId) {
        return Single.fromCallable(() -> Meeting.read(mActivity, meetingId))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
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
        Single.fromCallable(() -> Meeting.read(mActivity, meetingId))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(Meeting::delete,
                        throwable -> Log.v(TAG, "couldn't delete meeting " + meetingId, throwable));
    }

    /**
     * Read the data for the given meeting, then show an intent chooser to export this data as text.
     */
    public void export(long meetingId) {
        Log.v(TAG, "export meeting " + meetingId);
        // Export the meeting in a background thread.
        Single.fromCallable(() -> new MeetingExport(mActivity).exportMeeting(meetingId))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(success -> {
                    if (!success) Snackbar.make(mActivity.getWindow().getDecorView().getRootView(), R.string.error_sharing_meeting, Snackbar.LENGTH_LONG).show();
                });
    }
}
