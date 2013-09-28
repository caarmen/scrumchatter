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
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.dialog.ConfirmDialogFragment.DialogButtonListener;
import ca.rmen.android.scrumchatter.dialog.DialogFragmentFactory;
import ca.rmen.android.scrumchatter.meeting.Meetings;
import ca.rmen.android.scrumchatter.util.TextUtils;

import com.actionbarsherlock.app.SherlockFragmentActivity;

/**
 * Displays attributes of a meeting. Also contains a {@link MeetingFragment} which displays the list of team members participating in
 * this meeting.
 */
public class MeetingActivity extends SherlockFragmentActivity implements DialogButtonListener {

    private String TAG;

    private static final int LOADER_ID = MeetingLoaderTask.class.hashCode();
    private MeetingPagerAdapter mMeetingPagerAdapter;
    private ViewPager mViewPager;
    private long mMeetingId = -1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (TAG == null) TAG = Constants.TAG + "/" + MeetingActivity.class.getSimpleName() + "/" + System.currentTimeMillis();
        Log.v(TAG, "onCreate: savedInstanceState = " + savedInstanceState + ", intent = " + getIntent() + ", intent flags = " + getIntent().getFlags());
        setContentView(R.layout.meeting_activity);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setOnPageChangeListener(mOnPageChangeListener);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        long originalMeetingId = getIntent().getLongExtra(Meetings.EXTRA_MEETING_ID, -1);
        if (savedInstanceState != null) mMeetingId = savedInstanceState.getLong(Meetings.EXTRA_MEETING_ID);
        else
            mMeetingId = originalMeetingId;
        Bundle args = new Bundle(1);
        args.putLong(Meetings.EXTRA_MEETING_ID, mMeetingId);
        if (mMeetingId != originalMeetingId) getSupportLoaderManager().restartLoader(LOADER_ID, args, mLoaderCallbacks);
        else
            getSupportLoaderManager().initLoader(LOADER_ID, args, mLoaderCallbacks);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.v(TAG, "onSaveInstanceState, outState = " + outState + ", meetingId = " + mMeetingId);
        outState.putLong(Meetings.EXTRA_MEETING_ID, mMeetingId);
        super.onSaveInstanceState(outState);
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
        Log.v(TAG, "onOkClicked: actionId = " + actionId + ", extras = " + extras);
        if (isFinishing()) {
            Log.v(TAG, "Ignoring on click because this activity is closing.  You're either very quick or a monkey.");
            return;
        }
        // Not intuitive: instantiateItem will actually return an existing Fragment, whereas getItem() will instantiate a new Fragment.
        // We want to retrieve the existing fragment.
        MeetingFragment fragment = (MeetingFragment) mMeetingPagerAdapter.instantiateItem(mViewPager, mViewPager.getCurrentItem());
        if (actionId == R.id.action_delete_meeting) {
            getSupportLoaderManager().destroyLoader(LOADER_ID);
            fragment.deleteMeeting();
        } else if (actionId == R.id.btn_stop_meeting) fragment.stopMeeting();
    }

    @Override
    public void supportInvalidateOptionsMenu() {
        Log.v(TAG, "supportInvalidateOptionsMenu");
        mViewPager.post(new Runnable() {

            @Override
            public void run() {
                MeetingActivity.super.supportInvalidateOptionsMenu();

            }
        });
    }


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
            Log.v(TAG, "loaded meeting " + meeting);
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
        public void onLoadFinished(Loader<Meeting> loaderTask, final Meeting meeting) {
            Log.v(TAG, "onLoadFinished, meeting = " + meeting);
            if (meeting == null) {
                Log.w(TAG, "Could not load meeting, are you a monkey?");
                return;
            }
            getSupportActionBar().setTitle(TextUtils.formatDateTime(MeetingActivity.this, meeting.getStartDate()));
            new AsyncTask<MeetingPagerAdapter, Void, MeetingPagerAdapter>() {

                @Override
                protected MeetingPagerAdapter doInBackground(MeetingPagerAdapter... adapter) {
                    if (adapter[0] == null) return new MeetingPagerAdapter(MeetingActivity.this, getSupportFragmentManager());
                    else
                        return adapter[0];
                }

                @Override
                protected void onPostExecute(MeetingPagerAdapter result) {
                    if (mMeetingPagerAdapter != result) {
                        mMeetingPagerAdapter = result;
                        mViewPager.setAdapter(mMeetingPagerAdapter);
                    }
                    int position = mMeetingPagerAdapter.getPositionForMeetingId(meeting.getId());
                    mViewPager.setCurrentItem(position);
                }

            }.execute(mMeetingPagerAdapter);
        }

        @Override
        public void onLoaderReset(Loader<Meeting> meeting) {
            Log.v(TAG, "onLoaderReset: meeting = " + meeting);
        }
    };
    private OnPageChangeListener mOnPageChangeListener = new OnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
            Log.v(TAG, "onPageSelected, position = " + position);
            long meetingId = mMeetingPagerAdapter.getMeetingIdAt(position);
            if (mMeetingId != meetingId) {
                mMeetingId = meetingId;
                Bundle args = new Bundle(1);
                args.putLong(Meetings.EXTRA_MEETING_ID, mMeetingId);
                getSupportLoaderManager().restartLoader(LOADER_ID, args, mLoaderCallbacks);
            }
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

        @Override
        public void onPageScrollStateChanged(int state) {}
    };
}
