/*
 * Copyright (C) 2020 Paranoid Android
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

package com.android.settings.display;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemProperties;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.RadioButtonPreference;

import java.util.ArrayList;
import java.util.List;

public class RefreshRatePreferenceFragment extends SettingsPreferenceFragment
        implements RadioButtonPreference.OnClickListener {

    private static final String TAG = "RefreshRatePreferenceFragment";

    private static final String KEY_REFRESH_RATE_ADAPTIVE = "key_refresh_rate_adaptive";
    private static final String KEY_REFRESH_RATE_NORMAL = "key_refresh_rate_normal";
    private static final String KEY_REFRESH_RATE_HIGH = "key_refresh_rate_high";
    private static final String KEY_REFRESH_RATE_SUPER = "key_refresh_rate_super";

    private static final int REFRESH_RATE_ADAPTIVE = 0;
    private static final int REFRESH_RATE_NORMAL = 1;
    private static final int REFRESH_RATE_HIGH = 2;
    private static final int REFRESH_RATE_SUPER = 3;

    private static final String SHARED_PREFERENCES_NAME = "default_refresh_rate_properties";

    private static final String ADAPTIVE_REFRESH_RATE_PROPERTY = "ro.surface_flinger.use_smart_90_for_video";
    private static final String ADAPTIVE_REFRESH_RATE_IDLE_TIMER_PROPERTY = "ro.surface_flinger.set_idle_timer_ms";
    private static final String ADAPTIVE_REFRESH_RATE_TOUCH_TIMER_PROPERTY = "ro.surface_flinger.set_touch_timer_ms";
    private static final String ADAPTIVE_REFRESH_RATE_DISPLAY_POWER_TIMER_PROPERTY = "ro.surface_flinger.set_display_power_timer_ms";

    List<RadioButtonPreference> mRefreshRates = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.refresh_rate_settings);

        final boolean isUsingDefaultApdativeRefreshRate = SystemProperties.getBoolean(ADAPTIVE_REFRESH_RATE_PROPERTY, false);
        final int[] availableRefreshRates = getContext().getResources().getIntArray(R.array.config_availableRefreshRates);
        if (availableRefreshRates != null) {
            for (int refreshRate : availableRefreshRates) {
                RadioButtonPreference pref = new RadioButtonPreference(getContext());
                if (refreshRate == REFRESH_RATE_ADAPTIVE && isUsingDefaultApdativeRefreshRate) {
                    pref.setTitle("Adaptive");
                    pref.setKey(KEY_REFRESH_RATE_ADAPTIVE);
                } else if (refreshRate == REFRESH_RATE_NORMAL) {
                    pref.setTitle("60hz");
                    pref.setKey(KEY_REFRESH_RATE_NORMAL);
                } else if (refreshRate == REFRESH_RATE_HIGH) {
                    pref.setTitle("90hz");
                    pref.setKey(KEY_REFRESH_RATE_HIGH);
                } else if (refreshRate == REFRESH_RATE_SUPER) {
                    pref.setTitle("120hz");
                    pref.setKey(KEY_REFRESH_RATE_SUPER);
                }
                mRefreshRates.add(pref);
            }
        }

        final int defaultPeakValue = getContext().getResources().getInteger(com.android.internal.R.integer.config_defaultPeakRefreshRate);
        int peakRefreshRate = Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.PEAK_REFRESH_RATE, defaultPeakValue);
        int minRefreshRate = Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.MIN_REFRESH_RATE, 60);

        if (peakRefreshRate == 90 && isUsingDefaultApdativeRefreshRate) {
            SharedPreferences prefs = getContext().getSharedPreferences(SHARED_PREFERENCES_NAME,
                    Context.MODE_PRIVATE);
            prefs.edit().putBoolean("use_smart_90_for_video", true).apply();
            prefs.edit().putInt("set_idle_timer_ms", SystemProperties.getInt(ADAPTIVE_REFRESH_RATE_IDLE_TIMER_PROPERTY, 0)).apply();
            prefs.edit().putInt("set_touch_timer_ms", SystemProperties.getInt(ADAPTIVE_REFRESH_RATE_TOUCH_TIMER_PROPERTY, 0)).apply();
            prefs.edit().putInt("set_display_power_timer_ms", SystemProperties.getInt(ADAPTIVE_REFRESH_RATE_DISPLAY_POWER_TIMER_PROPERTY, 0)).apply();
            updateRefreshRateItems(REFRESH_RATE_ADAPTIVE);
        } else if (peakRefreshRate == 60) {
            updateRefreshRateItems(KEY_REFRESH_RATE_NORMAL);
        } else if (peakRefreshRate == 90) {
            updateRefreshRateItems(KEY_REFRESH_RATE_HIGH);
        } else if (peakRefreshRate == 120) {
            updateRefreshRateItems(KEY_REFRESH_RATE_SUPER);
        }
    }

    @Override
    public int getMetricsCategory() {
        return -1;
    }

    private void updateRefreshRateItems(String selectionKey) {
        for (RadioButtonPreference pref : mRefreshRates) {
            if (selectionKey.equals(pref.getKey())) {
                pref.setChecked(true);
            } else {
                pref.setChecked(false);
            }
        }
    }

    @Override
    public void onRadioButtonClicked(RadioButtonPreference pref) {
        switch (pref.getKey()) {
            case KEY_REFRESH_RATE_ADAPTIVE:
                int defaultPeakValue = getContext().getResources().getInteger(com.android.internal.R.integer.config_defaultPeakRefreshRate);
                Settings.System.putInt(getContext().getContentResolver(),
                        Settings.System.PEAK_REFRESH_RATE, 90);
                Settings.System.putInt(getContext().getContentResolver(),
                        Settings.System.MIN_REFRESH_RATE, 60);
                setAdpativeValues(true);
                break;
            case KEY_REFRESH_RATE_NORMAL:
                Settings.System.putInt(getContext().getContentResolver(),
                        Settings.System.PEAK_REFRESH_RATE, 60);
                Settings.System.putInt(getContext().getContentResolver(),
                        Settings.System.MIN_REFRESH_RATE, 60);
                setAdpativeValues(false);
                break;
            case KEY_REFRESH_RATE_HIGH:
                Settings.System.putInt(getContext().getContentResolver(),
                        Settings.System.PEAK_REFRESH_RATE, 90);
                Settings.System.putInt(getContext().getContentResolver(),
                        Settings.System.MIN_REFRESH_RATE, 90);
                setAdpativeValues(false);
                break;
            case KEY_REFRESH_RATE_SUPER:
                Settings.System.putInt(getContext().getContentResolver(),
                        Settings.System.PEAK_REFRESH_RATE, 120);
                Settings.System.putInt(getContext().getContentResolver(),
                        Settings.System.MIN_REFRESH_RATE, 120);
                setAdpativeValues(false);
                break;
        }
        updateRefreshRateItems(pref.getKey());
    }

    private void setAdpativeValues(boolean useAdaptive) {
        SystemProperties.set(ADAPTIVE_REFRESH_RATE_PROPERTY, useAdaptive ? "true" : "false");
        SystemProperties.set(ADAPTIVE_REFRESH_RATE_IDLE_TIMER_PROPERTY, useAdaptive ? Integer.toString(getDefaultIdleTimer()) : "0");
        SystemProperties.set(ADAPTIVE_REFRESH_RATE_TOUCH_TIMER_PROPERTY, useAdaptive ? Integer.toString(getDefaultTouchTimer()) : "0");
        SystemProperties.set(ADAPTIVE_REFRESH_RATE_DISPLAY_POWER_TIMER_PROPERTY, useAdaptive ? Integer.toString(getDefaultDisplayPowerTimer()) : "0");
    }

    private int getDefaultIdleTimer() {
        return mPrefs.getInt(ADAPTIVE_REFRESH_RATE_IDLE_TIMER_PROPERTY, 0);
    }

    private int getDefaultTouchTimer() {
        return mPrefs.getInt(ADAPTIVE_REFRESH_RATE_TOUCH_TIMER_PROPERTY, 0);
    }

    private int getDefaultDisplayPowerTimer() {
        return mPrefs.getInt(ADAPTIVE_REFRESH_RATE_DISPLAY_POWER_TIMER_PROPERTY, 0);
    }
}
