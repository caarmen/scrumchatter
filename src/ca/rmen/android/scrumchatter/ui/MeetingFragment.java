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
package ca.rmen.android.scrumchatter.ui;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
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
import ca.rmen.android.scrumchatter.adapter.MeetingCursorAdapter;
import ca.rmen.android.scrumchatter.provider.MeetingColumns;
import ca.rmen.android.scrumchatter.provider.MeetingColumns.State;
import ca.rmen.android.scrumchatter.provider.MeetingMemberColumns;
import ca.rmen.android.scrumchatter.provider.MemberColumns;

import com.actionbarsherlock.app.SherlockListFragment;

/**
 * Displays the list of members participating in a particular meeting.
 */
public class MeetingFragment extends SherlockListFragment {

    private static final String TAG = Constants.TAG + "/" + MeetingFragment.class.getSimpleName();

    private static final int LOADER_ID = 0;
    private static final String EXTRA_MEETING_STATE = MeetingFragment.class.getPackage().getName() + ".meeting_state";
    private long mMeetingId;

    private MeetingCursorAdapter mAdapter;

    public MeetingFragment() {
        super();
        mMeetingId = -1;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.meeting_fragment, null);
        return view;
    }

    /**
     * Set up this fragment to load the data for a particular meeting.
     * 
     * @param meetingId
     * @param onClickListener
     *            This will be forwarded to the adapter, so clicks on views in
     *            the list will be managed by this listener.
     */
    public void loadMeeting(long meetingId, State state, OnClickListener onClickListener) {
        Log.v(TAG, "loadMeeting");
        mMeetingId = meetingId;
        Bundle bundle = new Bundle(1);
        bundle.putInt(EXTRA_MEETING_STATE, state.ordinal());
        if (mAdapter == null) {
            mAdapter = new MeetingCursorAdapter(getActivity(), onClickListener);
            getLoaderManager().initLoader(LOADER_ID, bundle, mLoaderCallbacks);
        } else {
            getLoaderManager().restartLoader(LOADER_ID, bundle, mLoaderCallbacks);
        }
    }

    private LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
            Log.v(TAG, "onCreateLoader, loaderId = " + loaderId + ", bundle = " + bundle);
            State meetingState = State.values()[bundle.getInt(EXTRA_MEETING_STATE, State.NOT_STARTED.ordinal())];
            String selection = null;
            String orderBy = MemberColumns.NAME;
            if (meetingState == State.FINISHED) {
                selection = MeetingMemberColumns.TABLE_NAME + "." + MeetingMemberColumns.DURATION + ">0";
                orderBy = MeetingMemberColumns.TABLE_NAME + "." + MeetingMemberColumns.DURATION + " DESC";
            }
            String[] projection = new String[] { MemberColumns.TABLE_NAME + "." + MemberColumns._ID, MemberColumns.NAME,
                    MeetingMemberColumns.TABLE_NAME + "." + MeetingMemberColumns.DURATION, MeetingColumns.STATE, MeetingMemberColumns.TALK_START_TIME };

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
}
