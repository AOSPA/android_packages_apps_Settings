/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.network.telephony;

import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.TelephonyIntents;
import com.android.settings.datausage.DataUsageUtils;
import com.android.settings.flags.Flags;
import com.android.settings.network.MobileDataContentObserver;
import com.android.settings.network.SubscriptionsChangeListener;

/**
 * Controls whether switch mobile data to the non-default SIM if the non-default SIM has better
 * availability.
 *
 * This is used for temporarily allowing data on the non-default data SIM when on-default SIM
 * has better availability on DSDS devices, where better availability means strong
 * signal/connectivity.
 * If this feature is enabled, data will be temporarily enabled on the non-default data SIM,
 * including during any voice calls.
 */
public class AutoDataSwitchPreferenceController extends TelephonyTogglePreferenceController
        implements LifecycleObserver,
        SubscriptionsChangeListener.SubscriptionsChangeListenerClient {

    private static final String LOG_TAG = "AutoDataSwitchPrefCtrl";

    @Nullable
    private TwoStatePreference mPreference;
    @Nullable
    private SubscriptionsChangeListener mChangeListener;
    @Nullable
    private TelephonyManager mManager;
    @Nullable
    private MobileDataContentObserver mMobileDataContentObserver;
    @Nullable
    private PreferenceScreen mScreen;

    private final BroadcastReceiver mDefaultDataChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mPreference != null) {
                refreshPreference();
            }
        }
    };

    public AutoDataSwitchPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    void init(int subId) {
        this.mSubId = subId;
        mManager = mContext.getSystemService(TelephonyManager.class).createForSubscriptionId(subId);
    }

    @OnLifecycleEvent(ON_RESUME)
    public void onResume() {
        if (mChangeListener == null) {
            mChangeListener = new SubscriptionsChangeListener(mContext, this);
        }
        mChangeListener.start();
        if (mMobileDataContentObserver == null) {
            mMobileDataContentObserver = new MobileDataContentObserver(
                    new Handler(Looper.getMainLooper()));
            mMobileDataContentObserver.setOnMobileDataChangedListener(() -> refreshPreference());
        }
        mMobileDataContentObserver.register(mContext, mSubId);
// QTI_BEGIN: 2021-09-21: Telephony: Fix the data of nDDS sub can't be tore down
        final int defaultDataSub = SubscriptionManager.getDefaultDataSubscriptionId();
        if (defaultDataSub != mSubId) {
            mMobileDataContentObserver.register(mContext, defaultDataSub);
        }
// QTI_END: 2021-09-21: Telephony: Fix the data of nDDS sub can't be tore down
        mContext.registerReceiver(mDefaultDataChangedReceiver,
                new IntentFilter(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED));
    }

    @OnLifecycleEvent(ON_PAUSE)
    public void onPause() {
        if (mChangeListener != null) {
            mChangeListener.stop();
        }
        if (mMobileDataContentObserver != null) {
            mMobileDataContentObserver.unRegister(mContext);
        }
        if (mDefaultDataChangedReceiver != null) {
            mContext.unregisterReceiver(mDefaultDataChangedReceiver);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mScreen = screen;
    }

    @Override
    public boolean isChecked() {
        return mManager != null && mManager.isMobileDataPolicyEnabled(
                TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (mManager != null) {
            mManager.setMobileDataPolicyEnabled(
                    TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH,
                    isChecked);
        }
        return true;
    }

    @VisibleForTesting
    protected boolean hasMobileData() {
        return DataUsageUtils.hasMobileData(mContext);
    }

    @Override
    public int getAvailabilityStatus(int subId) {
// QTI_BEGIN: 2024-06-25: Telephony: Fix for Auto data switch UI option
        if (!SubscriptionManager.isValidSubscriptionId(subId)
// QTI_END: 2024-06-25: Telephony: Fix for Auto data switch UI option
                || SubscriptionManager.getDefaultDataSubscriptionId() == subId
                || (!hasMobileData())) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        if (mManager == null) {
            return CONDITIONALLY_UNAVAILABLE;
        }
// QTI_BEGIN: 2021-09-21: Telephony: Fix the data of nDDS sub can't be tore down
        boolean isDefDataEnabled = mManager.createForSubscriptionId(
                SubscriptionManager.getDefaultDataSubscriptionId())
                .isDataEnabled();
// QTI_END: 2021-09-21: Telephony: Fix the data of nDDS sub can't be tore down
// QTI_BEGIN: 2023-04-18: Telephony: Enable auto data switch feature for legacy targets
        // Do not show auto data switch preference when mobile data switch
// QTI_END: 2023-04-18: Telephony: Enable auto data switch feature for legacy targets
// QTI_BEGIN: 2021-09-21: Telephony: Fix the data of nDDS sub can't be tore down
        // for the DDS sub is turned off.
        if (!isDefDataEnabled) {
            return CONDITIONALLY_UNAVAILABLE;
        }
// QTI_END: 2021-09-21: Telephony: Fix the data of nDDS sub can't be tore down
// QTI_BEGIN: 2021-11-29: Telephony: FR73834: Subsidy lock feature support.

        if (TelephonyUtils.isSubsidyFeatureEnabled(mContext) &&
                !TelephonyUtils.isSubsidySimCard(mContext,
                SubscriptionManager.getSlotIndex(mSubId))) {
            return CONDITIONALLY_UNAVAILABLE;
        }
// QTI_END: 2021-11-29: Telephony: FR73834: Subsidy lock feature support.
// QTI_BEGIN: 2024-12-11: Telephony: Enable Dual SIM onboarding feature in Settings
        // Auto data switch preference has been moved to NetworkCellularGroupProvider
        // as per dual sim onboarding ui enhancements
        if (Flags.isDualSimOnboardingEnabled()) {
            Log.d(LOG_TAG, "Dual sim onboarding config is enabled");
// QTI_END: 2024-12-11: Telephony: Enable Dual SIM onboarding feature in Settings
// QTI_BEGIN: 2023-04-18: Telephony: Enable auto data switch feature for legacy targets
            return CONDITIONALLY_UNAVAILABLE;
        }

// QTI_END: 2023-04-18: Telephony: Enable auto data switch feature for legacy targets
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference == null) {
            return;
        }
        preference.setVisible(isAvailable());
    }

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {}

    @Override
    public void onSubscriptionsChanged() {
        updateState(mPreference);
    }

    /**
     * Trigger displaying preference when Mobile data content changed.
     */
    @VisibleForTesting
    public void refreshPreference() {
        if (mScreen != null) {
            super.displayPreference(mScreen);
        }
    }
}
