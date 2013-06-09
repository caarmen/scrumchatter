package ca.rmen.android.scrumchatter.provider;

import java.util.HashMap;

import android.database.Cursor;
import android.database.CursorWrapper;

public class MemberCursorWrapper extends CursorWrapper {
	private HashMap<String, Integer> mColumnIndexes = new HashMap<String, Integer>();
	
    public MemberCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    public Long getId() {
        Integer index = mColumnIndexes.get(MemberColumns._ID);
        if (index == null) {
        	index = getColumnIndexOrThrow(MemberColumns._ID);
        	mColumnIndexes.put(MemberColumns._ID, index);
        }
        if (isNull(index)) return null;
        return getLong(index);
    }

    public String getName() {
        Integer index = mColumnIndexes.get(MemberColumns.NAME);
        if (index == null) {
        	index = getColumnIndexOrThrow(MemberColumns.NAME);
        	mColumnIndexes.put(MemberColumns.NAME, index);
        }
        return getString(index);
    }
}
