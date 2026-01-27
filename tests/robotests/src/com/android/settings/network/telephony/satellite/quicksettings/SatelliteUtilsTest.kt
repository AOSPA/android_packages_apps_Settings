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
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.Mockito.mock
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
        setAttachRestrictionReasons(isLteNtnSupported = true)
        setupCarrierConfig(
            isAttachSupported = true,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
        )

        assertThat(SatelliteUtils.isLteBasedNtnSupportedByCarrier(context, SUB_ID)).isTrue()
    }

    @Test
    fun isLteBasedNtnSupportedByCarrier_attachRestricted_returnsFalse() {
        setAttachRestrictionReasons(isLteNtnSupported = false)
        setupCarrierConfig(
            isAttachSupported = true,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
        )

        assertThat(SatelliteUtils.isLteBasedNtnSupportedByCarrier(context, SUB_ID)).isFalse()
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
    fun isLteBasedNtnSupportedByCarrier_satelliteManagerThrowsException_returnsFalse() {
        val mockSatelliteManager = mock(SatelliteManager::class.java)
        setupCarrierConfig(
            isAttachSupported = true,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
        )
        // Force exception on SatelliteManager
        `when`(mockSatelliteManager.getAttachRestrictionReasonsForCarrier(anyInt()))
            .thenThrow(RuntimeException("Test Exception"))
        // Use ContextWrapper to inject the mock SatelliteManager
        val wrapperContext =
            object : android.content.ContextWrapper(context) {
                override fun getSystemService(name: String): Any? {
                    if (getSystemServiceName(SatelliteManager::class.java) == name) {
                        return mockSatelliteManager
                    }
                    return super.getSystemService(name)
                }
            }

        assertThat(SatelliteUtils.isLteBasedNtnSupportedByCarrier(wrapperContext, SUB_ID)).isFalse()
    }

    @Test
    fun isLteBasedNtnSupportedByCarrier_satelliteAttachNotSupported_returnsFalse() {
        setAttachRestrictionReasons(isLteNtnSupported = true)
        setupCarrierConfig(
            isAttachSupported = false,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
        )

        assertThat(SatelliteUtils.isLteBasedNtnSupportedByCarrier(context, SUB_ID)).isFalse()
    }

    @Test
    fun isLteBasedNtnSupportedByCarrier_connectTypeManual_returnsFalse() {
        setAttachRestrictionReasons(isLteNtnSupported = true)
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
        setAttachRestrictionReasons(isLteNtnSupported = false)
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
    fun isLteBasedNtnSupportedByDevice_noActiveSubscriptions_returnsFalse() {
        shadowSubscriptionManager.setActiveSubscriptionInfoList(emptyList())
        assertThat(SatelliteUtils.isLteBasedNtnSupportedByDevice(context)).isFalse()
    }

    @Test
    fun isLteBasedNtnSupportedByDevice_oneSubSupported_returnsTrue() {
        shadowSubscriptionManager.setActiveSubscriptionInfoList(listOf(subInfo1))
        setAttachRestrictionReasons(isLteNtnSupported = true, subId = SUB_ID)
        setupCarrierConfig(
            isAttachSupported = true,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
            subId = SUB_ID,
        )

        assertThat(SatelliteUtils.isLteBasedNtnSupportedByDevice(context)).isTrue()
    }

    @Test
    fun isLteBasedNtnSupportedByDevice_oneSubNotSupported_returnsFalse() {
        shadowSubscriptionManager.setActiveSubscriptionInfoList(listOf(subInfo1))
        setAttachRestrictionReasons(isLteNtnSupported = false, subId = SUB_ID)
        setupCarrierConfig(
            isAttachSupported = true,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
            subId = SUB_ID,
        )

        assertThat(SatelliteUtils.isLteBasedNtnSupportedByDevice(context)).isFalse()
    }

    @Test
    fun isLteBasedNtnSupportedByDevice_twoSubsOneSupported_returnsTrue() {
        shadowSubscriptionManager.setActiveSubscriptionInfoList(listOf(subInfo1, subInfo2))
        // SUB_ID is supported
        setAttachRestrictionReasons(isLteNtnSupported = true, subId = SUB_ID)
        setupCarrierConfig(
            isAttachSupported = true,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
            subId = SUB_ID,
        )
        // SUB_ID_2 is not supported
        setAttachRestrictionReasons(isLteNtnSupported = true, subId = SUB_ID_2)
        setupCarrierConfig(
            isAttachSupported = false,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
            subId = SUB_ID_2,
        )

        assertThat(SatelliteUtils.isLteBasedNtnSupportedByDevice(context)).isTrue()
    }

    @Test
    fun isLteBasedNtnSupportedByDevice_twoSubsNoneSupported_returnsFalse() {
        shadowSubscriptionManager.setActiveSubscriptionInfoList(listOf(subInfo1, subInfo2))
        // SUB_ID is not supported
        setAttachRestrictionReasons(isLteNtnSupported = false, subId = SUB_ID)
        setupCarrierConfig(
            isAttachSupported = true,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
            subId = SUB_ID,
        )
        // SUB_ID_2 is not supported
        setAttachRestrictionReasons(isLteNtnSupported = false, subId = SUB_ID_2)
        setupCarrierConfig(
            isAttachSupported = true,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
            subId = SUB_ID_2,
        )

        assertThat(SatelliteUtils.isLteBasedNtnSupportedByDevice(context)).isFalse()
    }

    /** Configures the test environment to simulate whether LTE NTN has restrictions */
    private fun setAttachRestrictionReasons(isLteNtnSupported: Boolean, subId: Int = SUB_ID) {
        val reasons = if (isLteNtnSupported) emptySet() else setOf(1)
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
