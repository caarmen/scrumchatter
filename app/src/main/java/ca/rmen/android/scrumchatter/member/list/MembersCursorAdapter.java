/**
 * Copyright 2013 Carmen Alvarez
 *
 * This file is part of Scrum Chatter.
 *
 * Scrum Chatter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
package ca.rmen.android.scrumchatter.member.list;

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
import ca.rmen.android.scrumchatter.member.list.Members.Member;
import ca.rmen.android.scrumchatter.provider.MemberCursorWrapper;
import ca.rmen.android.scrumchatter.util.ViewHolder;

/**
 * Adapter for the list of team members.
 */
class MembersCursorAdapter extends CursorAdapter {
    private final OnClickListener mOnClickListener;

    MembersCursorAdapter(Context context, OnClickListener onClickListener) {
        super(context, null, false);
        mOnClickListener = onClickListener;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return layoutInflater.inflate(R.layout.member_list_item, viewGroup, false);
    }

    /**
     * Set up the view with the data from the given team member
     * 
     * @param view
     *            a newly created, or recycled view
     * @param cursor
     *            a row for a given team member.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Get the data from the cursor
        @SuppressWarnings("resource")
        MemberCursorWrapper memberCursorWrapper = new MemberCursorWrapper(cursor);
        Long memberId = memberCursorWrapper.getId();
        String memberName = memberCursorWrapper.getName();
        Integer avgDuration = memberCursorWrapper.getAverageDuration();
        Integer sumDuration = memberCursorWrapper.getSumDuration();
        Member cache = new Member(memberId, memberName);

        // Find the views we need to update
        TextView tvName = ViewHolder.get(view, R.id.tv_name);
        TextView tvAvgDuration = ViewHolder.get(view, R.id.tv_avg_duration);
        TextView tvSumDuration = ViewHolder.get(view, R.id.tv_sum_duration);
        View btnDelete = ViewHolder.get(view, R.id.btn_delete_member);

        // Setup our views with the member data
        tvName.setText(memberName);
        tvAvgDuration.setText(DateUtils.formatElapsedTime(avgDuration));
        tvSumDuration.setText(DateUtils.formatElapsedTime(sumDuration));

        // Forward clicks to our OnClickListener, and use the tag
        // to pass data about the member that the OnClickListener needs.
        btnDelete.setOnClickListener(mOnClickListener);
        btnDelete.setTag(cache);
    }
}
