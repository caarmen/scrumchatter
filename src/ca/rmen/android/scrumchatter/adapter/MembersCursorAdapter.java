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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.provider.MemberCursorWrapper;

public class MembersCursorAdapter extends CursorAdapter {
	private static final String TAG = Constants.TAG + "/"
			+ MembersCursorAdapter.class.getSimpleName();
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
		Integer avgDuration = memberCursorWrapper.getAverageDuration();
		Integer sumDuration = memberCursorWrapper.getSumDuration();
		MemberItemCache cache = new MemberItemCache(memberId, memberName,
				avgDuration, sumDuration);

		TextView tvName = (TextView) view.findViewById(R.id.tv_name);
		tvName.setText(memberName);

		TextView tvAvgDuration = (TextView) view
				.findViewById(R.id.tv_avg_duration);
		tvAvgDuration.setText(DateUtils.formatElapsedTime(avgDuration));

		TextView tvSumDuration = (TextView) view
				.findViewById(R.id.tv_sum_duration);
		tvSumDuration.setText(DateUtils.formatElapsedTime(sumDuration));

		View btnDelete = view.findViewById(R.id.btn_delete);
		btnDelete.setOnClickListener(mOnClickListener);
		btnDelete.setTag(cache);
	}

	@Override
	protected void onContentChanged() {
		super.onContentChanged();
		Log.v(TAG, "onContentChanged");
	}

	// TODO for now this cache class is not really used to
	// cache data. The only fields that are used at all are
	// the id and name, when we show a popup dialog warning
	// before user deletion. We may not really need a cache as we
	// won't be dealing with large lists. Really, how many team
	// members attend meetings?
	public static class MemberItemCache {
		public final long id;
		public final String name;
		public final int avgChatterTimeSeconds;
		public final int totChatterTimeSeconds;

		public MemberItemCache(long id, String name, int avgChatterTimeSeconds,
				int totChatterTimeSeconds) {
			this.id = id;
			this.name = name;
			this.avgChatterTimeSeconds = avgChatterTimeSeconds;
			this.totChatterTimeSeconds = totChatterTimeSeconds;
		}
	}
}
