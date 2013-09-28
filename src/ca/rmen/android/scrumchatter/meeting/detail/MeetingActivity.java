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
 * Contains a ViewPager of {@link MeetingFragment}.
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

        // If this is the first time we open the activity, we will use the meeting id provided in the intent.
        // If we are recreating the activity (because of a device rotation, for example), we will display the meeting that the user 
        // had previously swiped to, using the ViewPager.
        long originalMeetingId = getIntent().getLongExtra(Meetings.EXTRA_MEETING_ID, -1);
        if (savedInstanceState != null) mMeetingId = savedInstanceState.getLong(Meetings.EXTRA_MEETING_ID);
        else
            mMeetingId = originalMeetingId;
        Bundle args = new Bundle(1);
        args.putLong(Meetings.EXTRA_MEETING_ID, mMeetingId);
        // The first time we open the activity, we will initialize the loader
        if (mMeetingId == originalMeetingId) getSupportLoaderManager().initLoader(LOADER_ID, args, mLoaderCallbacks);
        // If we had previously swiped to a different meeting, restart the loader with the new meeting id.
        else
            getSupportLoaderManager().restartLoader(LOADER_ID, args, mLoaderCallbacks);
    }

    /**
     * Save the id of the meeting which is currently visible.
     */
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
        // Not intuitive: instantiateItem will actually return an existing Fragment, whereas getItem() will always instantiate a new Fragment.
        // We want to retrieve the existing fragment.
        MeetingFragment fragment = (MeetingFragment) mMeetingPagerAdapter.instantiateItem(mViewPager, mViewPager.getCurrentItem());
        if (actionId == R.id.action_delete_meeting) {
            getSupportLoaderManager().destroyLoader(LOADER_ID);
            fragment.deleteMeeting();
        } else if (actionId == R.id.btn_stop_meeting) {
            fragment.stopMeeting();
        }
    }

    /**
     * Workaround for bug where the action icons disappear when rotating the device.
     * https://code.google.com/p/android/issues/detail?can=2&start=0&num=100&q=&colspec=ID%20Type%20Status%20Owner%20Summary%20Stars&groupby=&sort=&id=29472
     */
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
            // Create the pager adapter if we haven't already. The pager adapter constructor reads from the DB, so
            // we need to create it in a background thread.  When it's ready, we'll use it 
            // with the ViewPager, and open the ViewPager to the correct meeting.
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

    /**
     * When the user selects a meeting by swiping left or right, we need to load the data
     * from the meeting, to update the title in the action bar.
     */
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
