/**
 * Copyright 2016 Carmen Alvarez

 * This file is part of Scrum Chatter.

 * Scrum Chatter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * Scrum Chatter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with Scrum Chatter. If not, see //www.gnu.org/licenses/>.
 */


package ca.rmen.android.scrumchatter.settings


import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.TaskStackBuilder
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.PreferenceFragmentCompat
import ca.rmen.android.scrumchatter.Constants

import ca.rmen.android.scrumchatter.R
import ca.rmen.android.scrumchatter.util.Log


class SettingsActivity : AppCompatActivity() {

    private val TAG = Constants.TAG + SettingsActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        Theme.checkTheme(this)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val fragment = GeneralPreferenceFragment()
        supportFragmentManager.beginTransaction().replace(android.R.id.content, fragment).commit()
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(mListener)
    }

    private val mListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        val context:Context = applicationContext
        if (Theme.PREF_THEME == key) {
            // When the theme changes, restart the app
            val intent = Intent(context, SettingsActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val stackBuilder = TaskStackBuilder.create(context)
            stackBuilder.addNextIntentWithParentStack(intent)
            stackBuilder.startActivities()
        }
    }

    class GeneralPreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(bundle: Bundle?, s: String?) {
            addPreferencesFromResource(R.xml.pref_general)
        }
    }
}
