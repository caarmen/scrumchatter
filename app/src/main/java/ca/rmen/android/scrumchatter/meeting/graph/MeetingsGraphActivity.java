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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.databinding.MeetingsGraphActivityBinding;
import ca.rmen.android.scrumchatter.export.BitmapExport;
import ca.rmen.android.scrumchatter.provider.MeetingColumns;
import ca.rmen.android.scrumchatter.provider.MeetingMemberColumns;
import ca.rmen.android.scrumchatter.provider.MemberColumns;
import ca.rmen.android.scrumchatter.team.Teams;
import ca.rmen.android.scrumchatter.util.Log;


/**
 * Displays graphs for all meetings.
 */
public class MeetingsGraphActivity extends AppCompatActivity {

    private static final String TAG = Constants.TAG + "/" + MeetingsGraphActivity.class.getSimpleName();
    private static final int LOADER_MEETING_DURATION = 0;
    private static final int LOADER_MEMBER_SPEAKING_TIME = 1;

    private MeetingsGraphActivityBinding mBinding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.meetings_graph_activity);
        mBinding.setFabListener(new FabListener());
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) supportActionBar.setDisplayHomeAsUpEnabled(true);
        getSupportLoaderManager().initLoader(LOADER_MEETING_DURATION, null, mLoaderCallbacks);
        getSupportLoaderManager().initLoader(LOADER_MEMBER_SPEAKING_TIME, null, mLoaderCallbacks);
        mTeamLoader.execute();
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy");
        super.onDestroy();
    }

    private final LoaderManager.LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            long teamId = sharedPreferences.getInt(Constants.PREF_TEAM_ID, Constants.DEFAULT_TEAM_ID);
            String[] selectionArgs = new String[]{String.valueOf(teamId)};

            if (id == LOADER_MEETING_DURATION) {
                String selection = MeetingColumns.TEAM_ID + "=?";
                return new CursorLoader(
                        getApplicationContext(),
                        MeetingColumns.CONTENT_URI,
                        null,
                        selection,
                        selectionArgs,
                        MeetingColumns.MEETING_DATE);
            } else {
                return new CursorLoader(getApplicationContext(),
                        MeetingMemberColumns.CONTENT_URI,
                        new String[]{
                                MeetingMemberColumns.MEETING_ID,
                                MeetingColumns.MEETING_DATE,
                                MemberColumns.NAME,
                                MeetingMemberColumns.DURATION},
                        MeetingMemberColumns.DURATION + ">0 AND " + MeetingColumns.TEAM_ID + "=?",
                        selectionArgs,
                        MeetingMemberColumns.MEETING_ID + ", " + MemberColumns.NAME + " DESC");
            }
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            if (cursor != null) {
                if (loader.getId() == LOADER_MEETING_DURATION) {
                    MeetingsDurationGraph.populateMeetingDurationGraph(getApplicationContext(), mBinding.chartMeetingDuration, cursor);
                } else {
                    MemberSpeakingTimeGraph.populateMemberSpeakingTimeGraph(getApplicationContext(), mBinding.chartSpeakerTime, mBinding.legend, cursor);
                }
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
            mBinding.tvTitleMeetingDurationGraph.setText(getString(R.string.chart_meeting_duration_title, team.teamName));
            mBinding.tvTitleSpeakerTimeGraph.setText(getString(R.string.chart_speaker_time_title, team.teamName));
        }
    };

    private class GraphExportTask extends AsyncTask<Void, Void, Void> {

        private final View mView;
        private Bitmap mBitmap;

        GraphExportTask(View view) {
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
            BitmapExport export = new BitmapExport(MeetingsGraphActivity.this, mBitmap);
            export.export();
            return null;
        }

    }

    public class FabListener {
        public void onShareMeetingDuration(@SuppressWarnings("UnusedParameters") View view) {
            new GraphExportTask(mBinding.meetingDurationGraph).execute();

        }

        public void onShareSpeakerTime(@SuppressWarnings("UnusedParameters") View view) {
            new GraphExportTask(mBinding.speakerTimeGraph).execute();
        }
    }
}
