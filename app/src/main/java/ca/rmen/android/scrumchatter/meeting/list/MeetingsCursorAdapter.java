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
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.databinding.MeetingListItemBinding;
import ca.rmen.android.scrumchatter.meeting.detail.Meeting;
import ca.rmen.android.scrumchatter.provider.MeetingColumns.State;
import ca.rmen.android.scrumchatter.provider.MeetingCursorWrapper;
import ca.rmen.android.scrumchatter.util.TextUtils;

/**
 * Adapter for the list of meetings.
 */
class MeetingsCursorAdapter extends CursorAdapter {
    private final OnClickListener mOnClickListener;
    private final int mColorStateInProgress;
    private final int mColorStateDefault;
    private final String[] mMeetingStateNames;

    MeetingsCursorAdapter(Context context, OnClickListener onClickListener) {
        super(context, null, false);
        mOnClickListener = onClickListener;
        mColorStateInProgress = ContextCompat.getColor(context, R.color.meeting_state_in_progress);
        mColorStateDefault = ContextCompat.getColor(context, R.color.meeting_state_default);
        mMeetingStateNames = context.getResources().getStringArray(R.array.meeting_states);

    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        MeetingListItemBinding binding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.meeting_list_item, viewGroup, false);
        binding.getRoot().setTag(binding);
        return binding.getRoot();
    }

    /**
     * Fill the view's fields with data from the given meeting.
     * 
     * @param view
     *            a recently created view, or a recycled view
     * @param cursor
     *            a row for one meeting
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Get the data from the cursor
        MeetingCursorWrapper cursorWrapper = new MeetingCursorWrapper(cursor);
        Meeting meeting = Meeting.read(context, cursorWrapper);
        String dateString = TextUtils.formatDateTime(context, meeting.getStartDate());
        String duration = DateUtils.formatElapsedTime(meeting.getDuration());

        String stateName = mMeetingStateNames[meeting.getState().ordinal()];

        // Find the views we need to set up.
        MeetingListItemBinding binding = (MeetingListItemBinding) view.getTag();

        // Fill the date view.
        binding.tvMeetingDate.setText(dateString);

        // Fill the duration view. We will only show the duration if
        // the meeting is finished. For not-started or in-progress
        // meetings, we show the state.
        if (meeting.getState() == State.FINISHED) binding.tvMeetingDuration.setText(duration);
        else
            binding.tvMeetingDuration.setText(stateName);
        if (meeting.getState() == State.IN_PROGRESS) {
            Animation animBlink = AnimationUtils.loadAnimation(mContext, R.anim.blink);
            binding.tvMeetingDuration.startAnimation(animBlink);
            binding.tvMeetingDuration.setTextColor(mColorStateInProgress);
        } else {
            Animation anim = binding.tvMeetingDuration.getAnimation();
            if (anim != null) {
                anim.cancel();
                // Need to make sure the animation doesn't stay faded out.
                anim = AnimationUtils.loadAnimation(mContext, R.anim.show);
                binding.tvMeetingDuration.startAnimation(anim);
            }
            binding.tvMeetingDuration.setTextColor(mColorStateDefault);
        }

        // Forward clicks to our OnClickListener. We put the cache in the tag
        // so the listener can have access to data it needs to display
        // (showing the meeting date in the confirmation dialog to delete
        // a meeting).
        binding.btnDeleteMeeting.setTag(meeting);
        binding.btnDeleteMeeting.setOnClickListener(mOnClickListener);
    }
}
