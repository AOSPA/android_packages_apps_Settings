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

import static android.telephony.PinResult.PIN_RESULT_TYPE_INCORRECT;

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
 * Controller for the "Manual" sim protection mode preference.
 */
public class ManualSimProtectionModePreferenceController extends
        BaseSimProtectionModePreferenceController implements
        EnterSimPinDialogFragment.SimPinEntryListener {

    public ManualSimProtectionModePreferenceController(
            @NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void onRadioButtonClicked(SelectorWithWidgetPreference emiter) {
        if (isModeCurrentlyActive()) {
            // Already clicked, nothing to do.
            return;
        }

        // If subscribed to automatic PIN management, unenroll from that mode first (requires
        // user authentication).
        if (mAutoManagedSimPinHelper.isPinAutoManagedForSubscription(mSubId)) {
            showAuthenticationDialogAutoPinUnenrollment();
            return;
        }

        showPinEntryDialog();
    }

    @Override
    public boolean isModeCurrentlyActive() {
        return mAutoManagedSimPinHelper.isIccLockEnabled(mSubId)
                && !mAutoManagedSimPinHelper.isPinAutoManagedForSubscription(mSubId);
    }

    @Override
    public void onPinEntered(String pin) {
        if (mAutoManagedSimPinHelper.isPinValid(pin)) {
            enableIccLockAndUpdatePreference(pin);
        } else {
            showPinEntryDialogAfterInvalidPin();
        }
    }

    @Override
    public void onEntryCancelled() {
        setPreferenceCheckedAndRefresh(false);
    }

    private void enableIccLockAndUpdatePreference(String pin) {
        ListenableFuture<PinResult> future = mAutoManagedSimPinHelper.setIccLockState(pin,
                /* enabled= */ true, mSubId);
        Futures.addCallback(future, new FutureCallback<PinResult>() {
            @Override
            public void onSuccess(PinResult result) {
                // The PIN the user has entered is incorrect so PIN protection cannot be turned on.
                if (result.getResult() == PIN_RESULT_TYPE_INCORRECT) {
                    showPinEntryDialogAfterIncorrectPin(result.getAttemptsRemaining());
                } else {
                    setPreferenceCheckedAndRefresh(
                            result.getResult() == PinResult.PIN_RESULT_TYPE_SUCCESS);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.w(TAG, "Failure setting ICC lock state: " + t.getMessage());
                setPreferenceCheckedAndRefresh(false);
            }
        }, mContext.getMainExecutor());
    }

    private void showPinEntryDialog() {
        EnterSimPinDialogFragment df = EnterSimPinDialogFragment.newEnterCurrentPin();
        displayDialog(df);
    }

    private void showPinEntryDialogAfterInvalidPin() {
        EnterSimPinDialogFragment df = EnterSimPinDialogFragment.newEnterCurrentPinWithHint();
        displayDialog(df);
    }

    private void showPinEntryDialogAfterIncorrectPin(int remainingAttempts) {
        EnterSimPinDialogFragment df = EnterSimPinDialogFragment.newEnterCurrentPin(
                remainingAttempts);
        displayDialog(df);
    }

    private void displayDialog(EnterSimPinDialogFragment dialogFragment) {
        if (mFragment != null) {
            mFragment.setCurrentListener(this);
            dialogFragment.showNow(mFragment.getChildFragmentManager(), "CurrentPin");
        }
    }

    private void showAuthenticationDialogAutoPinUnenrollment() {
        final BiometricPrompt.AuthenticationCallback authenticationCallback =
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            BiometricPrompt.AuthenticationResult result) {
                        unenrollFromAutomaticPinManagement();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        Log.w(TAG, "Authentication error for unenrollment, error code "
                                + errorCode + " message: " + errString);
                        setPreferenceCheckedAndRefresh(false);
                    }
                };

        mAutoManagedSimPinHelper.showAuthenticationPromptForCallback(authenticationCallback);
    }

    private void unenrollFromAutomaticPinManagement() {
        mTelephonyManager.createForSubscriptionId(mSubId).unenrollSimFromAutoPinManagement(
                mContext.getMainExecutor(),
                new UnenrollmentResultReceiver());
    }

    private class UnenrollmentResultReceiver implements
            OutcomeReceiver<Void, TelephonyManager.SimAutoPinManagementException> {
        @Override
        public void onResult(Void result) {
            Log.d(TAG, "Unenrollment successful, turning on SIM PIN");
            showPinEntryDialog();
        }

        @Override
        public void onError(
                @NonNull TelephonyManager.SimAutoPinManagementException error) {
            OutcomeReceiver.super.onError(error);
            Log.w(TAG, "Error unenrolling: " + error.getErrorCode());
            setPreferenceCheckedAndRefresh(false);
        }
    }
}
