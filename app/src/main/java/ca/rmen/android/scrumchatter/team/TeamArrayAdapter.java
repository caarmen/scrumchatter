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
package ca.rmen.android.scrumchatter.team;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import ca.rmen.android.scrumchatter.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.provider.TeamColumns;

/**
 * Adapter for the list of teams. The adapter contains all the team names, sorted alphabetically. In addition, one additional item is added to the end, which is
 * a special string "New team...", which the user may tap on to create a new team. Each item, except for the last one, has a radio button.
 */
public class TeamArrayAdapter extends ArrayAdapter<CharSequence> {
    private static final String TAG = Constants.TAG + "/" + TeamArrayAdapter.class.getSimpleName();
    private final Context mContext;

    public TeamArrayAdapter(final Context context) {
        super(context, R.layout.scrum_chatter_select_singlechoice_material);
        Log.v(TAG, "Constructor");
        mContext = context;
        reload();
    }

    /**
     * Query the DB for the list of team names, then add the team names to the adapter.
     */
    public void reload() {
        new AsyncTask<Void, Void, List<CharSequence>>() {

            /**
             * Query the teams table, and return a list of the team names, plus the special "New team..." item.
             */
            @Override
            protected List<CharSequence> doInBackground(Void... params) {
                Log.v(TAG, "doInBackground");
                List<CharSequence> teamNames = new ArrayList<>();
                Cursor c = mContext.getContentResolver().query(TeamColumns.CONTENT_URI, new String[] { TeamColumns.TEAM_NAME }, null, null,
                        TeamColumns.TEAM_NAME + " COLLATE NOCASE");

                if (c != null) {
                    try {
                        // Add the names of all the teams
                        while (c.moveToNext()) {
                            teamNames.add(c.getString(0));
                        }
                        // Add the special element for a "new team"
                        teamNames.add(mContext.getString(R.string.new_team));
                    } finally {
                        c.close();
                    }
                }
                return teamNames;
            }

            /**
             * Reset the adapter data in one go (only one notification at the end).
             */
            @Override
            protected void onPostExecute(List<CharSequence> teamNames) {
                setNotifyOnChange(false);
                clear();
                for (CharSequence teamName : teamNames)
                    add(teamName);
                notifyDataSetChanged();
            }
        }.execute();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View result = super.getView(position, convertView, parent);
        if (result instanceof CheckedTextView) {
            CheckedTextView ctv = (CheckedTextView) result;
            // The last item is the special "New team..." item. Don't show a radio button for this item
            if (position == getCount() - 1) ctv.setCheckMarkDrawable(null);
            // Hack for Android 2.x: replace the radio button. See {@link DialogStyleHacks}
            //else
            //    ctv.setCheckMarkDrawable(R.drawable.btn_radio_holo_light);
        }
        return result;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == getCount() - 1) return 1;
        return 0;
    }
}
