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
import static com.android.settings.core.BasePreferenceController.DISABLED_DEPENDENT_SETTING;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.telephony.SubscriptionManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ChangeSimPinPreferenceControllerTest extends BaseSimProtectionControllerTest {
    private static final int SLOT_INDEX = 1;
    private static final int SUBSCRIPTION_ID = 3;

    @Rule
    public final CheckFlagsRule checkFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Mock
    private AutoManagedSimPinHelper mAutoManagedSimPinHelper;

    private ChangeSimPinPreferenceController mController;

    @Before
    public void setUp() {
        super.setUp();
        mController = new ChangeSimPinPreferenceController(mContext, "change_sim_pin_key",
                mAutoManagedSimPinHelper);
    }

    private void configureSlot() {
        when(mAutoManagedSimPinHelper.getSubscriptionIdForSlot(eq(SLOT_INDEX))).thenReturn(
                SUBSCRIPTION_ID);
        mController.setSlotIndex(SLOT_INDEX);
    }

    @Test
    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    public void getAvailabilityStatus_simPinTurnedOn() {
        configureSlot();
        when(mAutoManagedSimPinHelper.isIccLockEnabled(eq(SUBSCRIPTION_ID))).thenReturn(true);
        when(mAutoManagedSimPinHelper.isPinAutoManagedForSubscription(
                eq(SUBSCRIPTION_ID))).thenReturn(false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    public void getAvailabilityStatus_simPinTurnedOff() {
        configureSlot();
        when(mAutoManagedSimPinHelper.isIccLockEnabled(eq(SUBSCRIPTION_ID))).thenReturn(false);
        when(mAutoManagedSimPinHelper.isPinAutoManagedForSubscription(
                eq(SUBSCRIPTION_ID))).thenReturn(false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    public void getAvailabilityStatus_simPinTurnedOnAndPlatformManaged() {
        configureSlot();
        when(mAutoManagedSimPinHelper.isIccLockEnabled(eq(SUBSCRIPTION_ID))).thenReturn(true);
        when(mAutoManagedSimPinHelper.isPinAutoManagedForSubscription(
                eq(SUBSCRIPTION_ID))).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    public void getAvailabilityStatus_slotNotConfigured() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    public void getAvailabilityStatus_invalidSubscriptionId() {
        when(mAutoManagedSimPinHelper.getSubscriptionIdForSlot(eq(SLOT_INDEX))).thenReturn(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        mController.setSlotIndex(SLOT_INDEX);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }
}
