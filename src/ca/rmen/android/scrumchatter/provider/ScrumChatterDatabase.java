package ca.rmen.android.scrumchatter.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import ca.rmen.android.scrumchatter.Constants;

public class ScrumChatterDatabase extends SQLiteOpenHelper {
	private static final String TAG = Constants.TAG
			+ ScrumChatterDatabase.class.getSimpleName();

	public static final String DATABASE_NAME = "scrumchatter.db";
	private static final int DATABASE_VERSION = 1;

	// @formatter:off
	private static final String SQL_CREATE_TABLE_MEETING_MEMBER = "CREATE TABLE IF NOT EXISTS "
			+ MeetingMemberColumns.TABLE_NAME
			+ " ( "
			+ MeetingMemberColumns._ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ MeetingMemberColumns.MEETING_ID
			+ " INTEGER, "
			+ MeetingMemberColumns.MEMBER_ID
			+ " INTEGER, "
			+ MeetingMemberColumns.DURATION
			+ " INTEGER "
			+ ", CONSTRAINT UNIQUE_MEETING_MEMBER UNIQUE ( MEETING_ID, MEMBER_ID ) ON CONFLICT REPLACE"
			+ ", CONSTRAINT MEETING_ID_FK FOREIGN KEY (MEETING_ID) REFERENCES MEETING(_ID)"
			+ ", CONSTRAINT MEMBER_ID_FK FOREIGN KEY (MEMBER_ID) REFERENCES MEMBER(_ID)"
			+ " );";

	private static final String SQL_CREATE_TABLE_MEMBER = "CREATE TABLE IF NOT EXISTS "
			+ MemberColumns.TABLE_NAME
			+ " ( "
			+ MemberColumns._ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ MemberColumns.NAME
			+ " TEXT " + " );";

	private static final String SQL_CREATE_TABLE_MEETING = "CREATE TABLE IF NOT EXISTS "
			+ MeetingColumns.TABLE_NAME
			+ " ( "
			+ MeetingColumns._ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ MeetingColumns.MEETING_DATE
			+ " INTEGER, "
			+ MeetingColumns.DURATION
			+ " INTEGER, "
			+ MeetingColumns.AGE
			+ " INTEGER " + " );";

	// @formatter:on

	public ScrumChatterDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d(TAG, "onCreate");
		db.execSQL(SQL_CREATE_TABLE_MEETING_MEMBER);
		db.execSQL(SQL_CREATE_TABLE_MEMBER);
		db.execSQL(SQL_CREATE_TABLE_MEETING);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d(TAG, "Upgrading database from version " + oldVersion + " to "
				+ newVersion);
	}
}
