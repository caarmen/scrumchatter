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

import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.team.Teams;
import ca.rmen.android.scrumchatter.team.TeamsObserver;

class TeamNavigationMenu {

    private final Teams mTeams;
    private final Menu mNavigationMenu;
    private final TeamsObserver mTeamsObserver;

    TeamNavigationMenu(FragmentActivity activity, Menu navigationMenu) {
        mTeams = new Teams(activity);
        mNavigationMenu = navigationMenu;
        mTeamsObserver = new TeamsObserver(activity, this::load);
        mTeamsObserver.register();
    }

    void destroy() {
        mTeamsObserver.destroy();
    }

    void load() {
        mTeams.getAllTeams()
                .subscribe(teamsData -> {
                            // Update the navigation view.
                            SubMenu teamsMenu = mNavigationMenu.findItem(R.id.teams_list).getSubMenu();
                            teamsMenu.clear();
                            for (Teams.Team team : teamsData.teams) {
                                MenuItem teamMenuItem = teamsMenu.add(R.id.teams_list_items, Menu.NONE, Menu.NONE, team.teamName);
                                if (team.teamName.equals(teamsData.currentTeam.teamName)) {
                                    teamMenuItem.setChecked(true);
                                }
                            }
                        }
                );
    }
}
