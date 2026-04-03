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
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.telephony.CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC
import android.telephony.CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_HYBRID
import android.telephony.CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_MANUAL
import android.telephony.CarrierConfigManager.SATELLITE_DATA_SUPPORT_ALL
import android.telephony.CarrierConfigManager.SATELLITE_DATA_SUPPORT_BANDWIDTH_CONSTRAINED
import android.telephony.CarrierConfigManager.SATELLITE_DATA_SUPPORT_ONLY_RESTRICTED
import android.telephony.satellite.SatelliteManager.SATELLITE_COMMUNICATION_RESTRICTION_REASON_ENTITLEMENT
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import com.android.internal.telephony.flags.Flags
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils.ResourcesUtils
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever

@UiThreadTest
class SatelliteSettingIndicatorControllerTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val mContext: Context = ApplicationProvider.getApplicationContext()
    private var mController: SatelliteSettingIndicatorController? = null

    private lateinit var mFakeFeatureFactory: FakeFeatureFactory
    @Mock private lateinit var mMockSatelliteSettingsRepository: SatelliteSettingsRepository

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest()
        whenever(mFakeFeatureFactory.telephonyFeatureProvider.satelliteSettingsRepository)
            .thenReturn(mMockSatelliteSettingsRepository)

        mController = SatelliteSettingIndicatorController(mContext, KEY)
    }

    @Test
    fun updateState_autoModeAndAccEligibleDataAvailableAndSupported_correctString() {
        setAutoModeCarrierConfig()
        mController?.init(TEST_SUB_ID)
        mController?.setCarrierRoamingNtnAvailability(true, true, SATELLITE_DATA_SUPPORT_ALL)
        setRestrictionReasonContain(false)
        val preferenceManager = PreferenceManager(mContext)
        val screen = preferenceManager.createPreferenceScreen(mContext)
        val category = PreferenceCategory(mContext)
        val preference1 = Preference(mContext)
        val preference2 = Preference(mContext)
        setPreferences(screen, category, preference1, preference2)

        mController?.displayPreference(screen)
        mController?.updateState(null)

        assertThat(category.shouldDisableView).isFalse()
        assertThat(preference1.isEnabled).isTrue()
        assertThat(preference1.title)
            .isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "title_satellite_connection_guide")
            )
        assertThat(preference1.summary)
            .isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "summary_satellite_connection_guide")
            )
        assertThat(preference2.isEnabled).isTrue()
        assertThat(preference2.title)
            .isEqualTo(ResourcesUtils.getResourcesString(mContext, "title_supported_service"))
        assertThat(preference2.summary)
            .isEqualTo(
                ResourcesUtils.getResourcesString(
                    mContext,
                    "summary_supported_service_with_unconstrained_data",
                )
            )
    }

    @Test
    fun updateState_autoModeAndAccEligibleDataAvailableAndConstrained_correctString() {
        setAutoModeCarrierConfig()
        mController?.init(TEST_SUB_ID)
        mController?.setCarrierRoamingNtnAvailability(
            true,
            true,
            SATELLITE_DATA_SUPPORT_BANDWIDTH_CONSTRAINED,
        )
        setRestrictionReasonContain(false)
        val preferenceManager = PreferenceManager(mContext)
        val screen = preferenceManager.createPreferenceScreen(mContext)
        val category = PreferenceCategory(mContext)
        val preference1 = Preference(mContext)
        val preference2 = Preference(mContext)
        setPreferences(screen, category, preference1, preference2)

        mController?.displayPreference(screen)
        mController?.updateState(null)

        assertThat(category.shouldDisableView).isFalse()
        assertThat(preference1.isEnabled).isTrue()
        assertThat(preference1.title)
            .isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "title_satellite_connection_guide")
            )
        assertThat(preference1.summary)
            .isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "summary_satellite_connection_guide")
            )
        assertThat(preference2.isEnabled).isTrue()
        assertThat(preference2.title)
            .isEqualTo(ResourcesUtils.getResourcesString(mContext, "title_supported_service"))
        assertThat(preference2.summary)
            .isEqualTo(
                ResourcesUtils.getResourcesString(
                    mContext,
                    "summary_supported_service_with_constrained_data",
                )
            )
    }

    @Test
    fun updateState_autoModeAndAccIneligibleDataUnavailable_prefDisabledAndCorrectString() {
        setAutoModeCarrierConfig()
        mController?.init(TEST_SUB_ID)
        mController?.setCarrierRoamingNtnAvailability(
            true,
            false,
            SATELLITE_DATA_SUPPORT_ONLY_RESTRICTED,
        )
        setRestrictionReasonContain(true)
        val preferenceManager = PreferenceManager(mContext)
        val screen = preferenceManager.createPreferenceScreen(mContext)
        val category = PreferenceCategory(mContext)
        val preference1 = Preference(mContext)
        val preference2 = Preference(mContext)
        setPreferences(screen, category, preference1, preference2)

        mController?.displayPreference(screen)
        mController?.updateState(null)

        assertThat(category.shouldDisableView).isTrue()
        assertThat(preference1.isEnabled).isFalse()
        assertThat(preference1.title)
            .isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "title_satellite_connection_guide")
            )
        assertThat(preference1.summary)
            .isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "summary_satellite_connection_guide")
            )
        assertThat(preference2.isEnabled).isFalse()
        assertThat(preference2.title)
            .isEqualTo(ResourcesUtils.getResourcesString(mContext, "title_supported_service"))
        assertThat(preference2.summary)
            .isEqualTo(
                ResourcesUtils.getResourcesString(
                    mContext,
                    "summary_supported_service_without_data_supported",
                )
            )
    }

    @Test
    fun updateState_autoModeAndAccEligibleDataUnavailable_prefEnabledAndCorrectString() {
        setAutoModeCarrierConfig()
        mController?.init(TEST_SUB_ID)
        mController?.setCarrierRoamingNtnAvailability(
            true,
            false,
            SATELLITE_DATA_SUPPORT_ONLY_RESTRICTED,
        )
        setRestrictionReasonContain(false)
        val preferenceManager = PreferenceManager(mContext)
        val screen = preferenceManager.createPreferenceScreen(mContext)
        val category = PreferenceCategory(mContext)
        val preference1 = Preference(mContext)
        val preference2 = Preference(mContext)
        setPreferences(screen, category, preference1, preference2)

        mController?.displayPreference(screen)
        mController?.updateState(null)

        assertThat(category.shouldDisableView).isFalse()
        assertThat(preference1.isEnabled).isTrue()
        assertThat(preference1.title)
            .isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "title_satellite_connection_guide")
            )
        assertThat(preference1.summary)
            .isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "summary_satellite_connection_guide")
            )
        assertThat(preference2.isEnabled).isTrue()
        assertThat(preference2.title)
            .isEqualTo(ResourcesUtils.getResourcesString(mContext, "title_supported_service"))
        assertThat(preference2.summary)
            .isEqualTo(
                ResourcesUtils.getResourcesString(
                    mContext,
                    "summary_supported_service_without_data_supported",
                )
            )
    }

    @Test
    fun updateState_manualModeAndSmsAvailble_prefEnabledAndCorrectString() {
        setManualModeCarrierConfig()
        mController?.init(TEST_SUB_ID)
        mController?.setCarrierRoamingNtnAvailability(
            true,
            false,
            SATELLITE_DATA_SUPPORT_ONLY_RESTRICTED,
        )
        val preferenceManager = PreferenceManager(mContext)
        val screen = preferenceManager.createPreferenceScreen(mContext)
        val category = PreferenceCategory(mContext)
        val preference1 = Preference(mContext)
        val preference2 = Preference(mContext)
        setPreferences(screen, category, preference1, preference2)

        mController?.displayPreference(screen)
        mController?.updateState(null)

        assertThat(category.shouldDisableView).isFalse()
        assertThat(preference1.isEnabled).isTrue()
        assertThat(preference1.title)
            .isEqualTo(
                ResourcesUtils.getResourcesString(
                    mContext,
                    "title_satellite_connection_guide_for_manual_type",
                )
            )
        assertThat(preference1.summary)
            .isEqualTo(
                ResourcesUtils.getResourcesString(
                    mContext,
                    "summary_satellite_connection_guide_for_manual_type",
                )
            )
        assertThat(preference2.isEnabled).isTrue()
        assertThat(preference2.title)
            .isEqualTo(
                ResourcesUtils.getResourcesString(
                    mContext,
                    "title_supported_service_for_manual_type",
                )
            )
        assertThat(preference2.summary)
            .isEqualTo(
                ResourcesUtils.getResourcesString(
                    mContext,
                    "summary_supported_service_for_manual_type",
                )
            )
    }

    @Test
    fun updateState_manualModeAndSmsUnAvailble_prefDisabledAndCorrectString() {
        setManualModeCarrierConfig()
        mController?.init(TEST_SUB_ID)
        mController?.setCarrierRoamingNtnAvailability(
            false,
            false,
            SATELLITE_DATA_SUPPORT_ONLY_RESTRICTED,
        )
        val preferenceManager = PreferenceManager(mContext)
        val screen = preferenceManager.createPreferenceScreen(mContext)
        val category = PreferenceCategory(mContext)
        val preference1 = Preference(mContext)
        val preference2 = Preference(mContext)
        setPreferences(screen, category, preference1, preference2)

        mController?.displayPreference(screen)
        mController?.updateState(null)

        assertThat(category.shouldDisableView).isTrue()
        assertThat(preference1.isEnabled).isFalse()
        assertThat(preference1.title)
            .isEqualTo(
                ResourcesUtils.getResourcesString(
                    mContext,
                    "title_satellite_connection_guide_for_manual_type",
                )
            )
        assertThat(preference1.summary)
            .isEqualTo(
                ResourcesUtils.getResourcesString(
                    mContext,
                    "summary_satellite_connection_guide_for_manual_type",
                )
            )
        assertThat(preference2.isEnabled).isFalse()
        assertThat(preference2.title)
            .isEqualTo(
                ResourcesUtils.getResourcesString(
                    mContext,
                    "title_supported_service_for_manual_type",
                )
            )
        assertThat(preference2.summary)
            .isEqualTo(
                ResourcesUtils.getResourcesString(
                    mContext,
                    "summary_supported_service_for_manual_type",
                )
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_VZW_AST_SKYLO_FALLBACK)
    fun updateState_hybridModeAndAccNotEligibleSmsUnAvailble_preferenceDisabled() {
        setHybridModeCarrierConfig()
        mController?.init(TEST_SUB_ID)
        mController?.setCarrierRoamingNtnAvailability(
            false,
            false,
            SATELLITE_DATA_SUPPORT_ONLY_RESTRICTED,
        )
        setRestrictionReasonContain(true)
        val preferenceManager = PreferenceManager(mContext)
        val screen = preferenceManager.createPreferenceScreen(mContext)
        val category = PreferenceCategory(mContext)
        val preference1 = Preference(mContext)
        val preference2 = Preference(mContext)
        setPreferences(screen, category, preference1, preference2)

        mController?.displayPreference(screen)
        mController?.updateState(null)

        assertThat(category.shouldDisableView).isTrue()
        assertThat(preference1.isEnabled).isFalse()
        assertThat(preference1.title)
            .isEqualTo(
                ResourcesUtils.getResourcesString(
                    mContext,
                    "title_satellite_connection_guide_for_manual_type",
                )
            )
        assertThat(preference1.summary)
            .isEqualTo(
                ResourcesUtils.getResourcesString(
                    mContext,
                    "summary_satellite_connection_guide_for_manual_type",
                )
            )
        assertThat(preference2.isEnabled).isFalse()
        assertThat(preference2.title)
            .isEqualTo(
                ResourcesUtils.getResourcesString(
                    mContext,
                    "title_supported_service_for_manual_type",
                )
            )
        assertThat(preference2.summary)
            .isEqualTo(
                ResourcesUtils.getResourcesString(
                    mContext,
                    "summary_supported_service_for_manual_type",
                )
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_VZW_AST_SKYLO_FALLBACK)
    fun updateState_hybridModeAndAccNotEligibleSmsAvailbleDataUnavailable_prefEnabledAndCorrectString() {
        setHybridModeCarrierConfig()
        mController?.init(TEST_SUB_ID)
        mController?.setCarrierRoamingNtnAvailability(
            true,
            false,
            SATELLITE_DATA_SUPPORT_ONLY_RESTRICTED,
        )
        setRestrictionReasonContain(true)
        val preferenceManager = PreferenceManager(mContext)
        val screen = preferenceManager.createPreferenceScreen(mContext)
        val category = PreferenceCategory(mContext)
        val preference1 = Preference(mContext)
        val preference2 = Preference(mContext)
        setPreferences(screen, category, preference1, preference2)

        mController?.displayPreference(screen)
        mController?.updateState(null)

        assertThat(category.shouldDisableView).isFalse()
        assertThat(preference1.isEnabled).isTrue()
        assertThat(preference1.title)
            .isEqualTo(
                ResourcesUtils.getResourcesString(
                    mContext,
                    "title_satellite_connection_guide_for_manual_type",
                )
            )
        assertThat(preference1.summary)
            .isEqualTo(
                ResourcesUtils.getResourcesString(
                    mContext,
                    "summary_satellite_connection_guide_for_manual_type",
                )
            )
        assertThat(preference2.isEnabled).isTrue()
        assertThat(preference2.title)
            .isEqualTo(
                ResourcesUtils.getResourcesString(
                    mContext,
                    "title_supported_service_for_manual_type",
                )
            )
        assertThat(preference2.summary)
            .isEqualTo(
                ResourcesUtils.getResourcesString(
                    mContext,
                    "summary_supported_service_for_manual_type",
                )
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_VZW_AST_SKYLO_FALLBACK)
    fun updateState_hybridModeAndAccEligibleDataUnavailable_prefEnabledAndCorrectString() {
        setHybridModeCarrierConfig()
        mController?.init(TEST_SUB_ID)
        mController?.setCarrierRoamingNtnAvailability(
            true,
            false,
            SATELLITE_DATA_SUPPORT_ONLY_RESTRICTED,
        )
        setRestrictionReasonContain(false)
        val preferenceManager = PreferenceManager(mContext)
        val screen = preferenceManager.createPreferenceScreen(mContext)
        val category = PreferenceCategory(mContext)
        val preference1 = Preference(mContext)
        val preference2 = Preference(mContext)
        setPreferences(screen, category, preference1, preference2)

        mController?.displayPreference(screen)
        mController?.updateState(null)

        assertThat(category.shouldDisableView).isEqualTo(false)
        assertThat(preference1.isEnabled).isTrue()
        assertThat(preference1.title)
            .isEqualTo(
                ResourcesUtils.getResourcesString(
                    mContext,
                    "title_satellite_connection_guide_for_manual_type",
                )
            )
        assertThat(preference1.summary)
            .isEqualTo(
                ResourcesUtils.getResourcesString(
                    mContext,
                    "summary_satellite_connection_guide_for_manual_type",
                )
            )
        assertThat(preference2.isEnabled).isTrue()
        assertThat(preference2.title)
            .isEqualTo(
                ResourcesUtils.getResourcesString(
                    mContext,
                    "title_supported_service_for_manual_type",
                )
            )
        assertThat(preference2.summary)
            .isEqualTo(
                ResourcesUtils.getResourcesString(
                    mContext,
                    "summary_supported_service_for_manual_type",
                )
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_VZW_AST_SKYLO_FALLBACK)
    fun updateState_hybridModeAndAccEligibleAndDataAvailable_prefEnabledAndCorrectString() {
        setHybridModeCarrierConfig()
        mController?.init(TEST_SUB_ID)
        mController?.setCarrierRoamingNtnAvailability(
            true,
            true,
            SATELLITE_DATA_SUPPORT_ONLY_RESTRICTED,
        )
        setRestrictionReasonContain(false)
        val preferenceManager = PreferenceManager(mContext)
        val screen = preferenceManager.createPreferenceScreen(mContext)
        val category = PreferenceCategory(mContext)
        val preference1 = Preference(mContext)
        val preference2 = Preference(mContext)
        setPreferences(screen, category, preference1, preference2)

        mController?.displayPreference(screen)
        mController?.updateState(null)

        assertThat(category.shouldDisableView).isFalse()
        assertThat(preference1.isEnabled).isTrue()
        assertThat(preference1.title)
            .isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "title_satellite_connection_guide")
            )
        assertThat(preference1.summary)
            .isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "summary_satellite_connection_guide")
            )
        assertThat(preference2.isEnabled).isTrue()
        assertThat(preference2.title)
            .isEqualTo(ResourcesUtils.getResourcesString(mContext, "title_supported_service"))
        assertThat(preference2.summary)
            .isEqualTo(
                ResourcesUtils.getResourcesString(
                    mContext,
                    "summary_supported_service_without_data_supported",
                )
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_VZW_AST_SKYLO_FALLBACK)
    fun updateState_hybridModeAndAccEligibleAndDataAvailableDataConstrained_correctString() {
        setHybridModeCarrierConfig()
        mController?.init(TEST_SUB_ID)
        mController?.setCarrierRoamingNtnAvailability(
            true,
            true,
            SATELLITE_DATA_SUPPORT_BANDWIDTH_CONSTRAINED,
        )
        setRestrictionReasonContain(false)
        val preferenceManager = PreferenceManager(mContext)
        val screen = preferenceManager.createPreferenceScreen(mContext)
        val category = PreferenceCategory(mContext)
        val preference1 = Preference(mContext)
        val preference2 = Preference(mContext)
        setPreferences(screen, category, preference1, preference2)

        mController?.displayPreference(screen)
        mController?.updateState(null)

        assertThat(category.shouldDisableView).isFalse()
        assertThat(preference1.isEnabled).isTrue()
        assertThat(preference1.title)
            .isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "title_satellite_connection_guide")
            )
        assertThat(preference1.summary)
            .isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "summary_satellite_connection_guide")
            )
        assertThat(preference2.isEnabled).isTrue()
        assertThat(preference2.title)
            .isEqualTo(ResourcesUtils.getResourcesString(mContext, "title_supported_service"))
        assertThat(preference2.summary)
            .isEqualTo(
                ResourcesUtils.getResourcesString(
                    mContext,
                    "summary_supported_service_with_constrained_data",
                )
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_VZW_AST_SKYLO_FALLBACK)
    fun updateState_hybridModeAndAccEligibleAndDataAvailableDataSupported_correctString() {
        setHybridModeCarrierConfig()
        mController?.init(TEST_SUB_ID)
        mController?.setCarrierRoamingNtnAvailability(true, true, SATELLITE_DATA_SUPPORT_ALL)
        setRestrictionReasonContain(false)
        val preferenceManager = PreferenceManager(mContext)
        val screen = preferenceManager.createPreferenceScreen(mContext)
        val category = PreferenceCategory(mContext)
        val preference1 = Preference(mContext)
        val preference2 = Preference(mContext)
        setPreferences(screen, category, preference1, preference2)

        mController?.displayPreference(screen)
        mController?.updateState(null)

        assertThat(category.shouldDisableView).isFalse()
        assertThat(preference1.isEnabled).isTrue()
        assertThat(preference1.title)
            .isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "title_satellite_connection_guide")
            )
        assertThat(preference1.summary)
            .isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "summary_satellite_connection_guide")
            )
        assertThat(preference2.isEnabled).isTrue()
        assertThat(preference2.title)
            .isEqualTo(ResourcesUtils.getResourcesString(mContext, "title_supported_service"))
        assertThat(preference2.summary)
            .isEqualTo(
                ResourcesUtils.getResourcesString(
                    mContext,
                    "summary_supported_service_with_unconstrained_data",
                )
            )
    }

    private fun setPreferences(
        screen: PreferenceScreen,
        category: PreferenceCategory,
        preference1: Preference,
        preference2: Preference,
    ) {
        category.setKey(
            SatelliteSettingIndicatorController.Companion.PREF_KEY_CATEGORY_HOW_IT_WORKS
        )
        category.title = "test title"

        preference1.setKey(
            SatelliteSettingIndicatorController.Companion.KEY_SATELLITE_CONNECTION_GUIDE
        )
        preference1.title = "preference1"

        preference2.setKey(SatelliteSettingIndicatorController.Companion.KEY_SUPPORTED_SERVICE)
        preference2.title = "preference2"
        screen.addPreference(category)
        category.addPreference(preference1)
        category.addPreference(preference2)
    }

    private fun setHybridModeCarrierConfig() {
        whenever(mMockSatelliteSettingsRepository.getSatelliteNtnConnectType(anyInt()))
            .thenReturn(CARRIER_ROAMING_NTN_CONNECT_HYBRID)
        whenever(mMockSatelliteSettingsRepository.isSatelliteEntitlementSupported(anyInt()))
            .thenReturn(true)
    }

    private fun setManualModeCarrierConfig() {
        whenever(mMockSatelliteSettingsRepository.getSatelliteNtnConnectType(anyInt()))
            .thenReturn(CARRIER_ROAMING_NTN_CONNECT_MANUAL)
    }

    private fun setAutoModeCarrierConfig() {
        whenever(mMockSatelliteSettingsRepository.getSatelliteNtnConnectType(anyInt()))
            .thenReturn(CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC)
    }

    private fun setRestrictionReasonContain(isRestrictionReasonContain: Boolean) {
        val result =
            if (isRestrictionReasonContain)
                setOf(SATELLITE_COMMUNICATION_RESTRICTION_REASON_ENTITLEMENT)
            else emptySet()

        val wrapper = spy(SatelliteCarrierSettingUtils.SatelliteManagerWrapper(mContext))
        whenever(wrapper.getAttachRestrictionReasonsForCarrier(TEST_SUB_ID)).thenReturn(result)
        SatelliteCarrierSettingUtils.sSatelliteManagerWrapper = wrapper
    }

    companion object {
        private const val KEY = "SatelliteSettingIndicatorControllerTest"
        private const val TEST_SUB_ID = 5
    }
}
