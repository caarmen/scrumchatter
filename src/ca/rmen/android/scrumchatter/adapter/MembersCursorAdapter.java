package ca.rmen.android.scrumchatter.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import ca.rmen.android.scrumchatter.provider.MemberCursorWrapper;

public class MembersCursorAdapter extends CursorAdapter {
	public MembersCursorAdapter(Context context, Cursor c, boolean autoRequery) {
		super(context, c, autoRequery);
	}

	public MembersCursorAdapter(Context context, Cursor c, int flags) {
		super(context, c, flags);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		MemberCursorWrapper memberCursorWrapper = new MemberCursorWrapper(
				cursor);
		((TextView) view).setText(memberCursorWrapper.getName());
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
		MemberCursorWrapper memberCursorWrapper = new MemberCursorWrapper(
				cursor);
		TextView view = new TextView(context);
		((TextView) view).setText(memberCursorWrapper.getName());
		return view;
	}

}
