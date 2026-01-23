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

package com.android.settings.security;

import static android.security.Flags.FLAG_AUTO_SIM_PIN_MANAGEMENT;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.KeyguardManager;
import android.content.Context;
import android.platform.test.annotations.EnableFlags;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AutoSimPinManagementControllerTest {
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private KeyguardManager mKeyguardManager;
    private SubscriptionInfo mSubscriptionInfo;
    private Context mContext;
    private final int mSubscriptionId = 1234;
    private List<SubscriptionInfo> mSubscriptionInfoList;
    private AutoSimPinManagementController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application.getApplicationContext());

        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);
        when(mContext.getSystemService(KeyguardManager.class)).thenReturn(mKeyguardManager);

        SubscriptionInfo.Builder builder = new SubscriptionInfo.Builder();
        builder.setDisplayName("Test");
        builder.setCarrierName("fake carrier name");
        builder.setId(mSubscriptionId);
        builder.setSimSlotIndex(1);
        builder.setEmbedded(false);
        mSubscriptionInfo = builder.build();
        mSubscriptionInfoList = new ArrayList<>();
        mSubscriptionInfoList.add(mSubscriptionInfo);
        when(mSubscriptionManager.getAvailableSubscriptionInfoList()).thenReturn(
                mSubscriptionInfoList);

        when(mSubscriptionManager.setDisplayName(any(), anyInt(), anyInt())).thenReturn(0);
        when(mSubscriptionManager.getEnabledSubscriptionId(eq(1))).thenReturn(mSubscriptionId);
        when(mTelephonyManager.createForSubscriptionId(anyInt())).thenReturn(
                mTelephonyManager);
        when(mKeyguardManager.isDeviceSecure()).thenReturn(true);

        mController = new AutoSimPinManagementController(mContext, "key");
    }

    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    @Test
    public void isChecked_iccLockEnabled_manual() {
        when(mTelephonyManager.isIccLockEnabled()).thenReturn(true);
        when(mTelephonyManager.getSimAutoPinManagementEnrollmentStatus()).thenReturn(
                TelephonyManager.SIM_PIN_ENROLLMENT_STATUS_MANUALLY_MANAGED);
        assertThat(mController.isChecked()).isTrue();
    }

    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    @Test
    public void isChecked_iccLockEnabled_platformManaged() {
        when(mTelephonyManager.isIccLockEnabled()).thenReturn(false);
        when(mTelephonyManager.getSimAutoPinManagementEnrollmentStatus()).thenReturn(
                TelephonyManager.SIM_PIN_ENROLLMENT_STATUS_PLATFORM_MANAGED);
        assertThat(mController.isChecked()).isTrue();
    }

    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    @Test
    public void isChecked_iccLockDisabled() {
        when(mTelephonyManager.isIccLockEnabled()).thenReturn(false);
        when(mTelephonyManager.getSimAutoPinManagementEnrollmentStatus()).thenReturn(
                TelephonyManager.SIM_PIN_ENROLLMENT_STATUS_MANUALLY_MANAGED);
        assertThat(mController.isChecked()).isFalse();
    }

    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    @Test
    public void getAvailabilityStatus_eSim() {
        SubscriptionInfo.Builder builder = new SubscriptionInfo.Builder(mSubscriptionInfo);
        builder.setEmbedded(true);

        when(mSubscriptionManager.getActiveSubscriptionInfo(anyInt())).thenReturn(builder.build());
        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    @Test
    public void getAvailabilityStatus_pSim() {
        when(mSubscriptionManager.getActiveSubscriptionInfo(anyInt())).thenReturn(
                mSubscriptionInfo);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    @Test
    public void getSummary_platformManagedPin() {
        when(mTelephonyManager.getSimAutoPinManagementEnrollmentStatus()).thenReturn(
                TelephonyManager.SIM_PIN_ENROLLMENT_STATUS_PLATFORM_MANAGED);

        assertThat(mController.getSummary()).isEqualTo(mContext.getResources().getString(
                R.string.sim_protection_mode_protected_by_platform));
    }

    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    @Test
    public void getSummary_manuallyManagedPin() {
        when(mTelephonyManager.getSimAutoPinManagementEnrollmentStatus()).thenReturn(
                TelephonyManager.SIM_PIN_ENROLLMENT_STATUS_MANUALLY_MANAGED);
        when(mTelephonyManager.isIccLockEnabled()).thenReturn(true);

        assertThat(mController.getSummary()).isEqualTo(mContext.getResources().getString(
                R.string.sim_protection_mode_manually_managed));
    }

    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    @Test
    public void getSummary_noPin() {
        when(mTelephonyManager.getSimAutoPinManagementEnrollmentStatus()).thenReturn(
                TelephonyManager.SIM_PIN_ENROLLMENT_STATUS_MANUALLY_MANAGED);
        when(mTelephonyManager.isIccLockEnabled()).thenReturn(false);

        assertThat(mController.getSummary()).isEqualTo(mContext.getResources().getString(
                R.string.sim_choose_protection_mode_title));
    }
}
