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
import static com.android.settings.core.BasePreferenceController.DISABLED_DEPENDENT_SETTING;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.platform.test.annotations.EnableFlags;
import android.telephony.TelephonyManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ChangeSimPinPreferenceControllerTest extends BaseSimProtectionControllerTest {
    private ChangeSimPinPreferenceController mController;

    @Before
    public void setUp() {
        super.setUp();
        mController = new ChangeSimPinPreferenceController(mContext, "change_sim_pin_key");
    }

    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    @Test
    public void getAvailabilityStatus_simPinTurnedOn() {
        when(mTelephonyManager.isIccLockEnabled()).thenReturn(true);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    @Test
    public void getAvailabilityStatus_simPinTurnedOff() {
        when(mTelephonyManager.isIccLockEnabled()).thenReturn(false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    @Test
    public void getAvailabilityStatus_simPinTurnedOnAndPlatformManaged() {
        when(mTelephonyManager.isIccLockEnabled()).thenReturn(true);
        when(mTelephonyManager.getSimAutoPinManagementEnrollmentStatus()).thenReturn(
                TelephonyManager.SIM_PIN_ENROLLMENT_STATUS_PLATFORM_MANAGED);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }
}
