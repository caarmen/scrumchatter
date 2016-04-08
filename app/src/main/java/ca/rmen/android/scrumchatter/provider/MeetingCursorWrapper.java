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

public class MeetingCursorWrapper extends CursorWrapper {
    private final HashMap<String, Integer> mColumnIndexes = new HashMap<>();

    public MeetingCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    public Long getId() {
        Integer index = getIndex(MeetingColumns._ID);
        if (isNull(index)) return null;
        return getLong(index);
    }

    public Long getMeetingDate() {
        Integer index = getIndex(MeetingColumns.MEETING_DATE);
        if (isNull(index)) return null;
        return getLong(index);
    }

    public Long getTotalDuration() {
        Integer index = getIndex(MeetingColumns.TOTAL_DURATION);
        if (isNull(index)) return 0L;
        return getLong(index);
    }

    public MeetingColumns.State getState() {
        Integer index = getIndex(MeetingColumns.STATE);
        if (isNull(index)) return MeetingColumns.State.NOT_STARTED;
        int stateInt = getInt(index);
        return MeetingColumns.State.values()[stateInt];
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
