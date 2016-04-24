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

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQuery;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.util.Log;

/**
 * Provider for the Scrum Chatter app. This provider provides access to the
 * member, meeting, and meeting_member tables, and the member_stats view.
 * 
 * Part of this class was generated using the Android Content Provider
 * Generator: https://github.com/BoD/android-contentprovider-generator
 */
public class ScrumChatterProvider extends ContentProvider {
    private static final String TAG = Constants.TAG + ScrumChatterProvider.class.getSimpleName();

    private static final String TYPE_CURSOR_ITEM = "vnd.android.cursor.item/";
    private static final String TYPE_CURSOR_DIR = "vnd.android.cursor.dir/";

    public static final String AUTHORITY = "ca.rmen.android.scrumchatter.provider";
    static final String CONTENT_URI_BASE = "content://" + AUTHORITY;

    private static final String QUERY_NOTIFY = "QUERY_NOTIFY"; // NO_UCD (use private)
    private static final String QUERY_GROUP_BY = "QUERY_GROUP_BY"; // NO_UCD (use private)

    private static final int URI_TYPE_TEAM = 0;
    private static final int URI_TYPE_TEAM_ID = 1;

    private static final int URI_TYPE_MEETING_MEMBER = 2;
    private static final int URI_TYPE_MEETING_MEMBER_ID = 3;

    private static final int URI_TYPE_MEMBER = 4;
    private static final int URI_TYPE_MEMBER_ID = 5;

    private static final int URI_TYPE_MEETING = 6;
    private static final int URI_TYPE_MEETING_ID = 7;

    private static final int URI_TYPE_MEMBER_STATS = 8;

    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URI_MATCHER.addURI(AUTHORITY, TeamColumns.TABLE_NAME, URI_TYPE_TEAM);
        URI_MATCHER.addURI(AUTHORITY, TeamColumns.TABLE_NAME + "/#", URI_TYPE_TEAM_ID);

        URI_MATCHER.addURI(AUTHORITY, MeetingMemberColumns.TABLE_NAME, URI_TYPE_MEETING_MEMBER);
        URI_MATCHER.addURI(AUTHORITY, MeetingMemberColumns.TABLE_NAME + "/#", URI_TYPE_MEETING_MEMBER_ID);

        URI_MATCHER.addURI(AUTHORITY, MemberColumns.TABLE_NAME, URI_TYPE_MEMBER);
        URI_MATCHER.addURI(AUTHORITY, MemberColumns.TABLE_NAME + "/#", URI_TYPE_MEMBER_ID);

        URI_MATCHER.addURI(AUTHORITY, MeetingColumns.TABLE_NAME, URI_TYPE_MEETING);
        URI_MATCHER.addURI(AUTHORITY, MeetingColumns.TABLE_NAME + "/#", URI_TYPE_MEETING_ID);

        URI_MATCHER.addURI(AUTHORITY, MemberStatsColumns.VIEW_NAME, URI_TYPE_MEMBER_STATS);

    }

    private ScrumChatterDatabase mScrumChatterDatabase;
    private Context mContext;

    @Override
    public boolean onCreate() {
        mScrumChatterDatabase = new ScrumChatterDatabase(getContext());
        // Save a copy of the context, because if we call getContext() later on,
        // Android Studio warns us that calling getContext() can return null, even
        // though this isn't really the case (getContext() can only be null before
        // onCreate() is called).
        mContext = getContext();
        return true;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        final int match = URI_MATCHER.match(uri);
        switch (match) {
            case URI_TYPE_TEAM:
                return TYPE_CURSOR_DIR + TeamColumns.TABLE_NAME;
            case URI_TYPE_TEAM_ID:
                return TYPE_CURSOR_ITEM + TeamColumns.TABLE_NAME;

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

            case URI_TYPE_MEMBER_STATS:
                return TYPE_CURSOR_ITEM + MemberStatsColumns.VIEW_NAME;

        }
        return null;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        Log.d(TAG, "insert uri=" + uri + " values=" + values);
        final String table = uri.getLastPathSegment();
        SQLiteDatabase db = mScrumChatterDatabase.getWritableDatabase();
        final long rowId = db.insert(table, null, values);
        // When we insert a row into the meeting table, we have to add
        // all existing members to this meeting. To do this, we create
        // one row for each member into the meeting_member table for this team.
        if (table.equals(MeetingColumns.TABLE_NAME)) {
            int teamId = values.getAsInteger(MeetingColumns.TEAM_ID);
            Cursor members = db.query(MemberColumns.TABLE_NAME, new String[] { MemberColumns._ID }, MemberColumns.TEAM_ID + "=? AND " + MemberColumns.DELETED
                    + "=0 ", new String[] { String.valueOf(teamId) }, null, null, null);
            if (members != null) {
                ContentValues[] newMeetingMembers = new ContentValues[members.getCount()];
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
        if (rowId != -1 && !db.inTransaction()) notifyChange(uri);

        Uri result = uri.buildUpon().appendEncodedPath(String.valueOf(rowId)).build();
        Log.v(TAG, "Created row with uri " + result);
        return result;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
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
        if (res != 0 && !db.inTransaction()) notifyChange(uri);

        return res;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        StatementParams params = getStatementParams(uri, selection);
        Log.d(TAG, "update uri=" + uri + " values=" + values + " selection=" + selection + ", selectionArgs = " + Arrays.toString(selectionArgs));
        SQLiteDatabase db = mScrumChatterDatabase.getWritableDatabase();
        final int res = db.update(params.table, values, params.selection, selectionArgs);
        if (res != 0 && !db.inTransaction()) notifyChange(uri);
        return res;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        Log.d(TAG, "delete uri=" + uri + " selection=" + selection);
        StatementParams params = getStatementParams(uri, selection);
        SQLiteDatabase db = mScrumChatterDatabase.getWritableDatabase();
        final int res = db.delete(params.table, params.selection, selectionArgs);
        if (res != 0 && !db.inTransaction()) notifyChange(uri);
        return res;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        final String groupBy = uri.getQueryParameter(QUERY_GROUP_BY);
        Log.d(TAG,
                "query uri=" + uri + ", projection = " + Arrays.toString(projection) + " selection=" + selection + " selectionArgs = "
                        + Arrays.toString(selectionArgs) + " sortOrder=" + sortOrder + " groupBy=" + groupBy);
        final QueryParams queryParams = getQueryParams(uri, selection);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(queryParams.table);

        final Cursor res = qb.query(
                mScrumChatterDatabase.getReadableDatabase(), projection,
                queryParams.selection, selectionArgs, groupBy, null,
                sortOrder == null ? queryParams.orderBy : sortOrder);
        logCursor(res, selectionArgs);
        res.setNotificationUri(mContext.getContentResolver(), uri);

        return res;
    }

    /**
     * Perform all operations in a single transaction and notify all relevant URIs at the end. The {@link MemberStatsColumns#CONTENT_URI} uri is always notified
     * for a successful transaction.
     * 
     * @see android.content.ContentProvider#applyBatch(java.util.ArrayList)
     */
    @Override
    @NonNull public ContentProviderResult[] applyBatch(@NonNull ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        Log.v(TAG, "applyBatch: " + operations);
        Set<Uri> urisToNotify = new HashSet<>();
        for (ContentProviderOperation operation : operations)
            urisToNotify.add(operation.getUri());
        urisToNotify.add(MemberStatsColumns.CONTENT_URI);
        Log.v(TAG, "applyBatch: will notify these uris after persisting: " + urisToNotify);
        SQLiteDatabase db = mScrumChatterDatabase.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentProviderResult[] result = super.applyBatch(operations);
            db.setTransactionSuccessful();
            for (Uri uri : urisToNotify)
                notifyChange(uri);
            return result;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Log the query of the given cursor.
     */
    private void logCursor(Cursor cursor, String[] selectionArgs) {
        try {
            Field queryField = SQLiteCursor.class.getDeclaredField("mQuery");
            queryField.setAccessible(true);
            SQLiteQuery sqliteQuery = (SQLiteQuery) queryField.get(cursor);
            Log.v(TAG, sqliteQuery.toString() + ": " + Arrays.toString(selectionArgs));
        } catch (Exception e) {
            Log.v(TAG, e.getMessage(), e);
        }
    }

    private void notifyChange(Uri uri) {
        String notify = uri.getQueryParameter(QUERY_NOTIFY);
        Log.v(TAG, "notifyChange: uri = " + uri + ", notify = " + notify);
        if (notify == null || "true".equals(notify)) {
            // Notify the uri which changed.
            Set<Uri> urisToNotify = new HashSet<>();
            urisToNotify.add(uri);

            // Whether a meeting, meeting_member, or meeting table was
            // modified, update the member_stats view.
            urisToNotify.add(MemberStatsColumns.CONTENT_URI);

            // Notify other uris if they depend on the given uri which just
            // changed.
            int matchedId = URI_MATCHER.match(uri);
            // If a member changed, notify the the meeting_member uri.
            if (matchedId == URI_TYPE_MEMBER_ID || matchedId == URI_TYPE_MEMBER) {
                urisToNotify.add(MeetingMemberColumns.CONTENT_URI);
            }
            // If a meeting changed, notify the meeting_member uri, including
            // the meeting id in the uri to notify,
            // if the given uri is for a specific meeting.
            else if (matchedId == URI_TYPE_MEETING_ID || matchedId == URI_TYPE_MEETING) {
                Uri meetingMemberUriToNotify = MeetingMemberColumns.CONTENT_URI;
                // A specific meeting changed, notify meeting_member for that
                // meeting.
                if (matchedId == URI_TYPE_MEETING_ID) {
                    String meetingId = uri.getLastPathSegment();
                    meetingMemberUriToNotify = meetingMemberUriToNotify.buildUpon().appendPath(meetingId).build();
                }
                urisToNotify.add(meetingMemberUriToNotify);
            }

            // Notify all the relevant uris.
            for (Uri uriToNotify : urisToNotify) {
                Log.v(TAG, "notifyChange: notify uri " + uriToNotify);
                mContext.getContentResolver().notifyChange(uriToNotify, null);
            }
        }
    }

    /**
     * To be used for updates and deletes
     */
    private static class StatementParams {
        public String table;
        public String selection;

    }

    /**
     * To be used for queries
     */
    private static class QueryParams extends StatementParams {
        public String orderBy;
    }

    /**
     * Used for write operations (insert, update, delete).
     * 
     * Returns a StatementParams containing the table name and possibly a
     * selection, depending on whether a selection was provided by the user, and
     * if an id was provided in the Uri.
     * 
     * @param uri
     *            provided by the user of the ContentProvider. If the uri
     *            contains a path with an id, we will add the id to the
     *            selection.
     * @param selection
     *            provided by the user of the ContentProvider
     * @return the table and selection to use based on the uri and selection
     *         provided by the user of the ContentProvider.
     */
    private StatementParams getStatementParams(Uri uri, String selection) {
        StatementParams res = new StatementParams();
        String id = null;
        int matchedId = URI_MATCHER.match(uri);
        switch (matchedId) {
            case URI_TYPE_TEAM_ID:
                id = uri.getLastPathSegment();
            case URI_TYPE_TEAM:
                res.table = TeamColumns.TABLE_NAME;
                break;
            case URI_TYPE_MEETING_MEMBER_ID:
            case URI_TYPE_MEETING_MEMBER:
                res.table = MeetingMemberColumns.TABLE_NAME;
                break;
            case URI_TYPE_MEMBER_ID:
                id = uri.getLastPathSegment();
            case URI_TYPE_MEMBER:
                res.table = MemberColumns.TABLE_NAME;
                break;
            case URI_TYPE_MEETING_ID:
                id = uri.getLastPathSegment();
            case URI_TYPE_MEETING:
                res.table = MeetingColumns.TABLE_NAME;
                break;

            default:
                throw new IllegalArgumentException("The uri '" + uri + "' is not supported by this ContentProvider");
        }

        if (id != null) {
            if (selection != null) res.selection = BaseColumns._ID + "=" + id + " and (" + selection + ")";
            else
                res.selection = BaseColumns._ID + "=" + id;
        } else {
            res.selection = selection;
        }

        return res;
    }

    /**
     * Used for read operations (select).
     * 
     * @param uri
     *            provided by the user of the ContentProvider. If the uri
     *            contains a path with an id, we will add the id to the
     *            selection.
     * @param selection
     *            provided by the user of the ContentProvider
     * @return the full QueryParams based on the Uri and selection provided by
     *         the user of the ContentProvider.
     */
    private QueryParams getQueryParams(Uri uri, String selection) {
        QueryParams res = new QueryParams();
        String id = null;
        int matchedId = URI_MATCHER.match(uri);
        res.selection = selection;
        switch (matchedId) {
        // The meeting_member table is a join table between the meeting and
        // member tables.
        // This table does not have an _id field. If the Uri contains an id,
        // this will be used as the meeting id.
            case URI_TYPE_MEETING_MEMBER:
            case URI_TYPE_MEETING_MEMBER_ID:
                // The join contains the member, meeting_member, and meeting tables.
                res.table = MemberColumns.TABLE_NAME + " LEFT OUTER JOIN " + MeetingMemberColumns.TABLE_NAME + " ON " + MemberColumns.TABLE_NAME + "."
                        + MemberColumns._ID + " = " + MeetingMemberColumns.TABLE_NAME + "." + MeetingMemberColumns.MEMBER_ID + " LEFT OUTER JOIN "
                        + MeetingColumns.TABLE_NAME + " ON " + MeetingColumns.TABLE_NAME + "." + MeetingColumns._ID + " = " + MeetingMemberColumns.TABLE_NAME
                        + "." + MeetingMemberColumns.MEETING_ID;

                // If a specific meeting is specified, append a selection
                // on the meeting id to the end of the existing selection
                if (matchedId == URI_TYPE_MEETING_MEMBER_ID) {
                    String meetingId = uri.getLastPathSegment();
                    res.selection = MeetingMemberColumns.MEETING_ID + "=" + meetingId;
                    if (selection != null) res.selection = selection + " AND (" + res.selection + ") ";
                }
                res.orderBy = MemberColumns.NAME;
                break;

            case URI_TYPE_TEAM_ID:
                id = uri.getLastPathSegment();
            case URI_TYPE_TEAM:
                res.table = TeamColumns.TABLE_NAME;
                res.orderBy = TeamColumns.DEFAULT_ORDER;
                break;

            case URI_TYPE_MEMBER_ID:
                id = uri.getLastPathSegment();
            case URI_TYPE_MEMBER:
                res.table = MemberColumns.TABLE_NAME;
                res.orderBy = MemberColumns.DEFAULT_ORDER;
                break;

            case URI_TYPE_MEETING_ID:
                id = uri.getLastPathSegment();
            case URI_TYPE_MEETING:
                res.table = MeetingColumns.TABLE_NAME;
                res.orderBy = MeetingColumns.DEFAULT_ORDER;
                break;

            case URI_TYPE_MEMBER_STATS:
                res.table = MemberStatsColumns.VIEW_NAME;
                res.orderBy = MemberStatsColumns.DEFAULT_ORDER;
                break;

            default:
                throw new IllegalArgumentException("The uri '" + uri + "' is not supported by this ContentProvider");
        }

        if (id != null) {
            if (selection != null) res.selection = BaseColumns._ID + "=" + id + " and (" + selection + ")";
            else
                res.selection = BaseColumns._ID + "=" + id;
        }

        return res;
    }
}
