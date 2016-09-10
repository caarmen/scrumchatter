/**
 * Copyright 2013-2016 Carmen Alvarez
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
package ca.rmen.android.scrumchatter.meeting.detail;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.AnimationDrawable;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;

import ca.rmen.android.scrumchatter.databinding.MeetingMemberListItemBinding;
import ca.rmen.android.scrumchatter.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.provider.MeetingColumns.State;
import ca.rmen.android.scrumchatter.provider.MeetingMemberCursorWrapper;
import ca.rmen.android.scrumchatter.widget.ScrumChatterCursorAdapter;

/**
 * Adapter for the list of members in one meeting, and their speaking durations
 * for that meeting.
 */
public class MeetingCursorAdapter extends ScrumChatterCursorAdapter<MeetingCursorAdapter.MeetingViewHolder> {
    private static final String TAG = Constants.TAG + "/" + MeetingCursorAdapter.class.getSimpleName();
    private final MemberStartStopListener mMemberStartStopListener;
    private final int mColorChronoActive;
    private final int mColorChronoInactive;
    private final int mColorChronoNotStarted;

    public interface MemberStartStopListener {
        void toggleTalkingMember(long memberId);
    }

    /**
     * @param memberStartStopListener
     *            clicks on the start/stop button on each list item will be forwarded to this
     *            listener.
     */
    MeetingCursorAdapter(Context context, MemberStartStopListener memberStartStopListener) {
        super();
        Log.v(TAG, "Constructor");
        mMemberStartStopListener = memberStartStopListener;
        mColorChronoActive = ContextCompat.getColor(context, R.color.chrono_active);
        mColorChronoInactive = ContextCompat.getColor(context, R.color.chrono_inactive);
        mColorChronoNotStarted = ContextCompat.getColor(context, R.color.chrono_not_started);
    }

    @Override
    public MeetingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        MeetingMemberListItemBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.meeting_member_list_item, parent, false);
        binding.getRoot().setTag(binding);
        binding.setListener(mMemberStartStopListener);
        return new MeetingViewHolder(binding);
    }

    /**
     * Set the view elements (TextView text, etc) for the given member of a
     * meeting.
     */
    @Override
    public void onBindViewHolder(MeetingViewHolder holder, int position) {
        // Extract the fields we need from this cursor
        @SuppressWarnings("resource")
        MeetingMemberCursorWrapper cursorWrapper = new MeetingMemberCursorWrapper(getCursor());
        Long memberId = cursorWrapper.getMemberId();
        String memberName = cursorWrapper.getMemberName();
        long duration = cursorWrapper.getDuration();
        State meetingState = cursorWrapper.getMeetingState();
        Long talkStartTime = cursorWrapper.getTalkStartTime();

        // Find the Views we need to set up
        MeetingMemberListItemBinding binding = holder.binding;
        // Set up the member's name
        binding.tvName.setText(memberName);

        // if the talkStartTime is non-zero, this means the
        // member is talking (and started talking that long ago).
        boolean memberIsTalking = talkStartTime > 0;

        // Set up the start/stop button for this member.
        // If the meeting is finished, we hide the start/stop button.
        if (meetingState == State.FINISHED) {
            binding.btnStartStopMember.setVisibility(View.INVISIBLE);
        }
        // If the meeting is in progress, set the button to stop
        // or start, depending on whether the member is already talking
        // or not.
        else {
            binding.btnStartStopMember.setImageResource(memberIsTalking ? R.drawable.ic_action_stop : R.drawable.ic_action_start);
        }

        // If the member is currently talking, show the chronometer.
        // Otherwise, show the duration that they talked (if any).
        if (memberIsTalking) {
            long hasBeenTalkingFor = duration * 1000 + (System.currentTimeMillis() - talkStartTime);
            binding.tvDuration.setBase(SystemClock.elapsedRealtime() - hasBeenTalkingFor);
            binding.tvDuration.start();
            binding.tvDuration.setTextColor(mColorChronoActive);
            startAnimation(binding.ivChatterFace);
        } else {
            binding.tvDuration.stop();
            binding.tvDuration.setText(DateUtils.formatElapsedTime(duration));
            binding.tvDuration.setTextColor(duration > 0 ? mColorChronoInactive : mColorChronoNotStarted);
            stopAnimation(binding.ivChatterFace);
        }

        // Set the member id as a tag, so when the listener receives the
        // click action, it knows for which member the user clicked.
        binding.setMemberId(memberId);
    }

    /**
     * Show the imageView and start its animation drawable.
     */
    private void startAnimation(final ImageView imageView) {
        if (imageView.getVisibility() != View.VISIBLE) {
            Log.v(TAG, "startAnimation");
            imageView.setVisibility(View.VISIBLE);
            final AnimationDrawable animationDrawable = (AnimationDrawable) imageView.getDrawable();
            // On some devices, directly calling start() on the animation does not work.
            // We have to wait until the ImageView is visible before starting the animation.
            imageView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                @SuppressWarnings("deprecation")
                @Override
                public void onGlobalLayout() {
                    if (!animationDrawable.isRunning()) {
                        imageView.post(new Runnable() {

                            @Override
                            public void run() {
                                animationDrawable.setVisible(true, false);
                                animationDrawable.start();
                            }
                        });
                    }
                    imageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            });
        }
    }

    /**
     * Stop the animation drawable on this imageView and hide the imageView.
     */
    private void stopAnimation(final ImageView imageView) {
        if (imageView.getVisibility() == View.VISIBLE) {
            Log.v(TAG, "stopAnimation");
            imageView.setVisibility(View.INVISIBLE);
            final AnimationDrawable animationDrawable = (AnimationDrawable) imageView.getDrawable();
            animationDrawable.setVisible(false, false);
        }
    }

    public static class MeetingViewHolder extends RecyclerView.ViewHolder{

        public final MeetingMemberListItemBinding binding;

        public MeetingViewHolder(MeetingMemberListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

}
