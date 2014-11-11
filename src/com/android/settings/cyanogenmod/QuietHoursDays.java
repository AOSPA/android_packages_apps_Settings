/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package com.android.settings.cyanogenmod;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.cyanogenmod.SystemSettingCheckBoxPreference;

public class QuietHoursDays extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener  {

    private static final String KEY_QUIET_HOURS_MONDAY = "quiet_hours_monday";
    private static final String KEY_QUIET_HOURS_TUESDAY = "quiet_hours_tuesday";
    private static final String KEY_QUIET_HOURS_WEDNESDAY = "quiet_hours_wednesday";
    private static final String KEY_QUIET_HOURS_THURSDAY = "quiet_hours_thursday";
    private static final String KEY_QUIET_HOURS_FRIDAY = "quiet_hours_friday";
    private static final String KEY_QUIET_HOURS_SATURDAY = "quiet_hours_saturday";
    private static final String KEY_QUIET_HOURS_SUNDAY = "quiet_hours_sunday";

    private CheckBoxPreference mQuietHoursMonday;
    private CheckBoxPreference mQuietHoursTuesday;
    private CheckBoxPreference mQuietHoursWednesday;
    private CheckBoxPreference mQuietHoursThursday;
    private CheckBoxPreference mQuietHoursFriday;
    private CheckBoxPreference mQuietHoursSaturday;
    private CheckBoxPreference mQuietHoursSunday;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getPreferenceManager() != null) {
            addPreferencesFromResource(R.xml.quiet_hours_settings_days);

            mQuietHoursMonday = (CheckBoxPreference) findPreference(KEY_QUIET_HOURS_MONDAY);
            mQuietHoursTuesday = (CheckBoxPreference) findPreference(KEY_QUIET_HOURS_TUESDAY);
            mQuietHoursWednesday = (CheckBoxPreference) findPreference(KEY_QUIET_HOURS_WEDNESDAY);
            mQuietHoursThursday = (CheckBoxPreference) findPreference(KEY_QUIET_HOURS_THURSDAY);
            mQuietHoursFriday = (CheckBoxPreference) findPreference(KEY_QUIET_HOURS_FRIDAY);
            mQuietHoursSaturday = (CheckBoxPreference) findPreference(KEY_QUIET_HOURS_SATURDAY);
            mQuietHoursSunday = (CheckBoxPreference) findPreference(KEY_QUIET_HOURS_SUNDAY);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getApplicationContext().getContentResolver();
        boolean value;
        if (preference == mQuietHoursMonday) {
            value = mQuietHoursMonday.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(), Settings.System.QUIET_HOURS_MONDAY, value ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursTuesday) {
            value = mQuietHoursTuesday.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(), Settings.System.QUIET_HOURS_TUESDAY, value ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursWednesday) {
            value = mQuietHoursWednesday.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(), Settings.System.QUIET_HOURS_WEDNESDAY, value ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursThursday) {
            value = mQuietHoursThursday.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(), Settings.System.QUIET_HOURS_THURSDAY, value ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursFriday) {
            value = mQuietHoursFriday.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(), Settings.System.QUIET_HOURS_FRIDAY, value ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursSaturday) {
            value = mQuietHoursSaturday.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(), Settings.System.QUIET_HOURS_SATURDAY, value ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursSunday) {
            value = mQuietHoursSunday.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(), Settings.System.QUIET_HOURS_SUNDAY, value ? 1 : 0);
            return true;
        }
        return false;
    }
}
