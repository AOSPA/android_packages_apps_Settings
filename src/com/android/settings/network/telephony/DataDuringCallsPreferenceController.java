// QTI_BEGIN: 2023-01-04: Telephony: Revert auto data switch UI
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
 * limitations under the License
 */

package com.android.settings.network.telephony;

import static androidx.lifecycle.Lifecycle.Event.ON_PAUSE;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

// QTI_END: 2023-01-04: Telephony: Revert auto data switch UI
// QTI_BEGIN: 2023-01-12: Telephony: Add value-adds on data during call
import android.content.BroadcastReceiver;
// QTI_END: 2023-01-12: Telephony: Add value-adds on data during call
// QTI_BEGIN: 2023-01-04: Telephony: Revert auto data switch UI
import android.content.Context;
// QTI_END: 2023-01-04: Telephony: Revert auto data switch UI
// QTI_BEGIN: 2023-01-12: Telephony: Add value-adds on data during call
import android.content.Intent;
import android.content.IntentFilter;
// QTI_END: 2023-01-12: Telephony: Add value-adds on data during call
// QTI_BEGIN: 2023-01-04: Telephony: Revert auto data switch UI
import android.os.Handler;
import android.os.Looper;
// QTI_END: 2023-01-04: Telephony: Revert auto data switch UI
// QTI_BEGIN: 2025-10-15: Telephony: Google Auto DDS FR changes
import android.telephony.ServiceState;
// QTI_END: 2025-10-15: Telephony: Google Auto DDS FR changes
// QTI_BEGIN: 2023-01-04: Telephony: Revert auto data switch UI
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
// QTI_END: 2023-01-04: Telephony: Revert auto data switch UI
// QTI_BEGIN: 2023-01-12: Telephony: Add value-adds on data during call
import android.util.Log;
// QTI_END: 2023-01-12: Telephony: Add value-adds on data during call
// QTI_BEGIN: 2023-01-04: Telephony: Revert auto data switch UI

import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.annotations.VisibleForTesting;
// QTI_END: 2023-01-04: Telephony: Revert auto data switch UI
// QTI_BEGIN: 2023-01-12: Telephony: Add value-adds on data during call
import com.android.internal.telephony.TelephonyIntents;
// QTI_END: 2023-01-12: Telephony: Add value-adds on data during call
// QTI_BEGIN: 2023-01-04: Telephony: Revert auto data switch UI
import com.android.settings.datausage.DataUsageUtils;
import com.android.settings.network.MobileDataContentObserver;
// QTI_END: 2023-01-04: Telephony: Revert auto data switch UI
// QTI_BEGIN: 2025-10-15: Telephony: Google Auto DDS FR changes
import com.android.settings.network.RoamingPreferenceContentObserver;
// QTI_END: 2025-10-15: Telephony: Google Auto DDS FR changes
// QTI_BEGIN: 2023-01-04: Telephony: Revert auto data switch UI
import com.android.settings.network.SubscriptionsChangeListener;

public class DataDuringCallsPreferenceController extends TelephonyTogglePreferenceController
        implements LifecycleObserver,
        SubscriptionsChangeListener.SubscriptionsChangeListenerClient {
// QTI_END: 2023-01-04: Telephony: Revert auto data switch UI
// QTI_BEGIN: 2023-01-12: Telephony: Add value-adds on data during call
    private static final String TAG = "DataDuringCallsPreferenceController";
// QTI_END: 2023-01-12: Telephony: Add value-adds on data during call
// QTI_BEGIN: 2023-01-04: Telephony: Revert auto data switch UI

    private SwitchPreference mPreference;
    private SubscriptionsChangeListener mChangeListener;
    private TelephonyManager mManager;
    private MobileDataContentObserver mMobileDataContentObserver;
// QTI_END: 2023-01-04: Telephony: Revert auto data switch UI
// QTI_BEGIN: 2025-10-15: Telephony: Google Auto DDS FR changes
    private RoamingPreferenceContentObserver mRoamingPreferenceContentObserver;
// QTI_END: 2025-10-15: Telephony: Google Auto DDS FR changes
// QTI_BEGIN: 2023-01-04: Telephony: Revert auto data switch UI
    private PreferenceScreen mScreen;

// QTI_END: 2023-01-04: Telephony: Revert auto data switch UI
// QTI_BEGIN: 2023-01-12: Telephony: Add value-adds on data during call
    private final BroadcastReceiver mDefaultDataChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED)) {
                Log.d(TAG, "ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
                refreshPreference();
            }
        }
    };

// QTI_END: 2023-01-12: Telephony: Add value-adds on data during call
// QTI_BEGIN: 2023-01-04: Telephony: Revert auto data switch UI
    public DataDuringCallsPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
    }

    void init(int subId) {
        this.mSubId = subId;
        mManager = mContext.getSystemService(TelephonyManager.class).createForSubscriptionId(subId);
    }

    @OnLifecycleEvent(ON_RESUME)
    public void onResume() {
// QTI_END: 2023-01-04: Telephony: Revert auto data switch UI
// QTI_BEGIN: 2025-10-15: Telephony: Google Auto DDS FR changes
        Log.d(TAG, "onResume");
// QTI_END: 2025-10-15: Telephony: Google Auto DDS FR changes
// QTI_BEGIN: 2023-01-04: Telephony: Revert auto data switch UI
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
// QTI_END: 2023-01-04: Telephony: Revert auto data switch UI
// QTI_BEGIN: 2025-10-15: Telephony: Google Auto DDS FR changes

        if (mRoamingPreferenceContentObserver == null) {
            mRoamingPreferenceContentObserver = new RoamingPreferenceContentObserver(
                    new Handler(Looper.getMainLooper()));
            mRoamingPreferenceContentObserver.setOnRoamingPreferenceChangedListener(
                    () -> refreshPreference());
        }
        mRoamingPreferenceContentObserver.register(mContext, mSubId);
// QTI_END: 2025-10-15: Telephony: Google Auto DDS FR changes
// QTI_BEGIN: 2023-01-12: Telephony: Add value-adds on data during call
        final int defaultDataSub = SubscriptionManager.getDefaultDataSubscriptionId();
// QTI_END: 2023-01-12: Telephony: Add value-adds on data during call

// QTI_BEGIN: 2023-01-12: Telephony: Add value-adds on data during call
        if (defaultDataSub != mSubId) {
// QTI_END: 2023-01-12: Telephony: Add value-adds on data during call
// QTI_BEGIN: 2025-10-15: Telephony: Google Auto DDS FR changes
            // Listen to mobile data status of DDS on non-DDS SUB
// QTI_END: 2025-10-15: Telephony: Google Auto DDS FR changes
// QTI_BEGIN: 2023-01-12: Telephony: Add value-adds on data during call
            mMobileDataContentObserver.register(mContext, defaultDataSub);
// QTI_END: 2023-01-12: Telephony: Add value-adds on data during call
// QTI_BEGIN: 2025-10-15: Telephony: Google Auto DDS FR changes

            // Listen to roaming UI status of DDS on non-DDS SUB
            mRoamingPreferenceContentObserver.register(mContext, defaultDataSub);
// QTI_END: 2025-10-15: Telephony: Google Auto DDS FR changes
// QTI_BEGIN: 2023-01-12: Telephony: Add value-adds on data during call
        }
        mContext.registerReceiver(mDefaultDataChangedReceiver,
                new IntentFilter(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED));
// QTI_END: 2023-01-12: Telephony: Add value-adds on data during call
// QTI_BEGIN: 2023-01-04: Telephony: Revert auto data switch UI
    }

    @OnLifecycleEvent(ON_PAUSE)
    public void onPause() {
        if (mChangeListener != null) {
            mChangeListener.stop();
        }
        if (mMobileDataContentObserver != null) {
            mMobileDataContentObserver.unRegister(mContext);
        }
// QTI_END: 2023-01-04: Telephony: Revert auto data switch UI
// QTI_BEGIN: 2025-10-15: Telephony: Google Auto DDS FR changes
        if (mRoamingPreferenceContentObserver != null) {
            mRoamingPreferenceContentObserver.unRegister(mContext);
        }
// QTI_END: 2025-10-15: Telephony: Google Auto DDS FR changes
// QTI_BEGIN: 2023-01-12: Telephony: Add value-adds on data during call
        mContext.unregisterReceiver(mDefaultDataChangedReceiver);
// QTI_END: 2023-01-12: Telephony: Add value-adds on data during call
// QTI_BEGIN: 2023-01-04: Telephony: Revert auto data switch UI
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mScreen = screen;
    }

    @Override
    public boolean isChecked() {
// QTI_END: 2023-01-04: Telephony: Revert auto data switch UI
// QTI_BEGIN: 2023-01-12: Telephony: Add value-adds on data during call
        if (mManager == null) {
            return false;
        }
// QTI_END: 2023-01-12: Telephony: Add value-adds on data during call
// QTI_BEGIN: 2023-01-04: Telephony: Revert auto data switch UI
        return mManager.isMobileDataPolicyEnabled(
                TelephonyManager.MOBILE_DATA_POLICY_DATA_ON_NON_DEFAULT_DURING_VOICE_CALL);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mManager.setMobileDataPolicyEnabled(
                TelephonyManager.MOBILE_DATA_POLICY_DATA_ON_NON_DEFAULT_DURING_VOICE_CALL,
                isChecked);
        return true;
    }

    @VisibleForTesting
    protected boolean hasMobileData() {
        return DataUsageUtils.hasMobileData(mContext);
    }

    @Override
    public int getAvailabilityStatus(int subId) {
// QTI_END: 2023-01-04: Telephony: Revert auto data switch UI
// QTI_BEGIN: 2025-10-15: Telephony: Google Auto DDS FR changes
        Log.d(TAG, "getAvailabilityStatus : subId = " + subId);
// QTI_END: 2025-10-15: Telephony: Google Auto DDS FR changes
// QTI_BEGIN: 2023-01-04: Telephony: Revert auto data switch UI
        if (!SubscriptionManager.isValidSubscriptionId(subId)
                || SubscriptionManager.getDefaultDataSubscriptionId() == subId
                || (!hasMobileData())) {
            return CONDITIONALLY_UNAVAILABLE;
        }
// QTI_END: 2023-01-04: Telephony: Revert auto data switch UI
// QTI_BEGIN: 2023-01-12: Telephony: Add value-adds on data during call
        if (mManager == null) {
            return CONDITIONALLY_UNAVAILABLE;
        }

        boolean isDefDataEnabled = mManager.createForSubscriptionId(
                SubscriptionManager.getDefaultDataSubscriptionId()).isDataEnabled();
        // Do not show 'Data during calls' preference when mobile data switch
        // for the DDS sub is turned off.
        if (!isDefDataEnabled) {
            return CONDITIONALLY_UNAVAILABLE;
        }

// QTI_END: 2023-01-12: Telephony: Add value-adds on data during call
// QTI_BEGIN: 2025-10-15: Telephony: Google Auto DDS FR changes
        boolean isRoamingStateEnabled = mManager.createForSubscriptionId(
                SubscriptionManager.getDefaultDataSubscriptionId()).isDataRoamingEnabled();

        ServiceState serviceState =  mManager.createForSubscriptionId(
                SubscriptionManager.getDefaultDataSubscriptionId()).getServiceState();

        if (serviceState == null) {
            return CONDITIONALLY_UNAVAILABLE;
        }

        boolean isDefaultDataInRoaming = serviceState.getDataRoaming();

        Log.d(TAG, "getAvailabilityStatus : DDS Roaming UI = " + isRoamingStateEnabled
                + ", DDS in roaming state = " + isDefaultDataInRoaming);

        // Do not show 'Data during calls' preference when roaming UI switch
        // for the DDS sub is turned off when DDS is in roaming.
        if (!isRoamingStateEnabled && isDefaultDataInRoaming) {
            return CONDITIONALLY_UNAVAILABLE;
        }

// QTI_END: 2025-10-15: Telephony: Google Auto DDS FR changes
// QTI_BEGIN: 2023-01-12: Telephony: Add value-adds on data during call
        if (TelephonyUtils.isSubsidyFeatureEnabled(mContext) &&
                !TelephonyUtils.isSubsidySimCard(mContext,
                SubscriptionManager.getSlotIndex(mSubId))) {
            return CONDITIONALLY_UNAVAILABLE;
        }
// QTI_END: 2023-01-12: Telephony: Add value-adds on data during call
// QTI_BEGIN: 2023-04-18: Telephony: Enable auto data switch feature for legacy targets

        if (!TelephonyUtils.isSmartDdsSwitchFeatureAvailable()) {
            Log.d(TAG, "Smart DDS switch feature is not available");
            return CONDITIONALLY_UNAVAILABLE;
        }

// QTI_END: 2023-04-18: Telephony: Enable auto data switch feature for legacy targets
// QTI_BEGIN: 2023-01-04: Telephony: Revert auto data switch UI
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
     * Trigger displaying preference when Mobilde data content changed.
     */
    @VisibleForTesting
    public void refreshPreference() {
// QTI_END: 2023-01-04: Telephony: Revert auto data switch UI
// QTI_BEGIN: 2025-10-15: Telephony: Google Auto DDS FR changes
        Log.d(TAG, "refreshPreference");
// QTI_END: 2025-10-15: Telephony: Google Auto DDS FR changes
// QTI_BEGIN: 2023-01-04: Telephony: Revert auto data switch UI
        if (mScreen != null) {
            super.displayPreference(mScreen);
        }
    }
}
// QTI_END: 2023-01-04: Telephony: Revert auto data switch UI
