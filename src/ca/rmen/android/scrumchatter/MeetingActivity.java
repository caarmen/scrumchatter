package ca.rmen.android.scrumchatter;

import java.util.ArrayList;

import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;
import ca.rmen.android.scrumchatter.adapter.MeetingCursorAdapter.MemberItemCache;
import ca.rmen.android.scrumchatter.provider.MeetingColumns;
import ca.rmen.android.scrumchatter.provider.MeetingColumns.State;
import ca.rmen.android.scrumchatter.provider.MeetingCursorWrapper;
import ca.rmen.android.scrumchatter.provider.MeetingMemberColumns;
import ca.rmen.android.scrumchatter.provider.MeetingMemberCursorWrapper;
import ca.rmen.android.scrumchatter.provider.MemberColumns;
import ca.rmen.android.scrumchatter.provider.ScrumChatterProvider;
import ca.rmen.android.scrumchatter.ui.MeetingFragment;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class MeetingActivity extends SherlockFragmentActivity {

	private static final String TAG = Constants.TAG + "/"
			+ MeetingActivity.class.getSimpleName();

	public static final String EXTRA_MEETING_ID = MeetingActivity.class
			.getPackage().getName() + ".meeting_id";
	private TextView mTextViewDate;
	private View mBtnStopMeeting;
	private Chronometer mMeetingChronometer;
	private Uri mMeetingUri;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "onCreate: savedInstanceState = " + savedInstanceState);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.meeting_activity);

		mTextViewDate = (TextView) findViewById(R.id.tv_meeting_date);
		mBtnStopMeeting = findViewById(R.id.btn_stop_meeting);
		mMeetingChronometer = (Chronometer) findViewById(R.id.tv_meeting_duration);

		mBtnStopMeeting.setOnClickListener(mOnClickListener);

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
		mMeetingUri = Uri.withAppendedPath(MeetingColumns.CONTENT_URI,
				String.valueOf(meetingId));
		Cursor meetingCursor = getContentResolver().query(mMeetingUri, null,
				null, null, null);
		meetingCursor.moveToFirst();
		MeetingCursorWrapper cursorWrapper = new MeetingCursorWrapper(
				meetingCursor);
		long duration = cursorWrapper.getDuration();
		long date = cursorWrapper.getMeetingDate();
		State state = cursorWrapper.getState();
		if (state == State.IN_PROGRESS) {
			mBtnStopMeeting.setVisibility(View.VISIBLE);
			long timeSinceMeetingStartedMillis = System.currentTimeMillis()
					- date;
			Log.v(TAG, "meeting started "
					+ (timeSinceMeetingStartedMillis / 1000) + " seconds ago");
			mMeetingChronometer.setBase(SystemClock.elapsedRealtime()
					- timeSinceMeetingStartedMillis);
			mMeetingChronometer.start();
		} else if (state == State.FINISHED) {
			mMeetingChronometer.setText(DateUtils.formatElapsedTime(duration));
		}
		mMeetingChronometer.setText(DateUtils.formatElapsedTime(duration));
		mTextViewDate.setText(DateUtils.formatDateTime(this, date,
				DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME));

		cursorWrapper.close();
		MeetingFragment fragment = (MeetingFragment) getSupportFragmentManager()
				.findFragmentById(R.id.meeting_fragment);
		fragment.loadMeeting(meetingId, mOnClickListener);
	}

	private long createMeeting() {
		Log.v(TAG, "create new meeting");
		ContentValues values = new ContentValues();
		values.put(MeetingColumns.MEETING_DATE, System.currentTimeMillis());
		Uri newMeetingUri = getContentResolver().insert(
				MeetingColumns.CONTENT_URI, values);
		long meetingId = Long.parseLong(newMeetingUri.getLastPathSegment());
		return meetingId;
	}

	private State getMeetingState() {
		Cursor cursor = getContentResolver().query(mMeetingUri,
				new String[] { MeetingColumns.STATE }, null, null, null);
		State oldState = null;
		if (cursor != null) {
			MeetingCursorWrapper cursorWrapper = new MeetingCursorWrapper(
					cursor);
			if (cursorWrapper.moveToFirst())
				oldState = cursorWrapper.getState();
			cursorWrapper.close();
		}
		return oldState;
	}

	private void setMeetingState(State newState) {
		Log.v(TAG, "setMeetingState " + newState);
		ContentValues values = new ContentValues(1);
		values.put(MeetingColumns.STATE, newState.ordinal());
		getContentResolver().update(mMeetingUri, values, null, null);
	}

	private long getMeetingDate() {
		Cursor cursor = getContentResolver().query(mMeetingUri,
				new String[] { MeetingColumns.MEETING_DATE }, null, null, null);
		long meetingDate = 0;
		if (cursor != null) {
			MeetingCursorWrapper cursorWrapper = new MeetingCursorWrapper(
					cursor);
			if (cursorWrapper.moveToFirst())
				meetingDate = cursorWrapper.getMeetingDate();
			cursorWrapper.close();
		}
		return meetingDate;
	}

	private void setMeetingDuration(long duration) {
		ContentValues values = new ContentValues(1);
		values.put(MeetingColumns.DURATION, duration);
		getContentResolver().update(mMeetingUri, values, null, null);
	}

	private void resetMeetingDate() {
		Log.v(TAG, "resetMeetingDate");
		ContentValues values = new ContentValues(1);
		values.put(MeetingColumns.MEETING_DATE, System.currentTimeMillis());
		getContentResolver().update(mMeetingUri, values, null, null);
	}

	private void startMeeting() {
		State state = getMeetingState();
		if (state != State.IN_PROGRESS) {
			setMeetingState(State.IN_PROGRESS);
			mBtnStopMeeting.setVisibility(View.VISIBLE);
			resetMeetingDate();
			mMeetingChronometer.setBase(SystemClock.elapsedRealtime());
			mMeetingChronometer.start();
		}
	}

	private void stopMeeting() {
		setMeetingState(State.FINISHED);
		mBtnStopMeeting.setVisibility(View.INVISIBLE);
		mMeetingChronometer.stop();
		long meetingStartDate = getMeetingDate();
		long meetingDuration = System.currentTimeMillis() - meetingStartDate;
		setMeetingDuration(meetingDuration / 1000);
		shutEverybodyUp();
	}

	private void shutEverybodyUp() {
		String meetingId = mMeetingUri.getLastPathSegment();
		Cursor cursor = getContentResolver().query(
				MeetingMemberColumns.CONTENT_URI,
				new String[] { MemberColumns._ID,
						MeetingMemberColumns.DURATION,
						MeetingMemberColumns.TALK_START_TIME },
				MeetingMemberColumns.MEETING_ID + "=? AND "
						+ MeetingMemberColumns.TALK_START_TIME + ">0",
				new String[] { meetingId }, null);
		if (cursor != null) {
			ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
			MeetingMemberCursorWrapper cursorWrapper = new MeetingMemberCursorWrapper(
					cursor);
			if (cursorWrapper.moveToFirst()) {
				do {
					Builder builder = ContentProviderOperation
							.newUpdate(MeetingMemberColumns.CONTENT_URI);
					long memberId = cursorWrapper.getMemberId();
					long duration = cursorWrapper.getDuration();
					long talkStartTime = cursorWrapper.getTalkStartTime();
					long newDuration = duration
							+ (System.currentTimeMillis() - talkStartTime)
							/ 1000;
					builder.withValue(MeetingMemberColumns.DURATION,
							newDuration);
					builder.withValue(MeetingMemberColumns.TALK_START_TIME, 0);
					builder.withSelection(MeetingMemberColumns.MEMBER_ID
							+ "=? AND " + MeetingMemberColumns.MEETING_ID
							+ "=?", new String[] { String.valueOf(memberId),
							meetingId });
					operations.add(builder.build());
				} while (cursorWrapper.moveToNext());
			}
			cursorWrapper.close();
			try {

				getContentResolver().applyBatch(ScrumChatterProvider.AUTHORITY,
						operations);
			} catch (Exception e) {
				Log.v(TAG, "Couldn't close off meeting: " + e.getMessage(), e);
			}
		}

	}

	private void toggleTalkingMember(long memberId) {
		Log.v(TAG, "toggleTalkingMember " + memberId);

		String meetingId = mMeetingUri.getLastPathSegment();
		Uri meetingMemberUri = Uri.withAppendedPath(
				MeetingMemberColumns.CONTENT_URI, meetingId);
		Cursor cursor = getContentResolver().query(
				meetingMemberUri,
				new String[] { MeetingMemberColumns.TALK_START_TIME,
						MeetingMemberColumns.DURATION },
				MeetingMemberColumns.MEMBER_ID + "=?",
				new String[] { String.valueOf(memberId) }, null);
		long talkStartTime = 0;
		long duration = 0;
		if (cursor != null) {
			MeetingMemberCursorWrapper cursorWrapper = new MeetingMemberCursorWrapper(
					cursor);
			if (cursorWrapper.moveToFirst()) {
				talkStartTime = cursorWrapper.getTalkStartTime();
				duration = cursorWrapper.getDuration();
			}
			cursorWrapper.close();
		}
		Log.v(TAG, "Talking member: duration = " + duration
				+ ", talkStartTime = " + talkStartTime);
		ContentValues values = new ContentValues(2);
		if (talkStartTime > 0) {
			long justTalkedFor = (System.currentTimeMillis() - talkStartTime) / 1000;
			long newDuration = duration + justTalkedFor;
			values.put(MeetingMemberColumns.DURATION, newDuration);
			values.put(MeetingMemberColumns.TALK_START_TIME, 0);
		} else {
			values.put(MeetingMemberColumns.TALK_START_TIME,
					System.currentTimeMillis());
		}

		getContentResolver().update(
				MeetingMemberColumns.CONTENT_URI,
				values,
				MeetingMemberColumns.MEMBER_ID + "=? AND "
						+ MeetingMemberColumns.MEETING_ID + "=?",
				new String[] { String.valueOf(memberId), meetingId });
	}

	private final OnClickListener mOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.btn_start_stop_member:
				MemberItemCache cache = (MemberItemCache) v.getTag();
				Toast.makeText(MeetingActivity.this,
						"Clicked on " + cache.name, Toast.LENGTH_SHORT).show();
				startMeeting();
				toggleTalkingMember(cache.id);
				break;
			case R.id.btn_stop_meeting:
				stopMeeting();
				break;
			default:
				break;
			}
		}

	};
}
