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
		Integer index = getColumnIndex(MemberColumns._ID);
		if (isNull(index))
			return null;
		return getLong(index);
	}

	public String getName() {
		Integer index = getIndex(MemberColumns.NAME);
		return getString(index);
	}

	public Integer getAverageDuration() {
		Integer index = getIndex(MeetingMemberColumns.AVG_DURATION);
		return getInt(index);
	}

	public Integer getSumDuration() {
		Integer index = getIndex(MeetingMemberColumns.SUM_DURATION);
		return getInt(index);
	}

	private Integer getIndex(String columnName) {
		Integer index = mColumnIndexes.get(columnName);
		if (index == null) {
			index = getColumnIndexOrThrow(columnName);
			mColumnIndexes.put(columnName, index);
		}
		return index;
	}
}
