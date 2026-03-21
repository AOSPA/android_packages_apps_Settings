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
import static com.android.settings.security.SimPinProtectionToggleController.EnrollmentState.EXTRA_ENROLLMENT_STATE_VALUE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.os.Bundle;
import android.os.Looper;
import android.platform.test.annotations.EnableFlags;
import android.telephony.PinResult;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

import com.google.common.util.concurrent.Futures;

import org.junit.Before;
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
    @Mock
    private AutoManagedSimPinHelper mAutoManagedSimPinHelper;
    @Mock
    private PreferenceScreen mScreen;
    private SwitchPreferenceCompat mSimPinTogglePreference;

    private SimPinProtectionToggleController mController;
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

        mController = new SimPinProtectionToggleController(mContext, PREFERENCE_KEY);
        mSimPinTogglePreference = new SwitchPreferenceCompat(mContext);
        when(mScreen.findPreference(PREFERENCE_KEY)).thenReturn(mSimPinTogglePreference);

        mParent = new SimPinTestFragment();
        FragmentController.setupFragment(
                mParent, FragmentActivity.class, /* containerViewId= */ 0, /* bundle= */ null);
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

    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    @Test
    public void onPinEntered_manualEnrollment_correctPin() {
        SimPinProtectionToggleController controller =
                createControllerWithMockHelper();

        when(mAutoManagedSimPinHelper.setIccLockState(eq("1234"), eq(true), anyInt())).thenReturn(
                Futures.immediateFuture(new PinResult(PinResult.PIN_RESULT_TYPE_SUCCESS, 0)));

        controller.onPinEntered("1234");
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mSimPinTogglePreference.isChecked()).isTrue();
        assertThat(mSimPinTogglePreference.getSummary()).isEqualTo(
                mContext.getResources().getString(R.string.sim_protection_mode_manually_managed));
    }

    @EnableFlags(FLAG_AUTO_SIM_PIN_MANAGEMENT)
    @Test
    public void onPinEntered_manualEnrollment_incorrectPin() {
        SimPinProtectionToggleController controller =
                createControllerWithMockHelper();

        when(mAutoManagedSimPinHelper.setIccLockState(eq("1234"), eq(true), anyInt())).thenReturn(
                Futures.immediateFuture(new PinResult(PinResult.PIN_RESULT_TYPE_INCORRECT, 2)));

        controller.onPinEntered("1234");
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

    @NonNull
    private SimPinProtectionToggleController createControllerWithMockHelper() {
        SimPinProtectionToggleController controller = new SimPinProtectionToggleController(mContext,
                PREFERENCE_KEY, mAutoManagedSimPinHelper);

        Bundle stateBundle = getBundleForState(
                SimPinProtectionToggleController.EnrollmentState.ENROLL_TO_MANUAL_PIN_MANAGEMENT);
        controller.setFragment(mParent);
        controller.loadEnrollmentState(stateBundle);
        controller.displayPreference(mScreen);
        return controller;
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
