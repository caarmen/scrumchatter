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
package ca.rmen.android.scrumchatter.provider;

import java.util.HashMap;

import android.database.Cursor;
import android.database.CursorWrapper;
import ca.rmen.android.scrumchatter.provider.MeetingColumns.State;

public class MeetingMemberCursorWrapper extends CursorWrapper {
    private final HashMap<String, Integer> mColumnIndexes = new HashMap<>();

    public MeetingMemberCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    public long getMeetingId() {
        return getLongField(MeetingMemberColumns.MEETING_ID);
    }

    public long getMemberId() {
        return getLongField(MeetingMemberColumns.MEMBER_ID);
    }

    public String getMemberName() {
        Integer index = getIndex(MemberColumns.NAME);
        if (isNull(index)) return null;
        return getString(index);
    }

    public long getDuration() {
        return getLongField(MeetingMemberColumns.DURATION);
    }

    public long getTotalDuration() {
        return getLongField(MeetingColumns.TOTAL_DURATION);
    }

    public long getTalkStartTime() {
        return getLongField(MeetingMemberColumns.TALK_START_TIME);
    }

    public long getMeetingDate() {
        return getLongField(MeetingColumns.MEETING_DATE);
    }

    public State getMeetingState() {
        Integer index = getIndex(MeetingColumns.STATE);
        if (isNull(index)) return State.NOT_STARTED;
        int stateInt = getInt(index);
        return State.values()[stateInt];
    }

    private long getLongField(String columnName) {
        Integer index = getIndex(columnName);
        if (isNull(index)) return 0;
        return getLong(index);
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
