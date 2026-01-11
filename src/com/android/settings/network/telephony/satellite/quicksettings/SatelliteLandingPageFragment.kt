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
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.util.Log
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.spa.preference.ComposePreference
import com.android.settingslib.spaprivileged.template.app.AppListItem
import com.android.settingslib.spaprivileged.template.app.AppListItemModel
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.IllustrationPreference
import com.android.settingslib.widget.SettingsBasePreferenceFragment

/**
 * A fragment that displays the satellite landing page.
 *
 * This fragment is displayed when the user taps on the satellite quick settings tile. It displays a
 * list of satellite-optimized apps and a footer with more information about satellite services. The
 * content of the landing page is determined by the NTN type.
 */
class SatelliteLandingPageFragment : SettingsBasePreferenceFragment {

    private lateinit var packageManager: PackageManager

    private var appsRepository: SatelliteAppsRepository? = null

    @VisibleForTesting
    val viewModel: SatelliteLandingPageViewModel by viewModels {
        SatelliteLandingPageViewModelFactory(
            requireContext(),
            appsRepository ?: SatelliteAppsRepository(requireContext()),
            packageManager,
        )
    }

    /**
     * Test-only constructor to inject dependencies for testing.
     *
     * @param packageManager The [PackageManager] to use for testing.
     * @param appsRepository The [SatelliteAppsRepository] to use for testing.
     */
    @VisibleForTesting
    constructor(packageManager: PackageManager, appsRepository: SatelliteAppsRepository) : super() {
        this.packageManager = packageManager
        this.appsRepository = appsRepository
    }

    constructor() : super()

    private var activeSubId: Int = SubscriptionManager.INVALID_SUBSCRIPTION_ID

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        if (!::packageManager.isInitialized) {
            packageManager = requireContext().packageManager
        }
        setPreferencesFromResource(R.layout.satellite_landing_page_pref, rootKey)
        activeSubId = SubscriptionManager.getActiveDataSubscriptionId()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: activeSubId: $activeSubId")

        addIllustrationPreference()
        updateLandingPageContent()
    }

    override fun onResume() {
        super.onResume()
        updateLandingPageContent()
    }

    private fun updateLandingPageContent() {
        viewModel.loadSatelliteAppItems()
        setUpTryADemoButton()
        setUpSatelliteAppsContent()
        setUpFooterPreference()
    }

    /**
     * Adds the satellite landing page PUI illustration.
     *
     * The illustration is not shown if LTE NTN is supported.
     */
    private fun addIllustrationPreference() {
        findPreference<IllustrationPreference>(KEY_ILLUSTRATION)?.apply {
            isVisible = !shouldDisplayLteBasedLandingPage()
            if (isVisible) {
                setImageDrawable(context?.getDrawable(R.drawable.ill_satellite_landing))
            }
        }
    }

    /**
     * Sets up the "Try a demo" button.
     *
     * The button is not shown if LTE NTN is supported.
     */
    private fun setUpTryADemoButton() {
        val demoButtonPreference = findPreference<Preference>(KEY_TRY_A_DEMO_BUTTON) ?: return
        val isVisible = !shouldDisplayLteBasedLandingPage()
        demoButtonPreference.isVisible = isVisible
        if (isVisible) {
            demoButtonPreference.setOnPreferenceClickListener {
                startActivitySafely(
                    Intent(ACTION_ESOS_DEMO)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
                true
            }
        }
    }

    /** Populates the list of satellite-enabled apps by observing the ViewModel. */
    private fun setUpSatelliteAppsContent() {
        val composePreference = findPreference<ComposePreference>(KEY_SATELLITE_APPS_LIST)
        composePreference?.setContent {
            val satelliteAppItems by viewModel.satelliteAppItems.collectAsState()
            composePreference.isVisible = satelliteAppItems.isNotEmpty()
            Column {
                satelliteAppItems.forEach { item ->
                    val appListItemModel =
                        AppListItemModel(
                            record = item,
                            label = item.getAppLabel(packageManager),
                            summary = { "" },
                        )
                    appListItemModel.AppListItem(
                        onClick = { item.intent?.let { startActivitySafely(it) } }
                    )
                }
            }
        }
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
            startActivitySafely(intent)
        }
    }

    private fun startActivitySafely(intent: Intent) {
        try {
            requireContext().startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Failed to start activity: $intent", e)
        }
    }

    /**
     * The LTE landing page should be displayed if LTE-based NTN is supported by any of the active
     * subscription IDs. Otherwise, the NBIOT landing page should be displayed.
     */
    private fun shouldDisplayLteBasedLandingPage(): Boolean {
        return SatelliteUtils.isLteBasedNtnSupportedByDevice(requireContext())
    }

    companion object {
        private const val TAG = "SatelliteLandingPageFragment"
        private const val KEY_ILLUSTRATION = "illustration"
        private const val KEY_TRY_A_DEMO_BUTTON = "try_a_demo_button"
        private const val KEY_FOOTER = "footer"
        private const val KEY_SATELLITE_APPS_LIST = "satellite_apps_list"
        private const val ACTION_ESOS_DEMO = "com.google.android.apps.stargate.ACTION_ESOS_DEMO"
        private const val EXTRA_SHOW_FRAGMENT_AS_SUBSETTING =
            ":settings:show_fragment_as_subsetting"
        private const val EXTRA_SUB_ID = "sub_id"
    }
}
