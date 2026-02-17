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

import android.app.KeyguardManager;
import android.content.Context;
import android.hardware.biometrics.BiometricPrompt;
import android.os.OutcomeReceiver;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.PrimarySwitchPreference;

import java.util.List;

/**
 * Controller for the automatic SIM card PIN management preference.
 *
 * It is responsible for enabling/disabling the preference (in case the SIM PIN is manually
 * managed), triggering the authentication dialog for enrollment/unenrollment and reporting
 * the correct status for the toggle.
 */
public class AutoSimPinManagementController extends TogglePreferenceController {
    private static final String TAG = "AutoManagedSimPin";

    private TelephonyManager mTelephonyManager;
    private int mSubId;

    @Nullable
    private PrimarySwitchPreference mPreference = null;

    @Nullable
    private Fragment mFragment;
    private AutoManagedSimPinHelper mAutoManagedSimPinHelper;

    public AutoSimPinManagementController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
        SubscriptionManager subscriptionManager = context.getSystemService(
                SubscriptionManager.class);

        // TODO(b/476046816): Support multiple physical SIM card slots in the UI.
        mSubId = subscriptionManager.getEnabledSubscriptionId(
                getFirstPhysicalSimSlot(subscriptionManager));
        mAutoManagedSimPinHelper = new AutoManagedSimPinHelper(mContext);
    }

    private int getFirstPhysicalSimSlot(SubscriptionManager subscriptionManager) {
        final List<SubscriptionInfo> subInfoList =
                subscriptionManager.getActiveSubscriptionInfoList();
        if (subInfoList == null) {
            return 0;
        }

        for (SubscriptionInfo subInfo : subInfoList) {
            if (subInfo.isActive() && !subInfo.isEmbedded()) {
                return subInfo.getSimSlotIndex();
            }
        }

        return 0;
    }

    @Override
    public boolean isChecked() {
        boolean isPlatformManaged = mAutoManagedSimPinHelper.isPinAutoManagedForSubscription(
                mSubId);

        boolean isIccLockEnabled = mTelephonyManager.createForSubscriptionId(
                mSubId).isIccLockEnabled();

        return isPlatformManaged || isIccLockEnabled;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (isChecked) {
            showAuthenticationDialogSimEnrollment();
        } else {
            showAuthenticationDialogSimUnenrollment(
                    mTelephonyManager.createForSubscriptionId(mSubId));
        }

        return false;
    }

    @Override
    public int getAvailabilityStatus() {
        boolean isFlagEnabled = android.security.Flags.autoSimPinManagement();
        SubscriptionManager sm = mContext.getSystemService(SubscriptionManager.class);
        boolean isEmbeddedSim = sm.getActiveSubscriptionInfo(mSubId).isEmbedded();

        return !isEmbeddedSim && isFlagEnabled ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        int summaryId = R.string.sim_choose_protection_mode_title;
        if (mAutoManagedSimPinHelper.isPinAutoManagedForSubscription(mSubId)) {
            summaryId = R.string.sim_protection_mode_protected_by_platform;
        } else if (mTelephonyManager.createForSubscriptionId(mSubId).isIccLockEnabled()) {
            summaryId = R.string.sim_protection_mode_manually_managed;
        } else if (!isDeviceSecure()) {
            summaryId = R.string.sim_protection_mode_lskf_required;
        }
        return mContext.getResources().getString(summaryId);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_security;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());

        mPreference.setEnabled(isDeviceSecure());
        mPreference.setSummary(getSummary());
    }

    private boolean isDeviceSecure() {
        KeyguardManager keyguardManager = mContext.getSystemService(KeyguardManager.class);
        boolean isDeviceSecure = keyguardManager != null ? keyguardManager.isDeviceSecure() : false;
        return isDeviceSecure;
    }

    public void setFragment(Fragment fragment) {
        mFragment = fragment;
    }

    private class EnrollmentResultReceiver implements
            OutcomeReceiver<String, TelephonyManager.SimAutoPinManagementException> {

        @Override
        public void onResult(String generatedPin) {
            setPreferenceState(true, getSummary());

            DisplaySimPinDialogFragment df = DisplaySimPinDialogFragment.newInstance(true,
                    generatedPin);

            if (mFragment != null) {
                df.showNow(mFragment.getChildFragmentManager(), "PinShow");
            }
        }

        @Override
        public void onError(
                @NonNull TelephonyManager.SimAutoPinManagementException error) {
            OutcomeReceiver.super.onError(error);
            Log.w(TAG, "Error enrolling: " + error.getErrorCode());
            setPreferenceState(false, mContext.getString(R.string.sim_enrollment_failed));
        }
    }

    private void setPreferenceState(boolean isChecked, @Nullable CharSequence summary) {
        if (mPreference == null) {
            return;
        }
        mPreference.setChecked(isChecked);
        mPreference.setSummary(summary);
    }

    private class UnenrollmentResultReceiver implements
            OutcomeReceiver<Void, TelephonyManager.SimAutoPinManagementException> {
        @Override
        public void onResult(Void result) {
            setPreferenceState(false, getSummary());
            Log.d(TAG, "Unenrollment successful.");
        }

        @Override
        public void onError(
                @NonNull TelephonyManager.SimAutoPinManagementException error) {
            OutcomeReceiver.super.onError(error);
            Log.w(TAG, "Error unenrolling: " + error.getErrorCode());
        }
    }

    private void showPinEntryDialog() {
        EnterCurrentSimPinDialogFragment df = EnterCurrentSimPinDialogFragment.newInstance();

        if (mFragment != null) {
            df.showNow(mFragment.getChildFragmentManager(), "CurrentPin");
        }
    }

    /**
     * To be called by the SIM PIN entry dialog to proceed with enrollment after the user
     * has provided the PIN.
     *
     * @param pin The current SIM PIN as provided by the user.
     */
    public void tryEnrollingToAutoPinManagement(String pin) {
        TelephonyManager tm = mTelephonyManager.createForSubscriptionId(mSubId);
        tm.enrollSimInAutoPinManagement(pin, mContext.getMainExecutor(),
                new EnrollmentResultReceiver());
    }

    /**
     * To be called by the SIM PIN entry dialog when the user cancels the enrollment.
     */
    public void cancelEnrollment() {
        setPreferenceState(false, getSummary());
    }

    private void showAuthenticationDialogSimEnrollment() {
        final BiometricPrompt.AuthenticationCallback authenticationCallback =
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            BiometricPrompt.AuthenticationResult result) {
                        showPinEntryDialog();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        Log.w(TAG, "Authentication error for enrolling, error code " + errorCode
                                + " message: " + errString);
                        setPreferenceState(false, getSummary());
                    }
                };

        mAutoManagedSimPinHelper.showAuthenticationPromptForCallback(authenticationCallback);
    }

    private void showAuthenticationDialogSimUnenrollment(TelephonyManager telephonyManager) {
        final BiometricPrompt.AuthenticationCallback authenticationCallback =
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            BiometricPrompt.AuthenticationResult result) {
                        telephonyManager.unenrollSimFromAutoPinManagement(
                                mContext.getMainExecutor(),
                                new UnenrollmentResultReceiver());
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        Log.w(TAG, "Authentication error for unenrollment, error code "
                                + errorCode + " message: " + errString);
                    }
                };

        mAutoManagedSimPinHelper.showAuthenticationPromptForCallback(authenticationCallback);
    }
}
