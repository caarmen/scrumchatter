package ca.rmen.android.scrumchatter.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class MeetingMemberColumns implements BaseColumns {
	public static final String TABLE_NAME = "meeting_member";
	public static final Uri CONTENT_URI = Uri
			.parse(ScrumChatterProvider.CONTENT_URI_BASE + "/" + TABLE_NAME);

	public static final String MEETING_ID = "meeting_id";
	public static final String MEMBER_ID = "member_id";
	public static final String DURATION = "duration";

	public static final String SUM_DURATION = "sum_duration";
	public static final String AVG_DURATION = "avg_duration";
}