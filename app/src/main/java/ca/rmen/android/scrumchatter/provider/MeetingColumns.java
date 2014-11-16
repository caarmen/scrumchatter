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

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Table, column names, and column types (enums) for the meeting table.
 * 
 * A row in this table contains attributes of a meeting.
 */
public class MeetingColumns implements BaseColumns {
    static final String TABLE_NAME = "meeting";
    public static final Uri CONTENT_URI = Uri.parse(ScrumChatterProvider.CONTENT_URI_BASE + "/" + TABLE_NAME);

    static final String _ID = BaseColumns._ID;

    public static final String TEAM_ID = "meeting_team_id";
    public static final String MEETING_DATE = "meeting_date";
    public static final String TOTAL_DURATION = "total_duration";
    public static final String STATE = "state";

    static final String DEFAULT_ORDER = _ID;

    public enum State {
        NOT_STARTED, IN_PROGRESS, FINISHED
    }
}