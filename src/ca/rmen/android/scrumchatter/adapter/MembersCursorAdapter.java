package ca.rmen.android.scrumchatter.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import ca.rmen.android.scrumchatter.R;
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
		fillView(view, cursor);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
		LayoutInflater layoutInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = layoutInflater.inflate(R.layout.member_list_item, null);
		fillView(view, cursor);
		return view;
	}

	private void fillView(View view, Cursor cursor) {
		MemberCursorWrapper memberCursorWrapper = new MemberCursorWrapper(
				cursor);
		TextView tvName = (TextView) view.findViewById(R.id.tv_name);
		tvName.setText(memberCursorWrapper.getName());

	}

}
