package ca.rmen.android.scrumchatter.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.provider.MemberCursorWrapper;

public class MembersCursorAdapter extends CursorAdapter {
	private final OnClickListener mOnClickListener;

	public MembersCursorAdapter(Context context, OnClickListener onClickListener) {
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
		View view = layoutInflater.inflate(R.layout.member_list_item, null);
		fillView(view, cursor);
		return view;
	}

	private void fillView(View view, Cursor cursor) {
		MemberCursorWrapper memberCursorWrapper = new MemberCursorWrapper(
				cursor);
		Long memberId = memberCursorWrapper.getId();
		String memberName = memberCursorWrapper.getName();
		MemberItemCache cache = new MemberItemCache(memberId, memberName);

		TextView tvName = (TextView) view.findViewById(R.id.tv_name);
		tvName.setText(memberName);
		View btnDelete = view.findViewById(R.id.btn_delete);
		btnDelete.setOnClickListener(mOnClickListener);
		btnDelete.setTag(cache);
	}

	public static class MemberItemCache {
		public long id;
		public String name;

		public MemberItemCache(long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
