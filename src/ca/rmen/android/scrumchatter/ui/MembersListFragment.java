package ca.rmen.android.scrumchatter.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.widget.EditText;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.adapter.MembersCursorAdapter;
import ca.rmen.android.scrumchatter.provider.MemberColumns;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class MembersListFragment extends SherlockListFragment implements
		LoaderCallbacks<Cursor> {

	private static final int URL_LOADER = 0;

	private MembersCursorAdapter mAdapter;

	public MembersListFragment() {
		super();
		setHasOptionsMenu(true);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mAdapter = new MembersCursorAdapter(activity, null, true);
		setListAdapter(mAdapter);
		getLoaderManager().initLoader(URL_LOADER, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
		CursorLoader loader = new CursorLoader(getActivity(),
				MemberColumns.CONTENT_URI, null, null, null, null);
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
		inflater.inflate(R.menu.members_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_new_member) {
			final Activity activity = getActivity();
			AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			final EditText input = new EditText(activity);
			builder.setView(input);
			builder.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							String value = input.getText().toString();
							if (!TextUtils.isEmpty(value)) {
								ContentValues values = new ContentValues();
								values.put(MemberColumns.NAME, value);
								activity.getContentResolver().insert(
										MemberColumns.CONTENT_URI, values);
							}
						}
					});
			builder.setNegativeButton(android.R.string.cancel, null);
			builder.create().show();
			return true;
		}
		return true;
	}

}
