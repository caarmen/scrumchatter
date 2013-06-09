package ca.rmen.android.scrumchatter.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import ca.rmen.android.scrumchatter.provider.MeetingCursorWrapper;

public class MeetingsCursorAdapter extends CursorAdapter {
	public MeetingsCursorAdapter(Context context, Cursor c, boolean autoRequery) {
		super(context, c, autoRequery);
	}

	public MeetingsCursorAdapter(Context context, Cursor c, int flags) {
		super(context, c, flags);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		MeetingCursorWrapper cursorWrapper = new MeetingCursorWrapper(cursor);
		((TextView) view).setText(cursorWrapper.getMeetingDate() + ":"
				+ cursorWrapper.getDuration());
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
		MeetingCursorWrapper cursorWrapper = new MeetingCursorWrapper(cursor);
		TextView view = new TextView(context);
		((TextView) view).setText(cursorWrapper.getMeetingDate() + ":"
				+ cursorWrapper.getDuration());
		return view;
	}

}
