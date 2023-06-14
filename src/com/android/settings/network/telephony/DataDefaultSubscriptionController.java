/*
 * Copyright (c) 2020, The Linux Foundation. All rights reserved
 * Not a contribution
 *
 * Copyright (C) 2019 The Android Open Source Project
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

import android.content.Context;
import android.provider.Settings;
import android.sysprop.TelephonyProperties;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.mobile.dataservice.SubscriptionInfoEntity;

public class DataDefaultSubscriptionController extends DefaultSubscriptionController {

    private static final String SETTING_USER_PREF_DATA_SUB = "user_preferred_data_sub";
    private SubscriptionInfoEntity mSubscriptionInfoEntity;
    private boolean mHasAnyOngoingCallOnDevice = false;
    private TelephonyManager mTelephonyManager;

    public DataDefaultSubscriptionController(Context context, String preferenceKey,
              Lifecycle lifecycle, LifecycleOwner lifecycleOwner) {
        super(context, preferenceKey, lifecycle, lifecycleOwner);
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
    }

    @Override
    protected int getDefaultSubscriptionId() {
        int defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        for (SubscriptionInfoEntity subInfo : mSubInfoEntityList) {
            int subId = subInfo.getSubId();
            if (subInfo.isActiveSubscriptionId && subId == defaultDataSubId) {
                mSubscriptionInfoEntity = subInfo;
                return subId;
            }
        }
        return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }


    @Override
    protected void setDefaultSubscription(int subscriptionId) {
        mManager.setDefaultDataSubId(subscriptionId);
        setUserPrefDataSubIdInDb(subscriptionId);
    }

    @Override
    public CharSequence getSummary() {
        boolean isSmartDdsEnabled = isSmartDdsEnabled();
        if (isSmartDdsEnabled) {
            return mContext.getString(R.string.dds_preference_smart_dds_switch_is_on);
        }
        return MobileNetworkUtils.getPreferredStatus(isRtlMode(),
                mContext, false, true, mSubInfoEntityList);
    }

    private void setUserPrefDataSubIdInDb(int subId) {
        android.provider.Settings.Global.putInt(mContext.getContentResolver(),
                SETTING_USER_PREF_DATA_SUB, subId);
    }

    @Override
    public void onDefaultDataChanged(int defaultDataSubId) {
        updateEntries();
        refreshSummary(mPreference);
    }

    @Override
    protected boolean isAskEverytimeSupported() {
        return false;
    }

    @Override
    public void onAnyOngoingCallOnDevice(boolean isAnyCallOngoing) {
        mHasAnyOngoingCallOnDevice = isAnyCallOngoing;
        updateEntries();
        refreshSummary(mPreference);
    }

    @Override
    public void onResume() {
        super.onResume();
        mHasAnyOngoingCallOnDevice = mMobileNetworkRepository.isAnyOngoingCallOnDevice();
    }

    private boolean hasAnyOngoingCallOnDevice() {
        return mHasAnyOngoingCallOnDevice;
    }

    @Override
    protected void updatePreferenceState(Preference preference) {
        if (preference != null) {
            boolean isEcbmEnabled = mTelephonyManager.getEmergencyCallbackMode();
            boolean isScbmEnabled = TelephonyProperties.in_scbm().orElse(false);
            boolean isSmartDdsEnabled = isSmartDdsEnabled();

            if (!isSmartDdsEnabled) {
                preference.setEnabled(!hasAnyOngoingCallOnDevice() && !isEcbmEnabled
                        && !isScbmEnabled
                        && (!TelephonyUtils.isSubsidyFeatureEnabled(mContext)
                        || TelephonyUtils.allowUsertoSetDDS(mContext)));
            } else {
                preference.setEnabled(false);
            }
        }
    }

    private boolean isSmartDdsEnabled() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.SMART_DDS_SWITCH, 0) == 1;
    }
}
