/*
 * Copyright (C) 2018 Paranoid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.fuelgauge;

import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.MasterSwitchPreference;
import com.android.settingslib.core.AbstractPreferenceController;

import static android.provider.Settings.System.SHOW_BATTERY_PERCENT;

class BatteryPercentageController extends AbstractPreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String KEY_BATTERY_PERCENTAGE = "battery_percentage_summary_pa";
    private MasterSwitchPreference mBatteryPercentagePref;

    public BatteryPercentageController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BATTERY_PERCENTAGE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mBatteryPercentagePref = (MasterSwitchPreference) screen.findPreference(KEY_BATTERY_PERCENTAGE);
    }

    @Override
    public void updateState(Preference preference) {
        int setting = Settings.System.getInt(mContext.getContentResolver(),
                SHOW_BATTERY_PERCENT, 0);
        mBatteryPercentagePref.setChecked(setting == 1 || setting == 2);
        updateSummary();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean percentageOn = (Boolean) newValue;
        Settings.System.putInt(mContext.getContentResolver(), SHOW_BATTERY_PERCENT,
                percentageOn ? 1 : 0);
        updateSummary();
        return true;
    }

    private void updateSummary() {
        String summary = null;
        int setting = Settings.System.getInt(mContext.getContentResolver(),
                SHOW_BATTERY_PERCENT, 0);
        switch (setting) {
            case 0:
                summary = mContext.getString(R.string.battery_percentage_disabled_summary);
                break;
            case 1:
                summary = mContext.getString(R.string.battery_percentage_enabled_next_summary);
                break;
            case 2:
                summary = mContext.getString(R.string.battery_percentage_enabled_inside_summary);
                break;
        }
        mBatteryPercentagePref.setSummary(summary);
    }
}
