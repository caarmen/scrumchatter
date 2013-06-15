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
		Integer index = mColumnIndexes.get(MeetingMemberColumns.MEETING_ID);
		if (index == null) {
			index = getColumnIndexOrThrow(MeetingMemberColumns.MEETING_ID);
			mColumnIndexes.put(MeetingMemberColumns.MEETING_ID, index);
		}
		if (isNull(index))
			return null;
		return getLong(index);
	}

	public Long getMemberId() {
		Integer index = mColumnIndexes.get(MeetingMemberColumns.MEMBER_ID);
		if (index == null) {
			index = getColumnIndexOrThrow(MeetingMemberColumns.MEMBER_ID);
			mColumnIndexes.put(MeetingMemberColumns.MEMBER_ID, index);
		}
		if (isNull(index))
			return null;
		return getLong(index);
	}

	public Long getDuration() {
		Integer index = mColumnIndexes.get(MeetingMemberColumns.DURATION);
		if (index == null) {
			index = getColumnIndexOrThrow(MeetingMemberColumns.DURATION);
			mColumnIndexes.put(MeetingMemberColumns.DURATION, index);
		}
		if (isNull(index))
			return null;
		return getLong(index);
	}
}
