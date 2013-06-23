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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ListView;
import ca.rmen.android.scrumchatter.MeetingActivity;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.adapter.MeetingsCursorAdapter;
import ca.rmen.android.scrumchatter.adapter.MeetingsCursorAdapter.MeetingItemCache;
import ca.rmen.android.scrumchatter.provider.MeetingColumns;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * Displays the list of meetings that have taken place.
 */
public class MeetingsListFragment extends SherlockListFragment {

	private static final int URL_LOADER = 0;

	private MeetingsCursorAdapter mAdapter;

	public MeetingsListFragment() {
		super();
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.meeting_list, null);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		getLoaderManager().initLoader(URL_LOADER, null, mLoaderCallbacks);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.meetings_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Start a new meeting.
		if (item.getItemId() == R.id.action_new_meeting) {
			Intent intent = new Intent(getActivity(), MeetingActivity.class);
			startActivity(intent);
			return true;
		}
		return true;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		// The user clicked on the meeting. Let's go to the
		// details of that meeting.
		Intent intent = new Intent(getActivity(), MeetingActivity.class);
		intent.putExtra(MeetingActivity.EXTRA_MEETING_ID, id);
		startActivity(intent);
	}

	private LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderCallbacks<Cursor>() {
		@Override
		public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
			CursorLoader loader = new CursorLoader(getActivity(),
					MeetingColumns.CONTENT_URI, null, null, null, null);
			return loader;
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
			if (mAdapter == null) {
				mAdapter = new MeetingsCursorAdapter(getActivity(),
						mOnClickListener);
				setListAdapter(mAdapter);
			}
			getView().findViewById(R.id.progressContainer).setVisibility(
					View.GONE);
			mAdapter.changeCursor(cursor);
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			if (mAdapter != null)
				mAdapter.changeCursor(null);
		}
	};

	private final OnClickListener mOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			final MeetingItemCache cache = (MeetingItemCache) v.getTag();
			switch (v.getId()) {
			// The user wants to delete a meeting
			case R.id.btn_delete:
				final Activity activity = getActivity();
				// Let's ask him if he's sure first.
				AlertDialog.Builder builder = new AlertDialog.Builder(activity)
						.setTitle(R.string.action_delete_meeting)
						.setMessage(
								activity.getString(
										R.string.dialog_message_delete_meeting_confirm,
										cache.date))
						.setNegativeButton(android.R.string.cancel, null)
						.setPositiveButton(android.R.string.ok,
								new DialogInterface.OnClickListener() {
									// The user clicked ok. Let's delete the
									// meeting.
									public void onClick(DialogInterface dialog,
											int whichButton) {
										// TODO do on an AsyncTask
										Uri uri = Uri.withAppendedPath(
												MeetingColumns.CONTENT_URI,
												String.valueOf(cache.id));
										activity.getContentResolver().delete(
												uri, null, null);
									}
								});
				builder.create().show();
				break;
			default:
				break;
			}
		}
	};
}
