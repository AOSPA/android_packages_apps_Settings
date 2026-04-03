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
import android.os.Bundle;
import android.os.OutcomeReceiver;
import android.security.Flags;
import android.telephony.PinResult;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.PrimarySwitchPreference;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Controller for the SIM card protection preference (automatic and manual PIN management).
 *
 * It is responsible for enabling/disabling the preference (whether the SIM PIN is manually
 * or automatically managed), triggering the authentication dialog for enrollment/unenrollment and
 * reporting the correct status for the toggle.
 */
public class SimPinProtectionToggleController extends TogglePreferenceController implements
        EnterSimPinDialogFragment.SimPinEntryListener {
    private static final String TAG = "AutoManagedSimPin";
    private static final String MANUAL_PIN_ONLY_KEY = "sim_pin_manual_management_only_toggle";
    private static final String AUTO_PIN_KEY = "sim_pin_auto_management_toggle";
    private KeyguardManager mKeyguardManager;

    public enum EnrollmentState {
        ENROLL_TO_MANUAL_PIN_MANAGEMENT(0),
        ENROLL_TO_AUTOMATIC_PIN_MANAGEMENT(1),
        UNENROLL_FROM_MANUAL_PIN_MANAGEMENT(2);

        /** Bundle extra key for the enum value. */
        public static final String EXTRA_ENROLLMENT_STATE_VALUE = "enrollment_state_value";

        private final int mIntValue;

        EnrollmentState(int intValue) {
            this.mIntValue = intValue;
        }

        public int getIntValue() {
            return mIntValue;
        }

        /**
         * Creates an enrollment state from a bundle.
         * @param args bundle with state.
         * @return EnrollmentState created from the int stored in the bundle.
         */
        @NonNull
        public static EnrollmentState fromBundle(@Nullable Bundle args) {
            if (args == null) {
                return ENROLL_TO_MANUAL_PIN_MANAGEMENT;
            }
            int value = args.getInt(EXTRA_ENROLLMENT_STATE_VALUE,
                    ENROLL_TO_MANUAL_PIN_MANAGEMENT.mIntValue);
            return fromIntValue(value);
        }

        private static EnrollmentState fromIntValue(int value) {
            for (EnrollmentState entry : EnrollmentState.values()) {
                if (entry.mIntValue == value) {
                    return entry;
                }
            }
            return ENROLL_TO_MANUAL_PIN_MANAGEMENT;
        }

        /**
         * Stores the enum in the bundle.
         * @param bundle to store the enum in.
         */
        public void storeState(@NonNull Bundle bundle) {
            bundle.putInt(EXTRA_ENROLLMENT_STATE_VALUE, this.mIntValue);
        }
    }

    private TelephonyManager mTelephonyManager;
    private SubscriptionManager mSubscriptionManager;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

    private EnrollmentState mEnrollmentState;

    // Used when android.security.Flags.enableAutoSimPinUi() is false.
    @Nullable
    private SwitchPreferenceCompat mSwitchPreference = null;

    // Used when android.security.Flags.enableAutoSimPinUi() is true.
    @Nullable
    private PrimarySwitchPreference mPrimarySwitchPreference = null;

    @Nullable
    private BaseSimPinFragment mFragment;
    private AutoManagedSimPinHelper mAutoManagedSimPinHelper;

    public SimPinProtectionToggleController(@NonNull Context context,
            @NonNull String preferenceKey) {
        this(context, preferenceKey, new AutoManagedSimPinHelper(context));
    }

    @VisibleForTesting
    public SimPinProtectionToggleController(@NonNull Context context,
            @NonNull String preferenceKey, AutoManagedSimPinHelper autoManagedSimPinHelper) {
        super(context, preferenceKey);
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        mAutoManagedSimPinHelper = autoManagedSimPinHelper;
        mKeyguardManager = mContext.getSystemService(KeyguardManager.class);

        mEnrollmentState = EnrollmentState.ENROLL_TO_AUTOMATIC_PIN_MANAGEMENT;
    }

    @Override
    public boolean isChecked() {
        boolean isPlatformManaged = mAutoManagedSimPinHelper.isPinAutoManagedForSubscription(
                mSubId);
        boolean isIccLockEnabled = mAutoManagedSimPinHelper.isIccLockEnabled(mSubId);

        return isPlatformManaged || isIccLockEnabled;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (isChecked) {
            enableManualOrAutomaticSimPin();
        } else {
            disableManualOrAutomaticSimPin();
        }

        // The check state changes by default, so indicate that it was changed in case the user
        // cancels later and it has to be un-checked.
        return true;
    }

    private void disableManualOrAutomaticSimPin() {
        if (mAutoManagedSimPinHelper.isPinAutoManagedForSubscription(mSubId)) {
            showAuthenticationDialogSimUnenrollment(
                    mTelephonyManager.createForSubscriptionId(mSubId));
        } else if (mAutoManagedSimPinHelper.isIccLockEnabled(mSubId)) {
            mEnrollmentState = EnrollmentState.UNENROLL_FROM_MANUAL_PIN_MANAGEMENT;
            showPinEntryDialog();
        } else {
            Log.e(TAG, "Error: SIM PIN not automatically or manually managed.");
        }
    }

    private void enableManualOrAutomaticSimPin() {
        boolean isValidSubscription = mSubscriptionManager.isValidSubscriptionId(mSubId);
        boolean isEmbeddedSim =
                isValidSubscription && mSubscriptionManager.getActiveSubscriptionInfo(
                        mSubId).isEmbedded();

        if (isDeviceSecure() && !isEmbeddedSim && Flags.enableAutoSimPinUi()) {
            mEnrollmentState = EnrollmentState.ENROLL_TO_AUTOMATIC_PIN_MANAGEMENT;
            Log.i(TAG, "Enrolling into automatic PIN management.");
            showAuthenticationDialogSimEnrollment();
        } else {
            Log.i(TAG, "Enrolling into manual PIN management.");
            mEnrollmentState = EnrollmentState.ENROLL_TO_MANUAL_PIN_MANAGEMENT;
            showPinEntryDialog();
        }
    }

    @Override
    public int getAvailabilityStatus() {
        boolean isFlagEnabled = Flags.autoSimPinManagement();
        if (!isFlagEnabled) {
            return CONDITIONALLY_UNAVAILABLE;
        }

        if (!mSubscriptionManager.isValidSubscriptionId(mSubId)) {
            return CONDITIONALLY_UNAVAILABLE;
        }

        // Manual PIN management only available: Show this preference as available but not
        // the other one.
        if (getPreferenceKey().startsWith(MANUAL_PIN_ONLY_KEY) && !Flags.enableAutoSimPinUi()) {
            return AVAILABLE;
        }

        // Automatic PIN management as well as manual PIN management: Show this preference as
        // available but not the other one.
        if (getPreferenceKey().startsWith(AUTO_PIN_KEY) && Flags.enableAutoSimPinUi()) {
            return AVAILABLE;
        }

        return CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getResources().getString(getSummaryResId());
    }

    private int getSummaryResId() {
        if (mAutoManagedSimPinHelper.isPinAutoManagedForSubscription(mSubId)) {
            return R.string.sim_protection_mode_protected_by_platform;
        } else if (mAutoManagedSimPinHelper.isIccLockEnabled(mSubId)) {
            return R.string.sim_protection_mode_manually_managed;
        } else if (!isDeviceSecure() && Flags.enableAutoSimPinUi()) {
            return R.string.sim_protection_mode_lskf_required;
        }
        return R.string.sim_choose_protection_mode_title;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_security;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (getPreferenceKey().startsWith(AUTO_PIN_KEY)) {
            mPrimarySwitchPreference = screen.findPreference(getPreferenceKey());
        } else if (getPreferenceKey().startsWith(MANUAL_PIN_ONLY_KEY)) {
            mSwitchPreference = screen.findPreference(getPreferenceKey());
        } else {
            Log.e(TAG, "Ambiguous preference key: " + getPreferenceKey());
            return;
        }

        if (!mSubscriptionManager.isValidSubscriptionId(mSubId)) {
            Log.w(TAG, "invalid subscription, returning.");
            return;
        }

        setPreferenceSummary(getSummary());
    }

    /**
     * Stores the current enrollment state in the bundle.
     */
    void storeEnrollmentState(@NonNull Bundle bundle) {
        mEnrollmentState.storeState(bundle);
    }

    /**
     * Sets the enrollment state (in case of instance restore).
     * @param bundle the bundle to load the state from.
     */
    void loadEnrollmentState(@Nullable Bundle bundle) {
        mEnrollmentState = EnrollmentState.fromBundle(bundle);
    }

    /**
     * Sets the index of the SIM card slot that this controller is responsible for.
     * @param slotIndex index in the array of active slots.
     */
    public void setSlotIndex(int slotIndex) {
        mSubId = mAutoManagedSimPinHelper.getSubscriptionIdForSlot(slotIndex);
        Log.d(TAG, "Preference " + getPreferenceKey() + ": Subscription for slot index " + slotIndex
                + ": " + mSubId);
    }

    private boolean isDeviceSecure() {
        return mKeyguardManager != null ? mKeyguardManager.isDeviceSecure() : false;
    }

    public void setFragment(BaseSimPinFragment fragment) {
        mFragment = fragment;
    }

    private class EnrollmentResultReceiver implements
            OutcomeReceiver<String, TelephonyManager.SimAutoPinManagementException> {

        @Override
        public void onResult(String generatedPin) {
            setPreferenceState(true);

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

    private void setPreferenceSummary(CharSequence summary) {
        if (mPrimarySwitchPreference != null) {
            mPrimarySwitchPreference.setSummary(summary);
        } else if (mSwitchPreference != null) {
            mSwitchPreference.setSummary(summary);
        }
    }

    private void setPreferenceState(boolean isChecked) {
        setPreferenceState(isChecked, null);
    }

    private void setPreferenceState(boolean isChecked, @Nullable CharSequence summary) {
        if (mPrimarySwitchPreference != null) {
            mPrimarySwitchPreference.setChecked(isChecked);
            mPrimarySwitchPreference.setSummary(summary);
        } else if (mSwitchPreference != null) {
            mSwitchPreference.setChecked(isChecked);
            mSwitchPreference.setSummary(summary);
        }
        // Required to ensure that the checked state of the preference is updated.
        if (mFragment != null) {
            mFragment.forceUpdatePreferences();
        }
    }

    private class UnenrollmentResultReceiver implements
            OutcomeReceiver<Void, TelephonyManager.SimAutoPinManagementException> {
        @Override
        public void onResult(Void result) {
            setPreferenceState(false);
            Log.d(TAG, "Unenrollment successful.");
        }

        @Override
        public void onError(
                @NonNull TelephonyManager.SimAutoPinManagementException error) {
            OutcomeReceiver.super.onError(error);
            setPreferenceState(true);
            Log.w(TAG, "Error unenrolling: " + error.getErrorCode());
        }
    }

    private void showPinEntryDialog(int attemptsRemaining) {
        showPinEntryDialog(EnterSimPinDialogFragment.newEnterCurrentPin(attemptsRemaining));
    }

    private void showPinEntryDialog() {
        showPinEntryDialog(EnterSimPinDialogFragment.newEnterCurrentPin());
    }

    private void showPinEntryDialog(EnterSimPinDialogFragment df) {
        if (mFragment != null) {
            mFragment.setCurrentListener(this);
            df.showNow(mFragment.getChildFragmentManager(), "CurrentPin");
        }
    }

    /**
     * To be called by the SIM PIN entry dialog to proceed with enrollment after the user
     * has provided the PIN.
     *
     * @param pin The current SIM PIN as provided by the user.
     */
    @Override
    public void onPinEntered(String pin) {
        switch (mEnrollmentState) {
            case ENROLL_TO_MANUAL_PIN_MANAGEMENT -> {
                Log.d(TAG, "Enrolling into manual PIN management mode.");

                ListenableFuture<PinResult> future = mAutoManagedSimPinHelper.setIccLockState(
                        pin, /* enabled= */ true, mSubId);
                Futures.addCallback(future, new FutureCallback<PinResult>() {
                    @Override
                    public void onSuccess(PinResult result) {
                        boolean success = result.getResult() == PinResult.PIN_RESULT_TYPE_SUCCESS;
                        int summaryResId;
                        if (success) {
                            summaryResId = R.string.sim_protection_mode_manually_managed;
                        } else {
                            summaryResId = R.string.sim_pin_enable_failed;
                        }

                        setPreferenceState(success,
                                mContext.getResources().getString(summaryResId));
                        if (result.getResult() == PinResult.PIN_RESULT_TYPE_INCORRECT
                                && result.getAttemptsRemaining() > 0) {
                            showPinEntryDialog(result.getAttemptsRemaining());
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        setPreferenceState(false,
                                mContext.getResources().getString(R.string.sim_pin_enable_failed));
                    }
                }, mContext.getMainExecutor());
            }
            case ENROLL_TO_AUTOMATIC_PIN_MANAGEMENT -> {
                Log.d(TAG, "Enrolling into automatic PIN management mode.");
                TelephonyManager tm = mTelephonyManager.createForSubscriptionId(mSubId);
                tm.enrollSimInAutoPinManagement(pin, mContext.getMainExecutor(),
                        new EnrollmentResultReceiver());
            }
            case UNENROLL_FROM_MANUAL_PIN_MANAGEMENT -> {
                Log.d(TAG, "Unenrolling from manual PIN management mode.");

                ListenableFuture<PinResult> future = mAutoManagedSimPinHelper.setIccLockState(
                        pin, /* enabled= */ false, mSubId);
                Futures.addCallback(future, new FutureCallback<PinResult>() {
                    @Override
                    public void onSuccess(PinResult result) {
                        boolean success = result.getResult() == PinResult.PIN_RESULT_TYPE_SUCCESS;
                        int summaryResId;
                        if (success) {
                            summaryResId = R.string.sim_choose_protection_mode_title;
                        } else {
                            summaryResId = R.string.sim_pin_disable_failed;
                        }

                        setPreferenceState(!success,
                                mContext.getResources().getString(summaryResId));
                        if (result.getResult() == PinResult.PIN_RESULT_TYPE_INCORRECT
                                && result.getAttemptsRemaining() > 0) {
                            showPinEntryDialog(result.getAttemptsRemaining());
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        setPreferenceState(true,
                                mContext.getResources().getString(R.string.sim_pin_disable_failed));
                        Log.w(TAG, "Failure to set ICC lock state: " + t.getMessage());
                    }
                }, mContext.getMainExecutor());
            }
        }
    }

    /**
     * To be called by the SIM PIN entry dialog when the user cancels the enrollment.
     */
    @Override
    public void onEntryCancelled() {
        setPreferenceState(isChecked());
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
                        setPreferenceState(false);
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
                        setPreferenceState(false, getSummary());
                    }
                };

        mAutoManagedSimPinHelper.showAuthenticationPromptForCallback(authenticationCallback);
    }
}
