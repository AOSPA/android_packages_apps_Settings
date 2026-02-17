/*
 * Copyright (C) 2026 The Android Open Source Project
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

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.android.settings.R
import com.android.settingslib.widget.BannerMessagePreference

/**
 * Controller for managing the display and configuration of warning banners on the Satellite Landing
 * Page.
 *
 * This class encapsulates the logic for determining which banners to show based on the current
 * [SatelliteBannerState] and configures the [BannerMessagePreference] views accordingly. It
 * enforces a prioritization scheme to ensure the most critical information is displayed first.
 */
class SatelliteBannerController(
    private val context: Context,
    private val primaryBanner: BannerMessagePreference?,
    private val secondaryBanner: BannerMessagePreference?,
) {

    /**
     * Updates the visibility and content of the banners based on the provided state.
     *
     * @param state The current state of satellite conditions affecting banner display.
     */
    fun update(state: SatelliteBannerState) {
        // Reset visibility
        primaryBanner?.isVisible = false
        secondaryBanner?.isVisible = false

        if (primaryBanner == null) {
            Log.w(TAG, "Primary banner view is null, skipping update.")
            return
        }

        // Prioritization Logic
        // The order of addition determines the priority (First added = Highest priority).
        // 1. Network Connected (Service unavailable)
        // 2. Location Disabled (Service limited/unavailable)
        // 3. Satellite Unavailable in Region
        // 4. Unprovisioned
        // 5. Not Entitled
        // 6. User Restricted
        // 7. Default Messaging App Issue
        // 8. Carrier Info (Satellite Enabled)
        val bannersToShow = mutableListOf<BannerMessagePreference.() -> Unit>()

        if (state.isNetworkConnected) {
            bannersToShow.add { setupNetworkConnectedBanner(this) }
        }
        if (!state.isLocationEnabled) {
            bannersToShow.add { setupUnknownLocationBanner(this) }
        }
        if (!state.isSatelliteAvailableInRegion) {
            bannersToShow.add { setupSatelliteUnavailableInRegionBanner(this) }
        }
        if (!state.isProvisioned) {
            bannersToShow.add { setupUnprovisionedDeviceBanner(this) }
        }
        if (!state.isEntitled) {
            bannersToShow.add { setupNotEntitledBanner(this) }
        }
        if (state.isUserRestricted) {
            bannersToShow.add { setupUserRestrictedBanner(this) }
        }
        if (!state.isDefaultMessagingApp) {
            bannersToShow.add { setupNotDefaultMessagingAppBanner(this) }
        }
        if (state.isSatelliteEnabledByCarrier) {
            bannersToShow.add { setupSatelliteEnabledByCarrierBanner(this) }
        }

        // Apply to Views (Max 2 banners)
        if (bannersToShow.isNotEmpty()) {
            primaryBanner.isVisible = true
            bannersToShow[0](primaryBanner)
        }
        if (bannersToShow.size > 1 && secondaryBanner != null) {
            secondaryBanner.isVisible = true
            bannersToShow[1](secondaryBanner)
        }
    }

    private fun setupNetworkConnectedBanner(banner: BannerMessagePreference) {
        setupBannerPreference(
            banner,
            titleRes = R.string.satellite_not_available_warning_title,
            summaryRes = R.string.satellite_network_connected_warning_summary,
            buttonTextRes = 0,
            intent = null,
        )
    }

    private fun setupUserRestrictedBanner(banner: BannerMessagePreference) {
        setupBannerPreference(
            banner,
            titleRes = R.string.satellite_not_available_warning_title,
            summaryRes = R.string.satellite_user_restricted_warning_summary,
            buttonTextRes = R.string.satellite_view_carrier_settings_button,
            intent = SatelliteUtils.getCarrierSettingsIntent(context),
        )
    }

    private fun setupUnknownLocationBanner(banner: BannerMessagePreference) {
        setupBannerPreference(
            banner,
            titleRes = R.string.satellite_not_available_warning_title,
            summaryRes = R.string.unknown_location_warning,
            buttonTextRes = R.string.unknown_location_warning_button,
            intent = SatelliteUtils.getLocationSourceSettingsIntent(),
        )
    }

    private fun setupSatelliteUnavailableInRegionBanner(banner: BannerMessagePreference) {
        setupBannerPreference(
            banner,
            titleRes = R.string.satellite_not_available_warning_title,
            summaryRes = R.string.satellite_unavailable_in_region_warning_summary,
            buttonTextRes = R.string.satellite_view_coverage_button,
            intent = SatelliteUtils.getSatelliteCoverageIntent(),
        )
    }

    private fun setupUnprovisionedDeviceBanner(banner: BannerMessagePreference) {
        setupBannerPreference(
            banner,
            titleRes = R.string.satellite_not_available_warning_title,
            summaryRes = R.string.unprovisioned_device_warning,
            buttonTextRes = 0,
            intent = null,
        )
    }

    private fun setupNotEntitledBanner(banner: BannerMessagePreference) {
        setupBannerPreference(
            banner,
            titleRes = R.string.satellite_not_available_warning_title,
            summaryRes = R.string.satellite_plan_warning_summary,
            buttonTextRes = 0,
            intent = null,
        )
    }

    private fun setupNotDefaultMessagingAppBanner(banner: BannerMessagePreference) {
        setupBannerPreference(
            banner,
            titleRes = R.string.satellite_not_available_warning_title,
            summaryRes = R.string.satellite_default_app_warning_summary,
            buttonTextRes = R.string.satellite_change_default_app_button,
            intent = SatelliteUtils.getDefaultSmsAppIntent(),
        )
    }

    private fun setupSatelliteEnabledByCarrierBanner(banner: BannerMessagePreference) {
        setupBannerPreference(
            banner,
            titleRes = 0,
            summaryRes = R.string.satellite_carrier_enabled_info_summary,
            buttonTextRes = R.string.satellite_view_carrier_settings_button,
            intent = SatelliteUtils.getCarrierSettingsIntent(context),
        )
    }

    private fun setupBannerPreference(
        banner: BannerMessagePreference?,
        @StringRes titleRes: Int,
        @StringRes summaryRes: Int,
        @StringRes buttonTextRes: Int,
        intent: Intent?,
        @DrawableRes iconRes: Int = R.drawable.ic_info_outline_24,
        attentionLevel: BannerMessagePreference.AttentionLevel =
            BannerMessagePreference.AttentionLevel.NORMAL,
    ) {
        banner?.apply {
            setIcon(iconRes)
            if (titleRes != 0) setTitle(titleRes) else title = null
            setSummary(summaryRes)
            setAttentionLevel(attentionLevel)

            // Reset buttons
            setNegativeButtonText(null)
            setNegativeButtonOnClickListener(null)

            if (buttonTextRes != 0) {
                val listener =
                    android.view.View.OnClickListener {
                        intent?.let {
                            try {
                                context.startActivity(it)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to start activity", e)
                            }
                        }
                    }

                setNegativeButtonText(buttonTextRes)
                setNegativeButtonOnClickListener(listener)
            }
        }
    }

    companion object {
        private const val TAG = "SatelliteBannerController"
    }
}
