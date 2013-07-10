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
package ca.rmen.android.scrumchatter;

import java.util.ArrayList;

import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NavUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Chronometer;
import ca.rmen.android.scrumchatter.export.MeetingExport;
import ca.rmen.android.scrumchatter.provider.MeetingColumns;
import ca.rmen.android.scrumchatter.provider.MeetingColumns.State;
import ca.rmen.android.scrumchatter.provider.MeetingCursorWrapper;
import ca.rmen.android.scrumchatter.provider.MeetingMemberColumns;
import ca.rmen.android.scrumchatter.provider.MeetingMemberCursorWrapper;
import ca.rmen.android.scrumchatter.provider.ScrumChatterProvider;
import ca.rmen.android.scrumchatter.ui.MeetingFragment;
import ca.rmen.android.scrumchatter.ui.ScrumChatterDialog;
import ca.rmen.android.scrumchatter.util.TextUtils;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

/**
 * Displays attributes of a meeting as well as the team members participating in
 * this meeting.
 */
public class MeetingActivity extends SherlockFragmentActivity {

    private static final String TAG = Constants.TAG + "/" + MeetingActivity.class.getSimpleName();

    public static final String EXTRA_MEETING_ID = MeetingActivity.class.getPackage().getName() + ".meeting_id";
    private View mBtnStopMeeting;
    private View mProgressBarHeader;
    private Chronometer mMeetingChronometer;
    private Uri mMeetingUri;
    private long mMeetingId;
    private State mMeetingState = State.NOT_STARTED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate: savedInstanceState = " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.meeting_activity);

        mBtnStopMeeting = findViewById(R.id.btn_stop_meeting);
        mMeetingChronometer = (Chronometer) findViewById(R.id.tv_meeting_duration);
        mProgressBarHeader = findViewById(R.id.header_progress_bar);

        mBtnStopMeeting.setOnClickListener(mOnClickListener);

        Intent intent = getIntent();
        loadMeeting(intent);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onPause() {
        getContentResolver().unregisterContentObserver(mMeetingObserver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMeetingUri != null) getContentResolver().registerContentObserver(mMeetingUri, false, mMeetingObserver);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getSupportMenuInflater().inflate(R.menu.meeting_menu, menu);
        final MenuItem shareItem = menu.findItem(R.id.action_share);
        shareItem.setVisible(mMeetingState == State.FINISHED);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.action_share:
                // Export the meeting in a background thread.
                AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... params) {
                        MeetingExport export = new MeetingExport(MeetingActivity.this);
                        export.exportMeeting(mMeetingId);
                        return null;
                    }
                };
                asyncTask.execute();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Extract the meeting id from the intent and load the meeting data into the
     * activity.
     */
    private void loadMeeting(Intent intent) {
        Log.v(TAG, "loadMeeting " + intent);
        mMeetingId = intent.getLongExtra(EXTRA_MEETING_ID, -1);

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

            long mDuration;
            long mDate;

            @Override
            protected Void doInBackground(Void... params) {
                if (mMeetingId == -1) {
                    mMeetingId = createMeeting();
                }
                // Read the meeting attributes from the DB (don't really like
                // Cursor code in an Activity, but oh well...)
                mMeetingUri = Uri.withAppendedPath(MeetingColumns.CONTENT_URI, String.valueOf(mMeetingId));
                getContentResolver().registerContentObserver(mMeetingUri, false, mMeetingObserver);
                Cursor meetingCursor = getContentResolver().query(mMeetingUri, null, null, null, null);
                MeetingCursorWrapper cursorWrapper = new MeetingCursorWrapper(meetingCursor);
                cursorWrapper.moveToFirst();
                mDuration = cursorWrapper.getTotalDuration();
                mDate = cursorWrapper.getMeetingDate();
                mMeetingState = cursorWrapper.getState();
                cursorWrapper.close();
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if (mMeetingState == State.IN_PROGRESS) {
                    // If the meeting is in progress, show the Chronometer.
                    long timeSinceMeetingStartedMillis = System.currentTimeMillis() - mDate;
                    mMeetingChronometer.setBase(SystemClock.elapsedRealtime() - timeSinceMeetingStartedMillis);
                    mMeetingChronometer.start();
                } else if (mMeetingState == State.FINISHED) {
                    // For finished meetings, show the duration we retrieved
                    // from the
                    // db.
                    mMeetingChronometer.setText(DateUtils.formatElapsedTime(mDuration));
                }
                getSupportActionBar().setTitle(TextUtils.formatDateTime(MeetingActivity.this, mDate));
                onMeetingStateChanged();

                // Load the list of team members.
                MeetingFragment fragment = (MeetingFragment) getSupportFragmentManager().findFragmentById(R.id.meeting_fragment);
                fragment.loadMeeting(mMeetingId, mMeetingState, mOnClickListener);
            }
        };
        task.execute();
    }

    /**
     * Update UI components based on the meeting state.
     */
    private void onMeetingStateChanged() {
        Log.v(TAG, "onMeetingStateChanged: meetingState = " + mMeetingState);
        // Show the "stop meeting" button if the meeting is not finished.
        mBtnStopMeeting.setVisibility(mMeetingState == State.NOT_STARTED || mMeetingState == State.IN_PROGRESS ? View.VISIBLE : View.INVISIBLE);
        // Only enable the "stop meeting" button if the meeting is in progress.
        mBtnStopMeeting.setEnabled(mMeetingState == State.IN_PROGRESS);

        // Blink the chronometer when the meeting is in progress
        if (mMeetingState == State.IN_PROGRESS) {
            mProgressBarHeader.setVisibility(View.VISIBLE);
        } else {
            mProgressBarHeader.setVisibility(View.INVISIBLE);
        }
        supportInvalidateOptionsMenu();
    }

    /**
     * @return the id of the newly created meeting.
     */
    private long createMeeting() {
        Log.v(TAG, "create new meeting");
        ContentValues values = new ContentValues();
        values.put(MeetingColumns.MEETING_DATE, System.currentTimeMillis());
        Uri newMeetingUri = getContentResolver().insert(MeetingColumns.CONTENT_URI, values);
        long meetingId = Long.parseLong(newMeetingUri.getLastPathSegment());
        return meetingId;
    }

    /**
     * Change the state of the meeting.
     * 
     * @param newState
     *            the new state of the meeting
     */
    private void setMeetingState(State newState) {
        Log.v(TAG, "setMeetingState " + newState);
        ContentValues values = new ContentValues(1);
        values.put(MeetingColumns.STATE, newState.ordinal());
        getContentResolver().update(mMeetingUri, values, null, null);
    }

    /**
     * @return the date the meeting was created or started.
     */
    private long getMeetingDate() {
        Cursor cursor = getContentResolver().query(mMeetingUri, new String[] { MeetingColumns.MEETING_DATE }, null, null, null);
        long meetingDate = 0;
        if (cursor != null) {
            MeetingCursorWrapper cursorWrapper = new MeetingCursorWrapper(cursor);
            if (cursorWrapper.moveToFirst()) meetingDate = cursorWrapper.getMeetingDate();
            cursorWrapper.close();
        }
        return meetingDate;
    }

    /**
     * Change the duration of the meeting.
     * 
     * @param duration
     *            the new duration of the meeting.
     */
    private void setMeetingDuration(long duration) {
        ContentValues values = new ContentValues(1);
        values.put(MeetingColumns.TOTAL_DURATION, duration);
        getContentResolver().update(mMeetingUri, values, null, null);
    }

    /**
     * Start the meeting. Set the state to in-progress, start the chronometer,
     * and show the "stop meeting" button.
     */
    private void startMeeting() {
        AsyncTask<Void, Void, Long> task = new AsyncTask<Void, Void, Long>() {

            @Override
            protected Long doInBackground(Void... params) {
                setMeetingState(State.IN_PROGRESS);
                /**
                 * Change the date of the meeting to now. We do this when the
                 * meeting goes from not-started to in-progress. This way it is
                 * easier to track the duration of the meeting.
                 */
                ContentValues values = new ContentValues(1);
                long newMeetingStartDate = System.currentTimeMillis();
                values.put(MeetingColumns.MEETING_DATE, newMeetingStartDate);
                getContentResolver().update(mMeetingUri, values, null, null);
                return newMeetingStartDate;
            }

            @Override
            protected void onPostExecute(Long newMeetingStartDate) {
                mBtnStopMeeting.setVisibility(View.VISIBLE);
                getSupportActionBar().setTitle(TextUtils.formatDateTime(MeetingActivity.this, newMeetingStartDate));
                mMeetingChronometer.setBase(SystemClock.elapsedRealtime());
                mMeetingChronometer.start();
            }
        };
        task.execute();
    }

    /**
     * Stop the meeting. Set the state to finished, stop the chronometer, hide
     * the "stop meeting" button, persist the meeting duration, and stop the
     * chronometers for all team members who are still talking.
     */
    private void stopMeeting() {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                setMeetingState(State.FINISHED);
                long meetingStartDate = getMeetingDate();
                long meetingDuration = System.currentTimeMillis() - meetingStartDate;
                setMeetingDuration(meetingDuration / 1000);
                shutEverybodyUp();
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                mBtnStopMeeting.setVisibility(View.INVISIBLE);
                mMeetingChronometer.stop();
                // Reload the list of team members.
                MeetingFragment fragment = (MeetingFragment) getSupportFragmentManager().findFragmentById(R.id.meeting_fragment);
                fragment.loadMeeting(mMeetingId, State.FINISHED, mOnClickListener);
                supportInvalidateOptionsMenu();
            }
        };
        task.execute();
    }

    /**
     * Stop the chronometers of all team members who are still talking. Update
     * the duration for these team members.
     */
    private void shutEverybodyUp() {
        // Query all team members who are still talking in this meeting.
        Uri uri = Uri.withAppendedPath(MeetingMemberColumns.CONTENT_URI, String.valueOf(mMeetingId));
        Cursor cursor = getContentResolver().query(uri,
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
                            new String[] { String.valueOf(memberId), String.valueOf(mMeetingId) });
                    operations.add(builder.build());
                } while (cursorWrapper.moveToNext());
            }
            cursorWrapper.close();
            try {
                // Batch update these team members.
                getContentResolver().applyBatch(ScrumChatterProvider.AUTHORITY, operations);
            } catch (Exception e) {
                Log.v(TAG, "Couldn't close off meeting: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Switch a member from the talking to non-talking state:
     * 
     * If they were talking, they will no longer be talking, and their button
     * will go back to a "start" button.
     * 
     * If they were not talking, they will start talking, and their button will
     * be a "stop" button.
     * 
     * @param memberId
     */
    private void toggleTalkingMember(final long memberId) {
        Log.v(TAG, "toggleTalkingMember " + memberId);
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                // Find out if this member is currently talking:
                // read its talk_start_time and duration fields.
                Uri meetingMemberUri = Uri.withAppendedPath(MeetingMemberColumns.CONTENT_URI, String.valueOf(mMeetingId));
                Cursor cursor = getContentResolver().query(meetingMemberUri,
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

                getContentResolver().update(MeetingMemberColumns.CONTENT_URI, values,
                        MeetingMemberColumns.MEMBER_ID + "=? AND " + MeetingMemberColumns.MEETING_ID + "=?",
                        new String[] { String.valueOf(memberId), String.valueOf(mMeetingId) });
                return null;
            }

        };
        task.execute();

    }

    private final OnClickListener mOnClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
            // Start or stop the team member talking
                case R.id.btn_start_stop_member:
                    if (mMeetingState != State.IN_PROGRESS) startMeeting();
                    long memberId = (Long) v.getTag();
                    toggleTalkingMember(memberId);
                    break;
                // Stop the whole meeting.
                case R.id.btn_stop_meeting:
                    // Let's ask him if he's sure.
                    ScrumChatterDialog.showDialog(MeetingActivity.this, R.string.action_stop_meeting, R.string.dialog_confirm,
                            new DialogInterface.OnClickListener() {

                                // The user has confirmed to delete the
                                // member.
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which == DialogInterface.BUTTON_POSITIVE) {
                                        stopMeeting();
                                    }
                                }
                            });
                    break;
                default:
                    break;
            }
        }
    };

    private ContentObserver mMeetingObserver = new ContentObserver(null) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            // In a background thread, check if the meeting state has changed.
            // If it has changed, update the Views.
            AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {

                @Override
                protected Boolean doInBackground(Void... params) {
                    Cursor cursor = getContentResolver().query(mMeetingUri, new String[] { MeetingColumns.STATE }, null, null, null);
                    MeetingCursorWrapper cursorWrapper = null;
                    if (cursor != null) {
                        try {
                            cursorWrapper = new MeetingCursorWrapper(cursor);
                            if (cursorWrapper.moveToFirst()) {
                                State meetingState = cursorWrapper.getState();
                                if (mMeetingState != meetingState) {
                                    mMeetingState = cursorWrapper.getState();
                                    return true;
                                }
                            }
                        } finally {
                            cursorWrapper.close();
                        }
                    }
                    return false;
                }

                @Override
                protected void onPostExecute(Boolean meetingStateChanged) {
                    if (meetingStateChanged) {
                        onMeetingStateChanged();
                    }
                }

            };
            task.execute();
        }

    };
}
