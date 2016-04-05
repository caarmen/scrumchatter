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
package ca.rmen.android.scrumchatter.member.list;

/**
 * Displays the list of team members.
 */
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.member.list.Members.Member;
import ca.rmen.android.scrumchatter.provider.MemberColumns;
import ca.rmen.android.scrumchatter.provider.MemberStatsColumns;

public class MembersListFragment extends ListFragment {

    private static final String TAG = Constants.TAG + "/" + MembersListFragment.class.getSimpleName();

    private static final int URL_LOADER = 0;
    private String mOrderByField = MemberColumns.NAME + " COLLATE NOCASE";
    private TextView mTextViewName;
    private TextView mTextViewAvgDuration;
    private TextView mTextViewSumDuration;

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
        View view = inflater.inflate(R.layout.member_list, null);
        mTextViewName = (TextView) view.findViewById(R.id.tv_name);
        mTextViewAvgDuration = (TextView) view.findViewById(R.id.tv_avg_duration);
        mTextViewSumDuration = (TextView) view.findViewById(R.id.tv_sum_duration);
        mTextViewName.setOnClickListener(mOnClickListener);
        mTextViewAvgDuration.setOnClickListener(mOnClickListener);
        mTextViewSumDuration.setOnClickListener(mOnClickListener);
        TextView emptyText = (TextView) view.findViewById(android.R.id.empty);
        emptyText.setText(R.string.empty_list_members);
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mMembers = new Members((FragmentActivity) activity);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
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

    private LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
            Log.v(TAG, "onCreateLoader, order by " + mOrderByField);
            String[] projection = new String[] { MemberColumns._ID, MemberColumns.NAME, MemberStatsColumns.SUM_DURATION, MemberStatsColumns.AVG_DURATION };
            String selection = MemberStatsColumns.TEAM_ID + " =? AND " + MemberColumns.DELETED + "=0 ";
            String[] selectionArgs = new String[] { String.valueOf(mTeamId) };
            CursorLoader loader = new CursorLoader(getActivity(), MemberStatsColumns.CONTENT_URI, projection, selection, selectionArgs, mOrderByField);
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            Log.v(TAG, "onLoadFinished");
            if (mAdapter == null) {
                mAdapter = new MembersCursorAdapter(getActivity(), mOnClickListener);
                setListAdapter(mAdapter);
            }
            getView().findViewById(R.id.progressContainer).setVisibility(View.GONE);
            mAdapter.changeCursor(cursor);
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
            int selectedHeaderColor = getResources().getColor(R.color.selected_header);
            int unselectedHeaderColor = getResources().getColor(R.color.unselected_header);
            // Reset all the header text views to the default color
            mTextViewName.setTextColor(unselectedHeaderColor);
            mTextViewAvgDuration.setTextColor(unselectedHeaderColor);
            mTextViewSumDuration.setTextColor(unselectedHeaderColor);

            // Depending on the header column selected, change the sort order
            // field and highlight that header column.
            switch (viewId) {
                case R.id.tv_name:
                    mOrderByField = MemberColumns.NAME + " COLLATE NOCASE";
                    mTextViewName.setTextColor(selectedHeaderColor);
                    break;
                case R.id.tv_avg_duration:
                    mOrderByField = MemberStatsColumns.AVG_DURATION + " DESC, " + MemberColumns.NAME + " ASC ";
                    mTextViewAvgDuration.setTextColor(selectedHeaderColor);
                    break;
                case R.id.tv_sum_duration:
                    mOrderByField = MemberStatsColumns.SUM_DURATION + " DESC, " + MemberColumns.NAME + " ASC ";
                    mTextViewSumDuration.setTextColor(selectedHeaderColor);
                    break;
                default:
                    break;
            }
            // Requery if needed.
            if (!oldOrderByField.equals(mOrderByField)) getLoaderManager().restartLoader(URL_LOADER, null, mLoaderCallbacks);

        }
    };

    /**
     * Refresh the list when the selected team changes.
     */
    private OnSharedPreferenceChangeListener mPrefsListener = new OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            mTeamId = sharedPreferences.getInt(Constants.PREF_TEAM_ID, Constants.DEFAULT_TEAM_ID);
            getLoaderManager().restartLoader(URL_LOADER, null, mLoaderCallbacks);
        }
    };
}
