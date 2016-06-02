/**
 * Copyright 2016 Carmen Alvarez
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

import android.os.AsyncTask;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.team.TeamObserver;
import ca.rmen.android.scrumchatter.team.Teams;
import ca.rmen.android.scrumchatter.util.Log;


class TeamNavigationView {

    static final int MENU_ID_TEAM = 1000;

    private static final String TAG = Constants.TAG + "/" + TeamNavigationView.class.getSimpleName();

    private final Teams mTeams;
    private final NavigationView mNavigationView;
    private final TeamObserver mTeamObserver;

    TeamNavigationView(FragmentActivity activity, NavigationView navigationView) {
        mTeams = new Teams(activity);
        mNavigationView = navigationView;
        mTeamObserver = new TeamObserver(activity, new TeamObserver.OnTeamsChangedListener() {
            @Override
            public void onTeamsChanged() {
                load();
            }
        });
        mTeamObserver.register();
    }

    void destroy() {
        mTeamObserver.destroy();
    }

    void load() {

        new AsyncTask<Void, Void, Teams.TeamsData>() {

            /**
             * Query the teams table, and return a list of all teams, and the current team.
             */
            @Override
            protected Teams.TeamsData doInBackground(Void... params) {
                Log.v(TAG, "doInBackground");
                return mTeams.getAllTeams();
            }

            /**
             * Update the navigation view.
             */
            @Override
            protected void onPostExecute(Teams.TeamsData teamsData) {
                Menu menu = mNavigationView.getMenu();
                SubMenu teamsMenu = menu.findItem(R.id.teams_list).getSubMenu();
                teamsMenu.clear();
                for (Teams.Team team : teamsData.teams) {
                    MenuItem teamMenuItem = teamsMenu.add(Menu.NONE, MENU_ID_TEAM, Menu.NONE, team.teamName);
                    if (team.teamName.equals(teamsData.currentTeam.teamName)) {
                        teamMenuItem.setChecked(true);
                    }
                }
            }
        }.execute();
    }

}
