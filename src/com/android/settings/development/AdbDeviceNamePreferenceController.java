/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.development;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

/**
 * Controller for the device name preference in the Wireless debugging
 * fragment.
 */
public class AdbDeviceNamePreferenceController extends BasePreferenceController {
    public AdbDeviceNamePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setSummary(getSummary());
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }

        final Intent intent = new Intent(Settings.DEVICE_NAME_SETTINGS);
        mContext.startActivity(intent);
        return true;
    }


    @Override
    public CharSequence getSummary() {
        // Keep device name in sync with Settings > About phone > Device name
        String deviceName = Settings.Global.getString(mContext.getContentResolver(),
                Settings.Global.DEVICE_NAME);
        if (deviceName == null) {
            deviceName = Build.MODEL;
        }
        return deviceName;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }
}
