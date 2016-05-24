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

import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.databinding.MeetingChartFragmentBinding;
import ca.rmen.android.scrumchatter.meeting.Meetings;
import ca.rmen.android.scrumchatter.meeting.detail.Meeting;
import ca.rmen.android.scrumchatter.provider.MeetingMemberColumns;
import ca.rmen.android.scrumchatter.provider.MemberColumns;
import ca.rmen.android.scrumchatter.team.Teams;
import ca.rmen.android.scrumchatter.util.Log;
import ca.rmen.android.scrumchatter.util.TextUtils;

/**
 * Displays charts for one meeting
 */
public class MeetingChartFragment extends Fragment {

    private static final String TAG = Constants.TAG + "/" + MeetingChartFragment.class.getSimpleName();
    private static final int LOADER_MEMBER_SPEAKING_TIME = 0;

    private MeetingChartFragmentBinding mBinding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView");
        mBinding = DataBindingUtil.inflate(inflater, R.layout.meeting_chart_fragment, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(LOADER_MEMBER_SPEAKING_TIME, null, mLoaderCallbacks);
        setHasOptionsMenu(true);
        mMeetingLoader.execute(getActivity().getIntent().getLongExtra(Meetings.EXTRA_MEETING_ID, -1));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.meeting_chart_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_share) {
            new ChartExportTask(getContext(), mBinding.memberSpeakingTimeChartContent).execute();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private final LoaderManager.LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {

            long meetingId = getActivity().getIntent().getLongExtra(Meetings.EXTRA_MEETING_ID, -1);

            String[] projection = new String[]{
                    MeetingMemberColumns._ID,
                    MeetingMemberColumns.MEMBER_ID,
                    MemberColumns.NAME,
                    MeetingMemberColumns.DURATION,
                    MeetingMemberColumns.TALK_START_TIME};
            String selection = MeetingMemberColumns.DURATION + ">0";
            String orderBy = MeetingMemberColumns.DURATION + " DESC";

            Uri uri = Uri.withAppendedPath(MeetingMemberColumns.CONTENT_URI, String.valueOf(meetingId));
            return new CursorLoader(getActivity(), uri, projection, selection, null, orderBy);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            if (cursor != null) {
                if (loader.getId() == LOADER_MEMBER_SPEAKING_TIME) {
                    MeetingSpeakingTimeColumnChart.populateMeeting(getContext(),
                            mBinding.memberSpeakingTimeChart,
                            cursor);
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };

    private final AsyncTask<Long, Void, Void> mMeetingLoader = new AsyncTask<Long, Void, Void>() {
        private Meeting mMeeting;
        private Teams.Team mTeam;

        @Override
        protected Void doInBackground(Long... meetingId) {
            mTeam = new Teams(getActivity()).getCurrentTeam();
            mMeeting = Meeting.read(getContext(), meetingId[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mBinding.tvTitleMemberSpeakingTimeChart.setText(
                    getString(R.string.chart_member_speaking_time_title, mTeam.teamName));
            String meetingDate = TextUtils.formatDateTime(getContext(), mMeeting.getStartDate());
            String meetingDuration = DateUtils.formatElapsedTime(mMeeting.getDuration());
            mBinding.tvSubtitleDateMemberSpeakingTimeChart.setText(meetingDate);
            mBinding.tvSubtitleDurationMemberSpeakingTimeChart.setText(getString(R.string.chart_total_duration, meetingDuration));
        }
    };
}
