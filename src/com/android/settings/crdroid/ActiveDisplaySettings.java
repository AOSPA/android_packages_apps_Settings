/*
 * Copyright (C) 2013 The ChameleonOS Project
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

package com.android.settings.crdroid;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.format.DateFormat;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.android.internal.util.omni.DeviceUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ActiveDisplaySettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {
    private static final String TAG = "ActiveDisplaySettings";

    private static final String KEY_ENABLED = "ad_enable";
    private static final String KEY_SHOW_TEXT = "ad_text";
    private static final String KEY_SHOW_CONTENT = "ad_content";
    private static final String KEY_BYPASS_CONTENT = "ad_bypass";
    private static final String KEY_ALL_NOTIFICATIONS = "ad_all_notifications";
    private static final String KEY_HIDE_LOW_PRIORITY = "ad_hide_low_priority";
    private static final String KEY_POCKET_MODE = "ad_pocket_mode";
    private static final String KEY_SUNLIGHT_MODE = "ad_sunlight_mode";
    private static final String KEY_REDISPLAY = "ad_redisplay";
    private static final String KEY_EXCLUDED_APPS = "ad_excluded_apps";
    private static final String KEY_PRIVACY_APPS = "ad_privacy_apps";
    private static final String KEY_SHOW_DATE = "ad_show_date";
    private static final String KEY_SHOW_AMPM = "ad_show_ampm";
    private static final String KEY_BRIGHTNESS = "ad_brightness";
    private static final String KEY_TIMEOUT = "ad_timeout";
    private static final String KEY_THRESHOLD = "ad_threshold";
    private static final String KEY_TURNOFF_MODE = "ad_turnoff_mode";

    private ContentResolver mResolver;
    private Context mContext;

    private SwitchPreference mEnabledPref;
    private CheckBoxPreference mShowTextPref;
    private CheckBoxPreference mShowContentPref;
    private CheckBoxPreference mBypassPref;
    private CheckBoxPreference mShowDatePref;
    private CheckBoxPreference mShowAmPmPref;
    private CheckBoxPreference mAllNotificationsPref;
    private CheckBoxPreference mHideLowPriorityPref;
    private ListPreference mPocketModePref;
    private CheckBoxPreference mSunlightModePref;
    private ListPreference mRedisplayPref;
    private SeekBarPreferenceChOS mBrightnessLevel;
    private ListPreference mDisplayTimeout;
    private ListPreference mProximityThreshold;
    private CheckBoxPreference mTurnOffModePref;
    private AppMultiSelectListPreference mExcludedAppsPref;
    private AppMultiSelectListPreference mPrivacyAppsPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.active_display_settings);

        mContext = getActivity().getApplicationContext();
        mResolver = mContext.getContentResolver();
        PreferenceScreen prefSet = getPreferenceScreen();

        mEnabledPref = (SwitchPreference) prefSet.findPreference(KEY_ENABLED);
        mEnabledPref.setChecked((Settings.System.getInt(mResolver,
                Settings.System.ENABLE_ACTIVE_DISPLAY, 0) == 1));
        mEnabledPref.setOnPreferenceChangeListener(this);

        mShowTextPref = (CheckBoxPreference) prefSet.findPreference(KEY_SHOW_TEXT);
        mShowTextPref.setChecked((Settings.System.getInt(mResolver,
                Settings.System.ACTIVE_DISPLAY_TEXT, 0) == 1));

        mShowContentPref = (CheckBoxPreference) prefSet.findPreference(KEY_SHOW_CONTENT);
        mShowContentPref.setChecked((Settings.System.getInt(mResolver,
                Settings.System.ACTIVE_DISPLAY_CONTENT, 1) != 0));

        mBypassPref = (CheckBoxPreference) prefSet.findPreference(KEY_BYPASS_CONTENT);
        mPocketModePref = (ListPreference) prefSet.findPreference(KEY_POCKET_MODE);
        mProximityThreshold = (ListPreference) prefSet.findPreference(KEY_THRESHOLD);
        mTurnOffModePref = (CheckBoxPreference) prefSet.findPreference(KEY_TURNOFF_MODE);

        if (!DeviceUtils.deviceSupportsProximitySensor(mContext)) {
            prefSet.removePreference(mPocketModePref);
            prefSet.removePreference(mBypassPref);
            prefSet.removePreference(mProximityThreshold);
            prefSet.removePreference(mTurnOffModePref);
        } else {
            mBypassPref.setChecked((Settings.System.getInt(mResolver,
                Settings.System.ACTIVE_DISPLAY_BYPASS, 1) != 0));

        int mode = Settings.System.getInt(mResolver,
                   Settings.System.ACTIVE_DISPLAY_POCKET_MODE, 0);
            mPocketModePref.setValue(String.valueOf(mode));
            updatePocketModeSummary(mode);
            mPocketModePref.setOnPreferenceChangeListener(this);

            long threshold = Settings.System.getLong(mResolver,
                Settings.System.ACTIVE_DISPLAY_THRESHOLD, 5000L);
            mProximityThreshold.setValue(String.valueOf(threshold));
            updateThresholdSummary(threshold);
            mProximityThreshold.setOnPreferenceChangeListener(this);

            mTurnOffModePref.setChecked((Settings.System.getInt(mResolver,
                Settings.System.ACTIVE_DISPLAY_TURNOFF_MODE, 0) == 1));
        }

        mAllNotificationsPref = (CheckBoxPreference) prefSet.findPreference(KEY_ALL_NOTIFICATIONS);
        mAllNotificationsPref.setChecked((Settings.System.getInt(mResolver,
                Settings.System.ACTIVE_DISPLAY_ALL_NOTIFICATIONS, 0) == 1));

        mHideLowPriorityPref = (CheckBoxPreference) findPreference(KEY_HIDE_LOW_PRIORITY);
        mHideLowPriorityPref.setChecked((Settings.System.getInt(mResolver,
                Settings.System.ACTIVE_DISPLAY_HIDE_LOW_PRIORITY_NOTIFICATIONS, 0) == 1));

        mSunlightModePref = (CheckBoxPreference) prefSet.findPreference(KEY_SUNLIGHT_MODE);
        if (!DeviceUtils.deviceSupportsLightSensor(mContext)) {
            prefSet.removePreference(mSunlightModePref);
        } else {
            mSunlightModePref.setChecked((Settings.System.getInt(mResolver,
                Settings.System.ACTIVE_DISPLAY_SUNLIGHT_MODE, 0) == 1));
        }

        mRedisplayPref = (ListPreference) prefSet.findPreference(KEY_REDISPLAY);
        long timeout = Settings.System.getLong(mResolver,
                Settings.System.ACTIVE_DISPLAY_REDISPLAY, 0);
        mRedisplayPref.setValue(String.valueOf(timeout));
        updateRedisplaySummary(timeout);
        mRedisplayPref.setOnPreferenceChangeListener(this);

        mShowDatePref = (CheckBoxPreference) prefSet.findPreference(KEY_SHOW_DATE);
        mShowDatePref.setChecked((Settings.System.getInt(mResolver,
                Settings.System.ACTIVE_DISPLAY_SHOW_DATE, 0) == 1));

        mShowAmPmPref = (CheckBoxPreference) prefSet.findPreference(KEY_SHOW_AMPM);
        mShowAmPmPref.setChecked((Settings.System.getInt(mResolver,
                Settings.System.ACTIVE_DISPLAY_SHOW_AMPM, 0) == 1));

        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        int minimumBacklight = pm.getMinimumScreenBrightnessSetting();
        int maximumBacklight = pm.getMaximumScreenBrightnessSetting();

        mBrightnessLevel = (SeekBarPreferenceChOS) prefSet.findPreference(KEY_BRIGHTNESS);
        mBrightnessLevel.setMaxValue(maximumBacklight - minimumBacklight);
        mBrightnessLevel.setMinValue(minimumBacklight);
        mBrightnessLevel.setValue(Settings.System.getInt(mResolver,
                Settings.System.ACTIVE_DISPLAY_BRIGHTNESS, maximumBacklight) - minimumBacklight);
        mBrightnessLevel.setOnPreferenceChangeListener(this);

        try {
            if (Settings.System.getInt(mResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                mBrightnessLevel.setEnabled(false);
                mBrightnessLevel.setSummary(R.string.status_bar_toggle_info);
            }
        } catch (SettingNotFoundException e) {
        }

        mDisplayTimeout = (ListPreference) prefSet.findPreference(KEY_TIMEOUT);
        mDisplayTimeout.setOnPreferenceChangeListener(this);
        timeout = Settings.System.getLong(mResolver,
                Settings.System.ACTIVE_DISPLAY_TIMEOUT, 8000L);
        mDisplayTimeout.setValue(String.valueOf(timeout));
        updateTimeoutSummary(timeout);

        mExcludedAppsPref = (AppMultiSelectListPreference) prefSet.findPreference(KEY_EXCLUDED_APPS);
        Set<String> excludedApps = getExcludedApps();
        if (excludedApps != null) mExcludedAppsPref.setValues(excludedApps);
        mExcludedAppsPref.setOnPreferenceChangeListener(this);

        mPrivacyAppsPref = (AppMultiSelectListPreference) prefSet.findPreference(KEY_PRIVACY_APPS);
        Set<String> privacyApps = getPrivacyApps();
        if (privacyApps != null) mPrivacyAppsPref.setValues(privacyApps);
        mPrivacyAppsPref.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mRedisplayPref) {
            int timeout = Integer.valueOf((String) newValue);
            updateRedisplaySummary(timeout);
            return true;
        } else if (preference == mPocketModePref) {
            int mode = Integer.valueOf((String) newValue);
            updatePocketModeSummary(mode);
            return true;
        } else if (preference == mEnabledPref) {
            Settings.System.putInt(mResolver,
                    Settings.System.ENABLE_ACTIVE_DISPLAY,
                    ((Boolean) newValue).booleanValue() ? 1 : 0);
            return true;
        } else if (preference == mBrightnessLevel) {
            int brightness = ((Integer)newValue).intValue();
            Settings.System.putInt(mResolver,
                    Settings.System.ACTIVE_DISPLAY_BRIGHTNESS, brightness);
            return true;
        } else if (preference == mExcludedAppsPref) {
            storeExcludedApps((Set<String>) newValue);
            return true;
        } else if (preference == mPrivacyAppsPref) {
            storePrivacyApps((Set<String>) newValue);
            return true;
        } else if (preference == mDisplayTimeout) {
            long timeout = Integer.valueOf((String) newValue);
            updateTimeoutSummary(timeout);
            return true;
        } else if (preference == mProximityThreshold) {
            long threshold = Integer.valueOf((String) newValue);
            updateThresholdSummary(threshold);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;
        if (preference == mShowTextPref) {
            value = mShowTextPref.isChecked();
            Settings.System.putInt(mResolver,
                    Settings.System.ACTIVE_DISPLAY_TEXT,
                    value ? 1 : 0);
        } else if (preference == mShowContentPref) {
            value = mShowContentPref.isChecked();
            Settings.System.putInt(mResolver,
                    Settings.System.ACTIVE_DISPLAY_CONTENT,
                    value ? 1 : 0);
        } else if (preference == mBypassPref) {
            value = mBypassPref.isChecked();
            Settings.System.putInt(mResolver,
                    Settings.System.ACTIVE_DISPLAY_BYPASS,
                    value ? 1 : 0);
        } else if (preference == mAllNotificationsPref) {
            value = mAllNotificationsPref.isChecked();
            Settings.System.putInt(mResolver,
                    Settings.System.ACTIVE_DISPLAY_ALL_NOTIFICATIONS,
                    value ? 1 : 0);
        } else if (preference == mHideLowPriorityPref) {
            value = mHideLowPriorityPref.isChecked();
            Settings.System.putInt(mResolver,
                    Settings.System.ACTIVE_DISPLAY_HIDE_LOW_PRIORITY_NOTIFICATIONS,
                    value ? 1 : 0);
        } else if (preference == mSunlightModePref) {
            value = mSunlightModePref.isChecked();
            Settings.System.putInt(mResolver,
                    Settings.System.ACTIVE_DISPLAY_SUNLIGHT_MODE,
                    value ? 1 : 0);
        } else if (preference == mShowDatePref) {
            value = mShowDatePref.isChecked();
            Settings.System.putInt(mResolver,
                    Settings.System.ACTIVE_DISPLAY_SHOW_DATE,
                    value ? 1 : 0);
        } else if (preference == mShowAmPmPref) {
            value = mShowAmPmPref.isChecked();
            Settings.System.putInt(mResolver,
                    Settings.System.ACTIVE_DISPLAY_SHOW_AMPM,
                    value ? 1 : 0);
        } else if (preference == mTurnOffModePref) {
            value = mTurnOffModePref.isChecked();
            Settings.System.putInt(mResolver,
                    Settings.System.ACTIVE_DISPLAY_TURNOFF_MODE,
                    value ? 1 : 0);
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    private void updatePocketModeSummary(int value) {
        mPocketModePref.setSummary(
                mPocketModePref.getEntries()[mPocketModePref.findIndexOfValue("" + value)]);
        Settings.System.putInt(mResolver,
                Settings.System.ACTIVE_DISPLAY_POCKET_MODE, value);
    }

    private void updateRedisplaySummary(long value) {
        mRedisplayPref.setSummary(mRedisplayPref.getEntries()[mRedisplayPref.findIndexOfValue("" + value)]);
        Settings.System.putLong(mResolver,
                Settings.System.ACTIVE_DISPLAY_REDISPLAY, value);
    }

    private void updateThresholdSummary(long value) {
        try {
            mProximityThreshold.setSummary(mProximityThreshold.getEntries()[mProximityThreshold.findIndexOfValue("" + value)]);
            Settings.System.putLong(mResolver,
                    Settings.System.ACTIVE_DISPLAY_THRESHOLD, value);
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    private Set<String> getExcludedApps() {
        String excluded = Settings.System.getString(mResolver,
                Settings.System.ACTIVE_DISPLAY_EXCLUDED_APPS);
        if (TextUtils.isEmpty(excluded))
            return null;

        return new HashSet<String>(Arrays.asList(excluded.split("\\|")));
    }

    private void updateTimeoutSummary(long value) {
        try {
            mDisplayTimeout.setSummary(mDisplayTimeout.getEntries()[mDisplayTimeout.findIndexOfValue("" + value)]);
            Settings.System.putLong(mResolver,
                    Settings.System.ACTIVE_DISPLAY_TIMEOUT, value);
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    private void storeExcludedApps(Set<String> values) {
        StringBuilder builder = new StringBuilder();
        String delimiter = "";
        for (String value : values) {
            builder.append(delimiter);
            builder.append(value);
            delimiter = "|";
        }
        Settings.System.putString(mResolver,
                Settings.System.ACTIVE_DISPLAY_EXCLUDED_APPS, builder.toString());
    }

    private Set<String> getPrivacyApps() {
        String privacies = Settings.System.getString(mResolver,
                Settings.System.ACTIVE_DISPLAY_PRIVACY_APPS);
        if (TextUtils.isEmpty(privacies))
            return null;

        return new HashSet<String>(Arrays.asList(privacies.split("\\|")));
    }

    private void storePrivacyApps(Set<String> values) {
        StringBuilder builder = new StringBuilder();
        String delimiter = "";
        for (String value : values) {
            builder.append(delimiter);
            builder.append(value);
            delimiter = "|";
        }
        Settings.System.putString(mResolver,
                Settings.System.ACTIVE_DISPLAY_PRIVACY_APPS, builder.toString());
    }

    private boolean is24Hour() {
        return DateFormat.is24HourFormat(mContext);
    }
}
