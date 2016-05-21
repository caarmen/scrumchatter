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
package ca.rmen.android.scrumchatter.util;

import android.content.Context;
import android.text.format.DateUtils;

public class TextUtils {
    public static String formatDateTime(Context context, long dateMillis) {
        return DateUtils.formatDateTime(context, dateMillis, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);
    }

    public static String formatDate(Context context, long dateMillis) {
        return DateUtils.formatDateTime(context, dateMillis, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL |DateUtils.FORMAT_NUMERIC_DATE);
    }

}
