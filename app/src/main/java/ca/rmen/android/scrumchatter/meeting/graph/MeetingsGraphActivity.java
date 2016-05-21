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
package ca.rmen.android.scrumchatter.meeting.graph;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.databinding.MeetingsGraphActivityBinding;
import ca.rmen.android.scrumchatter.export.GraphExport;
import ca.rmen.android.scrumchatter.provider.MeetingColumns;
import ca.rmen.android.scrumchatter.team.Teams;
import ca.rmen.android.scrumchatter.util.Log;


/**
 * Displays graphs for all meetings.
 */
public class MeetingsGraphActivity extends AppCompatActivity {

    private static final String TAG = Constants.TAG + "/" + MeetingsGraphActivity.class.getSimpleName();
    private static final int URL_LOADER = 0;

    private MeetingsGraphActivityBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.meetings_graph_activity);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) supportActionBar.setDisplayHomeAsUpEnabled(true);
        getSupportLoaderManager().initLoader(URL_LOADER, null, mLoaderCallbacks);
        mTeamLoader.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.graph, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_share) {
            new GraphExportTask().execute();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy");
        super.onDestroy();
    }

    private final LoaderManager.LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            String selection = MeetingColumns.TEAM_ID + "=?";
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            long teamId = sharedPreferences.getInt(Constants.PREF_TEAM_ID, Constants.DEFAULT_TEAM_ID);
            String[] selectionArgs = new String[]{String.valueOf(teamId)};
            return new CursorLoader(getApplicationContext(), MeetingColumns.CONTENT_URI, null, selection, selectionArgs, MeetingColumns.MEETING_DATE
                    + " DESC");
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            if (cursor != null) {
                MeetingsGraph.populateMeetingsGraph(getApplicationContext(), mBinding.chart, cursor);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };

    private final AsyncTask<Void, Void, Teams.Team> mTeamLoader = new AsyncTask<Void, Void, Teams.Team>() {
        @Override
        protected Teams.Team doInBackground(Void... params) {
            return new Teams(MeetingsGraphActivity.this).getCurrentTeam();
        }
        @Override
        protected void onPostExecute(Teams.Team team) {
            mBinding.tvTitleMeetingsGraph.setText(getString(R.string.chart_meetings_title, team.teamName));
        }
    };

    private class GraphExportTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            Snackbar.make(mBinding.getRoot(), getString(R.string.chart_exporting_snackbar), Snackbar.LENGTH_LONG).show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            GraphExport export = new GraphExport(MeetingsGraphActivity.this, mBinding.graphContainer);
            export.export();
            return null;
        }

    }
}
