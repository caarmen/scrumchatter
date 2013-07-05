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
package ca.rmen.android.scrumchatter.provider;

import java.io.File;
import java.util.ArrayList;

import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;
import ca.rmen.android.scrumchatter.Constants;

public class DBImport {
    private static final String TAG = Constants.TAG + "/" + DBImport.class.getSimpleName();

    public static void importDB(Context context, File importDb) throws RemoteException, OperationApplicationException {
        Log.v(TAG, "importDB from " + importDb);
        SQLiteDatabase dbImport = SQLiteDatabase.openDatabase(importDb.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        operations.add(ContentProviderOperation.newDelete(MeetingMemberColumns.CONTENT_URI).build());
        operations.add(ContentProviderOperation.newDelete(MemberColumns.CONTENT_URI).build());
        operations.add(ContentProviderOperation.newDelete(MeetingColumns.CONTENT_URI).build());
        buildInsertOperations(dbImport, MemberColumns.CONTENT_URI, MemberColumns.TABLE_NAME, operations);
        buildInsertOperations(dbImport, MeetingColumns.CONTENT_URI, MeetingColumns.TABLE_NAME, operations);
        buildInsertOperations(dbImport, MeetingMemberColumns.CONTENT_URI, MeetingMemberColumns.TABLE_NAME, operations);
        context.getContentResolver().applyBatch(ScrumChatterProvider.AUTHORITY, operations);
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
