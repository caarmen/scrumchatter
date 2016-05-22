/**
 * Copyright 2013 Carmen Alvarez
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
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ListView;

import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.databinding.MemberListBinding;
import ca.rmen.android.scrumchatter.databinding.MemberListItemBinding;
import ca.rmen.android.scrumchatter.member.list.Members.Member;
import ca.rmen.android.scrumchatter.provider.MemberColumns;
import ca.rmen.android.scrumchatter.provider.MemberStatsColumns;
import ca.rmen.android.scrumchatter.util.Log;

public class MembersListFragment extends ListFragment {

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
        mBinding.tvName.setOnClickListener(mOnClickListener);
        mBinding.tvAvgDuration.setOnClickListener(mOnClickListener);
        mBinding.tvSumDuration.setOnClickListener(mOnClickListener);
        mBinding.listContent.empty.setText(R.string.empty_list_members);
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

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        MemberListItemBinding binding = (MemberListItemBinding) v.getTag();
        mMembers.promptRenameMember(mTeamId, id, binding.tvName.getText().toString());
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
                mAdapter = new MembersCursorAdapter(getActivity(), mOnClickListener);
                setListAdapter(mAdapter);
            }
            mBinding.listContent.progressContainer.setVisibility(View.GONE);
            mAdapter.changeCursor(cursor);
            getActivity().supportInvalidateOptionsMenu();
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            Log.v(TAG, "onLoaderReset");
            mAdapter.changeCursor(null);
        }

    };

    private final OnClickListener mOnClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            Log.v(TAG, "onClick: " + v.getId());
            switch (v.getId()) {
                // The user wants to delete a team member.
                case R.id.btn_delete_member:
                    if (v.getTag() instanceof Member) {
                        final Member member = (Member) v.getTag();
                        mMembers.confirmDeleteMember(member);
                    }
                    break;
                case R.id.tv_name:
                case R.id.tv_avg_duration:
                case R.id.tv_sum_duration:
                    setSortField(v.getId());
                    break;
                default:
                    break;
            }
        }

        /**
         * Resort the list of members by the given column
         *
         * @param viewId
         *            the header label on which the user clicked.
         */
        private void setSortField(int viewId) {
            String oldOrderByField = mOrderByField;
            int selectedHeaderColor = ContextCompat.getColor(getActivity(), R.color.selected_header);
            int unselectedHeaderColor = ContextCompat.getColor(getActivity(), R.color.unselected_header);
            // Reset all the header text views to the default color
            mBinding.tvName.setTextColor(unselectedHeaderColor);
            mBinding.tvAvgDuration.setTextColor(unselectedHeaderColor);
            mBinding.tvSumDuration.setTextColor(unselectedHeaderColor);

            // Depending on the header column selected, change the sort order
            // field and highlight that header column.
            switch (viewId) {
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
}
