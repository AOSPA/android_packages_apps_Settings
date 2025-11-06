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

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.util.Log
import android.view.View
import androidx.preference.Preference
import com.android.settings.R
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.IllustrationPreference
import com.android.settingslib.widget.SettingsBasePreferenceFragment

class SatelliteLandingPageFragment : SettingsBasePreferenceFragment() {
    private var activeSubId: Int = SubscriptionManager.INVALID_SUBSCRIPTION_ID

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.layout.satellite_landing_page_pref, rootKey)
        activeSubId = SubscriptionManager.getActiveDataSubscriptionId()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "onViewCreated: activeSubId: $activeSubId")

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
     * The button should only be visible if NTN LTE is not supported.
     */
    private fun setUpTryADemoButton() {
        val demoButtonPreference = findPreference<Preference>(KEY_TRY_A_DEMO_BUTTON) ?: return
        // Don't show the button if LTE NTN is supported.
        val isVisible = !SatelliteUtils.isLteBasedNtnSupportedByDevice(requireContext())
        demoButtonPreference.isVisible = isVisible
        if (isVisible) {
            startNewActivityOnPreferenceClick(
                demoButtonPreference,
                Intent(ACTION_ESOS_DEMO)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            )
        }
    }

    /** Sets up the satellite apps content based on the NTN type. */
    private fun setUpSatelliteAppsContent() {
        // TODO(danielbanta): Implement setUpSatelliteAppsContent
    }

    /** Sets up the footer preference based on the NTN type. */
    private fun setUpFooterPreference() {
        val footerPreference: FooterPreference? = findPreference(KEY_FOOTER)
        val footerTextResId =
            if (shouldDisplayLteBasedLandingPage()) {
                R.string.landing_page_footer_text_lte
            } else {
                R.string.landing_page_footer_text_nbiot
            }
        footerPreference?.setTitle(footerTextResId)
        footerPreference?.setLearnMoreText(getString(R.string.satellite_more_info_text))
        footerPreference?.setLearnMoreAction {
            // TODO(434793872): This action does nothing for Skylo only NB-IoT devices. For Skylo
            // only devices, we should launch SettingsGatewayActivity to get to the Satellite SOS
            // Settings page. Same for "Settings" in the satellite apps.
            val intent =
                Intent(Settings.ACTION_SATELLITE_SETTING).apply {
                    putExtra(EXTRA_SHOW_FRAGMENT_AS_SUBSETTING, true)
                    putExtra(EXTRA_SUB_ID, activeSubId)
                }
            requireContext().startActivity(intent)
        }
    }

    /**
     * Sets up the click listener for a preference.
     *
     * This method is used to start a new activity when a preference is clicked.
     */
    private fun startNewActivityOnPreferenceClick(preference: Preference, intent: Intent) {
        preference.setOnPreferenceClickListener {
            try {
                requireContext().startActivity(intent)
                true
            } catch (e: ActivityNotFoundException) {
                Log.e(TAG, "Failed to start activity: " + intent, e)
                false
            }
        }
    }

    /**
     * Returns true if the LTE landing page should be displayed.
     *
     * The LTE landing page should be displayed if LTE-based NTN is supported by any of the active
     * subscription IDs. Otherwise, the NBIOT landing page should be displayed.
     */
    private fun shouldDisplayLteBasedLandingPage(): Boolean {
        val lteBasedNtnSupported = SatelliteUtils.isLteBasedNtnSupportedByDevice(requireContext())

        Log.i(
            TAG,
            "shouldDisplayLteBasedLandingPage: isLteBasedNtnSupportedByDevice=$lteBasedNtnSupported",
        )

        return lteBasedNtnSupported
    }

    companion object {
        private const val TAG = "SatelliteLandingPageFragment"
        private const val KEY_ILLUSTRATION = "illustration"
        private const val KEY_TRY_A_DEMO_BUTTON = "try_a_demo_button"
        private const val KEY_FOOTER = "footer"
        private const val ACTION_ESOS_DEMO = "com.google.android.apps.stargate.ACTION_ESOS_DEMO"
        private const val EXTRA_SHOW_FRAGMENT_AS_SUBSETTING =
            ":settings:show_fragment_as_subsetting"
        private const val EXTRA_SUB_ID = "sub_id"
    }
}
