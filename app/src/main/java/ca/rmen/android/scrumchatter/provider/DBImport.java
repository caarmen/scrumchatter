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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import ca.rmen.android.scrumchatter.util.Log;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.util.IOUtils;

public class DBImport {
    private static final String TAG = Constants.TAG + "/" + DBImport.class.getSimpleName();

    public static void importDB(Context context, Uri uri) throws RemoteException, OperationApplicationException, FileNotFoundException {
        if (uri.getScheme().equals("file")) {
            File db = new File(uri.getEncodedPath());
            importDB(context, db);
        } else {
            InputStream is = context.getContentResolver().openInputStream(uri);
            File tempDb = new File(context.getCacheDir(), "temp" + System.currentTimeMillis() + ".db");
            FileOutputStream os = new FileOutputStream(tempDb);
            if (IOUtils.copy(is, os)) {
                importDB(context, tempDb);
                if (!tempDb.delete()) {
                    Log.v(TAG, "Couldn't delete the temporary database " + tempDb);
                }
            }
        }
    }

    private static void importDB(Context context, File importDb) throws RemoteException, OperationApplicationException {
        Log.v(TAG, "importDB from " + importDb);
        SQLiteDatabase dbImport = SQLiteDatabase.openDatabase(importDb.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        operations.add(ContentProviderOperation.newDelete(MeetingMemberColumns.CONTENT_URI).build());
        operations.add(ContentProviderOperation.newDelete(MemberColumns.CONTENT_URI).build());
        operations.add(ContentProviderOperation.newDelete(MeetingColumns.CONTENT_URI).build());
        operations.add(ContentProviderOperation.newDelete(TeamColumns.CONTENT_URI).build());
        buildInsertOperations(dbImport, TeamColumns.CONTENT_URI, TeamColumns.TABLE_NAME, operations);
        buildInsertOperations(dbImport, MemberColumns.CONTENT_URI, MemberColumns.TABLE_NAME, operations);
        buildInsertOperations(dbImport, MeetingColumns.CONTENT_URI, MeetingColumns.TABLE_NAME, operations);
        buildInsertOperations(dbImport, MeetingMemberColumns.CONTENT_URI, MeetingMemberColumns.TABLE_NAME, operations);
        context.getContentResolver().applyBatch(ScrumChatterProvider.AUTHORITY, operations);
        // Set the first available team as our selected team
        Cursor c = context.getContentResolver().query(TeamColumns.CONTENT_URI, new String[] { TeamColumns._ID }, null, null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                int teamId = c.getInt(0);
                PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(Constants.PREF_TEAM_ID, teamId).apply();
            }
            c.close();
        }
        dbImport.close();
    }

    private static void buildInsertOperations(SQLiteDatabase dbImport, Uri uri, String table, ArrayList<ContentProviderOperation> operations) {
        Log.v(TAG, "buildInsertOperations: uri = " + uri + ", table=" + table);
        Cursor c = dbImport.query(false, table, null, null, null, null, null, null, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    int columnCount = c.getColumnCount();
                    do {
                        Builder builder = ContentProviderOperation.newInsert(uri);
                        for (int i = 0; i < columnCount; i++) {
                            String columnName = c.getColumnName(i);
                            Object value = c.getString(i);
                            builder.withValue(columnName, value);
                        }
                        operations.add(builder.build());
                    } while (c.moveToNext());
                }
            } finally {
                c.close();
            }
        }

    }
}
