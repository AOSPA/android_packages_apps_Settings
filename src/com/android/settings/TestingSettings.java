/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings;

import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.UserManager;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settingslib.utils.ThreadUtils;

public class TestingSettings extends SettingsPreferenceFragment {


    private BroadcastReceiver mCarrierConfigReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.testing_settings);
        final Context context = getContext();
        if (context == null) return;
        ThreadUtils.postOnBackgroundThread(() -> {
            boolean isVisible = isRadioInfoVisible(context);
            if (!isVisible) {
                context.getMainExecutor().execute(this::removePhoneInfoOptionsFromHiddenMenu);
            }
        });

        if (isUserBuild()) {
            mCarrierConfigReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED
                            .equals(intent.getAction())) {
                        ThreadUtils.postOnBackgroundThread(() -> {
                            if (isRadioInfoAccessRestricted(context)) {
                                context.getMainExecutor().execute(
                                        TestingSettings.this::removePhoneInfoOptionsFromHiddenMenu);
                            }
                        });
                    }
                }
            };
            IntentFilter filter =
                    new IntentFilter(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED);
            context.registerReceiver(mCarrierConfigReceiver, filter, Context.RECEIVER_EXPORTED);
        }
    }

    @VisibleForTesting
    void removePhoneInfoOptionsFromHiddenMenu() {
        PreferenceScreen screen = getPreferenceScreen();
        if (screen == null) {
            return;
        }

        Preference radioInfoPref = findPreference("radio_info_settings");
        if (radioInfoPref != null) {
            screen.removePreference(radioInfoPref);
        }

        Preference phoneInfoPref = findPreference("phone_information_v2");
        if (phoneInfoPref != null) {
            screen.removePreference(phoneInfoPref);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Context context = getContext();
        if (context != null) {
            if (mCarrierConfigReceiver != null) {
                context.unregisterReceiver(mCarrierConfigReceiver);
            }
        }
    }

    @VisibleForTesting
    protected boolean isRadioInfoVisible(Context context) {
        UserManager um = context.getSystemService(UserManager.class);
        if (um != null) {
            if (!um.isAdminUser()) {
                return false;
            }
        }
        if (MobileNetworkUtils.isMobileNetworkUserRestricted(context)) {
            return false;
        }
        return !isRadioInfoAccessRestricted(context);
    }

    private boolean isUserBuild() {
        return "user".equals(Build.TYPE);
    }

    private boolean isRadioInfoAccessRestricted(Context context) {
        if (!isUserBuild()) return false;
        TelephonyManager tm = context.getSystemService(TelephonyManager.class);
        if (tm == null) {
            return false;
        }
        int phoneCount = tm.getActiveModemCount();

        for (int i = 0; i < phoneCount; i++) {
            int subId = SubscriptionManager.getSubscriptionId(i);
            if (SubscriptionManager.isValidSubscriptionId(subId)) {
                if (isRadioInfoDisabled(context, subId)) {
                    // ANY SIM restricted -> Restricted access
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isRadioInfoDisabled(Context context, int subId) {
        CarrierConfigManager configManager = context.getSystemService(CarrierConfigManager.class);
        if (configManager != null) {
            PersistableBundle b = configManager.getConfigForSubId(subId);
            if (b != null) {
                return b.getBoolean(CarrierConfigManager.KEY_HIDE_RADIO_INFO_ON_USER_BUILD_BOOL,
                        false);
            }
        }
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.TESTING;
    }
}
