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
package ca.rmen.android.scrumchatter.member.list;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.provider.MemberColumns;
import ca.rmen.android.scrumchatter.ui.ScrumChatterDialog;
import ca.rmen.android.scrumchatter.ui.ScrumChatterDialog.InputValidator;

/**
 * Provides both UI and DB logic regarding the management of members: creating, and deleting members for now.
 */
public class Members {
    private static final String TAG = Constants.TAG + "/" + Members.class.getSimpleName();
    private final Context mContext;

    public static class Member {
        private final long id;
        private final String name;

        public Member(long memberId, String memberName) {
            this.id = memberId;
            this.name = memberName;
        }
    };

    public Members(Context context) {
        mContext = context;
    }

    /**
     * Show a dialog with a text input for the new member name. Validate that the member doesn't already exist in the given team. Upon pressing "OK", create the
     * member.
     * 
     * @param teamId the id of the team in which the member should be added
     */
    public void createMember(final int teamId) {
        Log.v(TAG, "createMember, teamId = " + teamId);
        final EditText editText = new EditText(mContext);

        // Prevent the user from creating multiple team members with the same name.
        InputValidator validator = new MemberNameValidator(teamId);

        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {

                    final String memberName = editText.getText().toString().trim();

                    // Ignore an empty name.
                    if (!TextUtils.isEmpty(memberName)) {
                        // Create the new member in a background thread.
                        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

                            @Override
                            protected Void doInBackground(Void... params) {
                                ContentValues values = new ContentValues(2);
                                values.put(MemberColumns.NAME, memberName);
                                values.put(MemberColumns.TEAM_ID, teamId);
                                mContext.getContentResolver().insert(MemberColumns.CONTENT_URI, values);
                                return null;
                            }
                        };
                        task.execute();
                    }
                }
            }

        };
        ScrumChatterDialog.showEditTextDialog(mContext, R.string.action_new_member, R.string.dialog_message_new_member, editText, onClickListener, validator);

    }


    /**
     * Shows a confirmation dialog to the user. Upon pressing OK, the given member is deleted.
     */
    public void deleteMember(final Member member) {

        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {

            // The user has confirmed to delete the
            // member.
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {

                    // Delete the member in a background thread
                    AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

                        @Override
                        protected Void doInBackground(Void... params) {
                            Uri uri = Uri.withAppendedPath(MemberColumns.CONTENT_URI, String.valueOf(member.id));
                            ContentValues values = new ContentValues(1);
                            values.put(MemberColumns.DELETED, 1);
                            mContext.getContentResolver().update(uri, values, null, null);
                            return null;
                        }
                    };
                    task.execute();
                }
            }
        };

        // Let's ask him if he's sure.
        ScrumChatterDialog.showDialog(mContext, mContext.getString(R.string.action_delete_member),
                mContext.getString(R.string.dialog_message_delete_member_confirm, member.name), onClickListener);
    }


    /**
     * Returns an error if the user entered the name of another member in the given team. To prevent creating multiple members with the same name in the same
     * team.
     */
    private class MemberNameValidator implements InputValidator {

        private final int mTeamId;

        private MemberNameValidator(int teamId) {
            mTeamId = teamId;
        }

        @Override
        public String getError(CharSequence input) {
            // Query for a member with this name.
            Cursor existingMemberCountCursor = mContext.getContentResolver().query(MemberColumns.CONTENT_URI, new String[] { "count(*)" },
                    MemberColumns.NAME + "=? AND " + MemberColumns.TEAM_ID + "=?", new String[] { String.valueOf(input), String.valueOf(mTeamId) }, null);

            // Now Check if the team member exists.
            if (existingMemberCountCursor != null) {
                if (existingMemberCountCursor.moveToFirst()) {
                    int existingMemberCount = existingMemberCountCursor.getInt(0);
                    existingMemberCountCursor.close();
                    if (existingMemberCount > 0) return mContext.getString(R.string.error_member_exists, input);
                }
            }
            return null;
        }
    }
}
