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
package ca.rmen.android.scrumchatter.meeting.detail;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import ca.rmen.android.scrumchatter.databinding.MeetingActivityBinding;
import ca.rmen.android.scrumchatter.util.Log;
import android.view.View;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.dialog.ConfirmDialogFragment.DialogButtonListener;
import ca.rmen.android.scrumchatter.dialog.DialogFragmentFactory;
import ca.rmen.android.scrumchatter.meeting.Meetings;
import ca.rmen.android.scrumchatter.util.TextUtils;


/**
 * Contains a ViewPager of {@link MeetingFragment}.
 */
public class MeetingActivity extends AppCompatActivity implements DialogButtonListener {

    private String TAG;

    private MeetingPagerAdapter mMeetingPagerAdapter;
    private MeetingActivityBinding mBinding;

    public static void startNewMeeting(Context context) {
        Intent intent = new Intent(context, MeetingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    public static void startMeeting(Context context, long meetingId) {
        Intent intent = new Intent(context, MeetingActivity.class);
        intent.putExtra(Meetings.EXTRA_MEETING_ID, meetingId);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (TAG == null) TAG = Constants.TAG + "/" + MeetingActivity.class.getSimpleName() + "/" + System.currentTimeMillis();
        Log.v(TAG, "onCreate: savedInstanceState = " + savedInstanceState + ", intent = " + getIntent() + ", intent flags = " + getIntent().getFlags());

        mBinding = DataBindingUtil.setContentView(this, R.layout.meeting_activity);
        mBinding.pager.addOnPageChangeListener(mOnPageChangeListener);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) supportActionBar.setDisplayHomeAsUpEnabled(true);

        // If this is the first time we open the activity, we will use the meeting id provided in the intent.
        // If we are recreating the activity (because of a device rotation, for example), we will display the meeting that the user 
        // had previously swiped to, using the ViewPager.
        long originalMeetingId = getIntent().getLongExtra(Meetings.EXTRA_MEETING_ID, -1);
        final long meetingId;
        if (savedInstanceState != null) meetingId = savedInstanceState.getLong(Meetings.EXTRA_MEETING_ID);
        else
            meetingId = originalMeetingId;
        Log.v(TAG, "original meeting id " + originalMeetingId + ", saved meeting id " + meetingId);

        // Perform initialization which must be done on a background thread:
        // Create a new meeting if necessary.
        // Create the pager adapter. The pager adapter constructor reads from the DB, so
        // we need to create it in a background thread.  When it's ready, we'll use it 
        // with the ViewPager, and open the ViewPager to the correct meeting.
        new AsyncTask<Void, Void, MeetingPagerAdapter>() {
            private long mMeetingId;

            @Override
            protected MeetingPagerAdapter doInBackground(Void... param) {
                if (meetingId < 0) {
                    Meeting newMeeting = Meeting.createNewMeeting(MeetingActivity.this);
                    if (newMeeting != null) mMeetingId = newMeeting.getId();
                } else {
                    mMeetingId = meetingId;
                }
                int teamId = PreferenceManager.getDefaultSharedPreferences(MeetingActivity.this).getInt(Constants.PREF_TEAM_ID, Constants.DEFAULT_TEAM_ID);
                return new MeetingPagerAdapter(MeetingActivity.this, teamId, getSupportFragmentManager());
            }

            @Override
            protected void onPostExecute(MeetingPagerAdapter result) {
                mMeetingPagerAdapter = result;
                mBinding.activityLoading.setVisibility(View.GONE);
                mBinding.pager.setAdapter(mMeetingPagerAdapter);
                if (mMeetingId >= 0) {
                    int position = mMeetingPagerAdapter.getPositionForMeetingId(mMeetingId);
                    Log.v(TAG, "meeting " + mMeetingId + " is on page " + position);
                    mBinding.pager.setCurrentItem(position);
                }
            }
        }.execute();
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy");
        super.onDestroy();
        if (mMeetingPagerAdapter != null) mMeetingPagerAdapter.destroy();
        mBinding.pager.removeOnPageChangeListener(mOnPageChangeListener);
    }

    /**
     * Save the id of the meeting which is currently visible.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.v(TAG, "onSaveInstanceState, outState = " + outState);
        if (mMeetingPagerAdapter != null) {
            Meeting meeting = mMeetingPagerAdapter.getMeetingAt(mBinding.pager.getCurrentItem());
            outState.putLong(Meetings.EXTRA_MEETING_ID, meeting.getId());
        }
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
        MeetingFragment fragment = (MeetingFragment) mMeetingPagerAdapter.instantiateItem(mBinding.pager, mBinding.pager.getCurrentItem());
        if (actionId == R.id.action_delete_meeting) {
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
        mBinding.pager.post(new Runnable() {

            @Override
            public void run() {
                MeetingActivity.super.supportInvalidateOptionsMenu();
            }
        });
    }

    /**
     * When the user selects a meeting by swiping left or right, we need to load the data
     * from the meeting, to update the title in the action bar.
     */
    private final OnPageChangeListener mOnPageChangeListener = new OnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
            Log.v(TAG, "onPageSelected, position = " + position);
            Meeting meeting = mMeetingPagerAdapter.getMeetingAt(position);
            Log.v(TAG, "Selected meeting " + meeting);
            ActionBar supportActionBar = getSupportActionBar();
            if (supportActionBar != null) supportActionBar.setTitle(TextUtils.formatDateTime(MeetingActivity.this, meeting.getStartDate()));
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

        @Override
        public void onPageScrollStateChanged(int state) {}
    };
}
