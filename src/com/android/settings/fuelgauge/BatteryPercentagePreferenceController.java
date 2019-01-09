/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.fuelgauge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.core.BasePreferenceController;

import static android.provider.Settings.Secure.STATUS_BAR_BATTERY_STYLE;
import static android.provider.Settings.System.SHOW_BATTERY_PERCENT;

public class BatteryPercentagePreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    private BroadcastReceiver mBatteryStyleChangedReceiver;
    private ListPreference mBatteryPercentagePref;

    public BatteryPercentagePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);

        mBatteryStyleChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateState(mBatteryPercentagePref);
            }
        };
    }

    @Override
    public int getAvailabilityStatus() {
        if (disableBatteryPercentagePref())
            return DISABLED_DEPENDENT_SETTING;
        else
            return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mBatteryPercentagePref = (ListPreference) screen.findPreference(getPreferenceKey());
        int value = Settings.System.getInt(mContext.getContentResolver(), SHOW_BATTERY_PERCENT, 0);
        mBatteryPercentagePref.setValue(Integer.toString(value));

        if (mBatteryStyleChangedReceiver != null) {
            mContext.registerReceiver(mBatteryStyleChangedReceiver,
                    new IntentFilter("android.intent.action.BATTERY_STYLE"));
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(!disableBatteryPercentagePref());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int value = Integer.parseInt((String) newValue);
        Settings.System.putInt(mContext.getContentResolver(), SHOW_BATTERY_PERCENT, value);
        refreshSummary(preference);
        return true;
    }

    @Override
    public CharSequence getSummary() {
        int value = Settings.System.getInt(mContext.getContentResolver(), SHOW_BATTERY_PERCENT, 0);
        int index = mBatteryPercentagePref.findIndexOfValue(Integer.toString(value));
        return mBatteryPercentagePref.getEntries()[index];
    }

    private boolean disableBatteryPercentagePref() {
        int value = Settings.Secure.getInt(mContext.getContentResolver(), STATUS_BAR_BATTERY_STYLE, 0);

        // Disable battery percentage preference when text or hidden battery style is selected
        if (value == 2 || value == 3)
            return true;
        else
            return false;
    }
}
