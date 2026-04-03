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

package com.android.settings.deviceinfo;

import android.content.Context;
// QTI_BEGIN: 2019-03-19: Android_UI: Settings: support CT chipset PA requirements
import android.net.wifi.WifiInfo;
import android.util.Log;
// QTI_END: 2019-03-19: Android_UI: Settings: support CT chipset PA requirements

import com.android.settings.R;
// QTI_BEGIN: 2019-03-19: Android_UI: Settings: support CT chipset PA requirements
import com.android.settings.Utils;
// QTI_END: 2019-03-19: Android_UI: Settings: support CT chipset PA requirements
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.deviceinfo.AbstractWifiMacAddressPreferenceController;
// QTI_BEGIN: 2019-03-19: Android_UI: Settings: support CT chipset PA requirements
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
// QTI_END: 2019-03-19: Android_UI: Settings: support CT chipset PA requirements

/**
 * Concrete subclass of WIFI MAC address preference controller
 */
public class WifiMacAddressPreferenceController extends AbstractWifiMacAddressPreferenceController
        implements PreferenceControllerMixin {
    public WifiMacAddressPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, lifecycle);
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(R.bool.config_show_wifi_mac_address);
    }

// QTI_BEGIN: 2019-03-19: Android_UI: Settings: support CT chipset PA requirements
    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (Utils.isSupportCTPA(mContext)) {
            Preference macAddressPreference = screen.findPreference(getPreferenceKey());
            CharSequence oldValue = macAddressPreference.getSummary();
            String macAddress = Utils.getString(mContext, Utils.KEY_WIFI_MAC_ADDRESS);
            String unAvailable = mContext.getString(
                    com.android.settingslib.R.string.status_unavailable);
            Log.d(TAG, "displayPreference: macAddress = " + macAddress
                    + " oldValue = " + oldValue + " unAvailable = " + unAvailable);
            if (null == macAddress || macAddress.isEmpty()) {
                macAddress = unAvailable;
            }
            if (null != oldValue && (WifiInfo.DEFAULT_MAC_ADDRESS.equals(oldValue) ||
                    unAvailable.equals(oldValue))) {
                macAddressPreference.setSummary(macAddress);
            }
        }
    }
// QTI_END: 2019-03-19: Android_UI: Settings: support CT chipset PA requirements
    // This space intentionally left blank
}
