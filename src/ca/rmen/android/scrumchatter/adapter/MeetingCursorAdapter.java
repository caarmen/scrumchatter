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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.TextView;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.provider.MeetingColumns.State;
import ca.rmen.android.scrumchatter.provider.MeetingMemberCursorWrapper;

public class MeetingCursorAdapter extends CursorAdapter {
	private static final String TAG = Constants.TAG + "/"
			+ MeetingCursorAdapter.class.getSimpleName();
	private final OnClickListener mOnClickListener;

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

	private void fillView(View view, Cursor cursor) {
		@SuppressWarnings("resource")
		MeetingMemberCursorWrapper cursorWrapper = new MeetingMemberCursorWrapper(
				cursor);
		Long memberId = cursorWrapper.getMemberId();
		String memberName = cursorWrapper.getMemberName();
		long duration = cursorWrapper.getDuration();
		State meetingState = cursorWrapper.getMeetingState();

		MemberItemCache cache = new MemberItemCache(memberId, memberName);

		TextView tvName = (TextView) view.findViewById(R.id.tv_name);
		tvName.setText(memberName);

		Chronometer chronometer = (Chronometer) view
				.findViewById(R.id.tv_duration);

		ImageButton btnStartStop = (ImageButton) view
				.findViewById(R.id.btn_start_stop_member);
		Long talkStartTime = cursorWrapper.getTalkStartTime();
		if (meetingState == State.FINISHED) {
			btnStartStop.setVisibility(View.INVISIBLE);
		} else {
			btnStartStop.setOnClickListener(mOnClickListener);
			btnStartStop.setTag(cache);
			if (talkStartTime != null && talkStartTime > 0) {
				btnStartStop.setImageResource(R.drawable.ic_action_stop);
			} else {
				btnStartStop.setImageResource(R.drawable.ic_action_start);
			}
		}
		if (talkStartTime != null && talkStartTime > 0) {
			long hasBeenTalkingFor = duration * 1000
					+ (System.currentTimeMillis() - talkStartTime);
			chronometer.setBase(SystemClock.elapsedRealtime()
					- hasBeenTalkingFor);
			chronometer.start();
		} else {
			chronometer.stop();
			chronometer.setText(DateUtils.formatElapsedTime(duration));
		}
	}

	@Override
	protected void onContentChanged() {
		super.onContentChanged();
		Log.v(TAG, "onContentChanged");
	}

	public static class MemberItemCache {
		public final long id;
		public final String name;

		private MemberItemCache(long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
