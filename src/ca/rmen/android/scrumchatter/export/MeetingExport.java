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
package ca.rmen.android.scrumchatter.export;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.format.DateUtils;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.provider.MeetingColumns;
import ca.rmen.android.scrumchatter.provider.MeetingCursorWrapper;
import ca.rmen.android.scrumchatter.provider.MeetingMemberColumns;
import ca.rmen.android.scrumchatter.provider.MeetingMemberCursorWrapper;
import ca.rmen.android.scrumchatter.provider.MemberColumns;
import ca.rmen.android.scrumchatter.util.TextUtils;

public class MeetingExport {

	private Context mContext;

	public MeetingExport(Context context) {
		mContext = context;
	}

	public String exportMeeting(long meetingId) {
		StringBuilder sb = new StringBuilder();
		// Export info about the meeting (date, total duration)
		Cursor meetingCursor = mContext.getContentResolver().query(
				Uri.withAppendedPath(MeetingColumns.CONTENT_URI,
						String.valueOf(meetingId)),
				new String[] { MeetingColumns.MEETING_DATE,
						MeetingColumns.TOTAL_DURATION }, null, null, null);
		MeetingCursorWrapper meetingCursorWrapper = new MeetingCursorWrapper(
				meetingCursor);
		meetingCursorWrapper.moveToFirst();
		sb.append(mContext.getString(
				R.string.export_meeting_date,
				TextUtils.formatDateTime(mContext,
						meetingCursorWrapper.getMeetingDate())));
		sb.append("\n");
		sb.append(mContext
				.getString(R.string.export_meeting_duration, DateUtils
						.formatElapsedTime(meetingCursorWrapper.getTotalDuration())));
		sb.append("\n");
		meetingCursorWrapper.close();

		// Export the member times:
		Cursor meetingMemberCursor = mContext.getContentResolver().query(
				Uri.withAppendedPath(MeetingMemberColumns.CONTENT_URI,
						String.valueOf(meetingId)),
				new String[] { MemberColumns.NAME,
						MeetingMemberColumns.DURATION },

				MeetingMemberColumns.DURATION + ">0",
				null,
				MeetingMemberColumns.TABLE_NAME + "."
						+ MeetingMemberColumns.DURATION + " DESC ");
		MeetingMemberCursorWrapper meetingMemberCursorWrapper = new MeetingMemberCursorWrapper(
				meetingMemberCursor);
		if (meetingMemberCursorWrapper.moveToFirst()) {
			do {
				sb.append(meetingMemberCursorWrapper.getMemberName());
				sb.append(": ");
				sb.append(DateUtils
						.formatElapsedTime(meetingMemberCursorWrapper
								.getDuration()));
				sb.append("\n");
			} while (meetingMemberCursorWrapper.moveToNext());
		}
		meetingMemberCursorWrapper.close();
		return sb.toString();
	}
}
