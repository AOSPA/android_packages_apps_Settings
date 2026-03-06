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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import android.platform.test.annotations.EnableFlags;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SimPinProtectionToggleControllerTest extends BaseSimProtectionControllerTest {
    private SimPinProtectionToggleController mController;

    @Before
    public void setUp() {
        super.setUp();

        mController = new SimPinProtectionToggleController(mContext, "key");
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
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
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
