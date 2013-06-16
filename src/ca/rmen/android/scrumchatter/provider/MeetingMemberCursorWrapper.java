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
package ca.rmen.android.scrumchatter.provider;

import java.util.HashMap;

import android.database.Cursor;
import android.database.CursorWrapper;
import ca.rmen.android.scrumchatter.provider.MeetingColumns.State;

public class MeetingMemberCursorWrapper extends CursorWrapper {
	private HashMap<String, Integer> mColumnIndexes = new HashMap<String, Integer>();

	public MeetingMemberCursorWrapper(Cursor cursor) {
		super(cursor);
	}

	public Long getMeetingId() {
		Integer index = getIndex(MeetingMemberColumns.MEETING_ID);
		if (isNull(index))
			return null;
		return getLong(index);
	}

	public Long getMemberId() {
		Integer index = getIndex(MemberColumns._ID);
		if (isNull(index))
			return null;
		return getLong(index);
	}

	public String getMemberName() {
		Integer index = getIndex(MemberColumns.NAME);
		if (isNull(index))
			return null;
		return getString(index);
	}

	public long getDuration() {
		Integer index = getIndex(MeetingMemberColumns.DURATION);
		if (isNull(index))
			return 0;
		return getLong(index);
	}

	public long getTalkStartTime() {
		Integer index = getIndex(MeetingMemberColumns.TALK_START_TIME);
		if (isNull(index))
			return 0;
		return getLong(index);
	}

	public State getMeetingState() {
		Integer index = getIndex(MeetingColumns.STATE);
		if (isNull(index))
			return State.NOT_STARTED;
		int stateInt = getInt(index);
		return State.values()[stateInt];
	}

	private Integer getIndex(String columnName) {
		Integer index = mColumnIndexes.get(columnName);
		if (index == null) {
			index = getColumnIndexOrThrow(columnName);
			mColumnIndexes.put(columnName, index);
		}
		return index;
	}
}
