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
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.provider.MeetingColumns;
import ca.rmen.android.scrumchatter.provider.MeetingColumns.State;
import ca.rmen.android.scrumchatter.provider.MeetingCursorWrapper;

public class MeetingsCursorAdapter extends CursorAdapter {
	private final OnClickListener mOnClickListener;

	public MeetingsCursorAdapter(Context context,
			OnClickListener onClickListener) {
		super(context, null, true);
		mOnClickListener = onClickListener;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		fillView(context, view, cursor);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.meeting_list_item, null);
		fillView(context, view, cursor);
		return view;
	}

	private void fillView(Context context, View view, Cursor cursor) {
		@SuppressWarnings("resource")
		MeetingCursorWrapper cursorWrapper = new MeetingCursorWrapper(cursor);
		long id = cursorWrapper.getId();
		String date = DateUtils.formatDateTime(mContext,
				cursorWrapper.getMeetingDate(), DateUtils.FORMAT_SHOW_DATE
						| DateUtils.FORMAT_SHOW_TIME);
		String duration = DateUtils.formatElapsedTime(cursorWrapper
				.getDuration());
		MeetingColumns.State state = cursorWrapper.getState();

		// TODO cache the meeting state names
		String[] meetingStates = context.getResources().getStringArray(
				R.array.meeting_states);
		String stateName = meetingStates[state.ordinal()];
		MeetingItemCache cache = new MeetingItemCache(id, date);
		TextView tvDate = (TextView) view.findViewById(R.id.tv_meeting_date);
		TextView tvDuration = (TextView) view
				.findViewById(R.id.tv_meeting_duration);
		View btnDelete = view.findViewById(R.id.btn_delete);
		tvDate.setText(date);
		if (state == State.FINISHED)
			tvDuration.setText(duration);
		else
			tvDuration.setText(stateName);
		btnDelete.setTag(cache);
		btnDelete.setOnClickListener(mOnClickListener);
		tvDate.setTag(cache);
		tvDate.setOnClickListener(mOnClickListener);

	}

	public static class MeetingItemCache {
		public final long id;
		public final String date;

		private MeetingItemCache(long id, String date) {
			this.id = id;
			this.date = date;
		}

	}
}
