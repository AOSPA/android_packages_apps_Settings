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

import static com.android.settings.network.telephony.satellite.SatelliteSettingFooterController
        .KEY_FOOTER_PREFERENCE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;
import android.telephony.TelephonyManager;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.network.telephony.CarrierConfigRepository;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.ResourcesUtils;
import com.android.settingslib.widget.FooterPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class SatelliteSettingFooterControllerTest {
    private static final int TEST_SUB_ID = 5;
    private static final String TEST_OPERATOR_NAME = "test_operator_name";

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private FooterPreference mFooterPreference;
    @Mock
    private SatelliteSettingsRepository mSatelliteSettingsRepository;

    private Context mContext;
    private SatelliteSettingFooterController mController;
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

        mController = new SatelliteSettingFooterController(mContext,
                KEY_FOOTER_PREFERENCE);
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        when(mTelephonyManager.getSimOperatorName(TEST_SUB_ID)).thenReturn(TEST_OPERATOR_NAME);
        CarrierConfigRepository.Companion.resetForTest();
        CarrierConfigRepository.Companion.setStringForTest(TEST_SUB_ID,
                KEY_SATELLITE_INFORMATION_REDIRECT_URL_STRING, "");
    }

    @Test
    public void displayPreferenceScreen_updateContent_hasBasicContent() {
        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        when(mFooterPreference.getKey()).thenReturn(KEY_FOOTER_PREFERENCE);
        screen.addPreference(mFooterPreference);
        mController.init(TEST_SUB_ID);

        mController.displayPreference(screen);

        ArgumentCaptor<CharSequence> summary = ArgumentCaptor.forClass(CharSequence.class);
        verify(mFooterPreference).setSummary(summary.capture());
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_0"))).isTrue();
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_1"))).isTrue();
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_2"))).isTrue();
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_3"))).isTrue();
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_4"))).isTrue();
    }


    @Test
    public void displayPreferenceScreen_manualTypeAndNoEntitlement() {
        when(mSatelliteSettingsRepository.getSatelliteNtnConnectType(anyInt())).thenReturn(
                CARRIER_ROAMING_NTN_CONNECT_MANUAL);
        when(mSatelliteSettingsRepository.isSatelliteEntitlementSupported(anyInt())).thenReturn(
                false);

        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        when(mFooterPreference.getKey()).thenReturn(KEY_FOOTER_PREFERENCE);
        screen.addPreference(mFooterPreference);
        mController.init(TEST_SUB_ID);

        mController.displayPreference(screen);

        ArgumentCaptor<CharSequence> summary = ArgumentCaptor.forClass(CharSequence.class);
        verify(mFooterPreference).setSummary(summary.capture());

        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_7", TEST_OPERATOR_NAME))).isTrue();
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_5"))).isFalse();
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_6"))).isFalse();
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_8"))).isTrue();
    }

    @Test
    public void displayPreferenceScreen_autoTypeAndNoEntitlement() {
        when(mSatelliteSettingsRepository.getSatelliteNtnConnectType(anyInt())).thenReturn(
                CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC);
        when(mSatelliteSettingsRepository.isSatelliteEntitlementSupported(anyInt())).thenReturn(
                false);

        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        when(mFooterPreference.getKey()).thenReturn(KEY_FOOTER_PREFERENCE);
        screen.addPreference(mFooterPreference);
        mController.init(TEST_SUB_ID);

        mController.displayPreference(screen);

        ArgumentCaptor<CharSequence> summary = ArgumentCaptor.forClass(CharSequence.class);
        verify(mFooterPreference).setSummary(summary.capture());

        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_7", TEST_OPERATOR_NAME))).isFalse();
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_5"))).isTrue();
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_6"))).isTrue();
    }

    @Test
    public void displayPreferenceScreen_autoTypeAndHasEntitlement() {
        when(mSatelliteSettingsRepository.getSatelliteNtnConnectType(anyInt())).thenReturn(
                CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC);
        when(mSatelliteSettingsRepository.isSatelliteEntitlementSupported(anyInt())).thenReturn(
                true);

        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        when(mFooterPreference.getKey()).thenReturn(KEY_FOOTER_PREFERENCE);
        screen.addPreference(mFooterPreference);
        mController.init(TEST_SUB_ID);

        mController.displayPreference(screen);

        ArgumentCaptor<CharSequence> summary = ArgumentCaptor.forClass(CharSequence.class);
        verify(mFooterPreference).setSummary(summary.capture());

        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_5"))).isTrue();
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_6"))).isFalse();
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_7", TEST_OPERATOR_NAME))).isTrue();
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_8"))).isTrue();
    }

    @Test
    public void displayPreferenceScreen_manualTypeAndHasEntitlement() {
        when(mSatelliteSettingsRepository.getSatelliteNtnConnectType(anyInt())).thenReturn(
                CARRIER_ROAMING_NTN_CONNECT_MANUAL);
        when(mSatelliteSettingsRepository.isSatelliteEntitlementSupported(anyInt())).thenReturn(
                true);

        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        when(mFooterPreference.getKey()).thenReturn(KEY_FOOTER_PREFERENCE);
        screen.addPreference(mFooterPreference);
        mController.init(TEST_SUB_ID);

        mController.displayPreference(screen);

        ArgumentCaptor<CharSequence> summary = ArgumentCaptor.forClass(CharSequence.class);
        verify(mFooterPreference).setSummary(summary.capture());

        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_5"))).isFalse();
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_6"))).isFalse();
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_7", TEST_OPERATOR_NAME))).isTrue();
    }

    @Test
    public void displayPreferenceScreen_hybridTypeAndEligible() {
        when(mSatelliteSettingsRepository.getSatelliteNtnConnectType(anyInt())).thenReturn(
                CARRIER_ROAMING_NTN_CONNECT_HYBRID);
        when(mSatelliteSettingsRepository.isSatelliteEntitlementSupported(anyInt())).thenReturn(
                true);

        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        when(mFooterPreference.getKey()).thenReturn(KEY_FOOTER_PREFERENCE);
        screen.addPreference(mFooterPreference);
        mController = new SatelliteSettingFooterController(mContext,
                KEY_FOOTER_PREFERENCE) {
            @Override
            protected boolean isSatelliteEligible() {
                return true;
            }
        };
        mController.init(TEST_SUB_ID);
        mController.setCarrierRoamingNtnAvailability(true, true, 0);

        mController.displayPreference(screen);

        ArgumentCaptor<CharSequence> summary = ArgumentCaptor.forClass(CharSequence.class);
        verify(mFooterPreference).setSummary(summary.capture());

        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_5"))).isTrue();
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_7", TEST_OPERATOR_NAME))).isTrue();
    }

    @Test
    public void displayPreferenceScreen_hybridTypeAndIneligible() {
        when(mSatelliteSettingsRepository.getSatelliteNtnConnectType(anyInt())).thenReturn(
                CARRIER_ROAMING_NTN_CONNECT_HYBRID);
        when(mSatelliteSettingsRepository.isSatelliteEntitlementSupported(anyInt())).thenReturn(
                true);

        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        when(mFooterPreference.getKey()).thenReturn(KEY_FOOTER_PREFERENCE);
        screen.addPreference(mFooterPreference);
        mController = new SatelliteSettingFooterController(mContext,
                KEY_FOOTER_PREFERENCE) {
            @Override
            protected boolean isSatelliteEligible() {
                return false;
            }
        };
        mController.init(TEST_SUB_ID);
        mController.setCarrierRoamingNtnAvailability(true, true, 0);

        mController.displayPreference(screen);

        ArgumentCaptor<CharSequence> summary = ArgumentCaptor.forClass(CharSequence.class);
        verify(mFooterPreference).setSummary(summary.capture());

        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_5"))).isFalse();
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_7", TEST_OPERATOR_NAME))).isTrue();
    }

    @Test
    public void displayPreferenceScreen_hybridTypeAndNoEntitlement() {
        // Verifies footer content for HYBRID connection type without entitlement support.
        // In this case, section 7 and 8 should be displayed.
        when(mSatelliteSettingsRepository.getSatelliteNtnConnectType(anyInt())).thenReturn(
                CARRIER_ROAMING_NTN_CONNECT_HYBRID);
        when(mSatelliteSettingsRepository.isSatelliteEntitlementSupported(anyInt())).thenReturn(
                false);

        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        when(mFooterPreference.getKey()).thenReturn(KEY_FOOTER_PREFERENCE);
        screen.addPreference(mFooterPreference);
        mController.init(TEST_SUB_ID);

        // Trigger preference display
        mController.displayPreference(screen);

        // Capture and verify the summary
        ArgumentCaptor<CharSequence> summary = ArgumentCaptor.forClass(CharSequence.class);
        verify(mFooterPreference).setSummary(summary.capture());

        // Assert that the correct sections are present or absent
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_7", TEST_OPERATOR_NAME))).isTrue();
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_8"))).isTrue();
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_5"))).isFalse();
        assertThat(
                summary.getValue().toString().contains(ResourcesUtils.getResourcesString(mContext,
                        "satellite_footer_content_section_6"))).isFalse();
    }
}
