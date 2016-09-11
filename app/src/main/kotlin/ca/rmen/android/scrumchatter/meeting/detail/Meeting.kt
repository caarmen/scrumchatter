/**
 * Copyright 2013 Carmen Alvarez

 * This file is part of Scrum Chatter.

 * Scrum Chatter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * Scrum Chatter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with Scrum Chatter. If not, see //www.gnu.org/licenses/>.
 */
package ca.rmen.android.scrumchatter.meeting.detail

import java.util.ArrayList

import android.annotation.SuppressLint
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.preference.PreferenceManager
import ca.rmen.android.scrumchatter.util.Log
import ca.rmen.android.scrumchatter.Constants
import ca.rmen.android.scrumchatter.provider.MeetingColumns
import ca.rmen.android.scrumchatter.provider.MeetingColumns.State
import ca.rmen.android.scrumchatter.provider.MeetingCursorWrapper
import ca.rmen.android.scrumchatter.provider.MeetingMemberColumns
import ca.rmen.android.scrumchatter.provider.MeetingMemberCursorWrapper
import ca.rmen.android.scrumchatter.provider.ScrumChatterProvider

/**
 * Model of meetings, providing attributes and behavior.
 */
class Meeting private constructor(private val mContext: Context, val id: Long, startDate: Long, state: State, duration: Long) {
    private val mUri: Uri
    var startDate: Long = 0
        private set
    var state: State? = null
        private set
    var duration: Long = 0
        private set

    init {
        this.startDate = startDate
        this.state = state
        this.duration = duration
        mUri = Uri.withAppendedPath(MeetingColumns.CONTENT_URI, id.toString())
    }

    /**
     * Updates the start time to now, sets the state to in_progress, and persists the changes.
     */
    fun start() {
        /**
         * Change the date of the meeting to now. We do this when the
         * meeting goes from not-started to in-progress. This way it is
         * easier to track the duration of the meeting.
         */
        startDate = System.currentTimeMillis()
        state = State.IN_PROGRESS
        save()
    }

    /**
     * Updates the meeting duration to time elapsed since startDate, sets the state to finished, and persists the changes.
     */
    fun stop() {
        state = State.FINISHED
        val meetingDuration = System.currentTimeMillis() - startDate
        duration = meetingDuration / 1000
        shutEverybodyUp()
        save()
    }

    /**
     * Stop the chronometers of all team members who are still talking. Update
     * the duration for these team members.
     */
    private fun shutEverybodyUp() {
        Log.v(TAG, "shutEverybodyUp")
        // Query all team members who are still talking in this meeting.
        val uri = Uri.withAppendedPath(MeetingMemberColumns.CONTENT_URI, id.toString())
        // Closing the cursorWrapper also closes the cursor
        @SuppressLint("Recycle")
        val cursor = mContext.contentResolver.query(uri,
                arrayOf(MeetingMemberColumns.MEMBER_ID, MeetingMemberColumns.DURATION, MeetingMemberColumns.TALK_START_TIME),
                MeetingMemberColumns.TALK_START_TIME + ">0", null, null)
        if (cursor != null) {
            // Prepare some update statements to set the duration and reset the
            // talk_start_time, for these members.
            val operations = ArrayList<ContentProviderOperation>()
            val cursorWrapper = MeetingMemberCursorWrapper(cursor)
            if (cursorWrapper.moveToFirst()) {
                do {
                    // Prepare an update operation for one of these members.
                    val builder = ContentProviderOperation.newUpdate(MeetingMemberColumns.CONTENT_URI)
                    val memberId = cursorWrapper.memberId
                    // Calculate the total duration the team member talked
                    // during this meeting.
                    val duration = cursorWrapper.duration
                    val talkStartTime = cursorWrapper.talkStartTime
                    val newDuration = duration + (System.currentTimeMillis() - talkStartTime) / 1000
                    builder.withValue(MeetingMemberColumns.DURATION, newDuration)
                    builder.withValue(MeetingMemberColumns.TALK_START_TIME, 0)
                    builder.withSelection(MeetingMemberColumns.MEMBER_ID + "=? AND " + MeetingMemberColumns.MEETING_ID + "=?",
                            arrayOf(memberId.toString(), id.toString()))
                    operations.add(builder.build())
                } while (cursorWrapper.moveToNext())
            }
            cursorWrapper.close()
            try {
                // Batch update these team members.
                mContext.contentResolver.applyBatch(ScrumChatterProvider.AUTHORITY, operations)
            } catch (e: Exception) {
                Log.v(TAG, "Couldn't close off meeting: " + e.message, e)
            }

        }
    }

    /**
     * If the team member is talking, stop them. Otherwise, stop all other team members who may be talking, and start this one.
     */
    fun toggleTalkingMember(memberId: Long) {
        Log.v(TAG, "toggleTalkingMember " + memberId)

        // Find out if this member is currently talking:
        // read its talk_start_time and duration fields.
        val meetingMemberUri = Uri.withAppendedPath(MeetingMemberColumns.CONTENT_URI, id.toString())
        // Closing the cursorWrapper also closes the cursor
        @SuppressLint("Recycle")
        val cursor = mContext.contentResolver.query(meetingMemberUri,
                arrayOf(MeetingMemberColumns.TALK_START_TIME, MeetingMemberColumns.DURATION), MeetingMemberColumns.MEMBER_ID + "=?",
                arrayOf(memberId.toString()), null)
        var talkStartTime: Long = 0
        var duration: Long = 0
        if (cursor != null) {
            val cursorWrapper = MeetingMemberCursorWrapper(cursor)
            if (cursorWrapper.moveToFirst()) {
                talkStartTime = cursorWrapper.talkStartTime
                duration = cursorWrapper.duration
            }
            cursorWrapper.close()
        }
        Log.v(TAG, "Talking member: duration = $duration, talkStartTime = $talkStartTime")
        val values = ContentValues(2)
        // The member is currently talking if talkStartTime > 0.
        if (talkStartTime > 0) {
            val justTalkedFor = (System.currentTimeMillis() - talkStartTime) / 1000
            val newDuration = duration + justTalkedFor
            values.put(MeetingMemberColumns.DURATION, newDuration)
            values.put(MeetingMemberColumns.TALK_START_TIME, 0)
        } else {
            // shut up any other talking member before this one starts.
            shutEverybodyUp()
            values.put(MeetingMemberColumns.TALK_START_TIME, System.currentTimeMillis())
        }

        mContext.contentResolver.update(MeetingMemberColumns.CONTENT_URI, values,
                MeetingMemberColumns.MEMBER_ID + "=? AND " + MeetingMemberColumns.MEETING_ID + "=?",
                arrayOf(memberId.toString(), id.toString()))
    }

    /**
     * Delete this meeting from the DB
     */
    fun delete() {
        Log.v(TAG, "delete " + this)
        mContext.contentResolver.delete(mUri, null, null)
    }

    /**
     * Update this meeting in the DB.
     */
    private fun save() {
        Log.v(TAG, "save " + this)
        val values = ContentValues(3)
        values.put(MeetingColumns.STATE, state!!.ordinal)
        values.put(MeetingColumns.MEETING_DATE, startDate)
        values.put(MeetingColumns.TOTAL_DURATION, duration)
        mContext.contentResolver.update(mUri, values, null, null)
    }

    override fun toString(): String {
        return "Meeting [mId=$id, mUri=$mUri, mStartDate=$startDate, mState=$state, mDuration=$duration]"
    }

    companion object {
        private val TAG = Constants.TAG + "/" + Meeting::class.java.simpleName

        /**
         * Read an existing meeting from the DB.
         */
        fun read(context: Context, id: Long): Meeting? {
            Log.v(TAG, "read meeting with id " + id)
            // Read the meeting attributes from the DB 
            val uri = Uri.withAppendedPath(MeetingColumns.CONTENT_URI, id.toString())
            // Closing the cursorWrapper will also close meetingCursor
            @SuppressLint("Recycle") val meetingCursor = context.contentResolver.query(uri, null, null, null, null)
            val cursorWrapper = MeetingCursorWrapper(meetingCursor)
            //noinspection TryFinallyCanBeTryWithResources
            try {
                if (cursorWrapper.moveToFirst()) {

                    val duration = cursorWrapper.totalDuration!!
                    val startDate = cursorWrapper.meetingDate!!
                    val state = cursorWrapper.state
                    return Meeting(context, id, startDate, state, duration)
                } else {
                    Log.v(TAG, "No meeting for id " + id)
                    return null
                }
            } finally {
                cursorWrapper.close()
            }
        }

        /**
         * Read an existing meeting from the DB.
         */
        fun read(context: Context, cursorWrapper: MeetingCursorWrapper): Meeting {
            val id = cursorWrapper.id!!
            val startDate = cursorWrapper.meetingDate!!
            val duration = cursorWrapper.totalDuration!!
            val state = cursorWrapper.state
            return Meeting(context, id, startDate, state, duration)
        }

        /**
         * Create a new Meeting. This persists the new meeting to the DB.
         */
        fun createNewMeeting(context: Context): Meeting? {
            Log.v(TAG, "create new meeting")
            val teamId = PreferenceManager.getDefaultSharedPreferences(context).getInt(Constants.PREF_TEAM_ID, Constants.DEFAULT_TEAM_ID)
            val values = ContentValues()
            val startDate = System.currentTimeMillis()
            values.put(MeetingColumns.MEETING_DATE, System.currentTimeMillis())
            values.put(MeetingColumns.TEAM_ID, teamId)
            val newMeetingUri = context.contentResolver.insert(MeetingColumns.CONTENT_URI, values)
            if (newMeetingUri != null) {
                val meetingId = java.lang.Long.parseLong(newMeetingUri.lastPathSegment)
                return Meeting(context, meetingId, startDate, State.NOT_STARTED, 0)
            } else {
                Log.w(TAG, "Couldn't create a meeting for values " + values)
                return null
            }
        }
    }


}