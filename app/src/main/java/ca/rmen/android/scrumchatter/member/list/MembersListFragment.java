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
package ca.rmen.android.scrumchatter.member.list;

/**
 * Displays the list of team members.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.ContextCompat;
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
import ca.rmen.android.scrumchatter.databinding.MemberListBinding;
import ca.rmen.android.scrumchatter.member.list.Members.Member;
import ca.rmen.android.scrumchatter.provider.MemberColumns;
import ca.rmen.android.scrumchatter.provider.MemberStatsColumns;
import ca.rmen.android.scrumchatter.util.Log;

public class MembersListFragment extends Fragment {

    private static final String TAG = Constants.TAG + "/" + MembersListFragment.class.getSimpleName();

    private static final int URL_LOADER = 0;
    private String mOrderByField = MemberColumns.NAME + " COLLATE NOCASE";
    private MemberListBinding mBinding;

    private MembersCursorAdapter mAdapter;
    private SharedPreferences mPrefs;
    private int mTeamId;
    private Members mMembers;


    public MembersListFragment() {
        super();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.member_list, container, false);
        mBinding.setColumnHeaderListener(new ColumnHeaderListener());
        mBinding.recyclerViewContent.empty.setText(R.string.empty_list_members);
        mBinding.recyclerViewContent.recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        return mBinding.getRoot();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mMembers = new Members((FragmentActivity) context);
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
        inflater.inflate(R.menu.members_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Create a new team member
        if (item.getItemId() == R.id.action_new_member) {
            mMembers.promptCreateMember(mTeamId);
            return true;
        }
        return false;
    }

    private final LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
            Log.v(TAG, "onCreateLoader, order by " + mOrderByField);
            String[] projection = new String[]{MemberColumns._ID, MemberColumns.NAME, MemberStatsColumns.SUM_DURATION, MemberStatsColumns.AVG_DURATION};
            String selection = MemberStatsColumns.TEAM_ID + " =? AND " + MemberColumns.DELETED + "=0 ";
            String[] selectionArgs = new String[]{String.valueOf(mTeamId)};
            return new CursorLoader(getActivity(), MemberStatsColumns.CONTENT_URI, projection, selection, selectionArgs, mOrderByField);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            Log.v(TAG, "onLoadFinished");
            if (mAdapter == null) {
                mAdapter = new MembersCursorAdapter(mMemberClickListener);
                mBinding.recyclerViewContent.recyclerView.setAdapter(mAdapter);
            }
            mBinding.recyclerViewContent.progressContainer.setVisibility(View.GONE);
            mAdapter.changeCursor(cursor);
            if (mAdapter.getItemCount() > 0) {
                mBinding.recyclerViewContent.recyclerView.setVisibility(View.VISIBLE);
                mBinding.recyclerViewContent.empty.setVisibility(View.GONE);
            } else {
                mBinding.recyclerViewContent.recyclerView.setVisibility(View.GONE);
                mBinding.recyclerViewContent.empty.setVisibility(View.VISIBLE);
            }
            getActivity().supportInvalidateOptionsMenu();
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            Log.v(TAG, "onLoaderReset");
            mAdapter.changeCursor(null);
            mBinding.recyclerViewContent.recyclerView.setVisibility(View.GONE);
            mBinding.recyclerViewContent.empty.setVisibility(View.VISIBLE);
        }

    };

    private final MembersCursorAdapter.MemberListener mMemberClickListener = new MembersCursorAdapter.MemberListener() {

        @Override
        public void onMemberEdit(Member member) {
            Log.v(TAG, "onMemberEdit: " + member);
            mMembers.promptRenameMember(mTeamId, member);

        }
        @Override
        public void onMemberDelete(Member member) {
            Log.v(TAG, "onMemberDelete: " + member);
            mMembers.confirmDeleteMember(member);
        }
    };

    /**
     * Refresh the list when the selected team changes.
     */
    private final OnSharedPreferenceChangeListener mPrefsListener = new OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            mTeamId = sharedPreferences.getInt(Constants.PREF_TEAM_ID, Constants.DEFAULT_TEAM_ID);
            getLoaderManager().restartLoader(URL_LOADER, null, mLoaderCallbacks);
        }
    };

    public class ColumnHeaderListener {
        /**
         * Resort the list of members by the given column
         *
         * @param view
         *            the header label on which the user clicked.
         */
        public void onColumnHeaderClicked(View view) {
            String oldOrderByField = mOrderByField;
            int selectedHeaderColor = ContextCompat.getColor(getActivity(), R.color.selected_header);
            int unselectedHeaderColor = ContextCompat.getColor(getActivity(), R.color.unselected_header);
            // Reset all the header text views to the default color
            mBinding.tvName.setTextColor(unselectedHeaderColor);
            mBinding.tvAvgDuration.setTextColor(unselectedHeaderColor);
            mBinding.tvSumDuration.setTextColor(unselectedHeaderColor);

            // Depending on the header column selected, change the sort order
            // field and highlight that header column.
            switch (view.getId()) {
                case R.id.tv_name:
                    mOrderByField = MemberColumns.NAME + " COLLATE NOCASE";
                    mBinding.tvName.setTextColor(selectedHeaderColor);
                    break;
                case R.id.tv_avg_duration:
                    mOrderByField = MemberStatsColumns.AVG_DURATION + " DESC, " + MemberColumns.NAME + " ASC ";
                    mBinding.tvAvgDuration.setTextColor(selectedHeaderColor);
                    break;
                case R.id.tv_sum_duration:
                    mOrderByField = MemberStatsColumns.SUM_DURATION + " DESC, " + MemberColumns.NAME + " ASC ";
                    mBinding.tvSumDuration.setTextColor(selectedHeaderColor);
                    break;
                default:
                    break;
            }
            // Re-query if needed.
            if (!oldOrderByField.equals(mOrderByField))
                getLoaderManager().restartLoader(URL_LOADER, null, mLoaderCallbacks);

        }
    }
}
