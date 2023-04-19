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
 import android.telephony.SubscriptionManager;
 import androidx.lifecycle.LifecycleOwner;
 import com.android.settings.R;
 import com.android.settingslib.core.lifecycle.Lifecycle;
 import com.android.settingslib.mobile.dataservice.SubscriptionInfoEntity;

public class DataDefaultSubscriptionController extends DefaultSubscriptionController {

    private static final String SETTING_USER_PREF_DATA_SUB = "user_preferred_data_sub";
    private SubscriptionInfoEntity mSubscriptionInfoEntity;

    public DataDefaultSubscriptionController(Context context, String preferenceKey,
              Lifecycle lifecycle, LifecycleOwner lifecycleOwner) {
          super(context, preferenceKey, lifecycle, lifecycleOwner);
    }

    @Override
    protected SubscriptionInfoEntity getDefaultSubscriptionInfo() {
        return mSubscriptionInfoEntity;
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
        boolean isSmartDdsEnabled = Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.SMART_DDS_SWITCH, 0) == 1;
        if (isSmartDdsEnabled) {
            return mContext.getString(R.string.dds_preference_smart_dds_switch_is_on);
        }
        return super.getSummary();
    }

    private void setUserPrefDataSubIdInDb(int subId) {
        android.provider.Settings.Global.putInt(mContext.getContentResolver(),
                SETTING_USER_PREF_DATA_SUB, subId);
    }
}
