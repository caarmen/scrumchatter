package ca.rmen.android.scrumchatter.provider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;
import ca.rmen.android.scrumchatter.Constants;

public class ScrumChatterProvider extends ContentProvider {
	private static final String TAG = Constants.TAG
			+ ScrumChatterProvider.class.getSimpleName();

	private static final String TYPE_CURSOR_ITEM = "vnd.android.cursor.item/";
	private static final String TYPE_CURSOR_DIR = "vnd.android.cursor.dir/";

	public static final String AUTHORITY = "ca.rmen.android.scrumchatter.provider";
	public static final String CONTENT_URI_BASE = "content://" + AUTHORITY;

	public static final String QUERY_NOTIFY = "QUERY_NOTIFY";
	public static final String QUERY_GROUP_BY = "QUERY_GROUP_BY";

	private static final int URI_TYPE_MEETING_MEMBER = 0;
	private static final int URI_TYPE_MEETING_MEMBER_ID = 1;

	private static final int URI_TYPE_MEMBER = 2;
	private static final int URI_TYPE_MEMBER_ID = 3;

	private static final int URI_TYPE_MEETING = 4;
	private static final int URI_TYPE_MEETING_ID = 5;

	private static final UriMatcher URI_MATCHER = new UriMatcher(
			UriMatcher.NO_MATCH);

	static {
		URI_MATCHER.addURI(AUTHORITY, MeetingMemberColumns.TABLE_NAME,
				URI_TYPE_MEETING_MEMBER);
		URI_MATCHER.addURI(AUTHORITY, MeetingMemberColumns.TABLE_NAME + "/#",
				URI_TYPE_MEETING_MEMBER_ID);

		URI_MATCHER
				.addURI(AUTHORITY, MemberColumns.TABLE_NAME, URI_TYPE_MEMBER);
		URI_MATCHER.addURI(AUTHORITY, MemberColumns.TABLE_NAME + "/#",
				URI_TYPE_MEMBER_ID);

		URI_MATCHER.addURI(AUTHORITY, MeetingColumns.TABLE_NAME,
				URI_TYPE_MEETING);
		URI_MATCHER.addURI(AUTHORITY, MeetingColumns.TABLE_NAME + "/#",
				URI_TYPE_MEETING_ID);

	}

	private ScrumChatterDatabase mScrumChatterDatabase;

	@Override
	public boolean onCreate() {
		mScrumChatterDatabase = new ScrumChatterDatabase(getContext());
		return true;
	}

	@Override
	public String getType(Uri uri) {
		final int match = URI_MATCHER.match(uri);
		switch (match) {
		case URI_TYPE_MEETING_MEMBER:
			return TYPE_CURSOR_DIR + MeetingMemberColumns.TABLE_NAME;
		case URI_TYPE_MEETING_MEMBER_ID:
			return TYPE_CURSOR_ITEM + MeetingMemberColumns.TABLE_NAME;

		case URI_TYPE_MEMBER:
			return TYPE_CURSOR_DIR + MemberColumns.TABLE_NAME;
		case URI_TYPE_MEMBER_ID:
			return TYPE_CURSOR_ITEM + MemberColumns.TABLE_NAME;

		case URI_TYPE_MEETING:
			return TYPE_CURSOR_DIR + MeetingColumns.TABLE_NAME;
		case URI_TYPE_MEETING_ID:
			return TYPE_CURSOR_ITEM + MeetingColumns.TABLE_NAME;

		}
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		Log.d(TAG, "insert uri=" + uri + " values=" + values);
		final String table = uri.getLastPathSegment();
		final long rowId = mScrumChatterDatabase.getWritableDatabase().insert(
				table, null, values);
		// When we insert a row into the meeting table, we have to add
		// all existing members to this meeting.
		if (table.equals(MeetingColumns.TABLE_NAME)) {
			Cursor members = mScrumChatterDatabase.getReadableDatabase().query(
					MemberColumns.TABLE_NAME,
					new String[] { MemberColumns._ID }, null, null, null, null,
					null);
			if (members != null) {
				ContentValues[] newMeetingMembers = new ContentValues[members
						.getCount()];
				if (members.moveToFirst()) {
					int i = 0;
					do {
						long memberId = members.getLong(0);
						values = new ContentValues();
						values.put(MeetingMemberColumns.MEMBER_ID, memberId);
						values.put(MeetingMemberColumns.MEETING_ID, rowId);
						values.put(MeetingMemberColumns.DURATION, 0L);
						newMeetingMembers[i++] = values;
					} while (members.moveToNext());
				}
				bulkInsert(MeetingMemberColumns.CONTENT_URI, newMeetingMembers);
				members.close();
			}
		}
		if (rowId != -1)
			notifyChange(uri);

		return uri.buildUpon().appendEncodedPath(String.valueOf(rowId)).build();
	}

	@Override
	public int bulkInsert(Uri uri, ContentValues[] values) {
		Log.d(TAG, "bulkInsert uri=" + uri + " values.length=" + values.length);
		final String table = uri.getLastPathSegment();
		final SQLiteDatabase db = mScrumChatterDatabase.getWritableDatabase();
		int res = 0;
		db.beginTransaction();
		try {
			for (final ContentValues v : values) {
				final long id = db.insert(table, null, v);
				if (id != -1) {
					res++;
				}
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		if (res != 0)
			notifyChange(uri);

		return res;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		Log.d(TAG, "update uri=" + uri + " values=" + values + " selection="
				+ selection);
		final QueryParams queryParams = getQueryParams(uri, selection);
		final int res = mScrumChatterDatabase.getWritableDatabase()
				.update(queryParams.table, values, queryParams.selection,
						selectionArgs);
		if (res != 0)
			notifyChange(uri);
		return res;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		Log.d(TAG, "delete uri=" + uri + " selection=" + selection);
		final QueryParams queryParams = getQueryParams(uri, selection);
		final int res = mScrumChatterDatabase.getWritableDatabase().delete(
				queryParams.table, queryParams.selection, selectionArgs);
		if (res != 0)
			notifyChange(uri);
		return res;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		final String groupBy = uri.getQueryParameter(QUERY_GROUP_BY);
		Log.d(TAG,
				"query uri=" + uri + "projection = "
						+ Arrays.toString(projection) + " selection="
						+ selection + " sortOrder=" + sortOrder + " groupBy="
						+ groupBy);
		final QueryParams queryParams = getQueryParams(uri, selection);
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(queryParams.table);
		if (queryParams.projectionMap != null
				&& !queryParams.projectionMap.isEmpty())
			qb.setProjectionMap(queryParams.projectionMap);

		// @formatter:off
		final Cursor res = qb.query(
				mScrumChatterDatabase.getReadableDatabase(),
				projection == null ? queryParams.projection : projection,
				selection == null ? queryParams.selection : selection,
				selectionArgs == null ? queryParams.selectionArgs
						: selectionArgs, groupBy == null ? queryParams.groupBy
						: groupBy, null,
				sortOrder == null ? queryParams.orderBy : sortOrder);
		// @formatter:on
		res.setNotificationUri(getContext().getContentResolver(), uri);
		return res;
	}

	private void notifyChange(Uri uri) {
		String notify = uri.getQueryParameter(QUERY_NOTIFY);
		if (notify == null || "true".equals(notify)) {
			final String table = uri.getLastPathSegment();
			getContext().getContentResolver().notifyChange(uri, null);
			if (table.equals(MemberColumns.TABLE_NAME))
				getContext().getContentResolver().notifyChange(
						MeetingMemberColumns.CONTENT_URI, null);
		}
	}

	private static class QueryParams {
		public String table;
		public String[] projection;
		public String selection;
		public String[] selectionArgs;
		public String orderBy;
		public String groupBy;
		public Map<String, String> projectionMap;
	}

	private QueryParams getQueryParams(Uri uri, String selection) {
		QueryParams res = new QueryParams();
		String id = null;
		int matchedId = URI_MATCHER.match(uri);
		res.projectionMap = new HashMap<String, String>();
		switch (matchedId) {
		case URI_TYPE_MEETING_MEMBER:
			res.projectionMap.put(MeetingMemberColumns.AVG_DURATION, "avg("
					+ MeetingMemberColumns.TABLE_NAME + "."
					+ MeetingMemberColumns.DURATION + ") AS "
					+ MeetingMemberColumns.AVG_DURATION);
			res.projectionMap.put(MeetingMemberColumns.SUM_DURATION, "sum("
					+ MeetingMemberColumns.TABLE_NAME + "."
					+ MeetingMemberColumns.DURATION + ") AS "
					+ MeetingMemberColumns.SUM_DURATION);
		case URI_TYPE_MEETING_MEMBER_ID:
			String memberIdColumn = MemberColumns.TABLE_NAME + "."
					+ MemberColumns._ID;
			String meetingMemberIdColumn = MeetingMemberColumns.TABLE_NAME
					+ "." + MeetingMemberColumns.MEMBER_ID;
			res.table = MemberColumns.TABLE_NAME + " LEFT OUTER JOIN "
					+ MeetingMemberColumns.TABLE_NAME + " ON " + memberIdColumn
					+ " = " + meetingMemberIdColumn;
			res.projection = new String[] { MemberColumns._ID };
			if (matchedId == URI_TYPE_MEETING_MEMBER_ID) {
				String meetingId = uri.getLastPathSegment();
				res.selection = MeetingMemberColumns.MEETING_ID + "=?";
				res.selectionArgs = new String[] { meetingId };
				res.table += " LEFT OUTER JOIN " + MeetingColumns.TABLE_NAME
						+ " ON " + MeetingColumns.TABLE_NAME + "."
						+ MeetingColumns._ID + " = "
						+ MeetingMemberColumns.TABLE_NAME + "."
						+ MeetingMemberColumns.MEETING_ID;
				res.projectionMap.put(MeetingColumns.STATE,
						MeetingColumns.STATE);
			}
			res.orderBy = MemberColumns.DEFAULT_ORDER;
			res.groupBy = MemberColumns.TABLE_NAME + "." + MemberColumns._ID;
			res.projectionMap.put(MemberColumns._ID, memberIdColumn);
			res.projectionMap.put(MemberColumns.NAME, MemberColumns.TABLE_NAME
					+ "." + MemberColumns.NAME);
			res.projectionMap.put(MeetingMemberColumns.DURATION,
					MeetingMemberColumns.TABLE_NAME + "."
							+ MeetingMemberColumns.DURATION + " AS "
							+ MeetingMemberColumns.DURATION);
			break;

		case URI_TYPE_MEMBER:
		case URI_TYPE_MEMBER_ID:
			res.table = MemberColumns.TABLE_NAME;
			res.orderBy = MemberColumns.DEFAULT_ORDER;
			break;

		case URI_TYPE_MEETING:
		case URI_TYPE_MEETING_ID:
			res.table = MeetingColumns.TABLE_NAME;
			res.orderBy = MeetingColumns.DEFAULT_ORDER;
			break;

		default:
			throw new IllegalArgumentException("The uri '" + uri
					+ "' is not supported by this ContentProvider");
		}

		switch (matchedId) {
		case URI_TYPE_MEMBER_ID:
		case URI_TYPE_MEETING_ID:
			id = uri.getLastPathSegment();

			if (id != null) {
				if (selection != null) {
					res.selection = BaseColumns._ID + "=" + id + " and ("
							+ selection + ")";
				} else {
					res.selection = BaseColumns._ID + "=" + id;
				}
			} else {
				res.selection = selection;
			}
		}
		return res;
	}

	public static Uri notify(Uri uri, boolean notify) {
		return uri.buildUpon()
				.appendQueryParameter(QUERY_NOTIFY, String.valueOf(notify))
				.build();
	}

	public static Uri groupBy(Uri uri, String groupBy) {
		return uri.buildUpon().appendQueryParameter(QUERY_GROUP_BY, groupBy)
				.build();
	}
}
