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
		Integer index = getIndex(MeetingColumns._ID);
		if (isNull(index))
			return null;
		return getLong(index);
	}

	public Long getMeetingDate() {
		Integer index = getIndex(MeetingColumns.MEETING_DATE);
		if (isNull(index))
			return null;
		return getLong(index);
	}

	public Long getDuration() {
		Integer index = getIndex(MeetingColumns.DURATION);
		if (isNull(index))
			return Long.valueOf(0);
		return getLong(index);
	}

	public MeetingColumns.State getState() {
		Integer index = getIndex(MeetingColumns.STATE);
		if (isNull(index))
			return MeetingColumns.State.NOT_STARTED;
		int stateInt = getInt(index);
		return MeetingColumns.State.values()[stateInt];
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
