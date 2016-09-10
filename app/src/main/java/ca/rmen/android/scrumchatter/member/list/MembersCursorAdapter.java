/**
 * Copyright 2013-2016 Carmen Alvarez
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

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.databinding.MemberListItemBinding;
import ca.rmen.android.scrumchatter.member.list.Members.Member;
import ca.rmen.android.scrumchatter.provider.MemberCursorWrapper;
import ca.rmen.android.scrumchatter.widget.ScrumChatterCursorAdapter;

/**
 * Adapter for the list of team members.
 */
public class MembersCursorAdapter extends ScrumChatterCursorAdapter<MembersCursorAdapter.MemberViewHolder> {
    private final MemberListener mMemberListener;

    MembersCursorAdapter(MemberListener memberListener) {
        super();
        mMemberListener = memberListener;
    }

    public interface MemberListener {
        void onMemberEdit(Member member);
        void onMemberDelete(Member member);
    }

    @Override
    public MemberViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        MemberListItemBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.member_list_item, parent, false);
        binding.getRoot().setTag(binding);
        binding.setMemberListener(mMemberListener);
        return new MemberViewHolder(binding);
    }

    /**
     * Set up the view with the data from the given team member
     */
    @Override
    public void onBindViewHolder(MemberViewHolder holder, int position) {

        // Get the data from the cursor
        @SuppressWarnings("resource")
        MemberCursorWrapper memberCursorWrapper = new MemberCursorWrapper(getCursor());
        Long memberId = memberCursorWrapper.getId();
        String memberName = memberCursorWrapper.getName();
        Integer avgDuration = memberCursorWrapper.getAverageDuration();
        Integer sumDuration = memberCursorWrapper.getSumDuration();
        Member cache = new Member(memberId, memberName);

        // Find the views we need to update
        MemberListItemBinding binding = holder.binding;
        binding.setMember(cache);

        // Setup our views with the member data
        binding.tvName.setText(memberName);
        binding.tvAvgDuration.setText(DateUtils.formatElapsedTime(avgDuration));
        binding.tvSumDuration.setText(DateUtils.formatElapsedTime(sumDuration));

    }

    public static class MemberViewHolder extends RecyclerView.ViewHolder {

        public final MemberListItemBinding binding;

        public MemberViewHolder(MemberListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
