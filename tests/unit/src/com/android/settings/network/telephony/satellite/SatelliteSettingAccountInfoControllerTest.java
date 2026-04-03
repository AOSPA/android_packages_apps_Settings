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

package com.android.settings.network.telephony.satellite;

import static android.telephony.CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC;
import static android.telephony.CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_HYBRID;
import static android.telephony.CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_MANUAL;
import static android.telephony.CarrierConfigManager.KEY_SATELLITE_INFORMATION_REDIRECT_URL_STRING;
import static android.telephony.CarrierConfigManager.SATELLITE_DATA_SUPPORT_ALL;
import static android.telephony.CarrierConfigManager.SATELLITE_DATA_SUPPORT_BANDWIDTH_CONSTRAINED;

import static com.android.internal.telephony.flags.Flags.FLAG_VZW_AST_SKYLO_FALLBACK;
import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.settings.network.telephony.satellite.SatelliteSettingAccountInfoController
        .PREF_KEY_CATEGORY_YOUR_SATELLITE_PLAN;
import static com.android.settings.network.telephony.satellite.SatelliteSettingAccountInfoController
        .PREF_KEY_YOUR_SATELLITE_DATA_PLAN;
import static com.android.settings.network.telephony.satellite.SatelliteSettingAccountInfoController
        .PREF_KEY_YOUR_SATELLITE_PLAN;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.telephony.TelephonyManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.network.telephony.CarrierConfigRepository;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.ResourcesUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class SatelliteSettingAccountInfoControllerTest {
    private static final int TEST_SUB_ID = 5;
    private static final String TEST_OPERATOR_NAME = "test_operator_name";

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private SatelliteSettingsRepository mSatelliteSettingsRepository;

    private Context mContext;
    private SatelliteSettingAccountInfoController mController;
    private FakeFeatureFactory mFakeFeatureFactory;
    private CarrierConfigRepository mCarrierConfigRepository;

    @Before
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mContext = spy(ApplicationProvider.getApplicationContext());
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
        when(mFakeFeatureFactory.mTelephonyFeatureProvider.getSatelliteSettingsRepository())
                .thenReturn(mSatelliteSettingsRepository);
        mCarrierConfigRepository = new CarrierConfigRepository(mContext);
        when(mFakeFeatureFactory.mTelephonyFeatureProvider.getCarrierConfigRepository())
                .thenReturn(mCarrierConfigRepository);

        mController = new SatelliteSettingAccountInfoController(mContext,
                PREF_KEY_CATEGORY_YOUR_SATELLITE_PLAN);
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        when(mTelephonyManager.getSimOperatorName(TEST_SUB_ID)).thenReturn(TEST_OPERATOR_NAME);
    }

    @Test
    public void getAvailabilityStatus_entitlementNotSupport_returnConditionalUnavailable() {
        when(mSatelliteSettingsRepository.getSatelliteNtnConnectType(anyInt())).thenReturn(
                CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC);
        when(mSatelliteSettingsRepository.isSatelliteEntitlementSupported(anyInt())).thenReturn(
                false);
        mController.init(TEST_SUB_ID);
        mController.setCarrierRoamingNtnAvailability(false, false, -1);

        int result = mController.getAvailabilityStatus(TEST_SUB_ID);

        assertThat(result).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_entitlementIsSupported_returnConditionalUnavailable() {
        when(mSatelliteSettingsRepository.getSatelliteNtnConnectType(anyInt())).thenReturn(
                CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC);
        when(mSatelliteSettingsRepository.isSatelliteEntitlementSupported(anyInt())).thenReturn(
                true);
        mController.init(TEST_SUB_ID);
        mController.setCarrierRoamingNtnAvailability(false, false, -1);

        int result = mController.getAvailabilityStatus(TEST_SUB_ID);

        assertThat(result).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void getAvailabilityStatus_connectionTypeISManual_returnAvailable() {
        when(mSatelliteSettingsRepository.getSatelliteNtnConnectType(anyInt())).thenReturn(
                CARRIER_ROAMING_NTN_CONNECT_MANUAL);
        mController.init(TEST_SUB_ID);
        mController.setCarrierRoamingNtnAvailability(false, false, -1);

        int result = mController.getAvailabilityStatus(TEST_SUB_ID);

        assertThat(result).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    @EnableFlags(FLAG_VZW_AST_SKYLO_FALLBACK)
    public void getAvailabilityStatus_connectionTypeISHybrid_returnAvailable() {
        when(mSatelliteSettingsRepository.getSatelliteNtnConnectType(anyInt())).thenReturn(
                CARRIER_ROAMING_NTN_CONNECT_HYBRID);
        mController.init(TEST_SUB_ID);
        mController.setCarrierRoamingNtnAvailability(false, false, -1);

        int result = mController.getAvailabilityStatus(TEST_SUB_ID);

        assertThat(result).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void testSatelliteEligibility_ManualConnect_withSmsAvailable() {
        when(mSatelliteSettingsRepository.getSatelliteNtnConnectType(anyInt())).thenReturn(
                CARRIER_ROAMING_NTN_CONNECT_MANUAL);
        mController.init(TEST_SUB_ID);
        mController.setCarrierRoamingNtnAvailability(true, false, -1);

        assertTrue(mController.isSatelliteEligible());
    }

    @Test
    @EnableFlags(FLAG_VZW_AST_SKYLO_FALLBACK)
    public void testSatelliteEligibility_HybridConnect_withSmsAvailable() {
        boolean isSmsAvailable = true;
        when(mSatelliteSettingsRepository.getSatelliteNtnConnectType(anyInt())).thenReturn(
                CARRIER_ROAMING_NTN_CONNECT_HYBRID);
        mController.init(TEST_SUB_ID);
        mController.setCarrierRoamingNtnAvailability(isSmsAvailable, false, -1);

        assertThat(mController.isSatelliteEligible()).isEqualTo(isSmsAvailable);
    }

    @Test
    @EnableFlags(FLAG_VZW_AST_SKYLO_FALLBACK)
    public void testSatelliteEligibility_HybridConnect_withSmsUnavailable() {
        boolean isSmsAvailable = false;
        when(mSatelliteSettingsRepository.getSatelliteNtnConnectType(anyInt())).thenReturn(
                CARRIER_ROAMING_NTN_CONNECT_HYBRID);
        mController.init(TEST_SUB_ID);
        mController.setCarrierRoamingNtnAvailability(isSmsAvailable, false, -1);

        // When SatelliteManager is null (default in tests), isSatelliteAccountEligible
        // returns true.
        assertThat(mController.isSatelliteEligible()).isTrue();
    }

    @Test
    public void displayPreference_showCategoryTitle_correctOperatorName() {
        when(mSatelliteSettingsRepository.isSatelliteEntitlementSupported(anyInt())).thenReturn(
                true);
        mController.init(TEST_SUB_ID);
        mController.setCarrierRoamingNtnAvailability(false, false, -1);
        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        PreferenceCategory preferenceCategory = new PreferenceCategory(mContext);
        preferenceCategory.setKey(PREF_KEY_CATEGORY_YOUR_SATELLITE_PLAN);
        Preference preference = new Preference(mContext);
        preference.setKey(PREF_KEY_YOUR_SATELLITE_PLAN);
        screen.addPreference(preferenceCategory);
        screen.addPreference(preference);

        mController.displayPreference(screen);

        assertThat(preferenceCategory.getTitle().toString()).isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "category_title_your_satellite_plan",
                        TEST_OPERATOR_NAME));
    }

    @Test
    public void displayPreference_showEligibleUiButDataUnavailable_showSmsEligibleAccountState() {
        when(mSatelliteSettingsRepository.isSatelliteEntitlementSupported(anyInt())).thenReturn(
                true);
        mController = new SatelliteSettingAccountInfoController(mContext,
                PREF_KEY_CATEGORY_YOUR_SATELLITE_PLAN) {
            @Override
            protected boolean isSatelliteEligible() {
                return true;
            }
        };
        mController.init(TEST_SUB_ID);
        mController.setCarrierRoamingNtnAvailability(true, false, -1);
        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        PreferenceCategory preferenceCategory = new PreferenceCategory(mContext);
        preferenceCategory.setKey(PREF_KEY_CATEGORY_YOUR_SATELLITE_PLAN);
        Preference preference = new Preference(mContext);
        preference.setKey(PREF_KEY_YOUR_SATELLITE_PLAN);
        Preference preferenceData = new Preference(mContext);
        preferenceData.setKey(PREF_KEY_YOUR_SATELLITE_DATA_PLAN);
        screen.addPreference(preferenceCategory);
        screen.addPreference(preference);
        screen.addPreference(preferenceData);

        mController.displayPreference(screen);

        assertThat(preference.getTitle().toString()).isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "title_have_satellite_plan"));
        assertThat(preferenceData.getTitle()).isEqualTo(null);
    }

    @Test
    public void
            displayPreference_eligibleUiAndDataConstrained_showSmsAndDataEligibleAccountState() {
        when(mSatelliteSettingsRepository.isSatelliteEntitlementSupported(anyInt())).thenReturn(
                true);
        mController = new SatelliteSettingAccountInfoController(mContext,
                PREF_KEY_CATEGORY_YOUR_SATELLITE_PLAN) {
            @Override
            protected boolean isSatelliteEligible() {
                return true;
            }
        };
        mController.init(TEST_SUB_ID);
        mController.setCarrierRoamingNtnAvailability(true, true,
                SATELLITE_DATA_SUPPORT_BANDWIDTH_CONSTRAINED);
        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        PreferenceCategory preferenceCategory = new PreferenceCategory(mContext);
        preferenceCategory.setKey(PREF_KEY_CATEGORY_YOUR_SATELLITE_PLAN);
        Preference preference = new Preference(mContext);
        preference.setKey(PREF_KEY_YOUR_SATELLITE_PLAN);
        Preference preferenceData = new Preference(mContext);
        preferenceData.setKey(PREF_KEY_YOUR_SATELLITE_DATA_PLAN);
        screen.addPreference(preferenceCategory);
        screen.addPreference(preference);
        screen.addPreference(preferenceData);

        mController.displayPreference(screen);

        assertThat(preference.getTitle().toString()).isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "title_have_satellite_plan"));
        assertThat(preferenceData.getTitle().toString()).isEqualTo(
                ResourcesUtils.getResourcesString(mContext,
                        "title_have_satellite_constrained_data_plan"));
    }

    @Test
    public void
            displayPreference_eligibleUiAndDataUnconstrained_showSmsAndDataEligibleAccountState() {
        when(mSatelliteSettingsRepository.isSatelliteEntitlementSupported(anyInt())).thenReturn(
                true);
        mController = new SatelliteSettingAccountInfoController(mContext,
                PREF_KEY_CATEGORY_YOUR_SATELLITE_PLAN) {
            @Override
            protected boolean isSatelliteEligible() {
                return true;
            }
        };
        mController.init(TEST_SUB_ID);
        mController.setCarrierRoamingNtnAvailability(true, true, SATELLITE_DATA_SUPPORT_ALL);
        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        PreferenceCategory preferenceCategory = new PreferenceCategory(mContext);
        preferenceCategory.setKey(PREF_KEY_CATEGORY_YOUR_SATELLITE_PLAN);
        Preference preference = new Preference(mContext);
        preference.setKey(PREF_KEY_YOUR_SATELLITE_PLAN);
        Preference preferenceData = new Preference(mContext);
        preferenceData.setKey(PREF_KEY_YOUR_SATELLITE_DATA_PLAN);
        screen.addPreference(preferenceCategory);
        screen.addPreference(preference);
        screen.addPreference(preferenceData);

        mController.displayPreference(screen);

        assertThat(preference.getTitle().toString()).isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "title_have_satellite_plan"));
        assertThat(preferenceData.getTitle().toString()).isEqualTo(
                ResourcesUtils.getResourcesString(mContext,
                        "title_have_satellite_unconstrained_data_plan"));
    }

    @Test
    public void displayPreference_showIneligibleUi_showSmsAccountState() {
        when(mSatelliteSettingsRepository.isSatelliteEntitlementSupported(anyInt())).thenReturn(
                true);
        mController = new SatelliteSettingAccountInfoController(mContext,
                PREF_KEY_CATEGORY_YOUR_SATELLITE_PLAN) {
            @Override
            protected boolean isSatelliteEligible() {
                return false;
            }
        };
        mController.init(TEST_SUB_ID);
        mController.setCarrierRoamingNtnAvailability(false, false, -1);
        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        PreferenceCategory preferenceCategory = new PreferenceCategory(mContext);
        preferenceCategory.setKey(PREF_KEY_CATEGORY_YOUR_SATELLITE_PLAN);
        Preference preference = new Preference(mContext);
        preference.setKey(PREF_KEY_YOUR_SATELLITE_PLAN);
        screen.addPreference(preferenceCategory);
        screen.addPreference(preference);

        mController.displayPreference(screen);

        assertThat(preference.getTitle().toString()).isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "title_no_satellite_plan"));
    }

    @Test
    public void displayPreference_showEligibleUiAndNoSummary_showSmsAccountStateOnly() {
        CarrierConfigRepository.Companion.resetForTest();
        CarrierConfigRepository.Companion.setStringForTest(TEST_SUB_ID,
                KEY_SATELLITE_INFORMATION_REDIRECT_URL_STRING, "A link");
        when(mSatelliteSettingsRepository.isSatelliteEntitlementSupported(anyInt())).thenReturn(
                true);
        mController = new SatelliteSettingAccountInfoController(mContext,
                PREF_KEY_CATEGORY_YOUR_SATELLITE_PLAN) {
            @Override
            protected boolean isSatelliteEligible() {
                return false;
            }
        };
        mController.init(TEST_SUB_ID);
        mController.setCarrierRoamingNtnAvailability(true, false, -1);
        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        PreferenceCategory preferenceCategory = new PreferenceCategory(mContext);
        preferenceCategory.setKey(PREF_KEY_CATEGORY_YOUR_SATELLITE_PLAN);
        Preference preference = new Preference(mContext);
        preference.setKey(PREF_KEY_YOUR_SATELLITE_PLAN);
        screen.addPreference(preferenceCategory);
        screen.addPreference(preference);

        mController.displayPreference(screen);

        assertThat(preference.getTitle().toString()).isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "title_no_satellite_plan"));
        assertThat(preference.getSummary()).isNotEqualTo(null);

        // Test non eligible UI when UI is updated by async call.
        mController = new SatelliteSettingAccountInfoController(mContext,
                PREF_KEY_CATEGORY_YOUR_SATELLITE_PLAN) {
            @Override
            protected boolean isSatelliteEligible() {
                return true;
            }
        };
        mController.setCarrierRoamingNtnAvailability(true, false, -1);

        mController.displayPreference(screen);

        assertThat(preference.getTitle().toString()).isEqualTo(
                ResourcesUtils.getResourcesString(mContext, "title_have_satellite_plan"));
        assertThat(preference.getSummary()).isEqualTo(null);
    }
}
