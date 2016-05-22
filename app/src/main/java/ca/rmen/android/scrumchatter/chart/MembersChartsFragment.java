/**
 * Copyright 2016 Carmen Alvarez
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
package ca.rmen.android.scrumchatter.chart;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.databinding.MembersChartsFragmentBinding;
import ca.rmen.android.scrumchatter.export.BitmapExport;
import ca.rmen.android.scrumchatter.provider.MemberColumns;
import ca.rmen.android.scrumchatter.provider.MemberStatsColumns;
import ca.rmen.android.scrumchatter.team.Teams;
import ca.rmen.android.scrumchatter.util.Log;

/**
 * Displays charts for members.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class MembersChartsFragment extends Fragment {

    private static final String TAG = Constants.TAG + "/" + MembersChartsFragment.class.getSimpleName();
    private static final int LOADER_MEMBER_SPEAKING_TIME = 0;

    private MembersChartsFragmentBinding mBinding;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView");
        mBinding = DataBindingUtil.inflate(inflater, R.layout.members_charts_fragment, container, false);
        FabListener listener = new FabListener();

        mBinding.pieChartCardAvg.setFabListener(listener);
        mBinding.pieChartCardTotal.setFabListener(listener);
        mBinding.pieChartCardAvg.fabShareMemberSpeakingTime.setTag(mBinding.pieChartCardAvg.memberSpeakingTimeChart);
        mBinding.pieChartCardTotal.fabShareMemberSpeakingTime.setTag(mBinding.pieChartCardTotal.memberSpeakingTimeChart);
        return mBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(LOADER_MEMBER_SPEAKING_TIME, null, mLoaderCallbacks);
        mTeamLoader.execute();
    }


    private final LoaderManager.LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            long teamId = sharedPreferences.getInt(Constants.PREF_TEAM_ID, Constants.DEFAULT_TEAM_ID);
            String[] selectionArgs = new String[]{String.valueOf(teamId)};

            String[] projection = new String[]{MemberColumns._ID, MemberColumns.NAME, MemberStatsColumns.SUM_DURATION, MemberStatsColumns.AVG_DURATION};
            String selection = MemberStatsColumns.TEAM_ID + " =? AND " + MemberColumns.DELETED + "=0 ";
            return new CursorLoader(getContext(), MemberStatsColumns.CONTENT_URI, projection, selection, selectionArgs, null);

        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            if (cursor != null) {
                MemberSpeakingTimePieChart.populateMemberSpeakingTimeChart(getContext(),
                        mBinding.pieChartCardAvg.chartMemberSpeakingTime,
                        mBinding.pieChartCardTotal.chartMemberSpeakingTime,
                        cursor);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };

    private final AsyncTask<Void, Void, Teams.Team> mTeamLoader = new AsyncTask<Void, Void, Teams.Team>() {
        @Override
        protected Teams.Team doInBackground(Void... params) {
            return new Teams(getActivity()).getCurrentTeam();
        }

        @Override
        protected void onPostExecute(Teams.Team team) {
            mBinding.pieChartCardAvg.tvTitleMemberSpeakingTimeChart.setText(getString(R.string.chart_member_average_speaking_time_title, team.teamName));
            mBinding.pieChartCardTotal.tvTitleMemberSpeakingTimeChart.setText(getString(R.string.chart_member_total_speaking_time_title, team.teamName));
        }
    };

    private class ChartExportTask extends AsyncTask<Void, Void, Void> {

        private final View mView;
        private Bitmap mBitmap;

        ChartExportTask(View view) {
            super();
            mView = view;
        }

        @Override
        protected void onPreExecute() {
            Snackbar.make(mBinding.getRoot(), getString(R.string.chart_exporting_snackbar), Snackbar.LENGTH_LONG).show();
            mBitmap = Bitmap.createBitmap(mView.getWidth(), mView.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mBitmap);
            mView.draw(canvas);
        }

        @Override
        protected Void doInBackground(Void... params) {
            BitmapExport export = new BitmapExport(getActivity(), mBitmap);
            export.export();
            return null;
        }

    }

    public class FabListener {
        public void onShareMemberSpeakingTime(View view) {
            new ChartExportTask((View) view.getTag()).execute();
        }
    }
}
