/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.applications.specialaccess.premiumsms;

import android.content.Context;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import java.util.Arrays;
import java.util.Locale;

public class PremiumSmsController extends BasePreferenceController {

    public PremiumSmsController(Context context, String key) {
        super(context, key);
    }

    @AvailabilityStatus
    public int getAvailabilityStatus() {
        if (!mContext.getResources().getBoolean(R.bool.config_show_premium_sms)) {
            return UNSUPPORTED_ON_DEVICE;
        } else if (isAllSimAreJp()) {
            // According to b/413539432, it said "Premium SMS is not provided by carriers in
            // Japan anymore". Hence here check the SIM locale and if SIM cards are all Japan
            // locale,  Premium SMS shall be unavailable to show on list.
            return CONDITIONALLY_UNAVAILABLE;
        }
        return AVAILABLE;
    }

    private boolean isAllSimAreJp() {
        SubscriptionManager subscriptionManager = mContext.getSystemService(
                SubscriptionManager.class);
        TelephonyManager telephonyManager = mContext.getSystemService(TelephonyManager.class);
        if (subscriptionManager == null || telephonyManager == null) {
            return false;
        }
        int[] subActiveSims = subscriptionManager.getActiveSubscriptionIdList(true);
        if (subActiveSims.length == 0) {
            return false;
        }
        // If all SIMs are JP, do not show this function UI on list.
        return Arrays.stream(subActiveSims).allMatch(subId -> {
            TelephonyManager subTelephonyManager = telephonyManager.createForSubscriptionId(subId);
            if (subTelephonyManager == null) {
                return false;
            }
            String country = subTelephonyManager.getSimCountryIso();
            if (country == null) {
                return false;
            }
            return country.equalsIgnoreCase(Locale.JAPAN.getCountry());
        });
    }
}
