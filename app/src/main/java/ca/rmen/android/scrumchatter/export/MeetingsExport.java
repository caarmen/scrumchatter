/**
 * Copyright 2013 Carmen Alvarez
 *
 * This file is part of Scrum Chatter.
 *
 * Scrum Chatter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
package ca.rmen.android.scrumchatter.export;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.provider.MeetingColumns;
import ca.rmen.android.scrumchatter.provider.MeetingMemberColumns;
import ca.rmen.android.scrumchatter.provider.MeetingMemberCursorWrapper;
import ca.rmen.android.scrumchatter.provider.MemberColumns;
import ca.rmen.android.scrumchatter.provider.MemberCursorWrapper;
import ca.rmen.android.scrumchatter.provider.MemberStatsColumns;
import ca.rmen.android.scrumchatter.provider.TeamColumns;
import ca.rmen.android.scrumchatter.util.Log;
import jxl.CellView;
import jxl.JXLException;
import jxl.Workbook;
import jxl.format.Alignment;
import jxl.format.Border;
import jxl.format.BorderLineStyle;
import jxl.format.CellFormat;
import jxl.write.DateFormat;
import jxl.write.DateFormats;
import jxl.write.DateTime;
import jxl.write.Label;
import jxl.write.Number;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

/**
 * Export data for all meetings to an Excel file.
 */
public class MeetingsExport extends FileExport {
    private static final String TAG = Constants.TAG + "/" + MeetingsExport.class.getSimpleName();

    private static final String EXCEL_FILE = "scrumchatter.xls";
    private static final String MIME_TYPE = "application/vnd.ms-excel";

    private WritableWorkbook mWorkbook;
    private WritableSheet mSheet;
    private WritableCellFormat mDefaultFormat;
    private WritableCellFormat mBoldFormat;
    private final WritableCellFormat mLongDurationFormat = new WritableCellFormat(DateFormats.FORMAT8);
    private final WritableCellFormat mShortDurationFormat = new WritableCellFormat(DateFormats.FORMAT10);
    private final WritableCellFormat mDateFormat = new WritableCellFormat(new DateFormat("dd-MMM-yyyy HH:mm"));


    public MeetingsExport(Context context) {
        super(context, MIME_TYPE);
    }

    /**
     * Create and return an Excel file containing the speaking time for all members in all meetings.
     * 
     * @see ca.rmen.android.scrumchatter.export.FileExport#createFile()
     */
    protected File createFile() {
        Log.v(TAG, "export");

        File file = new File(mContext.getExternalFilesDir(null), EXCEL_FILE);

        try {
            mWorkbook = Workbook.createWorkbook(file);
        } catch (IOException e) {
            Log.v(TAG, e.getMessage(), e);
            return null;
        }
        // Create one worksheet for each team
        Cursor c = mContext.getContentResolver().query(TeamColumns.CONTENT_URI, new String[] { TeamColumns._ID, TeamColumns.TEAM_NAME }, null, null,
                TeamColumns.TEAM_NAME + " COLLATE NOCASE");
        if (c != null) {
            while (c.moveToNext()) {
                int teamId = c.getInt(0);
                String teamName = c.getString(1);
                export(teamId, teamName);
            }
            c.close();
        }
        // Clean up
        try {
            mWorkbook.write();
            mWorkbook.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        } catch (WriteException e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
        return file;
    }

    private void export(int teamId, String teamName) {
        // Build a cache of all member names, including the average and total duration for each member.
        List<String> memberNames = new ArrayList<>();
        Map<String, Integer> avgMemberDurations = new HashMap<>();
        Map<String, Integer> sumMemberDurations = new HashMap<>();
        // Closing the memberCursorWrapper will also close memberCursor
        @SuppressLint("Recycle")
        Cursor memberCursor = mContext.getContentResolver().query(MemberStatsColumns.CONTENT_URI,
                new String[] { MemberColumns.NAME, MemberStatsColumns.AVG_DURATION, MemberStatsColumns.SUM_DURATION },
                MemberStatsColumns.TEAM_ID + "=? AND " + "(" + MemberStatsColumns.SUM_DURATION + ">0 OR " + MemberStatsColumns.AVG_DURATION + " >0 " + ")",
                new String[] { String.valueOf(teamId) }, MemberColumns.NAME);
        if (memberCursor != null) {
            MemberCursorWrapper memberCursorWrapper = new MemberCursorWrapper(memberCursor);
            while (memberCursor.moveToNext()) {
                String memberName = memberCursorWrapper.getName();
                memberNames.add(memberName);
                avgMemberDurations.put(memberName, memberCursorWrapper.getAverageDuration());
                sumMemberDurations.put(memberName, memberCursorWrapper.getSumDuration());
            }
            memberCursorWrapper.close();
        }

        // Write out the column headings
        List<String> columnHeadings = new ArrayList<>();
        columnHeadings.add(mContext.getString(R.string.export_header_meeting_date));
        columnHeadings.addAll(memberNames);
        columnHeadings.add(mContext.getString(R.string.export_header_meeting_duration));
        writeHeader(teamName, columnHeadings);

        // Read all the meeting/member data
        // Closing meetingMemberCursorWrapper will also close meetingMemberCursor
        @SuppressLint("Recycle")
        Cursor meetingMemberCursor = mContext.getContentResolver().query(
                MeetingMemberColumns.CONTENT_URI,
                new String[]{
                        MeetingMemberColumns.MEETING_ID,
                        MeetingColumns.MEETING_DATE,
                        MeetingColumns.TOTAL_DURATION,
                        MemberColumns.NAME,
                        MeetingMemberColumns.DURATION},
                MeetingMemberColumns.DURATION + ">0 AND " + MeetingColumns.TEAM_ID + "=?",
                new String[]{String.valueOf(teamId)},
                MeetingColumns.MEETING_DATE + ", "
                        + MeetingMemberColumns.MEETING_ID + ", "
                        + MemberColumns.NAME);

        MeetingMemberCursorWrapper meetingMemberCursorWrapper = new MeetingMemberCursorWrapper(meetingMemberCursor);
        long totalMeetingDuration = 0;
        //noinspection TryFinallyCanBeTryWithResources
        try {
            long currentMeetingId;
            int rowNumber = 1;
            while (meetingMemberCursorWrapper.moveToNext()) {
                // Write one row to the Excel file, for one meeting.
                insertDateCell(meetingMemberCursorWrapper.getMeetingDate(), rowNumber);
                long meetingDuration = meetingMemberCursorWrapper.getTotalDuration();
                totalMeetingDuration += meetingDuration;
                insertDurationCell(meetingMemberCursorWrapper.getTotalDuration(), rowNumber, columnHeadings.size() - 1, null);
                currentMeetingId = meetingMemberCursorWrapper.getMeetingId();

                do {
                    long meetingId = meetingMemberCursorWrapper.getMeetingId();
                    if (meetingId != currentMeetingId) {
                        meetingMemberCursorWrapper.move(-1);
                        break;
                    }
                    String memberName = meetingMemberCursorWrapper.getMemberName();
                    int memberColumnIndex = memberNames.indexOf(memberName) + 1;
                    insertDurationCell(meetingMemberCursorWrapper.getDuration(), rowNumber, memberColumnIndex, null);
                } while (meetingMemberCursorWrapper.moveToNext());
                rowNumber++;
            }
            // Write the table footer containing the averages and totals
            writeFooter(rowNumber, memberNames, sumMemberDurations, avgMemberDurations, totalMeetingDuration);

        } finally {
            meetingMemberCursorWrapper.close();
        }
    }


    /**
     * Create the workbook, sheet, custom cell formats, and freeze row and
     * column. Also write the column headings.
     */
    private void writeHeader(String teamName, List<String> columnNames) {
        mSheet = mWorkbook.createSheet(teamName, 0);
        mSheet.insertRow(0);
        mSheet.getSettings().setHorizontalFreeze(1);
        mSheet.getSettings().setVerticalFreeze(1);
        createCellFormats();
        for (int i = 0; i < columnNames.size(); i++) {
            mSheet.insertColumn(i);
            insertCell(columnNames.get(i), 0, i, mBoldFormat);
        }
    }

    /**
     * Write the average and sum formulas at the bottom of the table.
     * 
     * @param rowNumber The row number for the row after the last row of the meetings.
     * @param memberNames The names of each team member.
     * @param sumMemberDurations The total speaking time per member, in seconds
     * @param avgMemberDurations The average speaking time per member, in seconds
     * @param totalMeetingDuration The total time of all meetings.
     */
    private void writeFooter(int rowNumber, List<String> memberNames, Map<String, Integer> sumMemberDurations, Map<String, Integer> avgMemberDurations,
            long totalMeetingDuration) {
        try {
            // Create formats we need for the bottom rows of the table.
            WritableCellFormat boldTopBorderLabel = new WritableCellFormat(mBoldFormat);
            boldTopBorderLabel.setBorder(Border.TOP, BorderLineStyle.DOUBLE);
            WritableCellFormat boldTopBorderLongDuration = new WritableCellFormat(mLongDurationFormat);
            boldTopBorderLongDuration.setFont(new WritableFont(mBoldFormat.getFont()));
            boldTopBorderLongDuration.setBorder(Border.TOP, BorderLineStyle.DOUBLE);
            WritableCellFormat boldShortDuration = new WritableCellFormat(mShortDurationFormat);
            boldShortDuration.setFont(new WritableFont(mBoldFormat.getFont()));

            // Insert the average and total titles.
            insertCell(mContext.getString(R.string.member_list_header_sum_duration), rowNumber, 0, boldTopBorderLabel);
            insertCell(mContext.getString(R.string.member_list_header_avg_duration), rowNumber + 1, 0, mBoldFormat);

            int columnCount = memberNames.size() + 2;

            // Insert the average and total durations for all members
            for (int i = 0; i < memberNames.size(); i++) {
                String memberName = memberNames.get(i);
                int col = i + 1;
                insertDurationCell(sumMemberDurations.get(memberName), rowNumber, col, boldTopBorderLongDuration);
                insertDurationCell(avgMemberDurations.get(memberName), rowNumber + 1, col, boldShortDuration);
            }

            // Insert the average and total durations for the meetings.
            insertDurationCell(totalMeetingDuration, rowNumber, columnCount - 1, boldTopBorderLongDuration);
            int numMeetings = rowNumber - 1;
            long averageMeetingDuration = numMeetings == 0 ? 0 : totalMeetingDuration / numMeetings;
            insertDurationCell(averageMeetingDuration, rowNumber + 1, columnCount - 1, boldShortDuration);

            // Now that the whole table is filled, auto-size the width of the first and last columns.
            CellView columnView = mSheet.getColumnView(0);
            columnView.setAutosize(true);
            mSheet.setColumnView(0, columnView);

            columnView = mSheet.getColumnView(columnCount - 1);
            columnView.setAutosize(true);
            mSheet.setColumnView(columnCount - 1, columnView);
        } catch (JXLException e) {
            Log.e(TAG, "Error adding formulas: " + e.getMessage(), e);
        }
    }

    /**
     * Write a single cell to the Excel file
     * 
     * @param text
     *            will be written as text in the cell.
     * @param format
     *            may be null for the default cell format.
     */
    private void insertCell(String text, int row, int column, CellFormat format) {
        Label label = format == null ? new Label(column, row, text, mDefaultFormat) : new Label(column, row, text, format);
        try {
            mSheet.addCell(label);
        } catch (JXLException e) {
            Log.e(TAG, "writeHeader Could not insert cell " + text + " at row=" + row + ", col=" + column, e);
        }
    }

    private void insertDurationCell(long durationInSeconds, int row, int column, CellFormat cellFormat) {
        double durationInDays = (double) durationInSeconds / (24 * 60 * 60);
        if (cellFormat == null) cellFormat = durationInSeconds >= 3600 ? mLongDurationFormat : mShortDurationFormat;
        Number number = new Number(column, row, durationInDays, cellFormat);
        try {
            mSheet.addCell(number);
        } catch (JXLException e) {
            Log.e(TAG, "writeHeader Could not insert cell " + durationInSeconds + " at row=" + row + ", col=" + column, e);
        }
    }

    private void insertDateCell(long dateInMillis, int row) {
        DateTime dateCell = new DateTime(0, row, new Date(dateInMillis), mDateFormat);
        try {
            mSheet.addCell(dateCell);
        } catch (JXLException e) {
            Log.e(TAG, "writeHeader Could not insert cell " + dateCell + " at row=" + row, e);
        }
    }

    /**
     * In order to set text to bold, red, or green, we need to create cell
     * formats for each style.
     */
    private void createCellFormats() {

        // Insert a dummy empty cell, so we can obtain its cell. This allows to
        // start with a default cell format.
        Label cell = new Label(0, 0, " ");
        CellFormat cellFormat = cell.getCellFormat();

        try {
            // Create the bold format
            final WritableFont boldFont = new WritableFont(cellFormat.getFont());
            mBoldFormat = new WritableCellFormat(cellFormat);
            boldFont.setBoldStyle(WritableFont.BOLD);
            mBoldFormat.setFont(boldFont);
            mBoldFormat.setAlignment(Alignment.CENTRE);

            // Center other formats
            mDefaultFormat = new WritableCellFormat(cellFormat);
            mDefaultFormat.setAlignment(Alignment.CENTRE);
            mLongDurationFormat.setAlignment(Alignment.CENTRE);
            mShortDurationFormat.setAlignment(Alignment.CENTRE);
            mDateFormat.setAlignment(Alignment.CENTRE);


        } catch (WriteException e) {
            Log.e(TAG, "createCellFormats Could not create cell formats", e);
        }
    }

}