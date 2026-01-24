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

import android.content.Context
import com.android.settings.R
import com.android.settingslib.widget.BannerMessagePreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowSubscriptionManager

@RunWith(RobolectricTestRunner::class)
class SatelliteBannerControllerTest {

    private lateinit var context: Context
    private lateinit var primaryBanner: BannerMessagePreference
    private lateinit var secondaryBanner: BannerMessagePreference
    private lateinit var controller: SatelliteBannerController

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        // Ensure the context has the resources we need (theme) to avoid inflation errors if any
        context.setTheme(R.style.Theme_Settings)
        ShadowSubscriptionManager.setActiveDataSubscriptionId(1)
        primaryBanner = BannerMessagePreference(context)
        secondaryBanner = BannerMessagePreference(context)
        controller = SatelliteBannerController(context, primaryBanner, secondaryBanner)
    }

    @Test
    fun update_withNetworkConnected_showsNetworkBanner() {
        val state = SatelliteBannerState(isNetworkConnected = true)
        controller.update(state)

        assertThat(primaryBanner.isVisible).isTrue()
        assertThat(primaryBanner.summary)
            .isEqualTo(context.getString(R.string.satellite_network_connected_warning_summary))
        assertThat(secondaryBanner.isVisible).isFalse()
    }

    @Test
    fun update_withMultipleIssues_prioritizesCorrectly() {
        // State with multiple issues:
        // 1. Network Connected (Priority 1)
        // 2. Location Disabled (Priority 2)
        // 3. Not Entitled (Priority 5)
        val state =
            SatelliteBannerState(
                isNetworkConnected = true,
                isLocationEnabled = false,
                isEntitled = false,
            )
        controller.update(state)

        // Primary should show Network Connected (Priority 1)
        assertThat(primaryBanner.isVisible).isTrue()
        assertThat(primaryBanner.summary)
            .isEqualTo(context.getString(R.string.satellite_network_connected_warning_summary))

        // Secondary should show Location Disabled (Priority 2)
        assertThat(secondaryBanner.isVisible).isTrue()
        assertThat(secondaryBanner.summary)
            .isEqualTo(context.getString(R.string.unknown_location_warning))
    }

    @Test
    fun update_withOnlyPrimaryBanner_showsHighestPriority() {
        // Controller with only primary banner
        controller = SatelliteBannerController(context, primaryBanner, null)

        val state =
            SatelliteBannerState(
                isNetworkConnected = false,
                isSatelliteAvailableInRegion = false, // Priority 3
            )
        controller.update(state)

        assertThat(primaryBanner.isVisible).isTrue()
        assertThat(primaryBanner.summary)
            .isEqualTo(context.getString(R.string.satellite_unavailable_in_region_warning_summary))
    }

    @Test
    fun update_whenAllGood_hidesBanners() {
        val state =
            SatelliteBannerState(
                isNetworkConnected = false,
                isLocationEnabled = true,
                isSatelliteAvailableInRegion = true,
                isProvisioned = true,
                isEntitled = true,
                isDefaultMessagingApp = true,
                isSatelliteEnabledByCarrier = false,
            )
        controller.update(state)

        assertThat(primaryBanner.isVisible).isFalse()
        assertThat(secondaryBanner.isVisible).isFalse()
    }

    @Test
    fun update_carrierEnabled_showsInfoBanner() {
        val state = SatelliteBannerState(isSatelliteEnabledByCarrier = true)
        controller.update(state)

        assertThat(primaryBanner.isVisible).isTrue()
        assertThat(primaryBanner.summary)
            .isEqualTo(context.getString(R.string.satellite_carrier_enabled_info_summary))
        // Should verify button intent too if possible, but basic content check is good start
    }
}
