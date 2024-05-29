/*
 * Copyright (C) 2024 The Android Open Source Project
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

 * Changes from Qualcomm Innovation Center are provided under the following license:
 * Copyright (c) 2024 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
*/

package com.android.settings.development;

import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class BluetoothXpanPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String TAG = "BluetoothXpanPreferenceController";

    private static final boolean DBG = Log.isLoggable(TAG, Log.DEBUG);

    private static final String BLUETOOTH_XPAN_DEV_SETTINGS_KEY
            = "bluetooth_xpan_dev_settings";

    private static final String PROP_BLUETOOTH_XPAN_PLATFORM_SUPPORT
            = "persist.vendor.qti.btadvaudio.target.support.xpan";

    private static final String PROP_BLUETOOTH_XPAN_ENABLE_DISABLE
            = "persist.vendor.service.bt.adv_transport";

    private static boolean mXpanSupport = false;

    public BluetoothXpanPreferenceController(Context context) {
        super(context);
        // Later change default to false
        mXpanSupport = SystemProperties.getBoolean(PROP_BLUETOOTH_XPAN_PLATFORM_SUPPORT, false);
        if (DBG) Log.d(TAG, "mXpanSupport " + mXpanSupport
                + " PROP_BLUETOOTH_XPAN_PLATFORM_SUPPORT " + PROP_BLUETOOTH_XPAN_PLATFORM_SUPPORT
                + " PROP_BLUETOOTH_XPAN_ENABLE_DISABLE " + PROP_BLUETOOTH_XPAN_ENABLE_DISABLE);
    }

    @Override
    public String getPreferenceKey() {
        return BLUETOOTH_XPAN_DEV_SETTINGS_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference.setVisible(mXpanSupport);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        SystemProperties.set(PROP_BLUETOOTH_XPAN_ENABLE_DISABLE, isEnabled ? "true" : "false");
        if (DBG) Log.d(TAG, "onPreferenceChange isEnabled " + isEnabled);
       return true;
    }

    @Override
    public void updateState(Preference preference) {
        final boolean isEnabled = SystemProperties.getBoolean(PROP_BLUETOOTH_XPAN_ENABLE_DISABLE, false);
        ((SwitchPreference) mPreference).setChecked(isEnabled);
        if (DBG) Log.d(TAG, "updateState " + isEnabled);
    }
}
