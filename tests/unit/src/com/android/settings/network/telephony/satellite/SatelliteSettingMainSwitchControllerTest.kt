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

package com.android.settings.network.telephony.satellite

import android.content.Context
import android.os.PersistableBundle
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.telephony.CarrierConfigManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.core.BasePreferenceController
import com.android.settings.flags.Flags.FLAG_ENABLE_SATELLITE_TOGGLE
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

@RunWith(AndroidJUnit4::class)
class SatelliteSettingMainSwitchControllerTest {

    @get:Rule
    val mockitoRule: MockitoRule = MockitoJUnit.rule()

    private lateinit var controller: SatelliteSettingMainSwitchController
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val carrierConfig = PersistableBundle()

    @Before
    fun setUp() {
        controller = SatelliteSettingMainSwitchController(context, "satellite_setting_main_switch")
    }

    @Test
    @DisableFlags(FLAG_ENABLE_SATELLITE_TOGGLE)
    fun getAvailabilityStatus_flagOff_isUnavailable() {
        controller.init(SUB_ID, carrierConfig)

        assertThat(controller.getAvailabilityStatus(SUB_ID)).isEqualTo(BasePreferenceController.CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    @EnableFlags(FLAG_ENABLE_SATELLITE_TOGGLE)
    fun getAvailabilityStatus_flagOn_manual_isUnavailable() {
        carrierConfig.putInt(
            CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
            CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_MANUAL
        )
        controller.init(SUB_ID, carrierConfig)

        assertThat(controller.getAvailabilityStatus(SUB_ID)).isEqualTo(BasePreferenceController.CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    @EnableFlags(FLAG_ENABLE_SATELLITE_TOGGLE)
    fun getAvailabilityStatus_flagOn_automatic_isAvailable() {
        carrierConfig.putInt(
            CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
            CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC
        )
        controller.init(SUB_ID, carrierConfig)

        assertThat(controller.getAvailabilityStatus(SUB_ID)).isEqualTo(BasePreferenceController.AVAILABLE)
    }

    @Test
    @EnableFlags(FLAG_ENABLE_SATELLITE_TOGGLE)
    fun getAvailabilityStatus_flagOn_hybrid_isAvailable() {
        carrierConfig.putInt(
            CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
            CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_HYBRID
        )
        controller.init(SUB_ID, carrierConfig)

        assertThat(controller.getAvailabilityStatus(SUB_ID)).isEqualTo(BasePreferenceController.AVAILABLE)
    }

    companion object {
        private const val SUB_ID = 1
    }
}
