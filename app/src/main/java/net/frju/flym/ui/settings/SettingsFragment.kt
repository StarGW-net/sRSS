/*
 * Copyright (c) 2012-2018 Frederic Julian
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package net.frju.flym.ui.settings

import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.preference.PreferenceFragment
import android.util.Log
import net.fred.feedex.R
import net.frju.flym.App
import net.frju.flym.data.utils.PrefConstants
import net.frju.flym.service.AutoRefreshJobService
import net.frju.flym.ui.main.MainActivity
import org.jetbrains.anko.startActivity


class SettingsFragment : PreferenceFragment() {

    var listener = OnSharedPreferenceChangeListener { prefs, key ->

        App.myLog("Settings changed: " + key)
        if ((key == PrefConstants.REFRESH_ENABLED) || (key == PrefConstants.REFRESH_INTERVAL))
        {
            AutoRefreshJobService.initAutoRefresh(activity)
        }

        if ((key == PrefConstants.THEME) || (key == PrefConstants.DISPLAY_THUMBS) || (key == PrefConstants.FONT_SIZE_HEADING ) ) {
            activity.finishAffinity()
            startActivity<MainActivity>()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        App.myLog("Settings loaded!")
        addPreferencesFromResource(R.xml.settings)

        // need to add an onpref change listener here...

        /*
        findPreference(PrefConstants.REFRESH_ENABLED)?.set = listener;

        findPreference(PrefConstants.REFRESH_INTERVAL)?.setOnPreferenceChangeListener { preference, any ->
            AutoRefreshJobService.initAutoRefresh(activity)
            App.myLog("Settings changed!")
            true
        }



        findPreference(PrefConstants.THEME)?.setOnPreferenceChangeListener { preference, any ->
            activity.finishAffinity()
            startActivity<MainActivity>()
            true
        }
        */

    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun onPause() {
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        super.onPause()
    }
}