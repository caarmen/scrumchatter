package ca.rmen.android.scrumchatter;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.TextView;
import ca.rmen.android.scrumchatter.provider.MeetingColumns;
import ca.rmen.android.scrumchatter.provider.MeetingCursorWrapper;
import ca.rmen.android.scrumchatter.provider.MeetingMemberColumns;
import ca.rmen.android.scrumchatter.provider.MemberColumns;
import ca.rmen.android.scrumchatter.ui.MeetingFragment;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class MeetingActivity extends SherlockFragmentActivity {

	private static final String TAG = Constants.TAG + "/"
			+ MeetingActivity.class.getSimpleName();

	public static final String EXTRA_MEETING_ID = MeetingActivity.class
			.getPackage().getName() + ".meeting_id";
	private TextView mTextViewDuration;
	private TextView mTextViewDate;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "onCreate: savedInstanceState = " + savedInstanceState);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.meeting_activity);
		mTextViewDuration = (TextView) findViewById(R.id.tv_meeting_duration);
		mTextViewDate = (TextView) findViewById(R.id.tv_meeting_date);

		Intent intent = getIntent();
		loadMeeting(intent);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		Log.v(TAG, "onNewIntent: intent = " + intent);
		super.onNewIntent(intent);
		loadMeeting(intent);
	}

	private void loadMeeting(Intent intent) {
		long meetingId = intent.getLongExtra(EXTRA_MEETING_ID, -1);
		// TODO do DB operations in an AsyncTask
		if (meetingId == -1) {
			meetingId = createMeeting();
		}
		Uri uri = Uri.withAppendedPath(MeetingColumns.CONTENT_URI,
				String.valueOf(meetingId));
		Cursor meetingCursor = getContentResolver().query(uri, null, null,
				null, null);
		meetingCursor.moveToFirst();
		MeetingCursorWrapper cursorWrapper = new MeetingCursorWrapper(
				meetingCursor);
		long duration = cursorWrapper.getDuration();
		long date = cursorWrapper.getMeetingDate();
		Log.v(TAG, "duration=" + duration + ", date = " + date);
		mTextViewDuration.setText(DateUtils.formatElapsedTime(duration));
		mTextViewDate.setText(DateUtils.formatDateTime(this, date,
				DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME));

		cursorWrapper.close();
		MeetingFragment fragment = (MeetingFragment) getSupportFragmentManager()
				.findFragmentById(R.id.meeting_fragment);
		fragment.loadMeeting(meetingId);
	}

	private long createMeeting() {
		Log.v(TAG, "create new meeting");
		ContentValues values = new ContentValues();
		values.put(MeetingColumns.MEETING_DATE, System.currentTimeMillis());
		Uri newMeetingUri = getContentResolver().insert(
				MeetingColumns.CONTENT_URI, values);
		long meetingId = Long.parseLong(newMeetingUri.getLastPathSegment());
		Cursor members = getContentResolver().query(MemberColumns.CONTENT_URI,
				new String[] { MemberColumns._ID }, null, null, null);
		if (members != null) {
			ContentValues[] newMeetingMembers = new ContentValues[members
					.getCount()];
			if (members.moveToFirst()) {
				int i = 0;
				do {
					long memberId = members.getLong(0);
					values = new ContentValues();
					values.put(MeetingMemberColumns.MEMBER_ID, memberId);
					values.put(MeetingMemberColumns.MEETING_ID, meetingId);
					values.put(MeetingMemberColumns.DURATION, 0L);
					newMeetingMembers[i++] = values;
				} while (members.moveToNext());
			}
			members.close();
			getContentResolver().bulkInsert(MeetingMemberColumns.CONTENT_URI,
					newMeetingMembers);
		}
		return meetingId;
	}
}
