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

public class MemberCursorWrapper extends CursorWrapper {
    private final HashMap<String, Integer> mColumnIndexes = new HashMap<>();

    public MemberCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    public Long getId() {
        Integer index = getColumnIndex(MemberColumns._ID);
        if (isNull(index)) return null;
        return getLong(index);
    }

    public String getName() {
        Integer index = getIndex(MemberColumns.NAME);
        return getString(index);
    }

    public Integer getAverageDuration() {
        Integer index = getIndex(MemberStatsColumns.AVG_DURATION);
        return getInt(index);
    }

    public Integer getSumDuration() {
        Integer index = getIndex(MemberStatsColumns.SUM_DURATION);
        return getInt(index);
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
