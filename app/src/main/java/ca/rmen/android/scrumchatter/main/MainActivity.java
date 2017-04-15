/*
 * Copyright 2016-2017 Carmen Alvarez
 * <p/>
 * This file is part of Scrum Chatter.
 * <p/>
 * Scrum Chatter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * Scrum Chatter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with Scrum Chatter. If not, see <http://www.gnu.org/licenses/>.
 */
package ca.rmen.android.scrumchatter.main;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Arrays;

import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.about.AboutActivity;
import ca.rmen.android.scrumchatter.chart.ChartsActivity;
import ca.rmen.android.scrumchatter.databinding.ActivityMainBinding;
import ca.rmen.android.scrumchatter.dialog.ChoiceDialogFragment.DialogItemListener;
import ca.rmen.android.scrumchatter.dialog.ConfirmDialogFragment.DialogButtonListener;
import ca.rmen.android.scrumchatter.dialog.DialogFragmentFactory;
import ca.rmen.android.scrumchatter.dialog.InputDialogFragment.DialogInputListener;
import ca.rmen.android.scrumchatter.dialog.ProgressDialogFragment;
import ca.rmen.android.scrumchatter.export.DBExport;
import ca.rmen.android.scrumchatter.export.FileExport;
import ca.rmen.android.scrumchatter.export.MeetingsExport;
import ca.rmen.android.scrumchatter.meeting.Meetings;
import ca.rmen.android.scrumchatter.meeting.detail.MeetingFragment;
import ca.rmen.android.scrumchatter.meeting.list.MeetingsListFragment;
import ca.rmen.android.scrumchatter.member.list.Members;
import ca.rmen.android.scrumchatter.provider.DBImport;
import ca.rmen.android.scrumchatter.provider.MeetingColumns;
import ca.rmen.android.scrumchatter.settings.SettingsActivity;
import ca.rmen.android.scrumchatter.settings.Theme;
import ca.rmen.android.scrumchatter.team.Teams;
import ca.rmen.android.scrumchatter.team.Teams.Team;
import ca.rmen.android.scrumchatter.team.TeamsObserver;
import ca.rmen.android.scrumchatter.util.Log;


/**
 * The main screen of the app. Part of this code was generated by the ADT
 * plugin.
 */
public class MainActivity extends AppCompatActivity implements DialogButtonListener, DialogItemListener,
        DialogInputListener {

    private static final String TAG = Constants.TAG + "/" + MainActivity.class.getSimpleName();
    private static final String EXTRA_IMPORT_URI = "import_uri";
    private static final String EXTRA_IMPORT_RESULT = "import_result";
    private static final String EXTRA_EXPORT_RESULT = "export_result";
    private static final String ACTION_IMPORT_COMPLETE = "action_import_complete";
    private static final String ACTION_EXPORT_COMPLETE = "action_export_complete";
    private static final int ACTIVITY_REQUEST_CODE_IMPORT = 1;
    private static final String PROGRESS_DIALOG_FRAGMENT_TAG = "progress_dialog_fragment_tag";

    /**
     * UI elements for the side menu (left drawer).
     */
    private ActionBarDrawerToggle mDrawerToggle;

    private final Teams mTeams = new Teams(this);
    private final Meetings mMeetings = new Meetings(this);
    private final Members mMembers = new Members(this);
    private Team mTeam = null;
    private int mTeamCount = 0;
    private ActivityMainBinding mBinding;
    private TeamNavigationMenu mTeamNavigationMenu;
    private TeamsObserver mTeamsObserver;
    private MainPagerAdapter mMainPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        Theme.checkTheme(this);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        // Use strict mode for monkey tests.  We can't enable strict mode for normal use
        // because, when sharing (exporting), the mail app may read the attachment in
        // the main thread.
        if (ActivityManager.isUserAMonkey())
            StrictMode.setThreadPolicy(new ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().penaltyLog().penaltyDeath().build());

        // Set up the action bar.
        setSupportActionBar(mBinding.toolbarTabs.toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        assert supportActionBar != null;
        supportActionBar.setDisplayHomeAsUpEnabled(true);
        supportActionBar.setHomeButtonEnabled(true);


        mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
                mBinding.drawerLayout, /* DrawerLayout object */
                R.string.drawer_open, /* "open drawer" description */
                R.string.drawer_close /* "close drawer" description */);

        // Explanation of setDrawerIndicatorEnabled and setHomeAsUpIndicator:
        // We want to only have a hamburger icon, always, without any animation.

        // If we don't call either of these methods, we use the default indicator,
        // which is the hamburger icon transitioning to a left-arrow icon, as the drawer is opened.

        // If we only call setDrawerIndicatorUpEnabled, we'll have the left arrow icon always.

        // If we only call setHomeAsUpIndicator (with a hamburger icon), we'll have a hamburger icon
        // but with a bug: If you open the drawer, rotate, and close it, you'll have the left arrow
        // again.

        // With the combination of both setDrawerIndicatorEnabled and setHomeAsUpIndicator, we
        // have a hamburger icon always.
        mDrawerToggle.setDrawerIndicatorEnabled(false);
        mDrawerToggle.setHomeAsUpIndicator(new DrawerArrowDrawable(this));

        mMainPagerAdapter = new MainPagerAdapter(this, getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mBinding.pager.setAdapter(mMainPagerAdapter);
        mBinding.toolbarTabs.tabs.setupWithViewPager(mBinding.pager);

        // If our activity was opened by choosing a file from a mail attachment, file browser, or other program,
        // import the database from this file.
        Intent intent = getIntent();
        if (intent != null) {
            if (Intent.ACTION_VIEW.equals(intent.getAction())) importDB(intent.getData());
        }

        // Register various observers.
        mTeamsObserver = new TeamsObserver(this, mOnTeamsChangedListener);
        mOnTeamsChangedListener.onTeamsChanged();
        mTeamsObserver.register();
        mTeamNavigationMenu = new TeamNavigationMenu(this, mBinding.leftDrawer.getMenu());
        mTeamNavigationMenu.load();
        mBinding.leftDrawer.setNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        IntentFilter filter = new IntentFilter(ACTION_IMPORT_COMPLETE);
        filter.addAction(ACTION_EXPORT_COMPLETE);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mBroadcastReceiver, filter);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onResumeFragments() {
        Log.v(TAG, "onResumeFragments: intent = " + getIntent());
        super.onResumeFragments();
        // The user chose a DB file to import.  We saved the URI in onActivityForResult.  Now
        // we show a confirmation dialog fragment to actually import the file.
        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            Uri importUri = getIntent().getExtras().getParcelable(EXTRA_IMPORT_URI);
            if (importUri != null) {
                // Remove the uri extra, otherwise the confirmation dialog will keep popping up when
                // we rotate the device.
                getIntent().removeExtra(EXTRA_IMPORT_URI);
                importDB(importUri);
            }
        }
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy");
        mTeamsObserver.destroy();
        mTeamNavigationMenu.destroy();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.v(TAG, "onPrepareOptionsMenu " + menu);
        // Only enable the "delete team" menu item if we have at least two teams.
        MenuItem deleteItem = menu.findItem(R.id.action_team_delete);
        deleteItem.setEnabled(mTeamCount > 1);
        // Add the current team name to the delete and rename menu items
        if (mTeam != null) {
            deleteItem.setTitle(getString(R.string.action_team_delete_name, mTeam.teamName));
            MenuItem renameItem = menu.findItem(R.id.action_team_rename);
            renameItem.setTitle(getString(R.string.action_team_rename_name, mTeam.teamName));
        }
        // Only show the settings in v14+.  Currently the only setting
        // we have is for the day/night theme switch, which only works on
        // v14+.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            MenuItem settingsItem = menu.findItem(R.id.action_settings);
            settingsItem.setVisible(false);
        }

        // Only show the menu items for sharing a meeting and stats for a meeting, if the selected meeting is finished.
        MeetingFragment meetingFragment = MeetingFragment.lookupMeetingFragment(getSupportFragmentManager());
        boolean meetingIsFinished = meetingFragment != null && meetingFragment.getState() == MeetingColumns.State.FINISHED;
        MenuItem menuItem = menu.findItem(R.id.action_share_meeting);
        if (menuItem != null) menuItem.setVisible(meetingIsFinished);
        menuItem = menu.findItem(R.id.action_charts_meeting);
        if (menuItem != null) menuItem.setVisible(meetingIsFinished);

        // Don't show the global share/stats menu items unless we have at least one meeting
        MeetingsListFragment meetingsListFragment = (MeetingsListFragment) mMainPagerAdapter.instantiateItem(mBinding.pager, 0);
        boolean hasMeetings = meetingsListFragment != null && meetingsListFragment.hasMeetings();
        menu.findItem(R.id.action_share).setVisible(hasMeetings);
        menu.findItem(R.id.action_charts).setVisible(hasMeetings);
        menuItem = menu.findItem(R.id.action_share_submenu);
        if (menuItem != null) menuItem.setVisible(hasMeetings);
        menuItem = menu.findItem(R.id.action_charts_submenu);
        if (menuItem != null) menuItem.setVisible(hasMeetings);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mBinding.drawerLayout.isDrawerVisible(GravityCompat.START)) {
                    mBinding.drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    mBinding.drawerLayout.openDrawer(GravityCompat.START);
                }
                return true;
            case R.id.action_team_rename:
                mTeams.promptRenameTeam(mTeam);
                return true;
            case R.id.action_team_delete:
                mTeams.confirmDeleteTeam(mTeam);
                return true;
            case R.id.action_import:
                startFileChooser();
                return true;
            case R.id.action_charts:
                startActivity(new Intent(this, ChartsActivity.class));
                return true;
            case R.id.action_share:
                // Build a chooser dialog for the file format.
                DialogFragmentFactory.showChoiceDialog(this, getString(R.string.export_choice_title), getResources().getStringArray(R.array.export_choices),
                        -1, R.id.action_share);
                return true;
            case R.id.action_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.action_about:
                Intent aboutIntent = new Intent(this, AboutActivity.class);
                startActivity(aboutIntent);
                return true;
        }
        super.onOptionsItemSelected(item);
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.v(TAG, "onActivityResult: requestCode = " + requestCode + ", resultCode = " + resultCode + ", intent = " + intent);
        // The user chose a DB file to import.
        if (requestCode == ACTIVITY_REQUEST_CODE_IMPORT && resultCode == Activity.RESULT_OK) {
            if (intent == null || intent.getData() == null) {
                Snackbar.make(mBinding.getRoot(), R.string.import_result_no_file, Snackbar.LENGTH_SHORT).show();
                return;
            }
            final String filePath = intent.getData().getPath();
            if (TextUtils.isEmpty(filePath)) {
                Snackbar.make(mBinding.getRoot(), R.string.import_result_no_file, Snackbar.LENGTH_SHORT).show();
                return;
            }
            // Save the uri of the file.  We will import it in onResumeFragments.
            getIntent().putExtra(EXTRA_IMPORT_URI, intent.getData());
        } else {
            super.onActivityResult(requestCode, resultCode, intent);
        }
    }

    @Override
    public void onBackPressed() {
        Log.v(TAG, "onBackPressed");
        // Prevent the monkey from exiting the app, to maximize the time the monkey spends testing the app.
        if (ActivityManager.isUserAMonkey()) {
            Log.v(TAG, "Sorry, monkeys must stay in the cage");
            return;
        }
        super.onBackPressed();
    }

    /**
     * Import the given database file. This will replace the current database.
     */
    private void importDB(final Uri uri) {
        Bundle extras = new Bundle(1);
        extras.putParcelable(EXTRA_IMPORT_URI, uri);
        DialogFragmentFactory.showConfirmDialog(this, getString(R.string.import_confirm_title),
                getString(R.string.import_confirm_message, uri.getEncodedPath()), R.id.action_import, extras);
    }

    /**
     * Share a file using an intent chooser.
     *
     * @param fileExport The object responsible for creating the file to share.
     */
    private void shareFile(final FileExport fileExport) {
        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPreExecute() {
                DialogFragmentFactory.showProgressDialog(MainActivity.this, getString(R.string.progress_dialog_message), PROGRESS_DIALOG_FRAGMENT_TAG);
            }

            @Override
            protected Void doInBackground(Void... params) {
                boolean result = fileExport.export();
                Intent intent = new Intent(ACTION_EXPORT_COMPLETE);
                intent.putExtra(EXTRA_EXPORT_RESULT, result);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                Log.v(TAG, "broadcast " + intent);
                return null;
            }

        };
        asyncTask.execute();
    }

    /**
     * Called when the current team was changed. Update our cache of the current team and update the ui (menu items, action bar title).
     */
    private final TeamsObserver.OnTeamsChangedListener mOnTeamsChangedListener
            = new TeamsObserver.OnTeamsChangedListener() {
        @Override
        public void onTeamsChanged() {

            AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... arg0) {
                    mTeam = mTeams.getCurrentTeam();
                    mTeamCount = mTeams.getTeamCount();
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    ActionBar supportActionBar = getSupportActionBar();
                    if (supportActionBar != null) {
                        // If the user has renamed the default team or added other teams, show the current team name in the title
                        if (mTeamCount > 1 || (mTeam != null && !mTeam.teamName.equals(Constants.DEFAULT_TEAM_NAME))) {
                            supportActionBar.setTitle(mTeam.teamName);
                        }
                        // otherwise the user doesn't care about team management: just show the app title.
                        else {
                            supportActionBar.setTitle(R.string.app_name);
                        }
                    }
                    supportInvalidateOptionsMenu();
                }
            };
            task.execute();
        }
    };

    /**
     * The user tapped on the OK button of a confirmation dialog. Execute the action requested by the user.
     *
     * @param actionId the action id which was provided to the {@link DialogFragmentFactory} when creating the dialog.
     * @param extras   any extras which were provided to the {@link DialogFragmentFactory} when creating the dialog.
     * @see ca.rmen.android.scrumchatter.dialog.ConfirmDialogFragment.DialogButtonListener#onOkClicked(int, android.os.Bundle)
     */
    @Override
    public void onOkClicked(int actionId, Bundle extras) {
        Log.v(TAG, "onClicked: actionId = " + actionId + ", extras = " + extras);
        if (actionId == R.id.action_delete_meeting) {
            long meetingId = extras.getLong(Meetings.EXTRA_MEETING_ID);
            mMeetings.delete(meetingId);
        } else if (actionId == R.id.btn_stop_meeting) {
            MeetingFragment meetingFragment = MeetingFragment.lookupMeetingFragment(getSupportFragmentManager());
            if (meetingFragment != null) meetingFragment.stopMeeting();
        } else if (actionId == R.id.action_delete_member) {
            long memberId = extras.getLong(Members.EXTRA_MEMBER_ID);
            mMembers.deleteMember(memberId);
        } else if (actionId == R.id.action_team_delete) {
            Uri teamUri = extras.getParcelable(Teams.EXTRA_TEAM_URI);
            mTeams.deleteTeam(teamUri);
        } else if (actionId == R.id.action_import) {
            final Uri uri = extras.getParcelable(EXTRA_IMPORT_URI);
            AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

                @Override
                protected void onPreExecute() {
                    DialogFragmentFactory.showProgressDialog(MainActivity.this, getString(R.string.progress_dialog_message), PROGRESS_DIALOG_FRAGMENT_TAG);
                }

                @Override
                protected Void doInBackground(Void... params) {
                    boolean result = false;
                    try {
                        Log.v(TAG, "Importing db from " + uri);
                        DBImport.importDB(MainActivity.this, uri);
                        result = true;
                    } catch (Exception e) {
                        Log.e(TAG, "Error importing db: " + e.getMessage(), e);
                    }
                    // Notify ourselves with a broadcast.  If the user rotated the device, this activity
                    // won't be visible any more. The new activity will receive the broadcast and update
                    // the UI.
                    Intent intent = new Intent(ACTION_IMPORT_COMPLETE).putExtra(EXTRA_IMPORT_RESULT, result);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                    return null;
                }
            };
            task.execute();
        }
    }

    /**
     * The user selected an item in a choice dialog. Perform the action for the selected item.
     *
     * @param actionId the action id which was provided to the {@link DialogFragmentFactory} when creating the dialog.
     * @param choices  the localized labels of the items.
     * @param which    the index of the item which was selected
     * @see ca.rmen.android.scrumchatter.dialog.ChoiceDialogFragment.DialogItemListener#onItemSelected(int, java.lang.CharSequence[], int)
     */
    @Override
    public void onItemSelected(int actionId, CharSequence[] choices, int which) {
        Log.v(TAG, "onItemSelected: actionId = " + actionId + ", choices = " + Arrays.toString(choices) + ", which = " + which);
        if (actionId == R.id.action_share) {
            FileExport fileExport = null;
            if (getString(R.string.export_format_excel).equals(choices[which]))
                fileExport = new MeetingsExport(MainActivity.this);
            else if (getString(R.string.export_format_db).equals(choices[which]))
                fileExport = new DBExport(MainActivity.this);
            shareFile(fileExport);
        }
    }

    /**
     * The user tapped on the OK button on a dialog in which s/he entered text.
     *
     * @param actionId the action id which was provided to the {@link DialogFragmentFactory} when creating the dialog.
     * @param input    the text entered by the user.
     * @param extras   any extras which were provided to the {@link DialogFragmentFactory} when creating the dialog.
     * @see ca.rmen.android.scrumchatter.dialog.InputDialogFragment.DialogInputListener#onInputEntered(int, java.lang.String, android.os.Bundle)
     */
    @Override
    public void onInputEntered(int actionId, String input, Bundle extras) {
        Log.v(TAG, "onInputEntered: actionId = " + actionId + ", input = " + input + ", extras = " + extras);
        if (actionId == R.id.fab_new_member) {
            long teamId = extras.getLong(Teams.EXTRA_TEAM_ID);
            mMembers.createMember(teamId, input);
        } else if (actionId == R.id.action_rename_member) {
            long memberId = extras.getLong(Members.EXTRA_MEMBER_ID);
            mMembers.renameMember(memberId, input);
        } else if (actionId == R.id.action_team) {
            mTeams.createTeam(input);
        } else if (actionId == R.id.action_team_rename) {
            Uri teamUri = extras.getParcelable(Teams.EXTRA_TEAM_URI);
            mTeams.renameTeam(teamUri, input);
        }
    }

    private void startFileChooser() {
        final Intent importIntent;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) importIntent = new Intent(Intent.ACTION_GET_CONTENT);
        else importIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        importIntent.setType("*/*");
        importIntent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(importIntent, getResources().getText(R.string.action_import)), ACTIVITY_REQUEST_CODE_IMPORT);
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "onReceive: intent = " + intent);
            // The DB import has completed.  Dismiss the progress dialog and show a toast.
            if (ACTION_IMPORT_COMPLETE.equals(intent.getAction())) {
                Boolean result = intent.getExtras().getBoolean(EXTRA_IMPORT_RESULT);
                ProgressDialogFragment dialogFragment = (ProgressDialogFragment) getSupportFragmentManager().findFragmentByTag(PROGRESS_DIALOG_FRAGMENT_TAG);
                if (dialogFragment != null) dialogFragment.dismiss();
                Snackbar.make(mBinding.getRoot(), result ? R.string.import_result_success : R.string.import_result_failed, Snackbar.LENGTH_SHORT).show();
            }
            // The file export has completed.  Dismiss the progress dialog and, if there was an error, show a toast.
            else if (ACTION_EXPORT_COMPLETE.equals(intent.getAction())) {
                Boolean result = intent.getExtras().getBoolean(EXTRA_EXPORT_RESULT);
                ProgressDialogFragment dialogFragment = (ProgressDialogFragment) getSupportFragmentManager().findFragmentByTag(PROGRESS_DIALOG_FRAGMENT_TAG);
                if (dialogFragment != null) dialogFragment.dismiss();
                if (!result)
                    Snackbar.make(mBinding.getRoot(), R.string.export_error, Snackbar.LENGTH_LONG).show();

            }
        }
    };

    /**
     * Select a team the user picked from the left drawer.
     */
    private final NavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new NavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Log.v(TAG, "onNavigationItemSelected: " + item.getTitle());
            // When running monkey tests, we should load a DB with enough members and some meetings,
            // before running the tests.  If the monkey tries to switch teams, and creates a new team,
            // it will require many random clicks before he creates a member, and therefore many random
            // clicks before he is able to create meetings.  So, we force the monkey to stay within the existing team.
            if (ActivityManager.isUserAMonkey()) {
                Log.v(TAG, "Sorry, monkeys are not allowed to switch teams");
                return false;
            }

            if (item.getGroupId() == R.id.teams_list_items) {
                CharSequence selectedTeamName = item.getTitle();
                mTeams.switchTeam(selectedTeamName);
            } else if (item.getItemId() == R.id.action_new_team) {
                mTeams.promptCreateTeam();
            }
            mBinding.drawerLayout.closeDrawers();
            return false;
        }
    };

}
