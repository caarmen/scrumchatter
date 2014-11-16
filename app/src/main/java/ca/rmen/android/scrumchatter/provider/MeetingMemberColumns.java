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
 * Table, column names, and column types (enums) for the meeting_member table.
 * 
 * A row in this table contains information about one member's participation in
 * one meeting. This is a join table between the meeting and member tables. This
 * table will contain multiple entries for a given member, and multiple entries
 * for a given meeting.
 */
public class MeetingMemberColumns implements BaseColumns {
    static final String TABLE_NAME = "meeting_member";
    public static final Uri CONTENT_URI = Uri.parse(ScrumChatterProvider.CONTENT_URI_BASE + "/" + TABLE_NAME);

    public static final String _ID = MemberColumns.TABLE_NAME + "." + MemberColumns._ID;
    public static final String MEETING_ID = "meeting_id";
    public static final String MEMBER_ID = "member_id";
    public static final String DURATION = "duration";
    public static final String TALK_START_TIME = "talk_start_time";

}