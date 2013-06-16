package ca.rmen.android.scrumchatter.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class CurrentMeetingColumns implements BaseColumns {
	public static final String TABLE_NAME = "current_meeting";
	public static final Uri CONTENT_URI = Uri
			.parse(ScrumChatterProvider.CONTENT_URI_BASE + "/" + TABLE_NAME);

	public static final String _ID = BaseColumns._ID;

	public static final String TALK_START_TIME = "talk_start_time";

	public static final String DEFAULT_ORDER = _ID;
}