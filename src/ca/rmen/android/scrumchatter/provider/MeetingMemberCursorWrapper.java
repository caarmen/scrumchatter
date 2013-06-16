package ca.rmen.android.scrumchatter.provider;

import java.util.HashMap;

import android.database.Cursor;
import android.database.CursorWrapper;

public class MeetingMemberCursorWrapper extends CursorWrapper {
	private HashMap<String, Integer> mColumnIndexes = new HashMap<String, Integer>();

	public MeetingMemberCursorWrapper(Cursor cursor) {
		super(cursor);
	}

	public Long getMeetingId() {
		Integer index = getIndex(MeetingMemberColumns.MEETING_ID);
		if (isNull(index))
			return null;
		return getLong(index);
	}

	public Long getMemberId() {
		Integer index = getIndex(MeetingMemberColumns.MEMBER_ID);
		if (isNull(index))
			return null;
		return getLong(index);
	}

	public String getMemberName() {
		Integer index = getIndex(MemberColumns.NAME);
		if (isNull(index))
			return null;
		return getString(index);
	}

	public Long getDuration() {
		Integer index = getIndex(MeetingMemberColumns.DURATION);
		if (isNull(index))
			return null;
		return getLong(index);
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
