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

import android.content.DialogInterface;
import android.database.ContentObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NavUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Chronometer;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.export.MeetingExport;
import ca.rmen.android.scrumchatter.provider.MeetingColumns.State;
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
    private Meeting mMeeting;
    private boolean mIsObserverRegistered = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate: savedInstanceState = " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.meeting_activity);

        mBtnStopMeeting = findViewById(R.id.btn_stop_meeting);
        mMeetingChronometer = (Chronometer) findViewById(R.id.tv_meeting_duration);
        mProgressBarHeader = findViewById(R.id.header_progress_bar);

        mBtnStopMeeting.setOnClickListener(mOnClickListener);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onPause() {
        Log.v(TAG, "onPause");
        mLoadMeetingAsyncTask.cancel(true);
        mMeetingObserverAsyncTask.cancel(true);
        mStartMeetingAsyncTask.cancel(true);
        mStopMeetingAsyncTask.cancel(true);
        unregisterObserver();
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.v(TAG, "onResume");
        super.onResume();
        mLoadMeetingAsyncTask.execute();
        if (mMeeting != null) {
            registerObserver();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getSupportMenuInflater().inflate(R.menu.meeting_menu, menu);
        final MenuItem shareItem = menu.findItem(R.id.action_share);
        shareItem.setVisible(mMeeting != null && mMeeting.getState() == State.FINISHED);
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
                        export.exportMeeting(mMeeting.getId());
                        return null;
                    }
                };
                asyncTask.execute();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void registerObserver() {
        Log.v(TAG, "registerObserver: registered=" + mIsObserverRegistered);
        synchronized (mMeetingObserver) {
            if (!mIsObserverRegistered) {
                getContentResolver().registerContentObserver(mMeeting.getUri(), false, mMeetingObserver);
                mIsObserverRegistered = true;
            }
        }
    }

    private void unregisterObserver() {
        Log.v(TAG, "unregisterObserver: registered=" + mIsObserverRegistered);
        synchronized (mMeetingObserver) {
            if (mIsObserverRegistered) {
                getContentResolver().unregisterContentObserver(mMeetingObserver);
                mIsObserverRegistered = false;
            }
        }
    }

    /**
     * Update UI components based on the meeting state.
     */
    private void onMeetingChanged() {
        Log.v(TAG, "onMeetingStateChanged: meetingState = " + mMeeting.getState());
        // Show the "stop meeting" button if the meeting is not finished.
        mBtnStopMeeting.setVisibility(mMeeting.getState() == State.NOT_STARTED || mMeeting.getState() == State.IN_PROGRESS ? View.VISIBLE : View.INVISIBLE);
        // Only enable the "stop meeting" button if the meeting is in progress.
        mBtnStopMeeting.setEnabled(mMeeting.getState() == State.IN_PROGRESS);

        // Blink the chronometer when the meeting is in progress
        if (mMeeting.getState() == State.IN_PROGRESS) {
            mProgressBarHeader.setVisibility(View.VISIBLE);
        } else {
            mProgressBarHeader.setVisibility(View.INVISIBLE);
        }
        supportInvalidateOptionsMenu();
    }

    /**
     * Start the meeting. Set the state to in-progress, start the chronometer,
     * and show the "stop meeting" button.
     */
    private AsyncTask<Void, Void, Void> mStartMeetingAsyncTask = new AsyncTask<Void, Void, Void>() {

        @Override
        protected Void doInBackground(Void... params) {
            mMeeting.start();
            return null;
        }

        @Override
        protected void onPostExecute(Void params) {
            mBtnStopMeeting.setVisibility(View.VISIBLE);
            getSupportActionBar().setTitle(TextUtils.formatDateTime(MeetingActivity.this, mMeeting.getStartDate()));
            mMeetingChronometer.setBase(SystemClock.elapsedRealtime());
            mMeetingChronometer.start();
        }
    };

    /**
     * Stop the meeting. Set the state to finished, stop the chronometer, hide
     * the "stop meeting" button, persist the meeting duration, and stop the
     * chronometers for all team members who are still talking.
     */
    private AsyncTask<Void, Void, Void> mStopMeetingAsyncTask = new AsyncTask<Void, Void, Void>() {

        @Override
        protected Void doInBackground(Void... params) {
            mMeeting.stop();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mBtnStopMeeting.setVisibility(View.INVISIBLE);
            mMeetingChronometer.stop();
            // Reload the list of team members.
            MeetingFragment fragment = (MeetingFragment) getSupportFragmentManager().findFragmentById(R.id.meeting_fragment);
            fragment.loadMeeting(mMeeting.getId(), State.FINISHED, mOnClickListener);
            supportInvalidateOptionsMenu();
        }
    };

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
                mMeeting.toggleTalkingMember(memberId);
                return null;
            }
        };
        task.execute();
    };

    private AsyncTask<Void, Void, Void> mLoadMeetingAsyncTask = new AsyncTask<Void, Void, Void>() {

        @Override
        protected Void doInBackground(Void... params) {
            long meetingId = getIntent().getLongExtra(EXTRA_MEETING_ID, -1);
            if (meetingId == -1) mMeeting = Meeting.createNewMeeting(MeetingActivity.this);
            else
                mMeeting = Meeting.read(MeetingActivity.this, meetingId);
            registerObserver();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (isFinishing()) {
                Log.v(TAG, "Finishing, will not continue to load meeting.");
                return;
            }
            if (mMeeting.getState() == State.IN_PROGRESS) {
                // If the meeting is in progress, show the Chronometer.
                long timeSinceMeetingStartedMillis = System.currentTimeMillis() - mMeeting.getStartDate();
                mMeetingChronometer.setBase(SystemClock.elapsedRealtime() - timeSinceMeetingStartedMillis);
                mMeetingChronometer.start();
            } else if (mMeeting.getState() == State.FINISHED) {
                // For finished meetings, show the duration we retrieved
                // from the
                // db.
                mMeetingChronometer.setText(DateUtils.formatElapsedTime(mMeeting.getDuration()));
            }
            getSupportActionBar().setTitle(TextUtils.formatDateTime(MeetingActivity.this, mMeeting.getStartDate()));
            onMeetingChanged();

            // Load the list of team members.
            MeetingFragment fragment = (MeetingFragment) getSupportFragmentManager().findFragmentById(R.id.meeting_fragment);
            fragment.loadMeeting(mMeeting.getId(), mMeeting.getState(), mOnClickListener);
        }
    };

    private final OnClickListener mOnClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
            // Start or stop the team member talking
                case R.id.btn_start_stop_member:
                    if (mMeeting.getState() != State.IN_PROGRESS) mStartMeetingAsyncTask.execute();
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
                                        mStopMeetingAsyncTask.execute();
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

        /**
         * Called when a meeting changes.
         */
        @Override
        public void onChange(boolean selfChange) {
            Log.v(TAG, "MeetingObserver onChange, selfChange: " + selfChange);
            super.onChange(selfChange);
            mMeetingObserverAsyncTask.execute();
        }

    };
    // In a background thread, reread the meeting.
    // In the UI thread, update the Views.
    private AsyncTask<Void, Void, Void> mMeetingObserverAsyncTask = new AsyncTask<Void, Void, Void>() {

        @Override
        protected Void doInBackground(Void... params) {
            mMeeting = Meeting.read(MeetingActivity.this, mMeeting.getId());
            return null;
        }

        @Override
        protected void onPostExecute(Void params) {
            onMeetingChanged();
        }

    };
}
