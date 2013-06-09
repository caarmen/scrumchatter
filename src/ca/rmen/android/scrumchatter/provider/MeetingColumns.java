package ca.rmen.android.scrumchatter.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class MeetingColumns implements BaseColumns {
    public static final String TABLE_NAME = "meeting";
    public static final Uri CONTENT_URI = Uri.parse(ScrumChatterProvider.CONTENT_URI_BASE + "/" + TABLE_NAME);

    public static final String _ID = BaseColumns._ID;

    public static final String MEETING_DATE = "meeting_date";
    public static final String DURATION = "duration";
    public static final String AGE = "age";

    public static final String DEFAULT_ORDER = _ID;
}