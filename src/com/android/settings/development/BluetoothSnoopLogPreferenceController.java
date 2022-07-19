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

package com.android.settings.development;

import android.content.Context;
import android.os.Build;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;

import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import java.util.Arrays;
import java.util.List;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class BluetoothSnoopLogPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String PREFERENCE_KEY = "bt_hci_snoop_log";
    @VisibleForTesting
    static final int BTSNOOP_LOG_MODE_DISABLED_INDEX = 0;
    @VisibleForTesting
    static final int BTSNOOP_LOG_MODE_FILTERED_INDEX = 1;
    @VisibleForTesting
    static final int BTSNOOP_LOG_MODE_FULL_INDEX = 2;
    @VisibleForTesting
    static final int BTSNOOP_LOG_MODE_SNOOPHEADERSFILTERED_INDEX = 3;
    @VisibleForTesting
    static final int BTSNOOP_LOG_MODE_MEDIAPKTSFILTERED_INDEX = 4;
    @VisibleForTesting

    static final String BLUETOOTH_BTSNOOP_LOG_MODE_PROPERTY = "persist.bluetooth.btsnooplogmode";
    static final String BLUETOOTH_BTSNOOP_LOG_MODE_PROPERTY_ADV = "persist.vendor.service.bt.adv_snoop";

    private final String[] mListValues;
    private final String[] mListEntries;
    private final List<String> mListEnhancedValues;
    private final String emptyVal = null;


    public BluetoothSnoopLogPreferenceController(Context context) {
        super(context);
        mListValues = context.getResources().getStringArray(R.array.bt_hci_snoop_log_values);
        mListEntries = context.getResources().getStringArray(R.array.bt_hci_snoop_log_entries);
        mListEnhancedValues = Arrays.asList(context.getResources().getStringArray(
                R.array.bt_hci_snoop_log_values_enhanced));
    }

    // Default mode is DISABLED. It can also be changed by modifying the global setting.
    public int getDefaultModeIndex() {
        if (!Build.IS_DEBUGGABLE) {
            return BTSNOOP_LOG_MODE_DISABLED_INDEX;
        }

        final String default_mode = Settings.Global.getString(mContext.getContentResolver(),
                Settings.Global.BLUETOOTH_BTSNOOP_DEFAULT_MODE);

        for (int i = 0; i < mListValues.length; i++) {
            if (TextUtils.equals(default_mode, mListValues[i])) {
                return i;
            }
        }

        return BTSNOOP_LOG_MODE_DISABLED_INDEX;
    }

    @Override
    public String getPreferenceKey() {
        return PREFERENCE_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String val = newValue.toString();
        if(mListEnhancedValues.contains(val)) {
            SystemProperties.set(BLUETOOTH_BTSNOOP_LOG_MODE_PROPERTY_ADV, val);
            SystemProperties.set(BLUETOOTH_BTSNOOP_LOG_MODE_PROPERTY, emptyVal);
        } else {
            SystemProperties.set(BLUETOOTH_BTSNOOP_LOG_MODE_PROPERTY, val);
            SystemProperties.set(BLUETOOTH_BTSNOOP_LOG_MODE_PROPERTY_ADV, emptyVal);
        }
        updateState(mPreference);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final ListPreference listPreference = (ListPreference) preference;
        String value = SystemProperties.get(BLUETOOTH_BTSNOOP_LOG_MODE_PROPERTY);
        String valueAdv = SystemProperties.get(BLUETOOTH_BTSNOOP_LOG_MODE_PROPERTY_ADV);
        final String currentValue = (TextUtils.isEmpty(value) ? valueAdv : value);

        int index = getDefaultModeIndex();
        for (int i = 0; i < mListValues.length; i++) {
            if (TextUtils.equals(currentValue, mListValues[i])) {
                index = i;
                break;
            }
        }
        if( index < mListValues.length && index < mListEntries.length ) {
            listPreference.setValue(mListValues[index]);
            listPreference.setSummary(mListEntries[index]);
        } else {
            Log.e(TAG, "missing some entries in xml file"
             + "\t some options in developer options will not be shown until added in xml file");
        }
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        SystemProperties.set(BLUETOOTH_BTSNOOP_LOG_MODE_PROPERTY, null);
        SystemProperties.set(BLUETOOTH_BTSNOOP_LOG_MODE_PROPERTY_ADV, null);
        ((ListPreference) mPreference).setValue(mListValues[getDefaultModeIndex()]);
        ((ListPreference) mPreference).setSummary(mListEntries[getDefaultModeIndex()]);
    }
}
