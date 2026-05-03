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

package com.android.settings.network.telephony;

import android.annotation.Nullable;
import android.content.Context;
import android.telephony.TelephonyManager;

import com.android.settings.Utils;

/** A repository for Telephony feature with application context. */
public class TelephonySettingsRepository {
    private final Context mContext;
    @Nullable
    private TelephonyManager mTelephonyManager;

    public TelephonySettingsRepository(Context appContext) {
        mContext = appContext;
    }

    @Nullable
    private TelephonyManager getTelephonyManagerForSubId(int subId) {
        if (mTelephonyManager == null) {
            mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        }
        return (mTelephonyManager == null) ? null
                : mTelephonyManager.createForSubscriptionId(subId);
    }

    /** Checks mobile data cable is connected */
    public boolean isMobileDataCable() {
        return Utils.isMobileDataCapable(mContext);
    }

    /** Checks mobile data switch policy is enabled */
    public boolean isMobileDataSwitchPolicyEnabled(int subId) {
        TelephonyManager tm = getTelephonyManagerForSubId(subId);
        return tm != null && tm.isMobileDataPolicyEnabled(
                TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH
        );
    }

    /** Sets mobile data switch policy enabled or not */
    public void setMobileDataSwitchPolicyEnabled(int subId, boolean isEnabled) {
        TelephonyManager tm = getTelephonyManagerForSubId(subId);
        if (tm != null) {
            tm.setMobileDataPolicyEnabled(
                    TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH,
                    isEnabled
            );
        }
    }
    /** Checks data roaming is enabled */
    public boolean isDataRoamingEnabled(int subId) {
        TelephonyManager tm = getTelephonyManagerForSubId(subId);
        return tm != null && tm.isDataRoamingEnabled();
    }

    /** Sets data roaming enabled or not */
    public void setDataRoamingEnabled(int subId, boolean isEnabled) {
        TelephonyManager tm = getTelephonyManagerForSubId(subId);
        if (tm != null) {
            tm.setDataRoamingEnabled(isEnabled);
        }
    }

    /**
     * Get Sim Operator Name for the given subscription id.
     *
     * @param subId The subscription id.
     * @return The sim operator name.
     */
    public String getSimOperatorName(int subId) {
        if (mTelephonyManager == null) {
            mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        }
        if (mTelephonyManager == null) {
            return "";
        }
        return mTelephonyManager.getSimOperatorName(subId);
    }
}
