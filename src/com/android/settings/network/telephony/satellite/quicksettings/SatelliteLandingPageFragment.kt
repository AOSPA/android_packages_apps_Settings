/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.network.telephony.satellite.quicksettings

import android.os.Bundle
import android.util.Log
import android.view.View
import com.android.settings.R
import com.android.settingslib.widget.IllustrationPreference
import com.android.settingslib.widget.SettingsBasePreferenceFragment

class SatelliteLandingPageFragment : SettingsBasePreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.layout.satellite_landing_page_pref, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.i(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)

        addIllustrationPreference()
        setUpTryADemoButton()
        setUpSatelliteAppsContent()
        setUpFooterPreference()
    }

    /** Adds the satellite landing page illustration. */
    private fun addIllustrationPreference() {
        val illustrationPreference = findPreference<IllustrationPreference>(KEY_ILLUSTRATION)
        illustrationPreference?.setImageDrawable(
            context?.getDrawable(R.drawable.ill_satellite_landing)
        )
    }

    /**
     * Sets up the "Try a demo" button.
     *
     * <p>The button should only be visible if NTN LTE is not supported.
     */
    private fun setUpTryADemoButton() {
        // TODO(b/434793872): Implement setUpTryADemoButton
    }

    /** Sets up the satellite apps content based on the NTN type. */
    private fun setUpSatelliteAppsContent() {
        // TODO(danielbanta): Implement setUpSatelliteAppsContent
    }

    /** Sets up the footer preference based on the NTN type. */
    private fun setUpFooterPreference() {
        // TODO(b/434793872): Implement setUpFooterPreference
    }

    companion object {
        private const val TAG = "SatelliteLandingPageFragment"
        private const val KEY_ILLUSTRATION = "illustration"
    }
}
