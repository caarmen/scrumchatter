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

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.meeting.Meetings;

/**
 * Loads a meeting and notifies the activity when it's loaded
 */
public class MeetingLoaderFragment extends Fragment { // NO_UCD (use default)

    private static final String TAG = Constants.TAG + "/" + MeetingLoaderFragment.class.getSimpleName();
    private MeetingLoaderTask mLoadMeetingTask;
    private long mMeetingId = -1;

    interface MeetingLoaderListener {
        void onMeetingLoaded(Meeting meeting);
    }

    public MeetingLoaderFragment() {
        super();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.v(TAG, "onActivityCreated: activity = " + getActivity());
        // If we already loaded the meeting, return it
        final FragmentActivity activity = getActivity();
        if (mLoadMeetingTask != null && mLoadMeetingTask.getStatus() != AsyncTask.Status.FINISHED) {
            Log.v(TAG, "Already loading meeting.  Ignoring");
            return;
        }
        // If we're not already loading a meeting, load it
        mLoadMeetingTask = new MeetingLoaderTask();
        if (mMeetingId == -1) mMeetingId = activity.getIntent().getLongExtra(Meetings.EXTRA_MEETING_ID, -1);
        mLoadMeetingTask.execute(mMeetingId);
    }

    /**
     * Extract the meeting id from the intent and load the meeting data into the
     * activity.
     */
    private class MeetingLoaderTask extends AsyncTask<Long, Void, Meeting> {

        @Override
        protected Meeting doInBackground(Long... params) {
            long meetingId = params[0];
            FragmentActivity activity = getActivity();
            Log.v(TAG, "doInBackground: meetingId = " + meetingId);
            final Meeting meeting;
            if (meetingId == -1) meeting = Meeting.createNewMeeting(activity);
            else
                meeting = Meeting.read(activity, meetingId);
            Log.v(TAG, "Loaded meeting " + meeting + " for meetingId " + meetingId);
            return meeting;
        }

        @Override
        protected void onPostExecute(Meeting result) {
            mMeetingId = result.getId();
            FragmentActivity activity = getActivity();
            if (activity instanceof MeetingLoaderListener) ((MeetingLoaderListener) activity).onMeetingLoaded(result);
        }
    };
}
