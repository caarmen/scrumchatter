/**
 * Copyright 2013 Carmen Alvarez
 *
 * This file is part of Scrum Chatter.
 *
 * Scrum Chatter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Scrum Chatter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Scrum Chatter. If not, see <http://www.gnu.org/licenses/>.
 */
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
import ca.rmen.android.scrumchatter.provider.MeetingColumns;
import ca.rmen.android.scrumchatter.provider.MeetingColumns.State;
import ca.rmen.android.scrumchatter.provider.MeetingCursorWrapper;
import ca.rmen.android.scrumchatter.provider.MeetingMemberColumns;
import ca.rmen.android.scrumchatter.provider.MeetingMemberCursorWrapper;
import ca.rmen.android.scrumchatter.provider.MemberColumns;
import ca.rmen.android.scrumchatter.provider.ScrumChatterProvider;
import ca.rmen.android.scrumchatter.ui.MeetingFragment;
import ca.rmen.android.scrumchatter.util.TextUtils;

import com.actionbarsherlock.app.SherlockFragmentActivity;

/**
 * Displays attributes of a meeting as well as the team members participating in
 * this meeting.
 */
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

	/**
	 * Extract the meeting id from the intent and load the meeting data into the
	 * activity.
	 */
	private void loadMeeting(Intent intent) {
		long meetingId = intent.getLongExtra(EXTRA_MEETING_ID, -1);
		// TODO do DB operations in an AsyncTask
		if (meetingId == -1) {
			meetingId = createMeeting();
		}
		// Read the meeting attributes from the DB (don't really like Cursor
		// code in an Activity, but oh well...)
		mMeetingUri = Uri.withAppendedPath(MeetingColumns.CONTENT_URI,
				String.valueOf(meetingId));
		Cursor meetingCursor = getContentResolver().query(mMeetingUri, null,
				null, null, null);
		MeetingCursorWrapper cursorWrapper = new MeetingCursorWrapper(
				meetingCursor);
		cursorWrapper.moveToFirst();
		long duration = cursorWrapper.getDuration();
		long date = cursorWrapper.getMeetingDate();
		State state = cursorWrapper.getState();
		cursorWrapper.close();

		// Update our views based on the meeting attributes.
		if (state == State.IN_PROGRESS) {
			// Only show the "stop meeting" button if the meeting is in
			// progress.
			mBtnStopMeeting.setVisibility(View.VISIBLE);
			// If the meeting is in progress, show the Chronometer.
			long timeSinceMeetingStartedMillis = System.currentTimeMillis()
					- date;
			mMeetingChronometer.setBase(SystemClock.elapsedRealtime()
					- timeSinceMeetingStartedMillis);
			mMeetingChronometer.start();
		} else if (state == State.FINISHED) {
			// For finished meetings, show the duration we retrieved from the
			// db.
			mMeetingChronometer.setText(DateUtils.formatElapsedTime(duration));
		}
		mTextViewDate.setText(TextUtils.formatDateTime(this, date));

		// Load the list of team members.
		MeetingFragment fragment = (MeetingFragment) getSupportFragmentManager()
				.findFragmentById(R.id.meeting_fragment);
		fragment.loadMeeting(meetingId, state, mOnClickListener);
	}

	/**
	 * @return the id of the newly created meeting.
	 */
	private long createMeeting() {
		Log.v(TAG, "create new meeting");
		ContentValues values = new ContentValues();
		values.put(MeetingColumns.MEETING_DATE, System.currentTimeMillis());
		Uri newMeetingUri = getContentResolver().insert(
				MeetingColumns.CONTENT_URI, values);
		long meetingId = Long.parseLong(newMeetingUri.getLastPathSegment());
		return meetingId;
	}

	/**
	 * @return the state of this meeting.
	 */
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

	/**
	 * Change the state of the meeting.
	 * 
	 * @param newState
	 *            the new state of the meeting
	 */
	private void setMeetingState(State newState) {
		Log.v(TAG, "setMeetingState " + newState);
		ContentValues values = new ContentValues(1);
		values.put(MeetingColumns.STATE, newState.ordinal());
		getContentResolver().update(mMeetingUri, values, null, null);
	}

	/**
	 * @return the date the meeting was created or started.
	 */
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

	/**
	 * Change the duration of the meeting.
	 * 
	 * @param duration
	 *            the new duration of the meeting.
	 */
	private void setMeetingDuration(long duration) {
		ContentValues values = new ContentValues(1);
		values.put(MeetingColumns.DURATION, duration);
		getContentResolver().update(mMeetingUri, values, null, null);
	}

	/**
	 * Change the date of the meeting to now. We do this when the meeting goes
	 * from not-started to in-progress. This way it is easier to track the
	 * duration of the meeting.
	 */
	private void resetMeetingDate() {
		Log.v(TAG, "resetMeetingDate");
		ContentValues values = new ContentValues(1);
		values.put(MeetingColumns.MEETING_DATE, System.currentTimeMillis());
		getContentResolver().update(mMeetingUri, values, null, null);
	}

	/**
	 * Start the meeting. Set the state to in-progress, start the chronometer,
	 * and show the "stop meeting" button.
	 */
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

	/**
	 * Stop the meeting. Set the state to finished, stop the chronometer, hide
	 * the "stop meeting" button, persist the meeting duration, and stop the
	 * chronometers for all team members who are still talking.
	 */
	private void stopMeeting() {
		setMeetingState(State.FINISHED);
		mBtnStopMeeting.setVisibility(View.INVISIBLE);
		mMeetingChronometer.stop();
		long meetingStartDate = getMeetingDate();
		long meetingDuration = System.currentTimeMillis() - meetingStartDate;
		setMeetingDuration(meetingDuration / 1000);
		shutEverybodyUp();
		// Reload the list of team members.
		long meetingId = Long.valueOf(mMeetingUri.getLastPathSegment());
		MeetingFragment fragment = (MeetingFragment) getSupportFragmentManager()
				.findFragmentById(R.id.meeting_fragment);
		fragment.loadMeeting(meetingId, State.FINISHED, mOnClickListener);
	}

	/**
	 * Stop the chronometers of all team members who are still talking. Update
	 * the duration for these team members.
	 */
	private void shutEverybodyUp() {
		// Query all team members who are still talking in this meeting.
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
			// Prepare some update statements to set the duration and reset the
			// talk_start_time, for these members.
			ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
			MeetingMemberCursorWrapper cursorWrapper = new MeetingMemberCursorWrapper(
					cursor);
			if (cursorWrapper.moveToFirst()) {
				do {
					// Prepare an update operation for one of these members.
					Builder builder = ContentProviderOperation
							.newUpdate(MeetingMemberColumns.CONTENT_URI);
					long memberId = cursorWrapper.getMemberId();
					// Calculate the total duration the team member talked
					// during this meeting.
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
				// Batch update these team members.
				getContentResolver().applyBatch(ScrumChatterProvider.AUTHORITY,
						operations);
			} catch (Exception e) {
				Log.v(TAG, "Couldn't close off meeting: " + e.getMessage(), e);
			}
		}
	}

	/**
	 * Switch a member from the talking to non-talking state:
	 * 
	 * If they were talking, they will no longer be talking, and their button
	 * will go back to a "start" button.
	 * 
	 * If they were not talking, they will start talking, and their button will
	 * be a "stop" button.
	 * 
	 * @param memberId
	 */
	private void toggleTalkingMember(long memberId) {
		Log.v(TAG, "toggleTalkingMember " + memberId);

		// Find out if this member is currently talking:
		// read its talk_start_time and duration fields.
		String meetingId = mMeetingUri.getLastPathSegment();
		Uri meetingMemberUri = Uri.withAppendedPath(
				MeetingMemberColumns.CONTENT_URI, meetingId);
		Cursor cursor = getContentResolver().query(
				meetingMemberUri,
				new String[] {
						MeetingMemberColumns.TALK_START_TIME,
						MeetingMemberColumns.TABLE_NAME + "."
								+ MeetingMemberColumns.DURATION },
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
		// The member is currently talking if talkStartTime > 0.
		if (talkStartTime > 0) {
			long justTalkedFor = (System.currentTimeMillis() - talkStartTime) / 1000;
			long newDuration = duration + justTalkedFor;
			values.put(MeetingMemberColumns.DURATION, newDuration);
			values.put(MeetingMemberColumns.TALK_START_TIME, 0);
		} else {
			// shut up any other talking member before this one starts.
			shutEverybodyUp();
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
			// Start or stop the team member talking
			case R.id.btn_start_stop_member:
				startMeeting();
				long memberId = (Long) v.getTag();
				toggleTalkingMember(memberId);
				break;
			// Stop the whole meeting.
			case R.id.btn_stop_meeting:
				stopMeeting();
				break;
			default:
				break;
			}
		}

	};
}
