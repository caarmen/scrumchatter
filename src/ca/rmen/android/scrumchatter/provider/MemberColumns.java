package ca.rmen.android.scrumchatter.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class MemberColumns implements BaseColumns {
    public static final String TABLE_NAME = "member";
    public static final Uri CONTENT_URI = Uri.parse(ScrumChatterProvider.CONTENT_URI_BASE + "/" + TABLE_NAME);

    public static final String _ID = BaseColumns._ID;

    public static final String NAME = "name";

    public static final String DEFAULT_ORDER = _ID;
}