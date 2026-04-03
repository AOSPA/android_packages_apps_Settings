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
import static android.security.Flags.FLAG_ENABLE_AUTO_SIM_PIN_UI;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.settings.security.SimPinProtectionToggleController.EnrollmentState.EXTRA_ENROLLMENT_STATE_VALUE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.os.Bundle;
import android.os.Looper;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.platform.test.flag.junit.SetFlagsRule;
import android.telephony.PinResult;
import android.telephony.SubscriptionInfo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settingslib.PrimarySwitchPreference;

import com.google.common.util.concurrent.Futures;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.androidx.fragment.FragmentController;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAlertDialogCompat.class})
public class SimPinProtectionToggleControllerTest extends BaseSimProtectionControllerTest {
    private static final String PREFERENCE_KEY = "sim_pin_auto_management_toggle";
    private static final String MANUAL_PIN_ONLY_KEY = "sim_pin_manual_management_only_toggle";
    private static final int SLOT_INDEX = 1;
    private static final int SUBSCRIPTION_ID = 3;

    @Rule
    public final CheckFlagsRule checkFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock
    private AutoManagedSimPinHelper mAutoManagedSimPinHelper;
    @Mock
    private PreferenceScreen mScreen;
    private SwitchPreferenceCompat mSimPinTogglePreference;
    private PrimarySwitchPreference mPrimarySwitchPreference;

    private SimPinProtectionToggleController mController;
    private SimPinProtectionToggleController mControllerForAutoPinManagement;
    private BaseSimPinFragment mParent;

    public static class SimPinTestFragment extends BaseSimPinFragment {
        @Override
        protected int getPreferenceScreenResId() {
            return 0;
        }
    }

    @Before
    public void setUp() {
        super.setUp();
        ShadowAlertDialogCompat.reset();

        mController = new SimPinProtectionToggleController(mContext, MANUAL_PIN_ONLY_KEY,
                mAutoManagedSimPinHelper);
        mControllerForAutoPinManagement = new SimPinProtectionToggleController(mContext,
                PREFERENCE_KEY, mAutoManagedSimPinHelper);
        mSimPinTogglePreference = new SwitchPreferenceCompat(mContext);
        mPrimarySwitchPreference = new PrimarySwitchPreference(mContext);
        when(mScreen.findPreference(MANUAL_PIN_ONLY_KEY)).thenReturn(mSimPinTogglePreference);
        when(mScreen.findPreference(PREFERENCE_KEY)).thenReturn(mPrimarySwitchPreference);

        mParent = new SimPinTestFragment();
        FragmentController.setupFragment(
                mParent, FragmentActivity.class, /* containerViewId= */ 0, /* bundle= */ null);

        when(mAutoManagedSimPinHelper.getSubscriptionIdForSlot(eq(SLOT_INDEX))).thenReturn(
                SUBSCRIPTION_ID);
        mController.setSlotIndex(SLOT_INDEX);
        mControllerForAutoPinManagement.setSlotIndex(SLOT_INDEX);
    }

    @Test
    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    public void isChecked_iccLockEnabled_manual() {
        when(mAutoManagedSimPinHelper.isIccLockEnabled(eq(SUBSCRIPTION_ID))).thenReturn(true);
        when(mAutoManagedSimPinHelper.isPinAutoManagedForSubscription(
                eq(SUBSCRIPTION_ID))).thenReturn(false);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    public void isChecked_iccLockEnabled_platformManaged() {
        when(mAutoManagedSimPinHelper.isIccLockEnabled(eq(SUBSCRIPTION_ID))).thenReturn(false);
        when(mAutoManagedSimPinHelper.isPinAutoManagedForSubscription(
                eq(SUBSCRIPTION_ID))).thenReturn(true);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    public void isChecked_iccLockDisabled() {
        when(mAutoManagedSimPinHelper.isIccLockEnabled(eq(SUBSCRIPTION_ID))).thenReturn(false);
        when(mAutoManagedSimPinHelper.isPinAutoManagedForSubscription(
                eq(SUBSCRIPTION_ID))).thenReturn(false);
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    @DisableFlags(FLAG_ENABLE_AUTO_SIM_PIN_UI)
    public void getAvailabilityStatus_eSim() {
        SubscriptionInfo.Builder builder = new SubscriptionInfo.Builder(mSubscriptionInfo);
        builder.setEmbedded(true);

        when(mSubscriptionManager.getActiveSubscriptionInfo(anyInt())).thenReturn(builder.build());
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
        assertThat(mControllerForAutoPinManagement.getAvailabilityStatus()).isEqualTo(
                CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    @DisableFlags(FLAG_ENABLE_AUTO_SIM_PIN_UI)
    public void getAvailabilityStatus_pSim() {
        when(mSubscriptionManager.getActiveSubscriptionInfo(anyInt())).thenReturn(
                mSubscriptionInfo);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
        assertThat(mControllerForAutoPinManagement.getAvailabilityStatus()).isEqualTo(
                CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    @EnableFlags({FLAG_AUTO_SIM_PIN_MANAGEMENT, FLAG_ENABLE_AUTO_SIM_PIN_UI})
    public void getAvailabilityStatus_eSim_autoManagedPinEnabled() {
        SubscriptionInfo.Builder builder = new SubscriptionInfo.Builder(mSubscriptionInfo);
        builder.setEmbedded(true);

        when(mSubscriptionManager.getActiveSubscriptionInfo(anyInt())).thenReturn(builder.build());
        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
        assertThat(mControllerForAutoPinManagement.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    @EnableFlags({FLAG_AUTO_SIM_PIN_MANAGEMENT, FLAG_ENABLE_AUTO_SIM_PIN_UI})
    public void getAvailabilityStatus_pSim_autoManagedPinEnabled() {
        when(mSubscriptionManager.getActiveSubscriptionInfo(anyInt())).thenReturn(
                mSubscriptionInfo);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
        assertThat(mControllerForAutoPinManagement.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    public void getSummary_platformManagedPin() {
        when(mAutoManagedSimPinHelper.isPinAutoManagedForSubscription(
                eq(SUBSCRIPTION_ID))).thenReturn(true);

        assertThat(mController.getSummary()).isEqualTo(mContext.getResources().getString(
                R.string.sim_protection_mode_protected_by_platform));
    }

    @Test
    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    public void getSummary_manuallyManagedPin() {
        when(mAutoManagedSimPinHelper.isPinAutoManagedForSubscription(
                eq(SUBSCRIPTION_ID))).thenReturn(false);
        when(mAutoManagedSimPinHelper.isIccLockEnabled(eq(SUBSCRIPTION_ID))).thenReturn(true);

        assertThat(mController.getSummary()).isEqualTo(mContext.getResources().getString(
                R.string.sim_protection_mode_manually_managed));
    }

    @Test
    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    public void getSummary_noPin() {
        when(mAutoManagedSimPinHelper.isPinAutoManagedForSubscription(
                eq(SUBSCRIPTION_ID))).thenReturn(false);
        when(mAutoManagedSimPinHelper.isIccLockEnabled(eq(SUBSCRIPTION_ID))).thenReturn(false);

        assertThat(mController.getSummary()).isEqualTo(mContext.getResources().getString(
                R.string.sim_choose_protection_mode_title));
    }

    @Test
    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    @DisableFlags(FLAG_ENABLE_AUTO_SIM_PIN_UI)
    public void onPinEntered_manualEnrollment_correctPin() {
        configureControllerForUiTest();

        when(mAutoManagedSimPinHelper.setIccLockState(eq("1234"), eq(true), anyInt())).thenReturn(
                Futures.immediateFuture(new PinResult(PinResult.PIN_RESULT_TYPE_SUCCESS, 0)));

        mController.onPinEntered("1234");
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mSimPinTogglePreference.isChecked()).isTrue();
        assertThat(mSimPinTogglePreference.getSummary()).isEqualTo(
                mContext.getResources().getString(R.string.sim_protection_mode_manually_managed));
    }

    @Test
    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    @DisableFlags(FLAG_ENABLE_AUTO_SIM_PIN_UI)
    public void onPinEntered_manualEnrollment_incorrectPin() {
        configureControllerForUiTest();

        when(mAutoManagedSimPinHelper.setIccLockState(eq("1234"), eq(true), anyInt())).thenReturn(
                Futures.immediateFuture(new PinResult(PinResult.PIN_RESULT_TYPE_INCORRECT, 2)));

        mController.onPinEntered("1234");
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mSimPinTogglePreference.isChecked()).isFalse();
        assertThat(mSimPinTogglePreference.getSummary()).isEqualTo(
                mContext.getResources().getString(R.string.sim_pin_enable_failed));

        AlertDialog alertDialog = getLatestAlertDialog();
        assertThat(alertDialog.isShowing()).isTrue();
        ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(alertDialog);
        assertThat(shadowDialog.getMessage()).isEqualTo(
                mContext.getResources().getString(R.string.enter_current_sim_pin_after_mismatch,
                        2));
        assertThat(shadowDialog.getTitle()).isEqualTo(
                mContext.getResources().getString(R.string.provide_current_sim_pin_title));
    }

    private void configureControllerForUiTest() {
        Bundle stateBundle = getBundleForState(
                SimPinProtectionToggleController.EnrollmentState.ENROLL_TO_MANUAL_PIN_MANAGEMENT);
        mController.setFragment(mParent);
        mController.loadEnrollmentState(stateBundle);
        mController.displayPreference(mScreen);
    }

    @NonNull
    private static Bundle getBundleForState(
            SimPinProtectionToggleController.EnrollmentState state) {
        Bundle stateBundle = new Bundle();
        stateBundle.putInt(EXTRA_ENROLLMENT_STATE_VALUE, state.getIntValue());
        return stateBundle;
    }

    private AlertDialog getLatestAlertDialog() {
        ShadowLooper.idleMainLooper();
        AlertDialog shadowAlertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(shadowAlertDialog).isNotNull();
        return shadowAlertDialog;
    }
}
