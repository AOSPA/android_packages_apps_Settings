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
import android.telephony.PinResult;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.settingslib.widget.SelectorWithWidgetPreference;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Controller for the "Automatic (Android-managed)" sim protection mode preference.
 */
public class AutomaticSimProtectionModePreferenceController extends
        BaseSimProtectionModePreferenceController implements
        EnterSimPinDialogFragment.SimPinEntryListener {
    private static final int STATE_ENROLLING_INTO_AUTO_PIN_MANAGEMENT = 0;
    private static final int STATE_DISABLING_ICC_LOCK = 1;

    private int mState = STATE_ENROLLING_INTO_AUTO_PIN_MANAGEMENT;

    public AutomaticSimProtectionModePreferenceController(
            @NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void onRadioButtonClicked(SelectorWithWidgetPreference emiter) {
        if (isModeCurrentlyActive()) {
            // Already in this mode, return.
            return;
        }

        if (mAutoManagedSimPinHelper.isIccLockEnabled(mSubId)) {
            mState = STATE_DISABLING_ICC_LOCK;
        }

        showAuthenticationDialogSimEnrollment();
    }

    @Override
    public int getAvailabilityStatus() {
        if (!isDeviceSecure()) {
            return DISABLED_DEPENDENT_SETTING;
        }

        return super.getAvailabilityStatus();
    }

    @Override
    public boolean isModeCurrentlyActive() {
        return mAutoManagedSimPinHelper.isPinAutoManagedForSubscription(mSubId);
    }

    @Override
    public void onPinEntered(String pin) {
        Log.d(TAG, "Received PIN, attempting enrollment, state: " + mState);

        if (mState != STATE_DISABLING_ICC_LOCK) {
            tryEnrollingToAutoPinManagement(pin);
            return;
        }

        // TODO: b/481609383 - Handle invalid PIN.
        ListenableFuture<PinResult> future = mAutoManagedSimPinHelper.setIccLockState(
                pin, /* enabled= */ false, mSubId);
        Futures.addCallback(future, new FutureCallback<PinResult>() {
            @Override
            public void onSuccess(PinResult result) {
                if (result.getResult() != PinResult.PIN_RESULT_TYPE_SUCCESS) {
                    // TODO: b/481609383 - Show the PIN entry dialog again.
                    Log.e(TAG, "Entered SIM is wrong.");
                    setPreferenceCheckedAndRefresh(false);
                    return;
                }

                mState = STATE_ENROLLING_INTO_AUTO_PIN_MANAGEMENT;
                tryEnrollingToAutoPinManagement(pin);
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Failure turning off ICC lock: " + t.getMessage());
                setPreferenceCheckedAndRefresh(false);
            }
        }, mContext.getMainExecutor());
    }

    @Override
    public void onEntryCancelled() {
        setPreferenceCheckedAndRefresh(false);
    }

    private boolean isDeviceSecure() {
        KeyguardManager keyguardManager = mContext.getSystemService(KeyguardManager.class);
        return keyguardManager != null ? keyguardManager.isDeviceSecure() : false;
    }

    private final class EnrollmentResultReceiver implements
            OutcomeReceiver<String, TelephonyManager.SimAutoPinManagementException> {

        @Override
        public void onResult(String generatedPin) {
            setPreferenceCheckedAndRefresh(true);

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
            // TODO: b/481609383 - Show the PIN entry dialog again if the error indicates the PIN
            // was wrong.
            setPreferenceCheckedAndRefresh(false);
        }
    }

    private void showPinEntryDialog() {
        EnterSimPinDialogFragment df = EnterSimPinDialogFragment.newEnterCurrentPin();

        if (mFragment != null) {
            mFragment.setCurrentListener(this);
            df.showNow(mFragment.getChildFragmentManager(), "CurrentPin");
        }
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
                        setPreferenceCheckedAndRefresh(false);
                    }
                };

        mAutoManagedSimPinHelper.showAuthenticationPromptForCallback(authenticationCallback);
    }

    private void tryEnrollingToAutoPinManagement(String pin) {
        TelephonyManager tm = mTelephonyManager.createForSubscriptionId(mSubId);
        tm.enrollSimInAutoPinManagement(pin, mContext.getMainExecutor(),
                new AutomaticSimProtectionModePreferenceController.EnrollmentResultReceiver());
    }
}
