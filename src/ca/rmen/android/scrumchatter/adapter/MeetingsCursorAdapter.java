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
		fillView(view, cursor);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.meeting_list_item, null);
		fillView(view, cursor);
		return view;
	}

	private void fillView(View view, Cursor cursor) {
		MeetingCursorWrapper cursorWrapper = new MeetingCursorWrapper(cursor);
		long id = cursorWrapper.getId();
		String date = DateUtils.formatDateTime(mContext,
				cursorWrapper.getMeetingDate(), DateUtils.FORMAT_SHOW_DATE
						| DateUtils.FORMAT_SHOW_TIME);
		String duration = DateUtils.formatElapsedTime(cursorWrapper
				.getDuration());
		MeetingItemCache cache = new MeetingItemCache(id, date, duration);
		TextView tvDate = (TextView) view.findViewById(R.id.tv_meeting_date);
		TextView tvDuration = (TextView) view
				.findViewById(R.id.tv_meeting_duration);
		View btnDelete = view.findViewById(R.id.btn_delete);
		tvDate.setText(date);
		tvDuration.setText(duration);
		btnDelete.setTag(cache);
		btnDelete.setOnClickListener(mOnClickListener);
	}

	public static class MeetingItemCache {
		public final long id;
		public final String date;
		public final String duration;

		public MeetingItemCache(long id, String date, String duration) {
			this.id = id;
			this.date = date;
			this.duration = duration;
		}

	}
}
