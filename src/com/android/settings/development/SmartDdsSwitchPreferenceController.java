/*
Copyright (c) 2021 The Linux Foundation. All rights reserved.

Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are
 met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
 copyright notice, this list of conditions and the following
 disclaimer in the documentation and/or other materials provided
 with the distribution.
 * Neither the name of The Linux Foundation nor the names of its
 contributors may be used to endorse or promote products derived
 from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
