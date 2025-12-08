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
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;

/**
 * A class for functionality used by various controllers of the automatic SIM card PIN management
 * feature.
 *
 * As the controllers cannot share a base class, this class contains the common functionality.
 */
class AutoManagedSimPinHelper {
    private final Context mContext;
    private final TelephonyManager mTelephonyManager;

    AutoManagedSimPinHelper(Context context) {
        mContext = context;
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
    }

    /**
     * Displays an authentication prompt that accepts strong biometric authenticators as well
     * as device lock.
     *
     * @param authenticationCallback The callback for authentication success/failure.
     */
    public void showAuthenticationPromptForCallback(
            BiometricPrompt.AuthenticationCallback authenticationCallback) {
        final BiometricPrompt.Builder builder = new BiometricPrompt.Builder(mContext)
                .setUseDefaultTitle();

        final BiometricManager bm = mContext.getSystemService(BiometricManager.class);
        int authenticators = BiometricManager.Authenticators.DEVICE_CREDENTIAL
                | BiometricManager.Authenticators.BIOMETRIC_STRONG;
        if (bm.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            authenticators = BiometricManager.Authenticators.DEVICE_CREDENTIAL;
        }
        builder.setAllowedAuthenticators(authenticators);
        builder.setSubtitle(bm.getStrings(authenticators).getPromptMessage());

        final BiometricPrompt bp = builder.build();
        final Handler handler = new Handler(Looper.getMainLooper());
        bp.authenticate(new CancellationSignal(),
                runnable -> handler.post(runnable),
                authenticationCallback);
    }

    /**
     * Returns true if the SIM for the provided subscription ID has its PIN managed by the
     * platform.
     * @param subscriptionId subscription identifying the SIM card.
     * @return true if the flag is enabled and the SIM in enrolled into auto PIN management.
     */
    public boolean isPinAutoManagedForSubscription(int subscriptionId) {
        if (!android.security.Flags.autoSimPinManagement()) {
            return false;
        }

        return mTelephonyManager.createForSubscriptionId(
                subscriptionId).getSimAutoPinManagementEnrollmentStatus()
                == TelephonyManager.SIM_PIN_ENROLLMENT_STATUS_PLATFORM_MANAGED;
    }
}
