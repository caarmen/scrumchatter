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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.MeetingActivity;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.adapter.MeetingsCursorAdapter;
import ca.rmen.android.scrumchatter.adapter.MeetingsCursorAdapter.MeetingItemCache;
import ca.rmen.android.scrumchatter.provider.MeetingColumns;
import ca.rmen.android.scrumchatter.provider.MemberColumns;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * Displays the list of meetings that have taken place.
 */
public class MeetingsListFragment extends SherlockListFragment {
    private static final String TAG = Constants.TAG + "/" + MeetingsListFragment.class.getSimpleName();
    private static final int URL_LOADER = 0;

    private MeetingsCursorAdapter mAdapter;
    private SharedPreferences mPrefs;
    private int mTeamId;

    public MeetingsListFragment() {
        super();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.meeting_list, null);
        TextView emptyText = (TextView) view.findViewById(android.R.id.empty);
        emptyText.setText(R.string.empty_list_meetings);
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        mPrefs.registerOnSharedPreferenceChangeListener(mPrefsListener);
        mTeamId = mPrefs.getInt(Constants.PREF_TEAM_ID, Constants.DEFAULT_TEAM_ID);
        getLoaderManager().initLoader(URL_LOADER, null, mLoaderCallbacks);
    }

    @Override
    public void onDetach() {
        mPrefs.unregisterOnSharedPreferenceChangeListener(mPrefsListener);
        super.onDetach();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.meetings_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Start a new meeting.
        // Check if we have any members first.  A meeting with no members is not much fun.
        if (item.getItemId() == R.id.action_new_meeting) {
            AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {

                @Override
                protected Boolean doInBackground(Void... params) {
                    Cursor c = getActivity().getContentResolver().query(MemberColumns.CONTENT_URI, new String[] { "count(*)" }, MemberColumns.TEAM_ID + "=?",
                            new String[] { String.valueOf(mTeamId) }, null);
                    try {
                        c.moveToFirst();
                        int memberCount = c.getInt(0);
                        return memberCount > 0;
                    } finally {
                        c.close();
                    }
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    if (result) {
                        Intent intent = new Intent(getActivity(), MeetingActivity.class);
                        startActivity(intent);
                    } else {
                        ScrumChatterDialog.showInfoDialog(getActivity(), R.string.dialog_error_title_one_member_required,
                                R.string.dialog_error_message_one_member_required);
                    }
                }

            };
            task.execute();

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
            Log.v(TAG, "onCreateLoader, loaderId = " + loaderId + ", bundle = " + bundle);
            String selection = MeetingColumns.TEAM_ID + "=?";
            String[] selectionArgs = new String[] { String.valueOf(mTeamId) };
            CursorLoader loader = new CursorLoader(getActivity(), MeetingColumns.CONTENT_URI, null, selection, selectionArgs, MeetingColumns.MEETING_DATE
                    + " DESC");
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            Log.v(TAG, "onLoadFinished, loader = " + loader + ", cursor = " + cursor);
            if (mAdapter == null) {
                mAdapter = new MeetingsCursorAdapter(getActivity(), mOnClickListener);
                setListAdapter(mAdapter);
            }
            getView().findViewById(R.id.progressContainer).setVisibility(View.GONE);
            mAdapter.changeCursor(cursor);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            Log.v(TAG, "onLoaderReset " + loader);
            if (mAdapter != null) mAdapter.changeCursor(null);
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
                    ScrumChatterDialog.showDialog(activity, activity.getString(R.string.action_delete_meeting),
                            activity.getString(R.string.dialog_message_delete_meeting_confirm, cache.date), new DialogInterface.OnClickListener() {
                                // The user clicked ok. Let's delete the
                                // meeting.
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which == DialogInterface.BUTTON_POSITIVE) {
                                        // Delete the meeting in a background
                                        // thread.
                                        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

                                            @Override
                                            protected Void doInBackground(Void... params) {
                                                Uri uri = Uri.withAppendedPath(MeetingColumns.CONTENT_URI, String.valueOf(cache.id));
                                                activity.getContentResolver().delete(uri, null, null);
                                                return null;
                                            }
                                        };
                                        task.execute();
                                    }
                                }
                            });
                    break;
                default:
                    break;
            }
        }
    };

    private OnSharedPreferenceChangeListener mPrefsListener = new OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            mTeamId = sharedPreferences.getInt(Constants.PREF_TEAM_ID, Constants.DEFAULT_TEAM_ID);
            getLoaderManager().restartLoader(URL_LOADER, null, mLoaderCallbacks);
        }
    };
}
