/* Copyright (c) 2021 The Linux Foundation. All rights reserved.
 *
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

package com.android.settings.development;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class SmartDdsSwitchPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String TAG = "SMART_DDS_SWITCH";

    public static final String ACTION_SMART_DDS_SWITCH_TOGGLED =
            "com.qualcomm.qti.telephonyservice.SMART_DDS_SWITCH_TOGGLED";
    private static final String SMART_DDS_SWITCH_TOGGLE_VALUE = "smartDdsSwitchValue";

    private static final int SETTING_VALUE_ON = 1;
    private static final int SETTING_VALUE_OFF = 0;

    private Context mContext;
    private final TelephonyManager mTelephonyManager;

    public SmartDdsSwitchPreferenceController(Context context) {
        super(context);
        mContext = context;
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
    }

    @Override
    public String getPreferenceKey() {
        return Settings.Global.SMART_DDS_SWITCH;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        Intent intent = new Intent(ACTION_SMART_DDS_SWITCH_TOGGLED);
        intent.putExtra(SMART_DDS_SWITCH_TOGGLE_VALUE, isEnabled);
        Log.d(TAG, "onPreferenceChange: isEnabled = " + isEnabled);
        mContext.sendBroadcastAsUser(intent, UserHandle.SYSTEM,
                "android.permission.MODIFY_PHONE_STATE");
        putSwitchValue(isEnabled ? SETTING_VALUE_ON : SETTING_VALUE_OFF);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final int smartDdsSwitch = getSwitchValue();
        ((SwitchPreference) mPreference).setChecked(smartDdsSwitch != SETTING_VALUE_OFF);
    }

    @Override
    public boolean isAvailable() {
        // Only show the toggle if more than one phone is active
        if (mTelephonyManager != null) {
            return mTelephonyManager.getActiveModemCount() > 1;
        } else {
            Log.e(TAG, "mTelephonyManager null");
            return false;
        }
    }

    private void putSwitchValue(int state) {
        Settings.Global.putInt(mContext.getContentResolver(), getPreferenceKey(), state);
    }

    private int getSwitchValue() {
        return Settings.Global.getInt(mContext.getContentResolver(), getPreferenceKey(),
                SETTING_VALUE_OFF);
    }
}