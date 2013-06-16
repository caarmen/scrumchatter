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
package ca.rmen.android.scrumchatter.ui;

/**
 * Displays the list of team members.
 */
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.adapter.MembersCursorAdapter;
import ca.rmen.android.scrumchatter.adapter.MembersCursorAdapter.MemberItemCache;
import ca.rmen.android.scrumchatter.provider.MeetingMemberColumns;
import ca.rmen.android.scrumchatter.provider.MemberColumns;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class MembersListFragment extends SherlockListFragment {

	private static final String TAG = Constants.TAG + "/"
			+ MembersListFragment.class.getSimpleName();

	private static final int URL_LOADER = 0;
	private String mOrderByField = MemberColumns.NAME;
	private TextView mTextViewName;
	private TextView mTextViewAvgDuration;
	private TextView mTextViewSumDuration;

	private MembersCursorAdapter mAdapter;

	public MembersListFragment() {
		super();
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.member_list, null);
		mTextViewName = (TextView) view.findViewById(R.id.tv_name);
		mTextViewAvgDuration = (TextView) view
				.findViewById(R.id.tv_avg_duration);
		mTextViewSumDuration = (TextView) view
				.findViewById(R.id.tv_sum_duration);
		mTextViewName.setOnClickListener(mOnClickListener);
		mTextViewAvgDuration.setOnClickListener(mOnClickListener);
		mTextViewSumDuration.setOnClickListener(mOnClickListener);
		return view;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mAdapter = new MembersCursorAdapter(activity, mOnClickListener);
		setListAdapter(mAdapter);
		getLoaderManager().initLoader(URL_LOADER, null, mLoaderCallbacks);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.members_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Create a new team member
		if (item.getItemId() == R.id.action_new_member) {
			final Activity activity = getActivity();
			// We'll just show a dialog with a simple EditText for the team
			// member's name.
			AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			final EditText input = new EditText(activity);
			builder.setView(input)
					.setTitle(R.string.action_new_member)
					.setMessage(R.string.dialog_message_new_member)
					.setNegativeButton(android.R.string.cancel, null)
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int whichButton) {
									String memberName = input.getText()
											.toString().trim();

									// Ignore an empty name.
									if (!TextUtils.isEmpty(memberName)) {
										// Create the new member. TODO put this
										// in an AsyncTask (we don't have enough
										// inner classes here yet...)
										ContentValues values = new ContentValues();
										values.put(MemberColumns.NAME,
												memberName);
										activity.getContentResolver().insert(
												MemberColumns.CONTENT_URI,
												values);
									}
								}
							});
			final AlertDialog dialog = builder.create();
			// Prevent the user from creating multiple team members with the
			// same name.
			input.addTextChangedListener(new TextWatcher() {

				@Override
				public void afterTextChanged(Editable s) {
					validateMemberName();
				}

				@Override
				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {
					validateMemberName();
				}

				@Override
				public void onTextChanged(CharSequence s, int start,
						int before, int count) {
					validateMemberName();
				}

				private void validateMemberName() {
					// Start off with everything a-ok.
					input.setError(null);
					Button okButton = dialog
							.getButton(AlertDialog.BUTTON_POSITIVE);
					okButton.setEnabled(true);

					// Check if this team member exists already.
					String memberName = input.getText().toString().trim();
					Cursor existingMemberCountCursor = activity
							.getContentResolver().query(
									MemberColumns.CONTENT_URI,
									new String[] { "count(*)" },
									MemberColumns.NAME + "=?",
									new String[] { memberName }, null);

					// Now Check if the team member exists.
					if (existingMemberCountCursor != null) {
						existingMemberCountCursor.moveToFirst();
						int existingMemberCount = existingMemberCountCursor
								.getInt(0);
						existingMemberCountCursor.close();
						// If the member exists, highlight the error
						// and disable the OK button.
						if (existingMemberCount > 0) {
							input.setError(activity.getString(
									R.string.error_member_exists, memberName));
							okButton.setEnabled(false);
						}
					}
				}
			});
			dialog.show();

			return true;
		}
		return true;
	}

	private LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderCallbacks<Cursor>() {
		@Override
		public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
			Log.v(TAG, "onCreateLoader, order by " + mOrderByField);
			String[] projection = new String[] { MemberColumns._ID,
					MemberColumns.NAME, MeetingMemberColumns.SUM_DURATION,
					MeetingMemberColumns.AVG_DURATION };
			CursorLoader loader = new CursorLoader(getActivity(),
					MeetingMemberColumns.CONTENT_URI, projection, null, null,
					mOrderByField);
			return loader;
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
			mAdapter.changeCursor(cursor);
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			mAdapter.changeCursor(null);
		}

	};

	private final OnClickListener mOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Log.v(TAG, "onClick: " + v.getId());
			switch (v.getId()) {
			// The user wants to delete a team member.
			case R.id.btn_delete:
				if (v.getTag() instanceof MemberItemCache) {
					final MemberItemCache cache = (MemberItemCache) v.getTag();
					final Activity activity = getActivity();
					// Let's ask him if he's sure.
					AlertDialog.Builder builder = new AlertDialog.Builder(
							activity);
					builder.setTitle(R.string.action_delete_member)
							.setMessage(
									activity.getString(
											R.string.dialog_message_delete_member_confirm,
											cache.name))
							.setNegativeButton(android.R.string.cancel, null)
							.setPositiveButton(android.R.string.ok,
									new DialogInterface.OnClickListener() {

										// The user has confirmed to delete the
										// member.
										public void onClick(
												DialogInterface dialog,
												int whichButton) {
											// TODO do on a background thread.
											Uri uri = Uri.withAppendedPath(
													MemberColumns.CONTENT_URI,
													String.valueOf(cache.id));
											activity.getContentResolver()
													.delete(uri, null, null);
										}
									});
					builder.create().show();
				}
				break;
			case R.id.tv_name:
			case R.id.tv_avg_duration:
			case R.id.tv_sum_duration:
				setSortField(v.getId());
				break;
			default:
				break;
			}
		}

		/**
		 * Resort the list of members by the given column
		 * 
		 * @param viewId
		 *            the header label on which the user clicked.
		 */
		private void setSortField(int viewId) {
			String oldOrderByField = mOrderByField;
			int selectedHeaderColor = getResources().getColor(
					R.color.selected_header);
			int unselectedHeaderColor = getResources().getColor(
					R.color.unselected_header);
			// Reset all the header text views to the default color
			mTextViewName.setTextColor(unselectedHeaderColor);
			mTextViewAvgDuration.setTextColor(unselectedHeaderColor);
			mTextViewSumDuration.setTextColor(unselectedHeaderColor);

			// Depending on the header column selected, change the sort order
			// field and highlight that header column.
			switch (viewId) {
			case R.id.tv_name:
				mOrderByField = MemberColumns.NAME;
				mTextViewName.setTextColor(selectedHeaderColor);
				break;
			case R.id.tv_avg_duration:
				mOrderByField = MeetingMemberColumns.AVG_DURATION + " DESC ";
				mTextViewAvgDuration.setTextColor(selectedHeaderColor);
				break;
			case R.id.tv_sum_duration:
				mOrderByField = MeetingMemberColumns.SUM_DURATION + " DESC ";
				mTextViewSumDuration.setTextColor(selectedHeaderColor);
				break;
			default:
				break;
			}
			// Requery if needed.
			if (!oldOrderByField.equals(mOrderByField))
				getLoaderManager().restartLoader(URL_LOADER, null,
						mLoaderCallbacks);

		}
	};
}
