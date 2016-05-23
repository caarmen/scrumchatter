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
import android.database.ContentObserver;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.app.NavUtils;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateUtils;

import ca.rmen.android.scrumchatter.chart.MeetingChartActivity;
import ca.rmen.android.scrumchatter.databinding.MeetingFragmentBinding;
import ca.rmen.android.scrumchatter.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.dialog.DialogFragmentFactory;
import ca.rmen.android.scrumchatter.meeting.Meetings;
import ca.rmen.android.scrumchatter.provider.MeetingColumns;
import ca.rmen.android.scrumchatter.provider.MeetingColumns.State;
import ca.rmen.android.scrumchatter.provider.MeetingMemberColumns;
import ca.rmen.android.scrumchatter.provider.MemberColumns;

/**
 * Displays info about a meeting (the duration) as well as the list of members participating in a particular meeting.
 */
public class MeetingFragment extends ListFragment {

    private String TAG = Constants.TAG + "/" + MeetingFragment.class.getSimpleName() + "/" + System.currentTimeMillis();

    private MeetingCursorAdapter mAdapter;
    private final MeetingObserver mMeetingObserver;
    private Meeting mMeeting;
    private long mMeetingId;
    private Meetings mMeetings;
    private MeetingFragmentBinding mBinding;

    public MeetingFragment() {
        super();
        Log.v(TAG, "Constructor");
        mMeetingObserver = new MeetingObserver(new Handler());
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        mMeetings = new Meetings((FragmentActivity) activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView: savedInstanceState = " + savedInstanceState);
        // Create our views
        mBinding = DataBindingUtil.inflate(inflater, R.layout.meeting_fragment, container, false);
        mBinding.btnStopMeeting.setOnClickListener(mOnClickListener);
        mMeetingId = getArguments().getLong(Meetings.EXTRA_MEETING_ID);
        if (!TAG.endsWith("" + mMeetingId)) TAG += "/" + mMeetingId;

        // Load the meeting and register for DB changes on the meeting
        Uri uri = Uri.withAppendedPath(MeetingColumns.CONTENT_URI, String.valueOf(mMeetingId));
        getActivity().getContentResolver().registerContentObserver(uri, false, mMeetingObserver);
        loadMeeting();
        return mBinding.getRoot();
    }

    @Override
    public void onDestroyView() {
        Log.v(TAG, "onDestroyView");
        getActivity().getContentResolver().unregisterContentObserver(mMeetingObserver);
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.v(TAG, "onCreateOptionsMenu: mMeeting =" + mMeeting);

        inflater.inflate(R.menu.meeting_menu, menu);
        // Only share and show charts for finished meetings
        final MenuItem shareItem = menu.findItem(R.id.action_share);
        shareItem.setVisible(mMeeting != null && mMeeting.getState() == State.FINISHED);
        final MenuItem chartItem = menu.findItem(R.id.action_charts);
        chartItem.setVisible(mMeeting != null && mMeeting.getState() == State.FINISHED);
        // Delete a meeting in any state.
        final MenuItem deleteItem = menu.findItem(R.id.action_delete_meeting);
        deleteItem.setVisible(mMeeting != null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.v(TAG, "onOptionsItemSelected: item = " + item.getItemId() + ": " + item.getTitle());
        if (getActivity().isFinishing()) {
            Log.w(TAG, "User clicked on a menu item while the activity is finishing.  Surely a monkey is involved");
            return true;
        }
        switch (item.getItemId()) {
        // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(getActivity());
                return true;
            case R.id.action_share:
                mMeetings.export(mMeeting.getId());
                return true;
            case R.id.action_charts:
                MeetingChartActivity.start(getContext(), mMeeting.getId());
                return true;
            case R.id.action_delete_meeting:
                mMeetings.confirmDelete(mMeeting);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     * As soon as this fragment is visible and we have loaded the meeting, let's update the action bar icons.
     */
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        Log.v(TAG, "setUserVisibleHint: " + isVisibleToUser);
        super.setUserVisibleHint(isVisibleToUser);
        setHasOptionsMenu(isVisibleToUser && mMeeting != null);
    }

    /**
     * Read the given meeting in the background. Init or restart the loader for the meeting members. Update the views for the meeting.
     */
    private void loadMeeting() {
        Log.v(TAG, "loadMeeting: current meeting = " + mMeeting);
        Context context = getActivity();
        if (context == null) {
            Log.w(TAG, "loadMeeting called when we are no longer attached to the activity. A monkey might be involved");
            return;
        }
        State meetingState = mMeeting == null ? (State) getArguments().getSerializable(Meetings.EXTRA_MEETING_STATE) : mMeeting.getState();
        Bundle bundle = new Bundle(1);
        bundle.putSerializable(Meetings.EXTRA_MEETING_STATE, meetingState);
        if (mAdapter == null) {
            mAdapter = new MeetingCursorAdapter(context, mOnClickListener);
            getLoaderManager().initLoader((int) mMeetingId, bundle, mLoaderCallbacks);
        } else {
            getLoaderManager().restartLoader((int) mMeetingId, bundle, mLoaderCallbacks);
        }

        AsyncTask<Long, Void, Meeting> task = new AsyncTask<Long, Void, Meeting>() {

            @Override
            protected Meeting doInBackground(Long... params) {
                long meetingId = params[0];
                Log.v(TAG, "doInBackground: meetingId = " + meetingId);

                FragmentActivity activity = getActivity();
                if (activity == null) {
                    Log.w(TAG, "No longer attached to activity: can't load meeting");
                    cancel(false);
                    return null;
                }
                return Meeting.read(activity, meetingId);
            }

            @Override
            protected void onPostExecute(Meeting meeting) {
                Log.v(TAG, "onPostExecute: meeting = " + meeting);
                // Don't do anything if the activity has been closed in the meantime
                AppCompatActivity activity = (AppCompatActivity) getActivity();
                if (activity == null) {
                    Log.w(TAG, "No longer attached to the activity: can't load meeting members");
                    return;
                }
                // Load the meeting member list
                if (meeting == null) {
                    Log.w(TAG, "Meeting was deleted, cannot load meeting");
                    activity.getContentResolver().unregisterContentObserver(mMeetingObserver);
                    return;
                }

                // We just finished loading the meeting for the first time.
                boolean firstLoad = mMeeting == null;
                mMeeting = meeting;
                if (getUserVisibleHint()) {
                    if (firstLoad) setHasOptionsMenu(true);
                    else
                        activity.supportInvalidateOptionsMenu();
                }
                // Update the UI views
                Log.v(TAG, "meetingState = " + meeting.getState());
                // Show the "stop meeting" button if the meeting is not finished.
                mBinding.btnStopMeeting.setVisibility(meeting.getState() == State.NOT_STARTED || meeting.getState() == State.IN_PROGRESS ? View.VISIBLE
                        : View.INVISIBLE);
                // Only enable the "stop meeting" button if the meeting is in progress.
                mBinding.btnStopMeeting.setEnabled(meeting.getState() == State.IN_PROGRESS);

                // Show the horizontal progress bar for in progress meetings
                mBinding.headerProgressBar.setVisibility(meeting.getState() == State.IN_PROGRESS ? View.VISIBLE : View.INVISIBLE);

                // Update the chronometer
                if (meeting.getState() == State.IN_PROGRESS) {
                    // If the meeting is in progress, show the Chronometer.
                    long timeSinceMeetingStartedMillis = System.currentTimeMillis() - meeting.getStartDate();
                    mBinding.tvMeetingDuration.setBase(SystemClock.elapsedRealtime() - timeSinceMeetingStartedMillis);
                    mBinding.tvMeetingDuration.start();
                } else if (meeting.getState() == State.FINISHED) {
                    // For finished meetings, show the duration we retrieved from the db.
                    mBinding.tvMeetingDuration.stop();
                    mBinding.tvMeetingDuration.setText(DateUtils.formatElapsedTime(meeting.getDuration()));
                }
            }
        };
        task.execute(mMeetingId);
    }

    /**
     * Stop the meeting. Set the state to finished, stop the chronometer, hide the "stop meeting" button, persist the meeting duration, and stop the
     * chronometers for all team members who are still talking.
     */
    void stopMeeting() {
        AsyncTask<Meeting, Void, Void> task = new AsyncTask<Meeting, Void, Void>() {

            @Override
            protected Void doInBackground(Meeting... meeting) {
                meeting[0].stop();
                return null;
            }
        };
        task.execute(mMeeting);
    }

    /**
     * Delete the current meeting, and close the activity, to return to the list of meetings.
     */
    void deleteMeeting() {
        AsyncTask<Meeting, Void, Void> task = new AsyncTask<Meeting, Void, Void>() {

            @Override
            protected void onPreExecute() {
                mBinding.btnStopMeeting.setVisibility(View.INVISIBLE);
                getActivity().getContentResolver().unregisterContentObserver(mMeetingObserver);
            }

            @Override
            protected Void doInBackground(Meeting... meeting) {
                meeting[0].delete();
                return null;
            }

            @Override
            protected void onPostExecute(Void params) {
                // TODO might be better for the activity to finish itself instead.
                FragmentActivity activity = getActivity();
                if (activity != null) activity.finish();
            }
        };
        task.execute(mMeeting);
    }

    /**
     * Cursor on the MeetingMember table
     */
    private final LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
            Log.v(TAG, "onCreateLoader, loaderId = " + loaderId + ", bundle = " + bundle);
            State meetingState = (State) bundle.getSerializable(Meetings.EXTRA_MEETING_STATE);
            String selection = null;
            String orderBy = MemberColumns.NAME + " COLLATE NOCASE";
            // For finished meetings, show the member who spoke the most first.
            // For meetings in progress (or not started), sort alphabetically.
            if (meetingState == State.FINISHED) {
                selection = MeetingMemberColumns.DURATION + ">0";
                orderBy = MeetingMemberColumns.DURATION + " DESC";
            }
            String[] projection = new String[] { MeetingMemberColumns._ID, MeetingMemberColumns.MEMBER_ID, MemberColumns.NAME, MeetingMemberColumns.DURATION, MeetingColumns.STATE,
                    MeetingMemberColumns.TALK_START_TIME };

            Uri uri = Uri.withAppendedPath(MeetingMemberColumns.CONTENT_URI, String.valueOf(loaderId));
            return new CursorLoader(getActivity(), uri, projection, selection, null, orderBy);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            Log.v(TAG, "onLoadFinished");
            if (getListAdapter() == null) {
                setListAdapter(mAdapter);
                mBinding.listContent.progressContainer.setVisibility(View.GONE);
            }
            mAdapter.changeCursor(cursor);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            Log.v(TAG, "onLoaderReset");
            mAdapter.changeCursor(null);
        }
    };

    /**
     * Observer on the Meeting table. When a meeting changes, we reload the meeting data itself as well as the
     * list of members for this meeting. The data on the meeting itself will impact how we display the list
     * of members.
     */
    private class MeetingObserver extends ContentObserver {

        private final String TAG = MeetingFragment.this.TAG + "/" + MeetingObserver.class.getSimpleName();

        public MeetingObserver(Handler handler) {
            super(handler);
            Log.v(TAG, "Constructor");
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.v(TAG, "MeetingObserver onChange, selfChange: " + selfChange + ", mMeeting = " + mMeetingId);
            super.onChange(selfChange);
            loadMeeting();
        }
    }

    /**
     * Manage clicks on items inside the meeting fragment.
     */
    private final OnClickListener mOnClickListener = new OnClickListener() {

        /**
         * Switch a member from the talking to non-talking state:
         * 
         * If they were talking, they will no longer be talking, and their button will go back to a "start" button.
         * If they were not talking, they will start talking, and their button will be a "stop" button.
         */
        private void toggleTalkingMember(final long memberId) {
            Log.v(TAG, "toggleTalkingMember " + memberId);
            AsyncTask<Meeting, Void, Void> task = new AsyncTask<Meeting, Void, Void>() {

                @Override
                protected Void doInBackground(Meeting... meeting) {
                    if (meeting[0].getState() != State.IN_PROGRESS) meeting[0].start();
                    meeting[0].toggleTalkingMember(memberId);
                    return null;
                }
            };
            task.execute(mMeeting);
        }

        @Override
        public void onClick(View v) {
            Log.v(TAG, "onClick, view: " + v);
            switch (v.getId()) {
            // Start or stop the team member talking
                case R.id.btn_start_stop_member:
                    long memberId = (Long) v.getTag();
                    toggleTalkingMember(memberId);
                    break;
                // Stop the whole meeting.
                case R.id.btn_stop_meeting:
                    // Let's ask him if he's sure.
                    DialogFragmentFactory.showConfirmDialog(getActivity(), getString(R.string.action_stop_meeting), getString(R.string.dialog_confirm),
                            R.id.btn_stop_meeting, null);
                    break;
            }
        }
    };
}
