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
 * Table, column names, and column types (enums) for the member table.
 * 
 * A row in this table contains attributes of a single team member.
 */
public class MemberStatsColumns implements BaseColumns {
    static final String VIEW_NAME = "member_stats";
    public static final Uri CONTENT_URI = Uri.parse(ScrumChatterProvider.CONTENT_URI_BASE + "/" + VIEW_NAME);

    private static final String _ID = BaseColumns._ID;
    public static final String TEAM_ID = "team_id";
    public static final String SUM_DURATION = "sum_duration";
    public static final String AVG_DURATION = "avg_duration";

    static final String DEFAULT_ORDER = _ID;
}