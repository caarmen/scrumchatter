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

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.os.SystemClock;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.provider.MeetingColumns.State;
import ca.rmen.android.scrumchatter.provider.MeetingMemberCursorWrapper;

/**
 * Adapter for the list of members in one meeting, and their speaking durations
 * for that meeting.
 */
class MeetingCursorAdapter extends CursorAdapter {
    private static final String TAG = Constants.TAG + "/" + MeetingCursorAdapter.class.getSimpleName();
    private final OnClickListener mOnClickListener;
    private final int mColorChronoActive;
    private final int mColorChronoInactive;
    private final int mColorChronoNotStarted;

    /**
     * @param onClickListener
     *            clicks on widgets on each list item will be forwarded to this
     *            listener.
     */
    MeetingCursorAdapter(Context context, OnClickListener onClickListener) {
        super(context, null, false);
        Log.v(TAG, "Constructor");
        mOnClickListener = onClickListener;
        mColorChronoActive = context.getResources().getColor(R.color.chrono_active);
        mColorChronoInactive = context.getResources().getColor(R.color.chrono_inactive);
        mColorChronoNotStarted = context.getResources().getColor(R.color.chrono_not_started);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        fillView(view, cursor);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.meeting_member_list_item, null);
        return view;
    }

    /**
     * Set the view elements (TextView text, etc) for the given member of a
     * meeting.
     * 
     * @param view
     *            a view we just created or recycled
     * @param cursor
     *            a row for one member in one meeting.
     */
    private void fillView(View view, Cursor cursor) {
        // Extract the fields we need from this cursor
        @SuppressWarnings("resource")
        MeetingMemberCursorWrapper cursorWrapper = new MeetingMemberCursorWrapper(cursor);
        Long memberId = cursorWrapper.getMemberId();
        String memberName = cursorWrapper.getMemberName();
        long duration = cursorWrapper.getDuration();
        State meetingState = cursorWrapper.getMeetingState();
        Long talkStartTime = cursorWrapper.getTalkStartTime();

        // Find the Views we need to set up
        TextView tvName = (TextView) view.findViewById(R.id.tv_name);
        Chronometer chronometer = (Chronometer) view.findViewById(R.id.tv_duration);
        ImageButton btnStartStop = (ImageButton) view.findViewById(R.id.btn_start_stop_member);
        final ImageView ivChatterFace = (ImageView) view.findViewById(R.id.iv_chatter_face);
        final AnimationDrawable animChatterFace = (AnimationDrawable) ivChatterFace.getDrawable();

        // Set up the member's name
        tvName.setText(memberName);

        // if the talkStartTime is non-zero, this means the
        // member is talking (and started talking that long ago).
        boolean memberIsTalking = talkStartTime != null && talkStartTime > 0;

        // Set up the start/stop button for this member.
        // If the meeting is finished, we hide the start/stop button.
        if (meetingState == State.FINISHED) {
            btnStartStop.setVisibility(View.INVISIBLE);
        }
        // If the meeting is in progress, set the button to stop
        // or start, depending on whether the member is already talking
        // or not.
        else {
            btnStartStop.setOnClickListener(mOnClickListener);
            btnStartStop.setImageResource(memberIsTalking ? R.drawable.ic_action_stop : R.drawable.ic_action_start);
        }

        // If the member is currently talking, show the chronometer.
        // Otherwise, show the duration that they talked (if any).
        if (memberIsTalking) {
            long hasBeenTalkingFor = duration * 1000 + (System.currentTimeMillis() - talkStartTime);
            chronometer.setBase(SystemClock.elapsedRealtime() - hasBeenTalkingFor);
            chronometer.start();
            chronometer.setTextColor(mColorChronoActive);
            startAnimation(ivChatterFace);
        } else {
            chronometer.stop();
            chronometer.setText(DateUtils.formatElapsedTime(duration));
            chronometer.setTextColor(duration > 0 ? mColorChronoInactive : mColorChronoNotStarted);
            stopAnimation(ivChatterFace);
        }

        // Set the member id as a tag, so when the OnClickListener receives the
        // click action, it knows for which member the user clicked.
        btnStartStop.setTag(memberId);
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
                @Override
                public void onGlobalLayout() {
                    if (!animationDrawable.isRunning()) {
                        imageView.post(new Runnable() {

                            @Override
                            public void run() {
                                animationDrawable.setVisible(true, true);
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
}
