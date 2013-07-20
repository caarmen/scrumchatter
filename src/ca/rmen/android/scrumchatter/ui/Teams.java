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
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.provider.TeamColumns;
import ca.rmen.android.scrumchatter.ui.ScrumChatterDialog.InputValidator;

/**
 * Provides both UI and DB logic regarding the management of Teams: renaming, choosing, creating, and deleting teams.
 */
public class Teams {
    private static final String TAG = Teams.class.getSimpleName();
    private final Context mContext;

    public static class Team {
        public final int teamId;
        public final Uri teamUri;
        public final String teamName;

        public Team(int teamId, Uri teamUri, String teamName) {
            this.teamId = teamId;
            this.teamUri = teamUri;
            this.teamName = teamName;
        }
    };

    public Teams(Context context) {
        mContext = context;
    }

    /**
     * Show a dialog with the list of teams. Upon selecting a team, update the shared preference for the current team.
     * 
     * @param team the current team being used.
     */
    public void selectTeam(final Team team) {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            CharSequence[] mTeamNames = null;
            int mSelectedTeam = -1;

            /**
             * Create the list of team names, excluding the currently selected team, and with a special last item to create a new team.
             */
            @Override
            protected Void doInBackground(Void... params) {
                Cursor c = mContext.getContentResolver().query(TeamColumns.CONTENT_URI, new String[] { TeamColumns.TEAM_NAME }, null, null,
                        TeamColumns.TEAM_NAME + " COLLATE NOCASE");

                if (c != null) {
                    try {
                        mTeamNames = new CharSequence[c.getCount() + 1];
                        int i = 0;
                        while (c.moveToNext()) {
                            String teamName = c.getString(0);
                            if (teamName.equals(team.teamName)) mSelectedTeam = i;
                            mTeamNames[i++] = teamName;
                        }
                        mTeamNames[i++] = mContext.getString(R.string.new_team);
                    } finally {
                        c.close();
                    }
                }
                return null;
            }

            /**
             * Show a dialog with the list of teams. Upon clicking a team name, switch to that team. Else upon clicking the "create new team" button, create a
             * new team.
             */
            @Override
            protected void onPostExecute(Void result) {
                if (mTeamNames != null && mTeamNames.length >= 1) {
                    OnClickListener itemListener = new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            // The user clicked on the "new team" item.
                            if (which == mTeamNames.length - 1) {
                                createTeam();
                            }
                            // The user selected an existing team.  Update the shared preference for this team, in the background.
                            else {
                                final CharSequence teamName = mTeamNames[which];
                                AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

                                    @Override
                                    protected Void doInBackground(Void... params) {
                                        Cursor c = mContext.getContentResolver().query(TeamColumns.CONTENT_URI, new String[] { TeamColumns._ID },
                                                TeamColumns.TEAM_NAME + " = ?", new String[] { String.valueOf(teamName) }, null);
                                        if (c != null) {
                                            try {
                                                c.moveToFirst();
                                                if (c.getCount() == 1) {
                                                    int teamId = c.getInt(0);
                                                    PreferenceManager.getDefaultSharedPreferences(mContext).edit().putInt(Constants.PREF_TEAM_ID, teamId)
                                                            .commit();
                                                } else {
                                                    Log.wtf(TAG, "Found " + c.getCount() + " teams for " + teamName);
                                                }

                                            } finally {
                                                c.close();
                                            }
                                        }
                                        return null;
                                    }
                                };
                                task.execute();
                            }
                        }
                    };
                    ScrumChatterDialog.showChoiceDialog(mContext, R.string.dialog_message_switch_team, mTeamNames, mSelectedTeam, itemListener);

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
                                Uri newTeamUri = mContext.getContentResolver().insert(TeamColumns.CONTENT_URI, values);
                                int newTeamId = Integer.valueOf(newTeamUri.getLastPathSegment());
                                PreferenceManager.getDefaultSharedPreferences(mContext).edit().putInt(Constants.PREF_TEAM_ID, newTeamId).commit();
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
    public void renameTeam(final Team team) {
        if (team != null) {
            // Show a dialog to input a new team name for the current team.
            TeamNameValidator validator = new TeamNameValidator(team.teamName);
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
                                    mContext.getContentResolver().update(team.teamUri, values, null, null);
                                    return null;
                                }
                            };
                            task.execute();
                        }
                    }
                }
            };
            editText.setText(team.teamName);
            ScrumChatterDialog.showEditTextDialog(mContext, R.string.action_team_rename, R.string.dialog_message_rename_team, editText, onClickListener,
                    validator);
        }
    }

    /**
     * Shows a confirmation dialog to the user. Upon pressing OK, the current team is deleted.
     */
    public void deleteTeam(final Team team) {

        AsyncTask<Void, Void, Integer> task = new AsyncTask<Void, Void, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                return getTeamCount();
            }

            @Override
            protected void onPostExecute(Integer teamCount) {
                // We need at least one team in the app.
                if (teamCount <= 1) {
                    ScrumChatterDialog.showInfoDialog(mContext, R.string.action_team_delete, R.string.dialog_error_one_team_required);
                }
                // Delete this team
                else if (team != null) {
                    DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == DialogInterface.BUTTON_POSITIVE) {
                                AsyncTask<Void, Void, Void> deleteTask = new AsyncTask<Void, Void, Void>() {
                                    @Override
                                    protected Void doInBackground(Void... params) {
                                        // delete this team
                                        mContext.getContentResolver().delete(team.teamUri, null, null);
                                        // pick another current team
                                        selectFirstTeam();
                                        return null;
                                    }

                                };
                                deleteTask.execute();
                            }
                        }
                    };
                    ScrumChatterDialog.showDialog(mContext, mContext.getString(R.string.action_team_delete),
                            mContext.getString(R.string.dialog_message_delete_team_confirm, team.teamName), onClickListener);
                }
            }
        };
        task.execute();
    }

    /**
     * Select the first team in our DB.
     */
    public void selectFirstTeam() {
        Cursor c = mContext.getContentResolver().query(TeamColumns.CONTENT_URI, new String[] { TeamColumns._ID }, null, null, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    int teamId = c.getInt(0);
                    PreferenceManager.getDefaultSharedPreferences(mContext).edit().putInt(Constants.PREF_TEAM_ID, teamId).commit();
                }
            } finally {
                c.close();
            }
        }
    }

    /**
     * @return the Team currently selected by the user.
     */
    public Team getCurrentTeam() {
        // Retrieve the current team name and construct a uri for the team based on the current team id.
        int teamId = PreferenceManager.getDefaultSharedPreferences(mContext).getInt(Constants.PREF_TEAM_ID, Constants.DEFAULT_TEAM_ID);
        Uri teamUri = Uri.withAppendedPath(TeamColumns.CONTENT_URI, String.valueOf(teamId));
        Cursor c = mContext.getContentResolver().query(teamUri, new String[] { TeamColumns.TEAM_NAME }, null, null, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    String teamName = c.getString(0);
                    return new Team(teamId, teamUri, teamName);
                }
            } finally {
                c.close();
            }
        }
        Log.wtf(TAG, "Could not get the curren team", new Throwable());
        return null;
    }

    /**
     * @return the total number of teams
     */
    public int getTeamCount() {
        Cursor c = mContext.getContentResolver().query(TeamColumns.CONTENT_URI, new String[] { "count(*)" }, null, null, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) return c.getInt(0);
            } finally {
                c.close();
            }
        }
        return 0;
    }

    /**
     * Returns an error if the user entered the name of an existing team. To prevent renaming or creating multiple teams with the same name.
     */
    private class TeamNameValidator implements InputValidator {
        private final String mTeamName;

        /**
         * @param teamName optional. If given, we won't show an error for renaming a team to its current name.
         */
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
