package ca.rmen.android.scrumchatter.ui;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.adapter.MeetingCursorAdapter;
import ca.rmen.android.scrumchatter.provider.MeetingMemberColumns;
import ca.rmen.android.scrumchatter.provider.MemberColumns;

import com.actionbarsherlock.app.SherlockListFragment;

public class MeetingFragment extends SherlockListFragment {

	private static final String TAG = Constants.TAG + "/"
			+ MeetingFragment.class.getSimpleName();

	private static final int URL_LOADER = 0;
	private long mMeetingId;

	private MeetingCursorAdapter mAdapter;

	public MeetingFragment() {
		super();
		mMeetingId = -1;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.v(TAG, "onCreateView");
		View view = inflater.inflate(R.layout.meeting_fragment, null);
		return view;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Log.v(TAG, "onAttach");
	}

	public void loadMeeting(long meetingId, OnClickListener onClickListener) {
		Log.v(TAG, "loadMeeting");
		mMeetingId = meetingId;
		mAdapter = new MeetingCursorAdapter(getActivity(), onClickListener);
		setListAdapter(mAdapter);
		getLoaderManager().initLoader(URL_LOADER, null, mLoaderCallbacks);
	}

	LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderCallbacks<Cursor>() {

		@Override
		public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
			String[] projection = new String[] { MemberColumns._ID,
					MemberColumns.NAME, MeetingMemberColumns.DURATION };

			Uri uri = Uri.withAppendedPath(MeetingMemberColumns.CONTENT_URI,
					String.valueOf(mMeetingId));
			CursorLoader loader = new CursorLoader(getActivity(), uri,
					projection, null, null, MemberColumns.NAME);
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

}
