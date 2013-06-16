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
package ca.rmen.android.scrumchatter.adapter;

import android.content.Context;
import android.database.Cursor;
import android.os.SystemClock;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.TextView;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.provider.MeetingColumns.State;
import ca.rmen.android.scrumchatter.provider.MeetingMemberCursorWrapper;

/**
 * Adapter for the list of members in one meeting, and their speaking durations
 * for that meeting.
 */
public class MeetingCursorAdapter extends CursorAdapter {
	private final OnClickListener mOnClickListener;

	/**
	 * @param onClickListener
	 *            clicks on widgets on each list item will be forwarded to this
	 *            listener.
	 */
	public MeetingCursorAdapter(Context context, OnClickListener onClickListener) {
		super(context, null, true);
		mOnClickListener = onClickListener;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		fillView(view, cursor);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
		LayoutInflater layoutInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = layoutInflater.inflate(R.layout.meeting_member_list_item,
				null);
		fillView(view, cursor);
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
		MeetingMemberCursorWrapper cursorWrapper = new MeetingMemberCursorWrapper(
				cursor);
		Long memberId = cursorWrapper.getMemberId();
		String memberName = cursorWrapper.getMemberName();
		long duration = cursorWrapper.getDuration();
		State meetingState = cursorWrapper.getMeetingState();
		Long talkStartTime = cursorWrapper.getTalkStartTime();

		// Find the Views we need to set up
		TextView tvName = (TextView) view.findViewById(R.id.tv_name);
		Chronometer chronometer = (Chronometer) view
				.findViewById(R.id.tv_duration);
		ImageButton btnStartStop = (ImageButton) view
				.findViewById(R.id.btn_start_stop_member);

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
			btnStartStop
					.setImageResource(memberIsTalking ? R.drawable.ic_action_stop
							: R.drawable.ic_action_start);
		}

		// If the member is currently talking, show the chronometer.
		// Otherwise, show the duration that they talked (if any).
		if (memberIsTalking) {
			long hasBeenTalkingFor = duration * 1000
					+ (System.currentTimeMillis() - talkStartTime);
			chronometer.setBase(SystemClock.elapsedRealtime()
					- hasBeenTalkingFor);
			chronometer.start();
		} else {
			chronometer.stop();
			chronometer.setText(DateUtils.formatElapsedTime(duration));
		}

		// Set the member id as a tag, so when the OnClickListener receives the
		// click action, it knows for which member the user clicked.
		btnStartStop.setTag(memberId);
	}
}
