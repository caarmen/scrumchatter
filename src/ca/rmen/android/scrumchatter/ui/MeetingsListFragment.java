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
import ca.rmen.android.scrumchatter.MeetingActivity;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.adapter.MeetingsCursorAdapter;
import ca.rmen.android.scrumchatter.adapter.MeetingsCursorAdapter.MeetingItemCache;
import ca.rmen.android.scrumchatter.provider.MeetingColumns;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class MeetingsListFragment extends SherlockListFragment implements
		LoaderCallbacks<Cursor> {

	private static final int URL_LOADER = 0;

	private MeetingsCursorAdapter mAdapter;

	public MeetingsListFragment() {
		super();
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.meeting_list, null);
		return view;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mAdapter = new MeetingsCursorAdapter(activity, mOnClickListener);
		setListAdapter(mAdapter);
		getLoaderManager().initLoader(URL_LOADER, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
		CursorLoader loader = new CursorLoader(getActivity(),
				MeetingColumns.CONTENT_URI, null, null, null, null);
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

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.meetings_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_new_meeting) {
			Intent intent = new Intent(getActivity(), MeetingActivity.class);
			startActivity(intent);
			return true;
		}
		return true;
	}

	private final OnClickListener mOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			final MeetingItemCache cache = (MeetingItemCache) v.getTag();
			switch (v.getId()) {
			case R.id.btn_delete:
				final Activity activity = getActivity();
				AlertDialog.Builder builder = new AlertDialog.Builder(activity);
				builder.setTitle(R.string.action_delete_meeting);
				builder.setMessage(activity.getString(
						R.string.dialog_message_delete_meeting_confirm,
						cache.date));
				builder.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,
									int whichButton) {
								// TODO do on a background thread.
								Uri uri = Uri.withAppendedPath(
										MeetingColumns.CONTENT_URI,
										String.valueOf(cache.id));
								activity.getContentResolver().delete(uri, null,
										null);
							}
						});
				builder.setNegativeButton(android.R.string.cancel, null);
				builder.create().show();
				break;
			case R.id.tv_meeting_date:
				Intent intent = new Intent(getActivity(), MeetingActivity.class);
				intent.putExtra(MeetingActivity.EXTRA_MEETING_ID, cache.id);
				startActivity(intent);
				break;
			default:
				break;
			}
		}
	};

}
