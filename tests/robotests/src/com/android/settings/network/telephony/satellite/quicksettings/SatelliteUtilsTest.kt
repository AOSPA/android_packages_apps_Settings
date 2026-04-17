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
import android.content.Intent
import android.provider.Settings
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.satellite.SatelliteManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.network.telephony.TelephonyFeatureProvider
import com.android.settings.network.telephony.satellite.SatelliteSettingsRepository
import com.android.settings.testutils.FakeFeatureFactory
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.ArgumentMatchers.anyInt
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

    private lateinit var fakeFeatureFactory: FakeFeatureFactory
    @Mock private lateinit var mockTelephonyFeatureProvider: TelephonyFeatureProvider
    @Mock private lateinit var mockSatelliteSettingsRepository: SatelliteSettingsRepository
    @Mock private lateinit var mockSatelliteStateRepository: SatelliteStateRepository
    @Mock private lateinit var mockSatelliteAppsRepository: SatelliteAppsRepository

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext<Application>()
        fakeFeatureFactory = FakeFeatureFactory.setupForTest()
        `when`(fakeFeatureFactory.telephonyFeatureProvider.satelliteSettingsRepository)
            .thenReturn(mockSatelliteSettingsRepository)

        shadowSatelliteManager =
            Shadow.extract(context.getSystemService(SatelliteManager::class.java))

        shadowCarrierConfigManager =
            Shadow.extract(context.getSystemService(CarrierConfigManager::class.java))

        shadowSubscriptionManager =
            shadowOf(context.getSystemService(SubscriptionManager::class.java))

        `when`(subInfo1.subscriptionId).thenReturn(SUB_ID)
        `when`(subInfo2.subscriptionId).thenReturn(SUB_ID_2)

        SatelliteStateRepository.setInstance(mockSatelliteStateRepository)
        SatelliteUtils.satelliteAppsRepositoryProvider = { mockSatelliteAppsRepository }
    }

    @After
    fun tearDown() {
        SatelliteStateRepository.setInstance(null)
        // Reset the provider to default
        SatelliteUtils.satelliteAppsRepositoryProvider = { SatelliteAppsRepository(it) }
    }

    @Test
    fun resolveSatelliteSettingsIntent_unconstrainedMode_returnsSettingsIntent() {
        `when`(mockSatelliteStateRepository.getSatelliteDataSupportMode(SUB_ID))
            .thenReturn(SatelliteManager.SATELLITE_DATA_SUPPORT_UNCONSTRAINED)
        `when`(mockSatelliteSettingsRepository.isSatelliteAttachSupported(SUB_ID)).thenReturn(true)
        ShadowSubscriptionManager.setActiveDataSubscriptionId(SUB_ID)

        val expectedIntent = Intent(Settings.ACTION_SATELLITE_SETTING)
        `when`(mockSatelliteAppsRepository.getSettingsIntent(true)).thenReturn(expectedIntent)

        val resultIntent = SatelliteUtils.resolveSatelliteSettingsIntent(context)

        assertThat(resultIntent).isEqualTo(expectedIntent)
    }

    @Test
    fun resolveSatelliteSettingsIntent_unconstrainedMode_settingsIntentNull_returnsLandingPageIntent() {
        `when`(mockSatelliteStateRepository.getSatelliteDataSupportMode(anyInt()))
            .thenReturn(SatelliteManager.SATELLITE_DATA_SUPPORT_UNCONSTRAINED)
        `when`(mockSatelliteSettingsRepository.isSatelliteAttachSupported(anyInt())).thenReturn(true)
        ShadowSubscriptionManager.setActiveDataSubscriptionId(SUB_ID)

        `when`(mockSatelliteAppsRepository.getSettingsIntent(true)).thenReturn(null)

        val resultIntent = SatelliteUtils.resolveSatelliteSettingsIntent(context)

        assertThat(resultIntent).isNotNull()
        assertThat(resultIntent.component?.className)
            .isEqualTo(SatelliteLandingPageActivity::class.java.name)
    }

    @Test
    fun resolveSatelliteSettingsIntent_constrainedMode_returnsLandingPageIntent() {
        `when`(mockSatelliteStateRepository.getSatelliteDataSupportMode(SUB_ID))
            .thenReturn(SatelliteManager.SATELLITE_DATA_SUPPORT_CONSTRAINED)
        ShadowSubscriptionManager.setActiveDataSubscriptionId(SUB_ID)

        val resultIntent = SatelliteUtils.resolveSatelliteSettingsIntent(context)

        assertThat(resultIntent.component?.className)
            .isEqualTo(SatelliteLandingPageActivity::class.java.name)
    }

    @Test
    fun isLteBasedNtnSupportedByCarrier_allConditionsMet_returnsTrue() {
        setupSatelliteRepository(
            isAttachSupported = true,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
        )

        assertThat(SatelliteUtils.isLteBasedNtnSupportedByCarrier(context, SUB_ID)).isTrue()
    }

    @Test
    fun isLteBasedNtnSupported_supportedConfig_hasUserRestriction_stillReturnsTrue() {
        // Set attach restriction reason to true (user restriction)
        setAttachRestrictionReasons(restricted = true)
        setupSatelliteRepository(
            isAttachSupported = true,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
        )

        assertThat(SatelliteUtils.isLteBasedNtnSupportedByCarrier(context, SUB_ID)).isTrue()
    }

    @Test
    fun isCarrierRoamingNtnSupported_invalidSubId_returnsFalse() {
        assertThat(
                SatelliteUtils.isCarrierRoamingNtnSupported(
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID
                )
            )
            .isFalse()
    }

    @Test
    fun isLteBasedNtnSupportedByCarrier_satelliteAttachNotSupported_returnsFalse() {
        setupSatelliteRepository(
            isAttachSupported = false,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
        )

        assertThat(SatelliteUtils.isLteBasedNtnSupportedByCarrier(context, SUB_ID)).isFalse()
    }

    @Test
    fun isLteBasedNtnSupportedByCarrier_connectTypeManual_returnsFalse() {
        setupSatelliteRepository(
            isAttachSupported = true,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_MANUAL,
        )

        assertThat(SatelliteUtils.isLteBasedNtnSupportedByCarrier(context, SUB_ID)).isFalse()
    }

    @Test
    fun isCarrierRoamingNtnSupported_allConditionsMet_returnsTrue() {
        setupSatelliteRepository(
            isAttachSupported = true,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_MANUAL,
        )

        assertThat(SatelliteUtils.isCarrierRoamingNtnSupported(SUB_ID)).isTrue()
    }

    @Test
    fun isCarrierRoamingNtnSupported_attachRestricted_stillReturnsTrue() {
        setAttachRestrictionReasons(restricted = true)
        setupSatelliteRepository(
            isAttachSupported = true,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_MANUAL,
        )

        assertThat(SatelliteUtils.isCarrierRoamingNtnSupported(SUB_ID)).isTrue()
    }

    @Test
    fun isCarrierRoamingNtnSupported_satelliteAttachNotSupported_returnsFalse() {
        setupSatelliteRepository(
            isAttachSupported = false,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_MANUAL,
        )

        assertThat(SatelliteUtils.isCarrierRoamingNtnSupported(SUB_ID)).isFalse()
    }

    @Test
    fun isLteBasedNtnSupportedByAnySub_noActiveSubscriptions_returnsFalse() {
        shadowSubscriptionManager.setActiveSubscriptionInfoList(emptyList())
        assertThat(SatelliteUtils.isLteBasedNtnSupportedByAnySub(context)).isFalse()
    }

    @Test
    fun isLteBasedNtnSupportedByAnySub_oneSubSupported_returnsTrue() {
        shadowSubscriptionManager.setActiveSubscriptionInfoList(listOf(subInfo1))
        setupSatelliteRepository(
            isAttachSupported = true,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
            subId = SUB_ID,
        )

        assertThat(SatelliteUtils.isLteBasedNtnSupportedByAnySub(context)).isTrue()
    }

    @Test
    fun isLteBasedNtnSupportedByAnySub_oneSubNotSupported_returnsFalse() {
        shadowSubscriptionManager.setActiveSubscriptionInfoList(listOf(subInfo1))
        setupSatelliteRepository(
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
        setupSatelliteRepository(
            isAttachSupported = true,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
            subId = SUB_ID,
        )
        // SUB_ID_2 is not supported
        setupSatelliteRepository(
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
        setupSatelliteRepository(
            isAttachSupported = false,
            connectType = CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
            subId = SUB_ID,
        )
        // SUB_ID_2 is not supported
        setupSatelliteRepository(
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

    private fun setupSatelliteRepository(
        isAttachSupported: Boolean,
        connectType: Int,
        subId: Int = SUB_ID,
    ) {
        `when`(mockSatelliteSettingsRepository.isSatelliteAttachSupported(subId))
            .thenReturn(isAttachSupported)
        `when`(mockSatelliteSettingsRepository.getSatelliteNtnConnectType(subId))
            .thenReturn(connectType)
    }
}
