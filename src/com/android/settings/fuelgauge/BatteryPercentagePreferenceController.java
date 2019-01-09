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

import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.core.BasePreferenceController;

import static android.provider.Settings.Secure.STATUS_BAR_BATTERY_STYLE;
import static android.provider.Settings.System.SHOW_BATTERY_PERCENT;

public class BatteryPercentagePreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    private ListPreference mBatteryPercentagePref;
    private int mBatteryStyleSelected;

    public BatteryPercentagePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        mBatteryStyleSelected = Settings.Secure.getInt(mContext.getContentResolver(), STATUS_BAR_BATTERY_STYLE, 0);
        // Disable pref when text battery style is selected
        if (mBatteryStyleSelected == 2)
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
}
