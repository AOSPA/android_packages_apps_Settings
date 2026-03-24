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

import android.app.Application
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.satellite.SatelliteManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowApplication
import org.robolectric.shadows.ShadowCarrierConfigManager
import org.robolectric.shadows.ShadowSatelliteManager
import org.robolectric.shadows.ShadowSubscriptionManager

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SatelliteUtilsTest {

    @Mock private lateinit var subInfo1: SubscriptionInfo
    @Mock private lateinit var subInfo2: SubscriptionInfo

    private lateinit var shadowSatelliteManager: ShadowSatelliteManager
    private lateinit var shadowCarrierConfigManager: ShadowCarrierConfigManager
    private lateinit var shadowSubscriptionManager: ShadowSubscriptionManager

    private lateinit var context: Application
    private lateinit var shadowApplication: ShadowApplication
    private val SUB_ID = 1
    private val SUB_ID_2 = 2

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext<Application>()

        shadowSatelliteManager =
            Shadow.extract(context.getSystemService(SatelliteManager::class.java))

        shadowCarrierConfigManager =
            Shadow.extract(context.getSystemService(CarrierConfigManager::class.java))
        shadowCarrierConfigManager.setConfigForSubId(SUB_ID, PersistableBundle())

        shadowSubscriptionManager =
            shadowOf(context.getSystemService(SubscriptionManager::class.java))

        `when`(subInfo1.subscriptionId).thenReturn(SUB_ID)
        `when`(subInfo2.subscriptionId).thenReturn(SUB_ID_2)
    }

    @Test
    fun isLteBasedNtnSupportedByCarrier_allConditionsMet_returnsTrue() {
        setupCarrierConfig(
            isAttachSupported = true,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
        )

        assertThat(SatelliteUtils.isLteBasedNtnSupportedByCarrier(context, SUB_ID)).isTrue()
    }

    @Test
    fun isLteBasedNtnSupported_bundle_returnsTrue() {
        val config =
            PersistableBundle().apply {
                putBoolean(CarrierConfigManager.KEY_SATELLITE_ATTACH_SUPPORTED_BOOL, true)
                putInt(
                    CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
                    CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
                )
            }

        assertThat(SatelliteUtils.isLteBasedNtnSupported(config)).isTrue()
    }

    @Test
    fun isLteBasedNtnSupported_supportedConfig_hasUserRestriction_stillReturnsTrue() {
        // Set attach restriction reason to true (user restriction)
        setAttachRestrictionReasons(restricted = true)
        setupCarrierConfig(
            isAttachSupported = true,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
        )

        assertThat(SatelliteUtils.isLteBasedNtnSupportedByCarrier(context, SUB_ID)).isTrue()
    }

    @Test
    fun isCarrierRoamingNtnSupported_invalidSubId_returnsFalse() {
        assertThat(
                SatelliteUtils.isCarrierRoamingNtnSupported(
                    context,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID,
                )
            )
            .isFalse()
    }

    @Test
    fun isLteBasedNtnSupportedByCarrier_satelliteAttachNotSupported_returnsFalse() {
        setupCarrierConfig(
            isAttachSupported = false,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
        )

        assertThat(SatelliteUtils.isLteBasedNtnSupportedByCarrier(context, SUB_ID)).isFalse()
    }

    @Test
    fun isLteBasedNtnSupportedByCarrier_connectTypeManual_returnsFalse() {
        setupCarrierConfig(
            isAttachSupported = true,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_MANUAL,
        )

        assertThat(SatelliteUtils.isLteBasedNtnSupportedByCarrier(context, SUB_ID)).isFalse()
    }

    @Test
    fun isCarrierRoamingNtnSupported_allConditionsMet_returnsTrue() {
        setupCarrierConfig(
            isAttachSupported = true,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_MANUAL,
        )

        assertThat(SatelliteUtils.isCarrierRoamingNtnSupported(context, SUB_ID)).isTrue()
    }

    @Test
    fun isCarrierRoamingNtnSupported_attachRestricted_stillReturnsTrue() {
        setAttachRestrictionReasons(restricted = true)
        setupCarrierConfig(
            isAttachSupported = true,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_MANUAL,
        )

        assertThat(SatelliteUtils.isCarrierRoamingNtnSupported(context, SUB_ID)).isTrue()
    }

    @Test
    fun isCarrierRoamingNtnSupported_satelliteAttachNotSupported_returnsFalse() {
        setupCarrierConfig(
            isAttachSupported = false,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_MANUAL,
        )

        assertThat(SatelliteUtils.isCarrierRoamingNtnSupported(context, SUB_ID)).isFalse()
    }

    @Test
    fun isLteBasedNtnSupportedByAnySub_noActiveSubscriptions_returnsFalse() {
        shadowSubscriptionManager.setActiveSubscriptionInfoList(emptyList())
        assertThat(SatelliteUtils.isLteBasedNtnSupportedByAnySub(context)).isFalse()
    }

    @Test
    fun isLteBasedNtnSupportedByAnySub_oneSubSupported_returnsTrue() {
        shadowSubscriptionManager.setActiveSubscriptionInfoList(listOf(subInfo1))
        setupCarrierConfig(
            isAttachSupported = true,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
            subId = SUB_ID,
        )

        assertThat(SatelliteUtils.isLteBasedNtnSupportedByAnySub(context)).isTrue()
    }

    @Test
    fun isLteBasedNtnSupportedByAnySub_oneSubNotSupported_returnsFalse() {
        shadowSubscriptionManager.setActiveSubscriptionInfoList(listOf(subInfo1))
        setupCarrierConfig(
            isAttachSupported = true,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_MANUAL,
            subId = SUB_ID,
        )

        assertThat(SatelliteUtils.isLteBasedNtnSupportedByAnySub(context)).isFalse()
    }

    @Test
    fun isLteBasedNtnSupportedByAnySub_twoSubsOneSupported_returnsTrue() {
        shadowSubscriptionManager.setActiveSubscriptionInfoList(listOf(subInfo1, subInfo2))
        // SUB_ID is supported
        setupCarrierConfig(
            isAttachSupported = true,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
            subId = SUB_ID,
        )
        // SUB_ID_2 is not supported
        setupCarrierConfig(
            isAttachSupported = false,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
            subId = SUB_ID_2,
        )

        assertThat(SatelliteUtils.isLteBasedNtnSupportedByAnySub(context)).isTrue()
    }

    @Test
    fun isLteBasedNtnSupportedByAnySub_twoSubsNoneSupported_returnsFalse() {
        shadowSubscriptionManager.setActiveSubscriptionInfoList(listOf(subInfo1, subInfo2))
        // SUB_ID is not supported
        setupCarrierConfig(
            isAttachSupported = false,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
            subId = SUB_ID,
        )
        // SUB_ID_2 is not supported
        setupCarrierConfig(
            isAttachSupported = true,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_MANUAL,
            subId = SUB_ID_2,
        )

        assertThat(SatelliteUtils.isLteBasedNtnSupportedByAnySub(context)).isFalse()
    }

    /** Configures the test environment to simulate whether LTE NTN has restrictions */
    private fun setAttachRestrictionReasons(restricted: Boolean, subId: Int = SUB_ID) {
        val reasons =
            if (restricted) setOf(SatelliteManager.SATELLITE_COMMUNICATION_RESTRICTION_REASON_USER)
            else emptySet()
        shadowSatelliteManager.setAttachRestrictionReasonsForCarrier(subId, reasons)
    }

    private fun setupCarrierConfig(
        isAttachSupported: Boolean,
        connectType: Int,
        subId: Int = SUB_ID,
    ) {
        val config =
            PersistableBundle().apply {
                putBoolean(
                    CarrierConfigManager.KEY_SATELLITE_ATTACH_SUPPORTED_BOOL,
                    isAttachSupported,
                )
                putInt(CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT, connectType)
            }
        shadowCarrierConfigManager.setConfigForSubId(subId, config)
    }
}
