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

package com.android.settings.network.telephony.satellite

import android.content.Context
import android.os.Looper
import android.os.OutcomeReceiver
import android.os.PersistableBundle
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.telephony.CarrierConfigManager
import android.telephony.satellite.EnableRequestAttributes
import android.telephony.satellite.EnableResponse
import android.telephony.satellite.SatelliteManager
import android.telephony.satellite.SatelliteManager.SATELLITE_RESULT_ERROR
import android.telephony.satellite.SatelliteManager.SATELLITE_RESULT_SUCCESS
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import androidx.test.core.app.ApplicationProvider
import com.android.settings.core.BasePreferenceController
import com.android.settings.flags.Flags.FLAG_ENABLE_SATELLITE_TOGGLE
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settingslib.widget.MainSwitchPreference
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import java.util.function.Consumer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.Resetter

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [SatelliteSettingMainSwitchControllerTest.ShadowSatelliteManager::class])
class SatelliteSettingMainSwitchControllerTest {

    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()
    @get:Rule val setFlagsRule: SetFlagsRule = SetFlagsRule()

    private lateinit var switchPreference: MainSwitchPreference
    private lateinit var preferenceScreen: PreferenceScreen
    private lateinit var context: Context
    private lateinit var controller: SatelliteSettingMainSwitchController
    private lateinit var satelliteManager: SatelliteManager
    private val carrierConfig = PersistableBundle()

    private lateinit var fakeFeatureFactory: FakeFeatureFactory
    @Mock private lateinit var mockSatelliteSettingsRepository: SatelliteSettingsRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        fakeFeatureFactory = FakeFeatureFactory.setupForTest()
        `when`(fakeFeatureFactory.telephonyFeatureProvider.satelliteSettingsRepository)
            .thenReturn(mockSatelliteSettingsRepository)

        satelliteManager = context.getSystemService(SatelliteManager::class.java)
        preferenceScreen = PreferenceManager(context).createPreferenceScreen(context)
        controller = SatelliteSettingMainSwitchController(context, "satellite_setting_main_switch")

        switchPreference = spy(MainSwitchPreference(context))
        switchPreference.key = controller.preferenceKey
        preferenceScreen.addPreference(switchPreference)

        ShadowSatelliteManager.reset()
        `when`(mockSatelliteSettingsRepository.isSatelliteAttachSupported(SUB_ID)).thenReturn(true)
        `when`(mockSatelliteSettingsRepository.getSatelliteNtnConnectType(SUB_ID))
            .thenReturn(CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC)
    }

    @Test
    @DisableFlags(FLAG_ENABLE_SATELLITE_TOGGLE)
    fun getAvailabilityStatus_flagOff_isUnavailable() {
        controller.init(SUB_ID, satelliteManager)

        assertThat(controller.getAvailabilityStatus(SUB_ID))
            .isEqualTo(BasePreferenceController.CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    @EnableFlags(FLAG_ENABLE_SATELLITE_TOGGLE)
    fun getAvailabilityStatus_manual_isUnavailable() {
        `when`(mockSatelliteSettingsRepository.getSatelliteNtnConnectType(SUB_ID))
            .thenReturn(CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_MANUAL)
        controller.init(SUB_ID, satelliteManager)

        assertThat(controller.getAvailabilityStatus(SUB_ID))
            .isEqualTo(BasePreferenceController.CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    @EnableFlags(FLAG_ENABLE_SATELLITE_TOGGLE)
    fun getAvailabilityStatus_attachNotSupported_isUnavailable() {
        `when`(mockSatelliteSettingsRepository.isSatelliteAttachSupported(SUB_ID)).thenReturn(false)
        `when`(mockSatelliteSettingsRepository.getSatelliteNtnConnectType(SUB_ID))
            .thenReturn(CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC)
        controller.init(SUB_ID, satelliteManager)

        assertThat(controller.getAvailabilityStatus(SUB_ID))
            .isEqualTo(BasePreferenceController.CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    @EnableFlags(FLAG_ENABLE_SATELLITE_TOGGLE)
    fun getAvailabilityStatus_automatic_isAvailable() {
        `when`(mockSatelliteSettingsRepository.getSatelliteNtnConnectType(SUB_ID))
            .thenReturn(CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC)
        controller.init(SUB_ID, satelliteManager)

        assertThat(controller.getAvailabilityStatus(SUB_ID))
            .isEqualTo(BasePreferenceController.AVAILABLE)
    }

    @Test
    @EnableFlags(FLAG_ENABLE_SATELLITE_TOGGLE)
    fun getAvailabilityStatus_hybrid_isAvailable() {
        `when`(mockSatelliteSettingsRepository.getSatelliteNtnConnectType(SUB_ID))
            .thenReturn(CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_HYBRID)
        controller.init(SUB_ID, satelliteManager)

        assertThat(controller.getAvailabilityStatus(SUB_ID))
            .isEqualTo(BasePreferenceController.AVAILABLE)
    }

    @Test
    @EnableFlags(FLAG_ENABLE_SATELLITE_TOGGLE)
    fun init_requestsIsEnabled() {
        // Set up expected result
        val expectedResponse = EnableResponse(true, false, false, intArrayOf())
        ShadowSatelliteManager.setRequestIsEnabledResult(expectedResponse)

        controller.init(SUB_ID, satelliteManager)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(controller.isChecked()).isTrue()
    }

    @Test
    @EnableFlags(FLAG_ENABLE_SATELLITE_TOGGLE)
    fun updateState_requestsIsEnabled() {
        val expectedResponse = EnableResponse(true, false, false, intArrayOf())
        ShadowSatelliteManager.setRequestIsEnabledResult(expectedResponse)

        controller.init(SUB_ID, satelliteManager)
        shadowOf(Looper.getMainLooper()).idle()

        controller.updateState(switchPreference)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(controller.isChecked()).isTrue()
    }

    @Test
    @EnableFlags(FLAG_ENABLE_SATELLITE_TOGGLE)
    fun requestIsEnabled_onResult_updatesStateAndPreference() {
        val expectedResponse = EnableResponse(true, false, false, intArrayOf())
        ShadowSatelliteManager.setRequestIsEnabledResult(expectedResponse)

        controller.displayPreference(preferenceScreen)
        controller.init(SUB_ID, satelliteManager)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(controller.isChecked()).isTrue()
        verify(switchPreference).isChecked = true
        verify(switchPreference).isEnabled = true
    }

    @Test
    @EnableFlags(FLAG_ENABLE_SATELLITE_TOGGLE)
    fun setChecked_true_success_updatesStateAndCallsManager() {
        ShadowSatelliteManager.setRequestEnabledResult(SATELLITE_RESULT_SUCCESS)

        controller.init(SUB_ID, satelliteManager)
        controller.displayPreference(preferenceScreen)

        val result = controller.setChecked(true)

        assertThat(result).isTrue()
        assertThat(controller.isChecked()).isTrue() // Optimistic update

        val attributes = ShadowSatelliteManager.getRequestEnabledAttributes()
        assertThat(attributes).isNotNull()
        assertThat(attributes!!.isEnabled).isTrue()
        assertThat(attributes.satelliteEnablementRequestReason)
            .isEqualTo(SatelliteManager.SATELLITE_ENABLEMENT_REQUEST_REASON_USER)

        assertThat(controller.isChecked()).isTrue()
    }

    @Test
    @EnableFlags(FLAG_ENABLE_SATELLITE_TOGGLE)
    fun setChecked_true_failure_revertsState() {
        // Initial state: Disabled
        ShadowSatelliteManager.setRequestIsEnabledResult(
            EnableResponse(false, false, false, intArrayOf())
        )
        controller.init(SUB_ID, satelliteManager)
        shadowOf(Looper.getMainLooper()).idle()
        controller.displayPreference(preferenceScreen)
        assertThat(controller.isChecked()).isFalse()

        // Set up failure for setChecked
        ShadowSatelliteManager.setRequestEnabledResult(SATELLITE_RESULT_ERROR)

        controller.setChecked(true)
        shadowOf(Looper.getMainLooper()).idle()

        // It should eventually revert to false
        assertThat(controller.isChecked()).isFalse()
        verify(switchPreference, times(2)).isChecked = false
    }

    @Implements(SatelliteManager::class)
    class ShadowSatelliteManager : org.robolectric.shadows.ShadowSatelliteManager() {

        companion object {
            private var requestIsEnabledResult: EnableResponse? = null
            private var requestEnabledResult: Int? = null
            private var requestEnabledAttributes: EnableRequestAttributes? = null

            fun setRequestIsEnabledResult(result: EnableResponse) {
                requestIsEnabledResult = result
            }

            fun setRequestEnabledResult(result: Int) {
                requestEnabledResult = result
            }

            fun getRequestEnabledAttributes(): EnableRequestAttributes? {
                return requestEnabledAttributes
            }

            @Resetter
            fun reset() {
                requestIsEnabledResult = null
                requestEnabledResult = null
                requestEnabledAttributes = null
                org.robolectric.shadows.ShadowSatelliteManager.reset()
            }
        }

        @Implementation
        protected fun requestIsEnabled(
            subId: Int,
            ntnConnectType: Int,
            executor: Executor,
            callback: OutcomeReceiver<EnableResponse, SatelliteManager.SatelliteException>,
        ) {
            executor.execute { requestIsEnabledResult?.let { callback.onResult(it) } }
        }

        @Implementation
        protected fun requestEnabled(
            subId: Int,
            attributes: EnableRequestAttributes,
            executor: Executor,
            callback: Consumer<Int>,
        ) {
            requestEnabledAttributes = attributes
            executor.execute { requestEnabledResult?.let { callback.accept(it) } }
        }
    }

    companion object {
        private const val SUB_ID = 1
    }
}
