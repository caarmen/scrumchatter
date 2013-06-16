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
		MeetingItemCache cache = new MeetingItemCache(id, date, duration,
				stateName);
		TextView tvDate = (TextView) view.findViewById(R.id.tv_meeting_date);
		TextView tvDuration = (TextView) view
				.findViewById(R.id.tv_meeting_duration);
		TextView tvState = (TextView) view.findViewById(R.id.tv_meeting_status);
		View btnDelete = view.findViewById(R.id.btn_delete);
		tvDate.setText(date);
		tvDuration.setText(duration);
		tvState.setText(stateName);
		btnDelete.setTag(cache);
		btnDelete.setOnClickListener(mOnClickListener);
		tvDate.setTag(cache);
		tvDate.setOnClickListener(mOnClickListener);

	}

	public static class MeetingItemCache {
		public final long id;
		public final String date;
		public final String duration;
		public final String state;

		public MeetingItemCache(long id, String date, String duration,
				String state) {
			this.id = id;
			this.date = date;
			this.duration = duration;
			this.state = state;
		}

	}
}
