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

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.provider.TeamColumns;
import ca.rmen.android.scrumchatter.ui.ScrumChatterDialog.InputValidator;

public class Teams {
    private static final String TAG = Teams.class.getSimpleName();
    private final Context mContext;

    public Teams(Context context) {
        mContext = context;
    }

    /**
     * Show a dialog with the list of teams. Upon selecting a team, update the shared preference for the current team.
     */
    public void selectTeam() {
        AsyncTask<Void, Void, CharSequence[]> task = new AsyncTask<Void, Void, CharSequence[]>() {

            @Override
            protected CharSequence[] doInBackground(Void... params) {
                Cursor c = mContext.getContentResolver().query(TeamColumns.CONTENT_URI, new String[] { TeamColumns.TEAM_NAME }, null, null,
                        TeamColumns.TEAM_NAME + " COLLATE NOCASE");
                if (c != null) {
                    try {
                        if (c.moveToFirst()) {
                            CharSequence[] result = new CharSequence[c.getCount() + 1];
                            int i = 0;
                            do {
                                result[i++] = c.getString(0);
                            } while (c.moveToNext());
                            result[i++] = mContext.getString(R.string.new_team);
                            return result;
                        }
                    } finally {
                        c.close();
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(final CharSequence[] result) {
                if (result != null && result.length > 1) {
                    OnClickListener itemListener = new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == result.length - 1) {
                                createTeam();
                            } else {
                                Toast.makeText(mContext, "Should switch teams to " + result[which], Toast.LENGTH_SHORT).show();
                            }
                        }
                    };
                    ScrumChatterDialog.showChoiceDialog(mContext, R.string.dialog_message_switch_team, result, itemListener);

                } else {
                    Log.wtf(TAG, "No existing teams found");
                }
            }

        };
        task.execute();
    }

    /**
     * Show a dialog with a text input for the new team name. Validate that the team doesn't already exist. Upon pressing "OK", create the team.
     */
    private void createTeam() {
        TeamNameValidator validator = new TeamNameValidator(null);
        final EditText editText = new EditText(mContext);
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {

                    final String teamName = editText.getText().toString().trim();

                    // Ignore an empty name.
                    if (!TextUtils.isEmpty(teamName)) {
                        // Create the new team in a background thread.
                        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

                            @Override
                            protected Void doInBackground(Void... params) {
                                ContentValues values = new ContentValues(1);
                                values.put(TeamColumns.TEAM_NAME, teamName);
                                mContext.getContentResolver().insert(TeamColumns.CONTENT_URI, values);
                                return null;
                            }
                        };
                        task.execute();
                    }
                }
            }
        };
        ScrumChatterDialog.showEditTextDialog(mContext, R.string.action_new_team, R.string.dialog_message_rename_team, editText, onClickListener, validator);
    }

    /**
     * Retrieve the currently selected team. Show a dialog with a text input to rename this team. Validate that the new name doesn't correspond to any other
     * existing team. Upon pressing ok, rename the current team.
     */
    public void renameTeam() {
        AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
            private Uri mTeamUri;
            private String mTeamName;

            @Override
            protected Boolean doInBackground(Void... args) {
                // Retrieve the current team name and construct a uri for the team based on the current team id.
                int teamId = PreferenceManager.getDefaultSharedPreferences(mContext).getInt(Constants.EXTRA_TEAM_ID, TeamColumns.DEFAULT_TEAM_ID);
                mTeamUri = Uri.withAppendedPath(TeamColumns.CONTENT_URI, String.valueOf(teamId));
                Cursor c = mContext.getContentResolver().query(mTeamUri, new String[] { TeamColumns.TEAM_NAME }, null, null, null);
                if (c != null) {
                    try {
                        if (c.moveToFirst()) {
                            mTeamName = c.getString(0);
                            return true;
                        }
                    } finally {
                        c.close();
                    }
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean gotTeamName) {
                if (gotTeamName) {

                    // Show a dialog to input a new team name for the current team.
                    TeamNameValidator validator = new TeamNameValidator(mTeamName);
                    final EditText editText = new EditText(mContext);
                    DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == DialogInterface.BUTTON_POSITIVE) {

                                final String teamName = editText.getText().toString().trim();

                                // Ignore an empty name.
                                if (!TextUtils.isEmpty(teamName)) {
                                    // Rename the team in a background thread.
                                    AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

                                        @Override
                                        protected Void doInBackground(Void... params) {
                                            ContentValues values = new ContentValues(1);
                                            values.put(TeamColumns.TEAM_NAME, teamName);
                                            mContext.getContentResolver().update(mTeamUri, values, null, null);
                                            return null;
                                        }
                                    };
                                    task.execute();
                                }
                            }
                        }
                    };
                    editText.setText(mTeamName);
                    ScrumChatterDialog.showEditTextDialog(mContext, R.string.action_team_rename, R.string.dialog_message_rename_team, editText,
                            onClickListener, validator);
                } else {
                    Log.wtf(TAG, "Could not determine the curent team name");
                }
            }
        };
        task.execute();
    }

    private class TeamNameValidator implements InputValidator {
        private final String mTeamName;

        TeamNameValidator(String teamName) {
            mTeamName = teamName;
        }

        @Override
        public String getError(CharSequence input) {
            // In the case of changing a team name, mTeamName will not be null, and we won't show an error if the name the user enters is the same as the existing team.
            // In the case of adding a new team, mTeamName will be null.
            if (!TextUtils.isEmpty(mTeamName) && !TextUtils.isEmpty(input) && mTeamName.equals(input.toString())) return null;

            // Query for a team with this name.
            Cursor cursor = mContext.getContentResolver().query(TeamColumns.CONTENT_URI, new String[] { "count(*)" }, TeamColumns.TEAM_NAME + "=?",
                    new String[] { String.valueOf(input) }, null);

            // Now Check if the team member exists.
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int existingTeamCount = cursor.getInt(0);
                    cursor.close();
                    if (existingTeamCount > 0) return mContext.getString(R.string.error_team_exists, input);
                }
            }
            return null;
        }

    }

}
