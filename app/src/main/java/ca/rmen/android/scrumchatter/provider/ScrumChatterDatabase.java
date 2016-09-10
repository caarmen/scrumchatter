/**
 * Copyright 2013 Carmen Alvarez
 * <p/>
 * This file is part of Scrum Chatter.
 * <p/>
 * Scrum Chatter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * Scrum Chatter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with Scrum Chatter. If not, see <http://www.gnu.org/licenses/>.
 */
package ca.rmen.android.scrumchatter.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.util.Log;
import ca.rmen.android.scrumchatter.R;

/**
 * Creates and upgrades database tables.
 */
public class ScrumChatterDatabase extends SQLiteOpenHelper {
    private static final String TAG = Constants.TAG + ScrumChatterDatabase.class.getSimpleName();

    public static final String DATABASE_NAME = "scrumchatter.db";
    private static final int DATABASE_VERSION = 3;

    private static final String TEMP_SUFFIX = "_temp";

    private static final String SQL_CREATE_TABLE_TEAM = "CREATE TABLE IF NOT EXISTS "
            + TeamColumns.TABLE_NAME
            + " ( "
            + TeamColumns._ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + TeamColumns.TEAM_NAME + " TEXT"
            + " );";

    private static final String SQL_CREATE_TABLE_MEETING_MEMBER = "CREATE TABLE IF NOT EXISTS "
            + MeetingMemberColumns.TABLE_NAME
            + " ( "
            + MeetingMemberColumns.MEETING_ID
            + " INTEGER, "
            + MeetingMemberColumns.MEMBER_ID
            + " INTEGER, "
            + MeetingMemberColumns.DURATION
            + " INTEGER, "
            + MeetingMemberColumns.TALK_START_TIME
            + " INTEGER "
            + ", CONSTRAINT UNIQUE_MEETING_MEMBER UNIQUE ( MEETING_ID, MEMBER_ID ) ON CONFLICT REPLACE"
            + ", CONSTRAINT MEETING_ID_FK FOREIGN KEY (MEETING_ID) REFERENCES MEETING(_ID) ON DELETE CASCADE "
            + ", CONSTRAINT MEMBER_ID_FK FOREIGN KEY (MEMBER_ID) REFERENCES MEMBER(_ID) ON DELETE CASCADE"
            + " );";


    private static final String SQL_CREATE_TABLE_MEMBER = "CREATE TABLE IF NOT EXISTS "
            + MemberColumns.TABLE_NAME
            + " ( "
            + MemberColumns._ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + MemberColumns.NAME
            + " TEXT, "
            + MemberColumns.TEAM_ID
            + " INTEGER NOT NULL, "
            + MemberColumns.DELETED
            + " INTEGER NOT NULL, "
            + " CONSTRAINT TEAM_ID_FK FOREIGN KEY (" + MemberColumns.TEAM_ID + ") REFERENCES TEAM(" + TeamColumns._ID + ") ON DELETE CASCADE"
            + " );";

    private static final String SQL_CREATE_TABLE_MEMBER_TEMP = "CREATE TABLE "
            + MemberColumns.TABLE_NAME + TEMP_SUFFIX
            + " ( "
            + MemberColumns._ID
            + " INTEGER , "
            + MemberColumns.NAME
            + " TEXT "
            + " );";

    private static final String SQL_INSERT_TABLE_MEMBER_TEMP = "INSERT INTO "
            + MemberColumns.TABLE_NAME + TEMP_SUFFIX
            + " SELECT "
            + MemberColumns._ID
            + "," + MemberColumns.NAME
            + " FROM " + MemberColumns.TABLE_NAME;

    private static final String SQL_INSERT_TABLE_MEMBER = "INSERT INTO "
            + MemberColumns.TABLE_NAME
            + " SELECT "
            + MemberColumns._ID
            + "," + MemberColumns.NAME
            + "," + Constants.DEFAULT_TEAM_ID
            + " FROM " + MemberColumns.TABLE_NAME + TEMP_SUFFIX;

    private static final String SQL_ALTER_TABLE_MEMBER_V3 = "ALTER TABLE "
            + MemberColumns.TABLE_NAME
            + " ADD COLUMN "
            + MemberColumns.DELETED + " INTEGER NOT NULL DEFAULT 0";

    private static final String SQL_DROP_TABLE_MEMBER = "DROP TABLE " + MemberColumns.TABLE_NAME;
    private static final String SQL_DROP_TABLE_MEMBER_TEMP = "DROP TABLE " + MemberColumns.TABLE_NAME + TEMP_SUFFIX;

    private static final String SQL_CREATE_TABLE_MEETING = "CREATE TABLE IF NOT EXISTS "
            + MeetingColumns.TABLE_NAME
            + " ( "
            + MeetingColumns._ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + MeetingColumns.MEETING_DATE
            + " INTEGER, "
            + MeetingColumns.TOTAL_DURATION
            + " INTEGER, "
            + MeetingColumns.STATE
            + " INTEGER NOT NULL DEFAULT "
            + MeetingColumns.State.NOT_STARTED.ordinal() + ", "
            + MeetingColumns.TEAM_ID
            + " INTEGER NOT NULL, "
            + " CONSTRAINT TEAM_ID_FK FOREIGN KEY(" + MeetingColumns.TEAM_ID + ") REFERENCES " + TeamColumns.TABLE_NAME + "(" + TeamColumns._ID + ") ON DELETE CASCADE"
            + " );";

    private static final String SQL_CREATE_TABLE_MEETING_TEMP = "CREATE TABLE "
            + MeetingColumns.TABLE_NAME + TEMP_SUFFIX
            + " ( "
            + MeetingColumns._ID
            + " INTEGER , "
            + MeetingColumns.MEETING_DATE
            + " INTEGER,  "
            + MeetingColumns.TOTAL_DURATION
            + " INTEGER,  "
            + MeetingColumns.STATE
            + " INTEGER "
            + " );";

    private static final String SQL_INSERT_TABLE_MEETING_TEMP = "INSERT INTO "
            + MeetingColumns.TABLE_NAME + TEMP_SUFFIX
            + " SELECT "
            + MeetingColumns._ID
            + "," + MeetingColumns.MEETING_DATE
            + "," + MeetingColumns.TOTAL_DURATION
            + "," + MeetingColumns.STATE
            + " FROM " + MeetingColumns.TABLE_NAME;

    private static final String SQL_INSERT_TABLE_MEETING = "INSERT INTO "
            + MeetingColumns.TABLE_NAME
            + " SELECT "
            + MeetingColumns._ID
            + "," + MeetingColumns.MEETING_DATE
            + "," + MeetingColumns.TOTAL_DURATION
            + "," + MeetingColumns.STATE
            + "," + Constants.DEFAULT_TEAM_ID
            + " FROM " + MeetingColumns.TABLE_NAME + TEMP_SUFFIX;

    private static final String SQL_DROP_TABLE_MEETING = "DROP TABLE " + MeetingColumns.TABLE_NAME;
    private static final String SQL_DROP_TABLE_MEETING_TEMP = "DROP TABLE " + MeetingColumns.TABLE_NAME + TEMP_SUFFIX;
    private static final String SQL_CREATE_VIEW_MEMBER_STATS = "CREATE VIEW "
            + MemberStatsColumns.VIEW_NAME + " AS " + " SELECT "
            + MemberColumns.TABLE_NAME + "." + MemberColumns._ID + " AS " + MemberColumns._ID + ", "
            + MemberColumns.TABLE_NAME + "." + MemberColumns.NAME + " AS " + MemberColumns.NAME + ", "
            + MemberColumns.TABLE_NAME + "." + MemberColumns.DELETED + " AS " + MemberColumns.DELETED + ", "
            + MemberColumns.TABLE_NAME + "." + MemberColumns.TEAM_ID + " AS " + MemberStatsColumns.TEAM_ID + ", "
            + " SUM(" + MeetingMemberColumns.TABLE_NAME + "." + MeetingMemberColumns.DURATION + ") AS " + MemberStatsColumns.SUM_DURATION + ","
            + " AVG(" + MeetingMemberColumns.TABLE_NAME + "." + MeetingMemberColumns.DURATION + ") AS " + MemberStatsColumns.AVG_DURATION
            + " FROM "
            + MemberColumns.TABLE_NAME + " LEFT OUTER JOIN "
            + MeetingMemberColumns.TABLE_NAME + " ON " + MemberColumns.TABLE_NAME + "." + MemberColumns._ID + " = " + MeetingMemberColumns.TABLE_NAME + "." + MeetingMemberColumns.MEMBER_ID
            + " AND " + MeetingMemberColumns.TABLE_NAME + "." + MeetingMemberColumns.DURATION + "> 0"
            + " GROUP BY "
            + MemberColumns.TABLE_NAME + "." + MemberColumns._ID + ", "
            + MemberColumns.TABLE_NAME + "." + MemberColumns.NAME + ", "
            + MemberColumns.TABLE_NAME + "." + MemberColumns.DELETED + ", "
            + MemberColumns.TABLE_NAME + "." + MemberColumns.TEAM_ID;

    private static final String SQL_DROP_VIEW_MEMBER_STATS = "DROP VIEW " + MemberStatsColumns.VIEW_NAME;

    private final Context mContext;

    ScrumChatterDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate");
        execSQL(db, SQL_CREATE_TABLE_TEAM);
        execSQL(db, SQL_CREATE_TABLE_MEETING_MEMBER);
        execSQL(db, SQL_CREATE_TABLE_MEMBER);
        execSQL(db, SQL_CREATE_TABLE_MEETING);
        execSQL(db, SQL_CREATE_VIEW_MEMBER_STATS);
        insertDefaultTeam(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        if (oldVersion < 2) {
            // Create the team table
            execSQL(db, SQL_CREATE_TABLE_TEAM);
            // Insert the default team
            insertDefaultTeam(db);
            // Update the member table so all members are in the default team
            execSQL(db, SQL_CREATE_TABLE_MEMBER_TEMP);
            execSQL(db, SQL_INSERT_TABLE_MEMBER_TEMP);
            execSQL(db, SQL_DROP_TABLE_MEMBER);
            execSQL(db, SQL_CREATE_TABLE_MEMBER);
            execSQL(db, SQL_INSERT_TABLE_MEMBER);
            execSQL(db, SQL_DROP_TABLE_MEMBER_TEMP);
            // Update the meeting table so all meetings are for the default team
            execSQL(db, SQL_CREATE_TABLE_MEETING_TEMP);
            execSQL(db, SQL_INSERT_TABLE_MEETING_TEMP);
            execSQL(db, SQL_DROP_TABLE_MEETING);
            execSQL(db, SQL_CREATE_TABLE_MEETING);
            execSQL(db, SQL_INSERT_TABLE_MEETING);
            execSQL(db, SQL_DROP_TABLE_MEETING_TEMP);
        }

        if (oldVersion < 3) {
            execSQL(db, SQL_ALTER_TABLE_MEMBER_V3);
            // Recreate the views
            execSQL(db, SQL_DROP_VIEW_MEMBER_STATS);
            execSQL(db, SQL_CREATE_VIEW_MEMBER_STATS);
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        Log.d(TAG, "onOpen");
        if (!db.isReadOnly()) {
            // Enable foreign key constraints
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    /**
     * Insert the default team
     */
    private void insertDefaultTeam(SQLiteDatabase db) {
        ContentValues values = new ContentValues(2);
        values.put(TeamColumns._ID, Constants.DEFAULT_TEAM_ID);
        values.put(TeamColumns.TEAM_NAME, Constants.DEFAULT_TEAM_NAME);
        db.insert(TeamColumns.TABLE_NAME, null, values);

        insertDefaultTeamMember(db, mContext.getString(R.string.default_team_member1));
        insertDefaultTeamMember(db, mContext.getString(R.string.default_team_member2));
    }

    private void insertDefaultTeamMember(SQLiteDatabase db, String memberName) {
        ContentValues values = new ContentValues(2);
        values.put(MemberColumns.NAME, memberName);
        values.put(MemberColumns.TEAM_ID, Constants.DEFAULT_TEAM_ID);
        values.put(MemberColumns.DELETED, 0);
        db.insert(MemberColumns.TABLE_NAME, null, values);
    }

    private void execSQL(SQLiteDatabase db, String sql) {
        Log.v(TAG, sql);
        db.execSQL(sql);
    }
}
