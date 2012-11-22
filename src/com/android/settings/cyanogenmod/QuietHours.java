/*
 * Copyright (C) 2012 The CyanogenMod Project
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
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class QuietHours extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener  {

    private static final String TAG = "QuietHours";
    private static final String KEY_QUIET_HOURS_TIMERANGE = "quiet_hours_timerange";
    private static final CharSequence KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled";
    private static final CharSequence KEY_QUIET_HOURS_RINGER = "quiet_hours_ringer";
    private static final CharSequence KEY_QUIET_HOURS_MUTE = "quiet_hours_mute";
    private static final CharSequence KEY_QUIET_HOURS_HAPTIC = "quiet_hours_haptic";
    private static final CharSequence KEY_QUIET_HOURS_SYSTEM = "quiet_hours_system";
    private static final CharSequence KEY_QUIET_HOURS_STILL = "quiet_hours_still";
    private static final CharSequence KEY_QUIET_HOURS_DIM = "quiet_hours_dim";

    private TimeRangePreference mQuietHoursTimeRange;
    private ListPreference mQuietHoursRinger;
    private CheckBoxPreference mQuietHoursEnabled;
    private CheckBoxPreference mQuietHoursMute;
    private CheckBoxPreference mQuietHoursHaptic;
    private CheckBoxPreference mQuietHoursSystem;
    private CheckBoxPreference mQuietHoursStill;
    private CheckBoxPreference mQuietHoursDim;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.quiet_hours_settings);
        Resources res = getResources();
        ContentResolver resolver = getContentResolver();

        // Load checkboxes
        mQuietHoursEnabled = (CheckBoxPreference) findPreference(KEY_QUIET_HOURS_ENABLED);
        mQuietHoursEnabled.setChecked(Settings.System.getInt(resolver,
                Settings.System.QUIET_HOURS_ENABLED, 0) == 1);
        mQuietHoursEnabled.setOnPreferenceChangeListener(this);
        mQuietHoursMute = (CheckBoxPreference) findPreference(KEY_QUIET_HOURS_MUTE);
        mQuietHoursMute.setChecked(Settings.System.getInt(resolver,
                Settings.System.QUIET_HOURS_MUTE, 0) == 1);
        mQuietHoursMute.setOnPreferenceChangeListener(this);
        mQuietHoursHaptic = (CheckBoxPreference) findPreference(KEY_QUIET_HOURS_HAPTIC);
        mQuietHoursHaptic.setChecked(Settings.System.getInt(resolver,
                Settings.System.QUIET_HOURS_HAPTIC, 0) == 1);
        mQuietHoursHaptic.setOnPreferenceChangeListener(this);
        mQuietHoursSystem = (CheckBoxPreference) findPreference(KEY_QUIET_HOURS_SYSTEM);
        mQuietHoursSystem.setChecked(Settings.System.getInt(resolver,
                Settings.System.QUIET_HOURS_SYSTEM, 0) == 1);
        mQuietHoursSystem.setOnPreferenceChangeListener(this);
        mQuietHoursStill = (CheckBoxPreference) findPreference(KEY_QUIET_HOURS_STILL);
        mQuietHoursStill.setChecked(Settings.System.getInt(resolver,
                Settings.System.QUIET_HOURS_STILL, 0) == 1);
        mQuietHoursStill.setOnPreferenceChangeListener(this);
        mQuietHoursDim = (CheckBoxPreference) findPreference(KEY_QUIET_HOURS_DIM);
        mQuietHoursDim.setChecked(Settings.System.getInt(resolver,
                Settings.System.QUIET_HOURS_DIM, 0) == 1);
        mQuietHoursDim.setOnPreferenceChangeListener(this);

        // Load the preferences
        mQuietHoursTimeRange =
                (TimeRangePreference) findPreference(KEY_QUIET_HOURS_TIMERANGE);
        mQuietHoursTimeRange.setTimeRange(
                Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_START, 0),
                Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_END, 0));
        mQuietHoursTimeRange.setOnPreferenceChangeListener(this);


        // Remove the ringer setting on non-telephony devices else enable it
        mQuietHoursRinger = (ListPreference) findPreference(KEY_QUIET_HOURS_RINGER);
        TelephonyManager telephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE) {
            getPreferenceScreen().removePreference(mQuietHoursRinger);
            mQuietHoursRinger = null;
        } else {
            int muteType = Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_RINGER, 0);
            mQuietHoursRinger.setValue(String.valueOf(muteType));
            mQuietHoursRinger.setSummary(mQuietHoursRinger.getEntry());
            mQuietHoursRinger.setOnPreferenceChangeListener(this);
        }

        // Remove the notification light setting if the device does not support it
        if (!res.getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed)) {
            removePreference(Settings.System.QUIET_HOURS_DIM);
        }

        // Remove the vibrator dependent settings if the device does not have a vibrator
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) {
               removePreference(Settings.System.QUIET_HOURS_STILL);
               removePreference(Settings.System.QUIET_HOURS_HAPTIC);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // reload
        ContentResolver resolver = getContentResolver();
        mQuietHoursEnabled.setChecked(Settings.System.getInt(resolver,
                Settings.System.QUIET_HOURS_ENABLED, 0) == 1);
        mQuietHoursMute.setChecked(Settings.System.getInt(resolver,
                Settings.System.QUIET_HOURS_MUTE, 0) == 1);
        mQuietHoursHaptic.setChecked(Settings.System.getInt(resolver,
                Settings.System.QUIET_HOURS_HAPTIC, 0) == 1);
        mQuietHoursSystem.setChecked(Settings.System.getInt(resolver,
                Settings.System.QUIET_HOURS_SYSTEM, 0) == 1);
        mQuietHoursStill.setChecked(Settings.System.getInt(resolver,
                Settings.System.QUIET_HOURS_STILL, 0) == 1);
        mQuietHoursDim.setChecked(Settings.System.getInt(resolver,
                Settings.System.QUIET_HOURS_DIM, 0) == 1);
        mQuietHoursTimeRange.setTimeRange(
                Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_START, 0),
                Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_END, 0));
        if(mQuietHoursRinger != null) {
            int muteType = Settings.System.getInt(resolver, Settings.System.QUIET_HOURS_RINGER, 0);
            mQuietHoursRinger.setValue(String.valueOf(muteType));
            mQuietHoursRinger.setSummary(mQuietHoursRinger.getEntry());
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getContentResolver();
        if (preference == mQuietHoursTimeRange) {
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_START,
                    mQuietHoursTimeRange.getStartTime());
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_END,
                    mQuietHoursTimeRange.getEndTime());
            return true;
        } else if (preference == mQuietHoursRinger) {
            int ringerMuteType = Integer.valueOf((String) newValue);
            int index = mQuietHoursRinger.findIndexOfValue((String) newValue);
            Settings.System.putInt(resolver, Settings.System.QUIET_HOURS_RINGER, ringerMuteType);
            mQuietHoursRinger.setSummary(mQuietHoursRinger.getEntries()[index]);
            return true;
        } else if (preference == mQuietHoursEnabled) {
            Settings.System.putInt(getContentResolver(), Settings.System.QUIET_HOURS_ENABLED,
                    mQuietHoursEnabled.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursMute) {
            Settings.System.putInt(getContentResolver(), Settings.System.QUIET_HOURS_MUTE,
                    mQuietHoursMute.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursHaptic) {
            Settings.System.putInt(getContentResolver(), Settings.System.QUIET_HOURS_HAPTIC,
                    mQuietHoursHaptic.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursSystem) {
            Settings.System.putInt(getContentResolver(), Settings.System.QUIET_HOURS_SYSTEM,
                    mQuietHoursSystem.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursStill) {
            Settings.System.putInt(getContentResolver(), Settings.System.QUIET_HOURS_STILL,
                    mQuietHoursStill.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mQuietHoursDim) {
            Settings.System.putInt(getContentResolver(), Settings.System.QUIET_HOURS_DIM,
                    mQuietHoursDim.isChecked() ? 1 : 0);
            return true;
        }
        return false;
    }
}
