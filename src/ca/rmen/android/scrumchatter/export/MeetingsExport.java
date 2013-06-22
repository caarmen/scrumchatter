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
package ca.rmen.android.scrumchatter.export;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jxl.JXLException;
import jxl.Workbook;
import jxl.format.CellFormat;
import jxl.write.Label;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.format.DateUtils;
import android.util.Log;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.provider.MeetingColumns;
import ca.rmen.android.scrumchatter.provider.MeetingMemberColumns;
import ca.rmen.android.scrumchatter.provider.MeetingMemberCursorWrapper;
import ca.rmen.android.scrumchatter.provider.MemberColumns;
import ca.rmen.android.scrumchatter.util.TextUtils;

/**
 * Export data for all meetings to an Excel file.
 */
public class MeetingsExport {
	private static final String TAG = Constants.TAG + "/"
			+ MeetingsExport.class.getSimpleName();

	private static final String EXCEL_FILE = "scrumchatter.xls";

	private final Context mContext;
	private final File mFile;
	private WritableWorkbook mWorkbook;
	private WritableSheet mSheet;
	private WritableCellFormat mBoldFormat;

	public MeetingsExport(Context context) throws FileNotFoundException {
		mContext = context;
		mFile = new File(context.getExternalFilesDir(null), EXCEL_FILE);
		Log.v(TAG, "Will export to " + mFile);
	}

	public boolean exportMeetings() {
		Log.v(TAG, "exportMeetings");

		// Build a cache of all member names
		List<String> memberNames = new ArrayList<String>();
		Cursor c = mContext.getContentResolver().query(
				MemberColumns.CONTENT_URI, new String[] { MemberColumns.NAME },
				null, null, MemberColumns.NAME);
		while (c.moveToNext()) {
			memberNames.add(c.getString(0));
		}
		c.close();

		// Write out the column headings
		List<String> columnHeadings = new ArrayList<String>();
		columnHeadings.add(mContext
				.getString(R.string.export_header_meeting_date));
		columnHeadings.addAll(memberNames);
		columnHeadings.add(mContext
				.getString(R.string.export_header_meeting_duration));
		try {
			writeHeader(columnHeadings);
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
			return false;
		}

		// Read all the meeting/member data
		c = mContext.getContentResolver().query(
				MeetingMemberColumns.CONTENT_URI,
				// @formatter:off
				new String[] {
						MeetingMemberColumns.MEETING_ID,
						MeetingColumns.MEETING_DATE,
						MeetingColumns.TOTAL_DURATION,
						MemberColumns.NAME,
						MeetingMemberColumns.DURATION },
				MeetingMemberColumns.DURATION + ">0",
				null,
				MeetingColumns.MEETING_DATE + ", " 
				+ MeetingMemberColumns.MEETING_ID + ", "
				+ MemberColumns.NAME);
				// @formatter:on

		MeetingMemberCursorWrapper cursorWrapper = new MeetingMemberCursorWrapper(
				c);
		try {
			long currentMeetingId = -1;
			int rowNumber = 1;
			while (cursorWrapper.moveToNext()) {
				String[] rowValues = new String[columnHeadings.size()];
				rowValues[0] = TextUtils.formatDateTime(mContext,
						cursorWrapper.getMeetingDate());
				rowValues[rowValues.length - 1] = DateUtils
						.formatElapsedTime(cursorWrapper.getTotalDuration());
				currentMeetingId = cursorWrapper.getMeetingId();

				do {
					long meetingId = cursorWrapper.getMeetingId();
					if (meetingId != currentMeetingId) {
						cursorWrapper.move(-1);
						break;
					}
					String memberName = cursorWrapper.getMemberName();
					int memberColumnIndex = memberNames.indexOf(memberName);
					long memberDuration = cursorWrapper.getDuration();
					rowValues[memberColumnIndex + 1] = DateUtils
							.formatElapsedTime(memberDuration);
				} while (cursorWrapper.moveToNext());

				// Write one row to the Excel file, for one meeting.
				try {
					writeRow(rowNumber++, rowValues);
				} catch (IOException e) {
					Log.e(TAG, e.getMessage(), e);
					return false;
				}
			}
		} finally {
			cursorWrapper.close();
		}

		// Clean up
		try {
			mWorkbook.write();
			mWorkbook.close();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
			return false;
		} catch (WriteException e) {
			Log.e(TAG, e.getMessage(), e);
			return false;
		}

		// Prompt the user to choose an app to share the file.
		showChooser();
		return true;

	}

	/**
	 * Create the workbook, sheet, custom cell formats, and freeze row and
	 * column. Also write the column headings.
	 */
	private void writeHeader(List<String> columnNames) throws IOException {
		mWorkbook = Workbook.createWorkbook(mFile);
		mSheet = mWorkbook
				.createSheet(mContext.getString(R.string.app_name), 0);
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
	 * Write a single row to the Excel file.
	 */
	private void writeRow(int rowNumber, String[] cellValues)
			throws IOException {
		mSheet.insertRow(rowNumber);
		for (int i = 0; i < cellValues.length; i++) {
			CellFormat cellFormat = null;
			insertCell(cellValues[i], rowNumber, i, cellFormat);
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
		Label label = format == null ? new Label(column, row, text)
				: new Label(column, row, text, format);
		try {
			mSheet.addCell(label);
		} catch (JXLException e) {
			Log.e(TAG, "writeHeader Could not insert cell " + text + " at row="
					+ row + ", col=" + column, e);
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

		} catch (WriteException e) {
			Log.e(TAG, "createCellFormats Could not create cell formats", e);
		}
	}

	/**
	 * Bring up the chooser to send the file.
	 */
	private void showChooser() {
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_SUBJECT,
				mContext.getString(R.string.export_message_subject));
		sendIntent.putExtra(Intent.EXTRA_TEXT,
				mContext.getString(R.string.export_message_body));
		sendIntent.putExtra(Intent.EXTRA_STREAM,
				Uri.parse("file://" + mFile.getAbsolutePath()));
		sendIntent.setType("application/vnd.ms-excel");
		mContext.startActivity(Intent.createChooser(sendIntent, mContext
				.getResources().getText(R.string.action_share)));
	}
}