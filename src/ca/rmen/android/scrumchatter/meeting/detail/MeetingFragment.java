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
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.provider.MeetingColumns;
import ca.rmen.android.scrumchatter.provider.MeetingColumns.State;
import ca.rmen.android.scrumchatter.provider.MeetingMemberColumns;
import ca.rmen.android.scrumchatter.provider.MemberColumns;

import com.actionbarsherlock.app.SherlockListFragment;

/**
 * Displays the list of members participating in a particular meeting.
 */
public class MeetingFragment extends SherlockListFragment { // NO_UCD (use default)

    private static final String TAG = Constants.TAG + "/" + MeetingFragment.class.getSimpleName();

    private static final int LOADER_ID = 0;
    private static final String EXTRA_MEETING_STATE = MeetingFragment.class.getPackage().getName() + ".meeting_state";
    private static final String EXTRA_MEETING_ID = MeetingFragment.class.getPackage().getName() + ".meeting_id";
    private long mMeetingId;

    private MeetingCursorAdapter mAdapter;
    private final MeetingObserver mMeetingObserver;

    public MeetingFragment() {
        super();
        Log.v(TAG, "Constructor");
        mMeetingId = -1;
        mMeetingObserver = new MeetingObserver(new Handler());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView: savedInstanceState = " + savedInstanceState);
        View view = inflater.inflate(R.layout.meeting_fragment, null);
        if (savedInstanceState != null) {
            mMeetingId = savedInstanceState.getLong(EXTRA_MEETING_ID);
            Uri uri = Uri.withAppendedPath(MeetingColumns.CONTENT_URI, String.valueOf(mMeetingId));
            getActivity().getContentResolver().registerContentObserver(uri, false, mMeetingObserver);
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        Log.v(TAG, "onDestroyView");
        getActivity().getContentResolver().unregisterContentObserver(mMeetingObserver);
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.v(TAG, "onSaveInstanceState: outState = " + outState);
        super.onSaveInstanceState(outState);
        outState.putLong(EXTRA_MEETING_ID, mMeetingId);
    }

    void loadMeeting(long meetingId) {
        Log.v(TAG, "loadMeeting: current meeting id = " + mMeetingId + ", new meeting id = " + meetingId);
        if (meetingId != mMeetingId) {
            mMeetingId = meetingId;
            Uri uri = Uri.withAppendedPath(MeetingColumns.CONTENT_URI, String.valueOf(mMeetingId));
            getActivity().getContentResolver().unregisterContentObserver(mMeetingObserver);
            getActivity().getContentResolver().registerContentObserver(uri, false, mMeetingObserver);
        }

        AsyncTask<Long, Void, State> task = new AsyncTask<Long, Void, MeetingColumns.State>() {

            @Override
            protected State doInBackground(Long... params) {
                long meetingId = params[0];
                Log.v(TAG, "doInBackground: meetingId = " + meetingId);
                Meeting meeting = Meeting.read(getActivity(), meetingId);
                if (meeting == null) {
                    Log.v(TAG, "Meeting was deleted");
                    return State.FINISHED;
                }
                return meeting.getState();
            }

            @Override
            protected void onPostExecute(State state) {
                Log.v(TAG, "onPostExecute: state = " + state);
                Bundle bundle = new Bundle(1);
                bundle.putInt(EXTRA_MEETING_STATE, state.ordinal());
                if (mAdapter == null) {
                    mAdapter = new MeetingCursorAdapter(getActivity(), mOnClickListener);
                    getLoaderManager().initLoader(LOADER_ID, bundle, mLoaderCallbacks);
                } else {
                    getLoaderManager().restartLoader(LOADER_ID, bundle, mLoaderCallbacks);
                }
            }
        };
        task.execute(mMeetingId);
    }


    private LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
            Log.v(TAG, "onCreateLoader, loaderId = " + loaderId + ", bundle = " + bundle);
            State meetingState = State.values()[bundle.getInt(EXTRA_MEETING_STATE, State.NOT_STARTED.ordinal())];
            String selection = null;
            String orderBy = MemberColumns.NAME + " COLLATE NOCASE";
            if (meetingState == State.FINISHED) {
                selection = MeetingMemberColumns.DURATION + ">0";
                orderBy = MeetingMemberColumns.DURATION + " DESC";
            }
            String[] projection = new String[] { MeetingMemberColumns._ID, MemberColumns.NAME, MeetingMemberColumns.DURATION, MeetingColumns.STATE,
                    MeetingMemberColumns.TALK_START_TIME };

            Uri uri = Uri.withAppendedPath(MeetingMemberColumns.CONTENT_URI, String.valueOf(mMeetingId));
            CursorLoader loader = new CursorLoader(getActivity(), uri, projection, selection, null, orderBy);
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            Log.v(TAG, "onLoadFinished");
            if (getListAdapter() == null) {
                setListAdapter(mAdapter);
                getActivity().findViewById(R.id.progressContainer).setVisibility(View.GONE);
            }
            mAdapter.changeCursor(cursor);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            Log.v(TAG, "onLoaderReset");
            mAdapter.changeCursor(null);
        }
    };

    private class MeetingObserver extends ContentObserver {

        private final String TAG = MeetingFragment.TAG + "/" + MeetingObserver.class.getSimpleName();

        public MeetingObserver(Handler handler) {
            super(handler);
            Log.v(TAG, "Constructor");
        }

        /**
         * Called when a meeting changes. Reload the list of members for this meeting.
         */
        @Override
        public void onChange(boolean selfChange) {
            Log.v(TAG, "MeetingObserver onChange, selfChange: " + selfChange + ", mMeetingId = " + mMeetingId);
            super.onChange(selfChange);
            loadMeeting(mMeetingId);
        }
    };

    /**
     * Manage clicks on items inside the meeting fragment.
     */
    private final OnClickListener mOnClickListener = new OnClickListener() {

        /**
         * Switch a member from the talking to non-talking state:
         * 
         * If they were talking, they will no longer be talking, and their button will go back to a "start" button.
         * 
         * If they were not talking, they will start talking, and their button will be a "stop" button.
         * 
         * @param memberId
         */
        private void toggleTalkingMember(final long memberId) {
            Log.v(TAG, "toggleTalkingMember " + memberId);
            AsyncTask<Long, Void, Void> task = new AsyncTask<Long, Void, Void>() {

                @Override
                protected Void doInBackground(Long... meetingId) {
                    Context context = getActivity();
                    Meeting meeting = Meeting.read(context, meetingId[0]);
                    if (meeting.getState() != State.IN_PROGRESS) meeting.start();
                    meeting.toggleTalkingMember(memberId);
                    return null;
                }
            };
            task.execute(mMeetingId);
        };

        @Override
        public void onClick(View v) {
            Log.v(TAG, "onClick, view: " + v);
            switch (v.getId()) {
            // Start or stop the team member talking
                case R.id.btn_start_stop_member:
                    long memberId = (Long) v.getTag();
                    toggleTalkingMember(memberId);
                    break;
            }
        }
    };
}
