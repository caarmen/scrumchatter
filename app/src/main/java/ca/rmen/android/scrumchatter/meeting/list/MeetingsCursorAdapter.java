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
package ca.rmen.android.scrumchatter.meeting.list;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.databinding.MeetingListItemBinding;
import ca.rmen.android.scrumchatter.meeting.detail.Meeting;
import ca.rmen.android.scrumchatter.provider.MeetingColumns.State;
import ca.rmen.android.scrumchatter.provider.MeetingCursorWrapper;
import ca.rmen.android.scrumchatter.util.TextUtils;
import ca.rmen.android.scrumchatter.widget.ScrumChatterCursorAdapter;

/**
 * Adapter for the list of meetings.
 */
public class MeetingsCursorAdapter extends ScrumChatterCursorAdapter<MeetingsCursorAdapter.MeetingViewHolder> {
    private final MeetingListener mMeetingListener;
    private final int mColorStateInProgress;
    private final int mColorStateDefault;
    private final String[] mMeetingStateNames;

    MeetingsCursorAdapter(Context context, MeetingListener meetingListener) {
        mMeetingListener = meetingListener;
        mColorStateInProgress = ContextCompat.getColor(context, R.color.meeting_state_in_progress);
        mColorStateDefault = ContextCompat.getColor(context, R.color.meeting_state_default);
        mMeetingStateNames = context.getResources().getStringArray(R.array.meeting_states);
    }


    public interface MeetingListener {
        void onMeetingDelete(Meeting meeting);
        void onMeetingOpen(Meeting meeting);
    }

    @Override
    public MeetingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        MeetingListItemBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.meeting_list_item, parent, false);
        binding.getRoot().setTag(binding);
        binding.setMeetingListener(mMeetingListener);
        return new MeetingViewHolder(binding);
    }

    /**
     * Fill the view holder's fields with data from the given meeting.
     */
    @Override
    public void onBindViewHolder(MeetingViewHolder holder, int position) {
        getCursor().moveToPosition(position);
        Context context = holder.binding.getRoot().getContext();
        // Get the data from the cursor
        MeetingCursorWrapper cursorWrapper = new MeetingCursorWrapper(getCursor());
        Meeting meeting = Meeting.read(context, cursorWrapper);
        String dateString = TextUtils.formatDateTime(context, meeting.getStartDate());
        String duration = DateUtils.formatElapsedTime(meeting.getDuration());

        String stateName = mMeetingStateNames[meeting.getState().ordinal()];

        MeetingListItemBinding binding = holder.binding;
        // Find the views we need to set up.
        binding.setMeeting(meeting);

        // Fill the date view.
        binding.tvMeetingDate.setText(dateString);

        // Fill the duration view. We will only show the duration if
        // the meeting is finished. For not-started or in-progress
        // meetings, we show the state.
        if (meeting.getState() == State.FINISHED) binding.tvMeetingDuration.setText(duration);
        else
            binding.tvMeetingDuration.setText(stateName);
        if (meeting.getState() == State.IN_PROGRESS) {
            Animation animBlink = AnimationUtils.loadAnimation(context, R.anim.blink);
            binding.tvMeetingDuration.startAnimation(animBlink);
            binding.tvMeetingDuration.setTextColor(mColorStateInProgress);
        } else {
            Animation anim = binding.tvMeetingDuration.getAnimation();
            if (anim != null) {
                anim.cancel();
                // Need to make sure the animation doesn't stay faded out.
                anim = AnimationUtils.loadAnimation(context, R.anim.show);
                binding.tvMeetingDuration.startAnimation(anim);
            }
            binding.tvMeetingDuration.setTextColor(mColorStateDefault);
        }
    }

    public static class MeetingViewHolder extends RecyclerView.ViewHolder {
        public final MeetingListItemBinding binding;

        public MeetingViewHolder(MeetingListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
