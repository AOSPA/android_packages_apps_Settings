/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.network;

import static android.os.UserHandle.myUserId;
import static android.os.UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS;

import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;

import static androidx.lifecycle.Lifecycle.Event;

import android.content.BroadcastReceiver;
// QTI_BEGIN: 2018-02-22: Android_UI: Enable proprietary MobileNetworkSettings
import android.content.ComponentName;
// QTI_END: 2018-02-22: Android_UI: Enable proprietary MobileNetworkSettings
import android.content.Context;
// QTI_BEGIN: 2018-02-22: Android_UI: Enable proprietary MobileNetworkSettings
import android.content.Intent;
// QTI_END: 2018-02-22: Android_UI: Enable proprietary MobileNetworkSettings
import android.content.IntentFilter;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
// QTI_BEGIN: 2018-05-23: Telephony: Fix to show operator names of both the SIMs
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
// QTI_END: 2018-05-23: Telephony: Fix to show operator names of both the SIMs
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedPreference;
// QTI_BEGIN: 2018-02-22: Android_UI: Enable proprietary MobileNetworkSettings
import com.android.settings.Utils;
// QTI_END: 2018-02-22: Android_UI: Enable proprietary MobileNetworkSettings
import com.android.settingslib.core.AbstractPreferenceController;

// QTI_BEGIN: 2018-05-23: Telephony: Fix to show operator names of both the SIMs
import java.util.List;

// QTI_END: 2018-05-23: Telephony: Fix to show operator names of both the SIMs
public class MobileNetworkPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver {

    @VisibleForTesting
    static final String KEY_MOBILE_NETWORK_SETTINGS = "mobile_network_settings";

    private final boolean mIsSecondaryUser;
    private final TelephonyManager mTelephonyManager;
    private final UserManager mUserManager;
    private Preference mPreference;
    @VisibleForTesting
    MobileNetworkTelephonyCallback mTelephonyCallback;
// QTI_BEGIN: 2018-05-23: Telephony: Fix to show operator names of both the SIMs
    private SubscriptionManager mSubscriptionManager;
// QTI_END: 2018-05-23: Telephony: Fix to show operator names of both the SIMs

    private BroadcastReceiver mAirplanModeChangedReceiver;

// QTI_BEGIN: 2018-05-23: Telephony: Fix to show operator names of both the SIMs
    private String mSummary;

// QTI_END: 2018-05-23: Telephony: Fix to show operator names of both the SIMs
    public MobileNetworkPreferenceController(Context context) {
        super(context);
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mIsSecondaryUser = !mUserManager.isAdminUser();

        mAirplanModeChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
// QTI_BEGIN: 2018-05-23: Telephony: Fix to show operator names of both the SIMs
                updateDisplayName();
// QTI_END: 2018-05-23: Telephony: Fix to show operator names of both the SIMs
                updateState(mPreference);
            }
        };
// QTI_BEGIN: 2018-05-23: Telephony: Fix to show operator names of both the SIMs
        mSubscriptionManager = SubscriptionManager.from(context);
// QTI_END: 2018-05-23: Telephony: Fix to show operator names of both the SIMs
    }

    @Override
    public boolean isAvailable() {
        return !isUserRestricted() && !Utils.isWifiOnly(mContext);
    }

    public boolean isUserRestricted() {
        return mIsSecondaryUser ||
                RestrictedLockUtilsInternal.hasBaseUserRestriction(
                        mContext,
                        DISALLOW_CONFIG_MOBILE_NETWORKS,
                        myUserId());
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public String getPreferenceKey() {
        return KEY_MOBILE_NETWORK_SETTINGS;
    }

    class MobileNetworkTelephonyCallback extends TelephonyCallback implements
            TelephonyCallback.ServiceStateListener {
        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            updateDisplayName();
            updateState(mPreference);
        }
    }

    @OnLifecycleEvent(Event.ON_START)
    public void onStart() {
// QTI_BEGIN: 2018-05-23: Telephony: Fix to show operator names of both the SIMs
        if (mSubscriptionManager != null)
            mSubscriptionManager.addOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
// QTI_END: 2018-05-23: Telephony: Fix to show operator names of both the SIMs
        if (isAvailable()) {
            if (mTelephonyCallback == null) {
                mTelephonyCallback = new MobileNetworkTelephonyCallback();
            }
            mTelephonyManager.registerTelephonyCallback(
                    mContext.getMainExecutor(), mTelephonyCallback);
        }
        if (mAirplanModeChangedReceiver != null) {
            mContext.registerReceiver(mAirplanModeChangedReceiver,
                new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
        }
    }

// QTI_BEGIN: 2018-05-23: Telephony: Fix to show operator names of both the SIMs
    private void updateDisplayName() {
        if (mPreference != null) {
            List<SubscriptionInfo> list = mSubscriptionManager.getActiveSubscriptionInfoList();
            if (list != null && !list.isEmpty()) {
                boolean useSeparator = false;
                StringBuilder builder = new StringBuilder();
                for (SubscriptionInfo subInfo : list) {
                    if (isSubscriptionInService(subInfo.getSubscriptionId())) {
                        if (useSeparator) builder.append(", ");
                        builder.append(mTelephonyManager.getNetworkOperatorName
                                (subInfo.getSubscriptionId()));
                        useSeparator = true;
                    }
                }
                mSummary = builder.toString();
            } else {
                mSummary = mTelephonyManager.getNetworkOperatorName();
            }
        }
    }

    private boolean isSubscriptionInService(int subId) {
        if (mTelephonyManager != null) {
            if (mTelephonyManager.getServiceStateForSubscriber(subId).getState()
                    == ServiceState.STATE_IN_SERVICE) {
                return true;
            }
        }
        return false;
    }

    private final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangeListener
            = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
             updateDisplayName();
             updateState(mPreference);
        }
    };

// QTI_END: 2018-05-23: Telephony: Fix to show operator names of both the SIMs
    @OnLifecycleEvent(Event.ON_STOP)
    public void onStop() {
        if (mTelephonyCallback != null) {
            mTelephonyManager.unregisterTelephonyCallback(mTelephonyCallback);
        }
// QTI_BEGIN: 2018-05-23: Telephony: Fix to show operator names of both the SIMs
        mSubscriptionManager
                .removeOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
// QTI_END: 2018-05-23: Telephony: Fix to show operator names of both the SIMs
        if (mAirplanModeChangedReceiver != null) {
            mContext.unregisterReceiver(mAirplanModeChangedReceiver);
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        if (preference instanceof RestrictedPreference &&
            ((RestrictedPreference) preference).isDisabledByAdmin()) {
                return;
        }
        preference.setEnabled(Settings.Global.getInt(
            mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) == 0);
    }
// QTI_BEGIN: 2018-02-22: Android_UI: Enable proprietary MobileNetworkSettings

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
// QTI_END: 2018-02-22: Android_UI: Enable proprietary MobileNetworkSettings
        if (KEY_MOBILE_NETWORK_SETTINGS.equals(preference.getKey())) {
            final Intent intent = new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS);
            intent.setPackage(SETTINGS_PACKAGE_NAME);
// QTI_BEGIN: 2019-12-30: Telephony: Remove hooks that lauched vendor network settings
            mContext.startActivity(intent);
// QTI_END: 2019-12-30: Telephony: Remove hooks that lauched vendor network settings
// QTI_BEGIN: 2018-02-22: Android_UI: Enable proprietary MobileNetworkSettings
            return true;
        }
        return false;
    }
// QTI_END: 2018-02-22: Android_UI: Enable proprietary MobileNetworkSettings

    @Override
    public CharSequence getSummary() {
// QTI_BEGIN: 2018-05-23: Telephony: Fix to show operator names of both the SIMs
        return mSummary;
// QTI_END: 2018-05-23: Telephony: Fix to show operator names of both the SIMs
    }
}
