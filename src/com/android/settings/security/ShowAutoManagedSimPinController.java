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

import android.content.Context;
import android.hardware.biometrics.BiometricPrompt;
import android.os.OutcomeReceiver;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

/**
 * Controller for showing the automatically-generated SIM PIN. Ensures that the user is
 * authenticated before calling into the TelephonyManager for retrieving the SIM PIN.
 */
public class ShowAutoManagedSimPinController extends BasePreferenceController implements
        Preference.OnPreferenceClickListener {
    private static final String TAG = "AutoManagedSimPin";
    // The tag used for the DialogFragment's showNow method.
    private static final String FRAGMENT_TAG = "PinShow";

    private final SubscriptionManager mSubscriptionManager;
    private TelephonyManager mTelephonyManager;

    private int mSubId;

    @Nullable
    private Fragment mFragment;

    private Preference mPreference;

    private AutoManagedSimPinHelper mAutoManagedSimPinHelper;

    public ShowAutoManagedSimPinController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
        mSubId = mSubscriptionManager.getEnabledSubscriptionId(0);

        mAutoManagedSimPinHelper = new AutoManagedSimPinHelper(mContext);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!android.security.Flags.autoSimPinManagement()) {
            return CONDITIONALLY_UNAVAILABLE;
        }

        if (!mAutoManagedSimPinHelper.isPinAutoManagedForSubscription(mSubId)) {
            return DISABLED_DEPENDENT_SETTING;
        }

        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = screen.findPreference(getPreferenceKey());
        mPreference.setOnPreferenceClickListener(this);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        preference.setEnabled(mAutoManagedSimPinHelper.isPinAutoManagedForSubscription(mSubId));
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        showAuthenticationDialog();
        return true;
    }

    public void setFragment(Fragment fragment) {
        mFragment = fragment;
    }

    private void showAuthenticationDialog() {
        final BiometricPrompt.AuthenticationCallback authenticationCallback =
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            BiometricPrompt.AuthenticationResult result) {
                        Log.d(TAG, "Authentication succeeded, asking for PIN.");
                        getPlatformManagedPin();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        Log.w(TAG, "Authentication error for showing PIN, error code "
                                + errorCode + " message: " + errString);
                    }
                };

        mAutoManagedSimPinHelper.showAuthenticationPromptForCallback(authenticationCallback);
    }

    private class GetAutoManagedPinReceiver implements
            OutcomeReceiver<String, TelephonyManager.SimAutoPinManagementException> {
        private GetAutoManagedPinReceiver() {
        }

        @Override
        public void onResult(String result) {
            DisplaySimPinDialogFragment df = DisplaySimPinDialogFragment.newInstance(true, result);

            if (mFragment != null) {
                df.showNow(mFragment.getChildFragmentManager(), FRAGMENT_TAG);
            }
        }

        @Override
        public void onError(@NonNull TelephonyManager.SimAutoPinManagementException error) {
            Log.w(TAG, "Failure getting PIN: " + error);
            DisplaySimPinDialogFragment df = DisplaySimPinDialogFragment.newInstance(false, "");
            if (mFragment != null) {
                df.showNow(mFragment.getChildFragmentManager(), FRAGMENT_TAG);
            }

            OutcomeReceiver.super.onError(error);
        }
    }

    private void getPlatformManagedPin() {
        mTelephonyManager.createForSubscriptionId(mSubId).getAutoManagedPinForSim(
                mContext.getMainExecutor(),
                new GetAutoManagedPinReceiver());
    }
}
