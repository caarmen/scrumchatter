/**
 * Copyright 2013-2016 Carmen Alvarez
 * <p/>
 * This file is part of Scrum Chatter.
 * <p/>
 * Scrum Chatter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * Scrum Chatter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with Scrum Chatter. If not, see <http://www.gnu.org/licenses/>.
 */
package ca.rmen.android.scrumchatter.meeting.list;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.databinding.MeetingsBinding;
import ca.rmen.android.scrumchatter.dialog.DialogFragmentFactory;
import ca.rmen.android.scrumchatter.meeting.Meetings;
import ca.rmen.android.scrumchatter.meeting.detail.Meeting;
import ca.rmen.android.scrumchatter.meeting.detail.MeetingActivity;
import ca.rmen.android.scrumchatter.meeting.detail.MeetingFragment;
import ca.rmen.android.scrumchatter.provider.MeetingColumns;
import ca.rmen.android.scrumchatter.util.Log;

/**
 * Displays the list of meetings that have taken place.
 */
public class MeetingsListFragment extends Fragment {
    private static final String TAG = Constants.TAG + "/" + MeetingsListFragment.class.getSimpleName();
    private static final int URL_LOADER = 0;

    private MeetingsCursorAdapter mAdapter;
    private SharedPreferences mPrefs;
    private Meetings mMeetings;
    private int mTeamId;
    private MeetingsBinding mBinding;

    public MeetingsListFragment() {
        super();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.meetings, container, false);
        mBinding.meetingList.recyclerViewContent.empty.setText(R.string.empty_list_meetings);
        mBinding.meetingList.recyclerViewContent.recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        return mBinding.getRoot();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // No way around this cast to FragmentActivity
        mMeetings = new Meetings((FragmentActivity) context);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mPrefs.registerOnSharedPreferenceChangeListener(mPrefsListener);
        mTeamId = mPrefs.getInt(Constants.PREF_TEAM_ID, Constants.DEFAULT_TEAM_ID);
        getLoaderManager().initLoader(URL_LOADER, null, mLoaderCallbacks);
    }

    @Override
    public void onDetach() {
        mPrefs.unregisterOnSharedPreferenceChangeListener(mPrefsListener);
        super.onDetach();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.meetings_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean hasMeetings = mAdapter != null && mAdapter.getItemCount() > 0;
        menu.findItem(R.id.action_share).setVisible(hasMeetings);
        menu.findItem(R.id.action_charts).setVisible(hasMeetings);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Start a new meeting.
        // Check if we have any members first.  A meeting with no members is not much fun.
        if (item.getItemId() == R.id.action_new_meeting) {
            mMeetings.createMeeting(mTeamId, new Meetings.MeetingCreationCallback() {
                @Override
                public void onMeetingCreated(Meeting meeting) {
                    if (meeting != null) {
                        mMeetingListener.onMeetingOpen(meeting);
                    } else {
                        DialogFragmentFactory.showInfoDialog(getActivity(), R.string.dialog_error_title_one_member_required,
                                R.string.dialog_error_message_one_member_required);
                    }

                }
            });
            return true;
        }
        super.onOptionsItemSelected(item);
        return false;
    }

    private final LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
            Log.v(TAG, "onCreateLoader, loaderId = " + loaderId + ", bundle = " + bundle);
            String selection = MeetingColumns.TEAM_ID + "=?";
            String[] selectionArgs = new String[]{String.valueOf(mTeamId)};
            return new CursorLoader(getActivity(), MeetingColumns.CONTENT_URI, null, selection, selectionArgs, MeetingColumns.MEETING_DATE
                    + " DESC");
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            Log.v(TAG, "onLoadFinished, loader = " + loader + ", cursor = " + cursor);
            if (mAdapter == null) {
                mAdapter = new MeetingsCursorAdapter(getActivity(), mMeetingListener);
                mBinding.meetingList.recyclerViewContent.recyclerView.setAdapter(mAdapter);
            }
            mBinding.meetingList.recyclerViewContent.progressContainer.setVisibility(View.GONE);
            mAdapter.changeCursor(cursor);
            if (mAdapter.getItemCount() > 0) {
                mBinding.meetingList.recyclerViewContent.recyclerView.setVisibility(View.VISIBLE);
                mBinding.meetingList.recyclerViewContent.empty.setVisibility(View.GONE);
            } else {
                mBinding.meetingList.recyclerViewContent.recyclerView.setVisibility(View.GONE);
                mBinding.meetingList.recyclerViewContent.empty.setVisibility(View.VISIBLE);
            }
            if (mBinding.meetingFragmentPlaceholder != null) {
                autoSelectMeeting();
            }
            getActivity().supportInvalidateOptionsMenu();
        }

        private void autoSelectMeeting() {
            final MeetingsCursorAdapter adapter
                    = (MeetingsCursorAdapter) mBinding.meetingList.recyclerViewContent.recyclerView.getAdapter();

            if (adapter.getItemCount() > 0) {
                MeetingFragment meetingFragment = MeetingFragment.lookupMeetingFragment(getFragmentManager());
                final int positionToSelect;
                // No meeting selected yet: select the first one
                if (adapter.getSelectedPosition() < 0) {
                    positionToSelect = 0;
                }
                // A meeting out of bounds is selected: select the last one
                // Ex: the user deleted the last meeting
                else if (adapter.getSelectedPosition() >= adapter.getItemCount()) {
                    positionToSelect = adapter.getItemCount() - 1;
                }
                // Keep the current selected position, but reopen the meeting
                // Ex: the user deleted a meeting in the middle. We will select the previous meeting
                else if (meetingFragment.getMeetingId() != adapter.getItemId(adapter.getSelectedPosition())){
                    positionToSelect = adapter.getSelectedPosition();
                }
                // Keep the current selected position and don't reopen the meeting
                // Ex: the user stopped the meeting.  No need to reopen it.
                else {
                    positionToSelect = -1;
                }
                if (positionToSelect >= 0) {
                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            adapter.selectItem(positionToSelect);
                        }
                    });
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            Log.v(TAG, "onLoaderReset " + loader);
            if (mAdapter != null) mAdapter.changeCursor(null);
            mBinding.meetingList.recyclerViewContent.recyclerView.setVisibility(View.GONE);
            mBinding.meetingList.recyclerViewContent.empty.setVisibility(View.VISIBLE);
        }
    };

    private final MeetingsCursorAdapter.MeetingListener mMeetingListener = new MeetingsCursorAdapter.MeetingListener() {

        @Override
        public void onMeetingOpen(Meeting meeting) {

            if (mBinding.meetingFragmentPlaceholder != null) {
                MeetingFragment.startMeeting(getFragmentManager(), meeting.getId());
            } else {
                MeetingActivity.startMeeting(getActivity(), meeting.getId());
            }
        }

        @Override
        public void onMeetingDelete(Meeting meeting) {
            mMeetings.confirmDelete(meeting);
        }

    };

    private final OnSharedPreferenceChangeListener mPrefsListener = new OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            mTeamId = sharedPreferences.getInt(Constants.PREF_TEAM_ID, Constants.DEFAULT_TEAM_ID);
            getLoaderManager().restartLoader(URL_LOADER, null, mLoaderCallbacks);
        }
    };
}
