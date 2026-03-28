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
import android.telephony.PinResult;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.annotation.Nullable;

import com.android.settings.network.SubscriptionUtil;
import com.android.settingslib.utils.ThreadUtils;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;

/**
 * A class for functionality used by various controllers of the automatic SIM card PIN management
 * feature.
 *
 * As the controllers cannot share a base class, this class contains the common functionality.
 */
class AutoManagedSimPinHelper {
    private final Context mContext;
    private final TelephonyManager mTelephonyManager;
    private final SubscriptionManager mSubscriptionManager;
    AutoManagedSimPinHelper(Context context) {
        mContext = context;
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
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

        if (!android.telephony.SubscriptionManager.isValidSubscriptionId(subscriptionId)) {
            return false;
        }

        return mTelephonyManager.createForSubscriptionId(
                subscriptionId).getSimAutoPinManagementEnrollmentStatus()
                == TelephonyManager.SIM_PIN_ENROLLMENT_STATUS_PLATFORM_MANAGED;
    }

    public boolean isIccLockEnabled(int subscriptionId) {
        return mTelephonyManager.createForSubscriptionId(subscriptionId).isIccLockEnabled();
    }

    /**
     * Returns true if the provided PIN is valid. Assumes the string is numeric only since that's
     * the input field type.
     * @param pin String to check.
     * @return true if it is between 4 and 8 characters, false otherwise.
     */
    public static boolean isPinValid(@Nullable String pin) {
        if (pin == null) {
            return false;
        }

        if (pin.length() < 4 || pin.length() > 8) {
            return false;
        }

        return true;
    }

    public int getFirstPhysicalSimSlot() {
        final List<SubscriptionInfo> subInfoList =
                mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subInfoList == null) {
            return SubscriptionManager.DEFAULT_SIM_SLOT_INDEX;
        }

        for (SubscriptionInfo subInfo : subInfoList) {
            if (subInfo.isActive() && !subInfo.isEmbedded()) {
                return subInfo.getSimSlotIndex();
            }
        }

        return SubscriptionManager.DEFAULT_SIM_SLOT_INDEX;
    }

    public int getSubIdForFirstPhysicalSimSlot() {
        int firstSimSlot = getFirstPhysicalSimSlot();
        if (firstSimSlot == SubscriptionManager.DEFAULT_SIM_SLOT_INDEX) {
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }

        return mSubscriptionManager.getEnabledSubscriptionId(firstSimSlot);
    }

    /**
     * Returns an array of active slots, without duplicates, sorted from the lowest to the highest
     * slot index.
     * @return an array of active slots.
     */
    public int[] getActiveSlots() {
        final List<SubscriptionInfo> subInfoList =
                SubscriptionUtil.getActiveSubscriptions(mSubscriptionManager);
        if (subInfoList == null || subInfoList.isEmpty()) {
            return new int[0];
        }

        int[] activeSlots = new int[subInfoList.size()];
        int i = 0;
        for (SubscriptionInfo subInfo : subInfoList) {
            activeSlots[i] = subInfo.getSimSlotIndex();
            i++;
        }
        return IntStream.of(activeSlots).sorted().distinct().toArray();
    }

    public int getSubscriptionIdForSlot(int slotIndex) {
        int[] activeSlots = getActiveSlots();

        if (activeSlots == null || activeSlots.length < (slotIndex + 1)) {
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }

        int activeSlot = activeSlots[slotIndex];

        SubscriptionInfo info = mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(
                activeSlot);
        if (info == null) {
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }

        return info.getSubscriptionId();
    }

    /**
     * Callable for setting the ICC Lock state. Needed as this should not be called on the UI
     * thread (as opposed to API related to automatic SIM PIN management, which takes an outcome
     * receiver and calls it).
     */
    private static final class SetIccLockState implements Callable<PinResult> {
        private final String mPin;
        private final boolean mEnabled;
        private final int mSubId;
        private final TelephonyManager mTelephonyManager;

        SetIccLockState(String pin, boolean enabled, int subId, TelephonyManager telephonyManager) {
            mPin = pin;
            mEnabled = enabled;
            mSubId = subId;
            mTelephonyManager = telephonyManager;
        }

        @Override
        public PinResult call() throws Exception {
            TelephonyManager tm = mTelephonyManager.createForSubscriptionId(mSubId);
            return tm.setIccLockEnabled(mEnabled, mPin);
        }
    }

    ListenableFuture<PinResult> setIccLockState(String pin, boolean enabled, int subId) {
        Callable<PinResult> setIccLockState = new SetIccLockState(pin, enabled, subId,
                mTelephonyManager);
        return ThreadUtils.postOnBackgroundThread(setIccLockState);
    }
}
