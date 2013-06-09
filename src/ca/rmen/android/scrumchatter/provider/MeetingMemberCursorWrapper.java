package ca.rmen.android.scrumchatter.provider;

import java.util.HashMap;

import android.database.Cursor;
import android.database.CursorWrapper;

public class MeetingMemberCursorWrapper extends CursorWrapper {
	private HashMap<String, Integer> mColumnIndexes = new HashMap<String, Integer>();
	
    public MeetingMemberCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    public Long getId() {
        Integer index = mColumnIndexes.get(MeetingMemberColumns._ID);
        if (index == null) {
        	index = getColumnIndexOrThrow(MeetingMemberColumns._ID);
        	mColumnIndexes.put(MeetingMemberColumns._ID, index);
        }
        if (isNull(index)) return null;
        return getLong(index);
    }

    public Long getMeetingId() {
        Integer index = mColumnIndexes.get(MeetingMemberColumns.MEETING_ID);
        if (index == null) {
        	index = getColumnIndexOrThrow(MeetingMemberColumns.MEETING_ID);
        	mColumnIndexes.put(MeetingMemberColumns.MEETING_ID, index);
        }
        if (isNull(index)) return null;
        return getLong(index);
    }

    public Long getTeamMemberId() {
        Integer index = mColumnIndexes.get(MeetingMemberColumns.TEAM_MEMBER_ID);
        if (index == null) {
        	index = getColumnIndexOrThrow(MeetingMemberColumns.TEAM_MEMBER_ID);
        	mColumnIndexes.put(MeetingMemberColumns.TEAM_MEMBER_ID, index);
        }
        if (isNull(index)) return null;
        return getLong(index);
    }

    public Long getDuration() {
        Integer index = mColumnIndexes.get(MeetingMemberColumns.DURATION);
        if (index == null) {
        	index = getColumnIndexOrThrow(MeetingMemberColumns.DURATION);
        	mColumnIndexes.put(MeetingMemberColumns.DURATION, index);
        }
        if (isNull(index)) return null;
        return getLong(index);
    }
}
