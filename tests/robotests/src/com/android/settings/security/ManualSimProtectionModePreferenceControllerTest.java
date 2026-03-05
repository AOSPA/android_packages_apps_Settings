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

import static org.mockito.Mockito.when;

import android.platform.test.annotations.EnableFlags;
import android.telephony.TelephonyManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ManualSimProtectionModePreferenceControllerTest extends
        BaseSimProtectionControllerTest {
    private ManualSimProtectionModePreferenceController mController;
    @Before
    public void setUp() {
        super.setUp();
        mController = new ManualSimProtectionModePreferenceController(mContext,
                "sim_protection_mode_manual_key");
    }

    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    @Test
    public void getAvailabilityStatus_deviceIsSecure() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    @Test
    public void getAvailabilityStatus_deviceIsNotSecure() {
        when(mKeyguardManager.isDeviceSecure()).thenReturn(false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    @Test
    public void isModeCurrentlyActive_platformManaged() {
        when(mTelephonyManager.isIccLockEnabled()).thenReturn(true);
        when(mTelephonyManager.getSimAutoPinManagementEnrollmentStatus()).thenReturn(
                TelephonyManager.SIM_PIN_ENROLLMENT_STATUS_PLATFORM_MANAGED);

        assertThat(mController.isModeCurrentlyActive()).isFalse();
    }

    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    @Test
    public void isModeCurrentlyActive_manuallyManaged() {
        when(mTelephonyManager.isIccLockEnabled()).thenReturn(true);
        when(mTelephonyManager.getSimAutoPinManagementEnrollmentStatus()).thenReturn(
                TelephonyManager.SIM_PIN_ENROLLMENT_STATUS_MANUALLY_MANAGED);

        assertThat(mController.isModeCurrentlyActive()).isTrue();
    }

    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    @Test
    public void isModeCurrentlyActive_manuallyManagedAndIccLockOff() {
        when(mTelephonyManager.isIccLockEnabled()).thenReturn(false);
        when(mTelephonyManager.getSimAutoPinManagementEnrollmentStatus()).thenReturn(
                TelephonyManager.SIM_PIN_ENROLLMENT_STATUS_MANUALLY_MANAGED);

        assertThat(mController.isModeCurrentlyActive()).isFalse();
    }
}
