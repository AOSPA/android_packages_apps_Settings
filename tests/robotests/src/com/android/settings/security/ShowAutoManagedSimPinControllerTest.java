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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.platform.test.annotations.EnableFlags;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ShowAutoManagedSimPinControllerTest {
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    private Context mContext;
    private final int mSubscriptionId = 1234;


    private ShowAutoManagedSimPinController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application.getApplicationContext());

        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);

        when(mSubscriptionManager.getEnabledSubscriptionId(eq(0))).thenReturn(mSubscriptionId);
        when(mTelephonyManager.createForSubscriptionId(anyInt())).thenReturn(
                mTelephonyManager);

        mController = new ShowAutoManagedSimPinController(mContext, "key");
    }

    @Test
    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    public void getAvailabilityStatus_pinManuallyManaged() {
        when(mTelephonyManager.getSimAutoPinManagementEnrollmentStatus()).thenReturn(
                TelephonyManager.SIM_PIN_ENROLLMENT_STATUS_MANUALLY_MANAGED);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    public void getAvailabilityStatus_pinPlatformManaged() {
        when(mTelephonyManager.getSimAutoPinManagementEnrollmentStatus()).thenReturn(
                TelephonyManager.SIM_PIN_ENROLLMENT_STATUS_PLATFORM_MANAGED);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }
}
