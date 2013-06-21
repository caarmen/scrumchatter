package ca.rmen.android.scrumchatter.export;

import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.provider.MeetingColumns;
import ca.rmen.android.scrumchatter.provider.MeetingCursorWrapper;
import ca.rmen.android.scrumchatter.provider.MeetingMemberColumns;
import ca.rmen.android.scrumchatter.provider.MeetingMemberCursorWrapper;
import ca.rmen.android.scrumchatter.provider.MemberColumns;
import ca.rmen.android.scrumchatter.util.TextUtils;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.format.DateUtils;

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
						MeetingColumns.DURATION }, null, null, null);
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
						.formatElapsedTime(meetingCursorWrapper.getDuration())));
		sb.append("\n");
		meetingCursorWrapper.close();

		// Export the member times:
		Cursor meetingMemberCursor = mContext.getContentResolver().query(
				Uri.withAppendedPath(MeetingMemberColumns.CONTENT_URI,
						String.valueOf(meetingId)),
				new String[] {
						MemberColumns.NAME,
						MeetingMemberColumns.TABLE_NAME + "."
								+ MeetingMemberColumns.DURATION },
				MeetingMemberColumns.TABLE_NAME + "."
						+ MeetingMemberColumns.DURATION + ">0",
				null,
				MeetingMemberColumns.TABLE_NAME + "."
						+ MeetingMemberColumns.DURATION + " DESC ");
		MeetingMemberCursorWrapper meetingMemberCursorWrapper = new MeetingMemberCursorWrapper(meetingMemberCursor);
		if(meetingMemberCursorWrapper.moveToFirst()){
			do{
				sb.append(meetingMemberCursorWrapper.getMemberName());
				sb.append(": ");
				sb.append(DateUtils.formatElapsedTime(meetingMemberCursorWrapper.getDuration()));
				sb.append("\n");
			}while(meetingMemberCursorWrapper.moveToNext());
		}
		meetingMemberCursorWrapper.close();
		return sb.toString();
	}
}
