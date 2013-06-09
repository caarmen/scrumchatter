package ca.rmen.android.scrumchatter.provider;

import java.util.HashMap;

import android.database.Cursor;
import android.database.CursorWrapper;

public class MeetingCursorWrapper extends CursorWrapper {
	private HashMap<String, Integer> mColumnIndexes = new HashMap<String, Integer>();
	
    public MeetingCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    public Long getId() {
        Integer index = mColumnIndexes.get(MeetingColumns._ID);
        if (index == null) {
        	index = getColumnIndexOrThrow(MeetingColumns._ID);
        	mColumnIndexes.put(MeetingColumns._ID, index);
        }
        if (isNull(index)) return null;
        return getLong(index);
    }

    public Long getMeetingDate() {
        Integer index = mColumnIndexes.get(MeetingColumns.MEETING_DATE);
        if (index == null) {
        	index = getColumnIndexOrThrow(MeetingColumns.MEETING_DATE);
        	mColumnIndexes.put(MeetingColumns.MEETING_DATE, index);
        }
        if (isNull(index)) return null;
        return getLong(index);
    }

    public Long getDuration() {
        Integer index = mColumnIndexes.get(MeetingColumns.DURATION);
        if (index == null) {
        	index = getColumnIndexOrThrow(MeetingColumns.DURATION);
        	mColumnIndexes.put(MeetingColumns.DURATION, index);
        }
        if (isNull(index)) return null;
        return getLong(index);
    }

    public Long getAge() {
        Integer index = mColumnIndexes.get(MeetingColumns.AGE);
        if (index == null) {
        	index = getColumnIndexOrThrow(MeetingColumns.AGE);
        	mColumnIndexes.put(MeetingColumns.AGE, index);
        }
        if (isNull(index)) return null;
        return getLong(index);
    }
}
