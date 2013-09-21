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
import android.database.ContentObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.app.NavUtils;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Chronometer;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.dialog.ConfirmDialogFragment.DialogButtonListener;
import ca.rmen.android.scrumchatter.dialog.DialogFragmentFactory;
import ca.rmen.android.scrumchatter.meeting.Meetings;
import ca.rmen.android.scrumchatter.provider.MeetingColumns.State;
import ca.rmen.android.scrumchatter.util.TextUtils;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

/**
 * Displays attributes of a meeting. Also contains a {@link MeetingFragment} which displays the list of team members participating in
 * this meeting.
 */
public class MeetingActivity extends SherlockFragmentActivity implements DialogButtonListener {

    private String TAG;

    private static final int LOADER_ID = MeetingLoaderTask.class.hashCode();

    private View mBtnStopMeeting;
    private View mProgressBarHeader;
    private Chronometer mMeetingChronometer;
    private Meeting mMeeting;
    private Meetings mMeetings;
    private MeetingObserver mMeetingObserver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (TAG == null) TAG = Constants.TAG + "/" + MeetingActivity.class.getSimpleName() + "/" + System.currentTimeMillis();
        Log.v(TAG, "onCreate: savedInstanceState = " + savedInstanceState + ", intent = " + getIntent() + ", intent flags = " + getIntent().getFlags());
        setContentView(R.layout.meeting_activity);
        mMeetings = new Meetings(this);

        mBtnStopMeeting = findViewById(R.id.btn_stop_meeting);
        mMeetingChronometer = (Chronometer) findViewById(R.id.tv_meeting_duration);
        mProgressBarHeader = findViewById(R.id.header_progress_bar);

        mBtnStopMeeting.setOnClickListener(mOnClickListener);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Bundle args = new Bundle(1);
        long meetingId = getIntent().getLongExtra(Meetings.EXTRA_MEETING_ID, -1);
        args.putLong(Meetings.EXTRA_MEETING_ID, meetingId);
        getSupportLoaderManager().initLoader(LOADER_ID, args, mLoaderCallbacks);
        mMeetingObserver = new MeetingObserver(new Handler());
    }

    @Override
    protected void onPause() {
        Log.v(TAG, "onPause");
        Log.v(TAG, "unregister observer " + mMeetingObserver);
        getContentResolver().unregisterContentObserver(mMeetingObserver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.v(TAG, "onResume");
        super.onResume();
        if (mMeeting != null) {
            Log.v(TAG, "register observer " + mMeetingObserver);
            getContentResolver().registerContentObserver(mMeeting.getUri(), false, mMeetingObserver);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        Log.v(TAG, "onCreateOptionsMenu: mMeeting =" + mMeeting);

        getSupportMenuInflater().inflate(R.menu.meeting_menu, menu);
        // Only share finished meetings
        final MenuItem shareItem = menu.findItem(R.id.action_share);
        shareItem.setVisible(mMeeting != null && mMeeting.getState() == State.FINISHED);
        // Delete a meeting in any state.
        final MenuItem deleteItem = menu.findItem(R.id.action_delete_meeting);
        deleteItem.setVisible(mMeeting != null);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.v(TAG, "onOptionsItemSelected: item = " + item.getItemId() + ": " + item.getTitle());
        if (isFinishing()) {
            Log.v(TAG, "User clicked on a menu item while the activity is finishing.  Surely a monkey is involved");
            return true;
        }
        switch (item.getItemId()) {
        // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.action_share:
                mMeetings.export(mMeeting.getId());
                return true;
            case R.id.action_delete_meeting:
                mMeetings.confirmDelete(mMeeting);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * The user tapped on the OK button of a confirmation dialog. Execute the action requested by the user.
     * 
     * @param actionId the action id which was provided to the {@link DialogFragmentFactory} when creating the dialog.
     * @param extras any extras which were provided to the {@link DialogFragmentFactory} when creating the dialog.
     * @see ca.rmen.android.scrumchatter.dialog.ConfirmDialogFragment.DialogButtonListener#onOkClicked(int, android.os.Bundle)
     */
    @Override
    public void onOkClicked(int actionId, Bundle extras) {
        Log.v(TAG, "onClicked: actionId = " + actionId + ", extras = " + extras);
        if (isFinishing()) {
            Log.v(TAG, "Ignoring on click because this activity is closing.  You're either very quick or a monkey.");
            return;
        }
        if (actionId == R.id.action_delete_meeting) mMeetings.delete(mMeeting.getId());
        else if (actionId == R.id.btn_stop_meeting) stopMeeting();
    }

    /**
     * Update UI components based on the meeting state.
     */
    private void onMeetingChanged() {
        Log.v(TAG, "onMeetingChanged: meeting = " + mMeeting);
        supportInvalidateOptionsMenu();
        if (mMeeting == null) {
            Log.v(TAG, "No more meeting, quitting this activity: finishing=" + isFinishing());
            if (!isFinishing()) {
                getSupportLoaderManager().destroyLoader(LOADER_ID);
                mBtnStopMeeting.setVisibility(View.INVISIBLE);
                getContentResolver().unregisterContentObserver(mMeetingObserver);
                finish();
            }
            return;
        }
        Log.v(TAG, "meetingState = " + mMeeting.getState());
        // Show the "stop meeting" button if the meeting is not finished.
        mBtnStopMeeting.setVisibility(mMeeting.getState() == State.NOT_STARTED || mMeeting.getState() == State.IN_PROGRESS ? View.VISIBLE : View.INVISIBLE);
        // Only enable the "stop meeting" button if the meeting is in progress.
        mBtnStopMeeting.setEnabled(mMeeting.getState() == State.IN_PROGRESS);
        getSupportActionBar().setTitle(TextUtils.formatDateTime(MeetingActivity.this, mMeeting.getStartDate()));

        // Show the horizontal progress bar for in progress meetings
        mProgressBarHeader.setVisibility(mMeeting.getState() == State.IN_PROGRESS ? View.VISIBLE : View.INVISIBLE);

        // Update the chronometer
        if (mMeeting.getState() == State.IN_PROGRESS) {
            // If the meeting is in progress, show the Chronometer.
            long timeSinceMeetingStartedMillis = System.currentTimeMillis() - mMeeting.getStartDate();
            mMeetingChronometer.setBase(SystemClock.elapsedRealtime() - timeSinceMeetingStartedMillis);
            mMeetingChronometer.start();
        } else if (mMeeting.getState() == State.FINISHED) {
            // For finished meetings, show the duration we retrieved from the db.
            mMeetingChronometer.stop();
            mMeetingChronometer.setText(DateUtils.formatElapsedTime(mMeeting.getDuration()));
        }
    }

    /**
     * Stop the meeting. Set the state to finished, stop the chronometer, hide the "stop meeting" button, persist the meeting duration, and stop the
     * chronometers for all team members who are still talking.
     */
    private void stopMeeting() {
        AsyncTask<Meeting, Void, Void> task = new AsyncTask<Meeting, Void, Void>() {

            @Override
            protected Void doInBackground(Meeting... meeting) {
                meeting[0].stop();
                return null;
            }
        };
        task.execute(mMeeting);
    }



    private final OnClickListener mOnClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            Log.v(TAG, "onClick, view: " + v);
            if (isFinishing()) {
                Log.v(TAG, "Ignoring on click because this activity is closing.  You're either very quick or a monkey.");
                return;
            }
            switch (v.getId()) {
            // Stop the whole meeting.
                case R.id.btn_stop_meeting:
                    // Let's ask him if he's sure.
                    DialogFragmentFactory.showConfirmDialog(MeetingActivity.this, getString(R.string.action_stop_meeting), getString(R.string.dialog_confirm),
                            R.id.btn_stop_meeting, null);
                    break;
                default:
                    break;
            }
        }
    };

    private class MeetingObserver extends ContentObserver {

        private final String TAG;

        public MeetingObserver(Handler handler) {
            super(handler);
            TAG = MeetingActivity.this.TAG + "/" + MeetingObserver.class.getSimpleName();
            Log.v(TAG, "Constructor");
        }

        /**
         * Called when a meeting changes.
         */
        @Override
        public void onChange(boolean selfChange) {
            Log.v(TAG, "MeetingObserver onChange, selfChange: " + selfChange + ", mMeeting = " + mMeeting);
            super.onChange(selfChange);
            Bundle args = new Bundle(1);
            args.putLong(Meetings.EXTRA_MEETING_ID, mMeeting.getId());
            getSupportLoaderManager().restartLoader(LOADER_ID, args, mLoaderCallbacks);
        }

        @Override
        public String toString() {
            return TAG;
        }

    };

    /**
     * Loads the meeting for the given meeting id. If the meeting id is -1, a new meeting is created.
     */
    private static class MeetingLoaderTask extends AsyncTaskLoader<Meeting> {
        private long mMeetingId;

        private static final String TAG = Constants.TAG + "/" + MeetingLoaderTask.class.getSimpleName();

        private MeetingLoaderTask(Context context, long meetingId) {
            super(context);
            mMeetingId = meetingId;
        }

        @Override
        public Meeting loadInBackground() {
            Log.v(TAG, "loadInBackground");
            final Meeting meeting;
            if (mMeetingId == -1) meeting = Meeting.createNewMeeting(getContext());
            else
                meeting = Meeting.read(getContext(), mMeetingId);
            return meeting;
        }
    };

    private LoaderCallbacks<Meeting> mLoaderCallbacks = new LoaderCallbacks<Meeting>() {

        /**
         * Create a {@link MeetingLoaderTask} which will load the meeting for the meeting id given in the Bundle.
         * 
         * @bundle should have the id of the meeting to load, in the extra {@link Meetings#EXTRA_MEETING_ID}. If the id is -1, the loader will create a new
         *         meeting.
         */
        @Override
        public Loader<Meeting> onCreateLoader(int id, Bundle bundle) {
            long meetingId = bundle.getLong(Meetings.EXTRA_MEETING_ID, -1);
            Log.v(TAG, "onCreateLoader, meetingId = " + meetingId);
            MeetingLoaderTask loaderTask = new MeetingLoaderTask(MeetingActivity.this, meetingId);
            loaderTask.forceLoad();
            return loaderTask;
        }

        /**
         * Update the UI for the given meeting.
         */
        @Override
        public void onLoadFinished(Loader<Meeting> loaderTask, Meeting meeting) {
            Log.v(TAG, "onLoadFinished, meeting = " + meeting);
            mMeeting = meeting;
            onMeetingChanged();
            if (meeting == null) {
                Log.w(TAG, "Could not load meeting, are you a monkey?");
                return;
            }
            Log.v(TAG, "register observer " + mMeetingObserver + "isFinishing: " + isFinishing());
            getContentResolver().registerContentObserver(mMeeting.getUri(), false, mMeetingObserver);

            // Load the list of team members.
            MeetingFragment fragment = (MeetingFragment) getSupportFragmentManager().findFragmentById(R.id.meeting_fragment);
            fragment.loadMeeting(mMeeting.getId());
        }

        @Override
        public void onLoaderReset(Loader<Meeting> meeting) {
            Log.v(TAG, "onLoaderReset: meeting = " + meeting);
            mMeeting = null;
            onMeetingChanged();
        }
    };
}
