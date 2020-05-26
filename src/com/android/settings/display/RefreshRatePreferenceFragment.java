/*
 * Copyright (C) 2020 Paranoid Android
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
package com.android.settings.display;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.SystemProperties;
import android.provider.SearchIndexableResource;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.CandidateInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("WeakerAccess")
@SearchIndexable
public class RefreshRatePreferenceFragment extends RadioButtonPickerFragment {

    private static final String TAG = "RefreshRatePreferenceFragment";

    @VisibleForTesting
    static final String KEY_REFRESH_RATE_ADAPTIVE = "refresh_rate_adaptive";
    @VisibleForTesting
    static final String KEY_REFRESH_RATE_NORMAL = "refresh_rate_normal";
    @VisibleForTesting
    static final String KEY_REFRESH_RATE_HIGH = "refresh_rate_high";
    @VisibleForTesting
    static final String KEY_REFRESH_RATE_SUPER = "refresh_rate_super";

    private static final int REFRESH_RATE_ADAPTIVE = 0;
    private static final int REFRESH_RATE_NORMAL = 1;
    private static final int REFRESH_RATE_HIGH = 2;
    private static final int REFRESH_RATE_SUPER = 3;

    private static final String SHARED_PREFERENCES_NAME = "default_refresh_rate_properties";

    private static final String ADAPTIVE_REFRESH_RATE_PROPERTY = "ro.surface_flinger.use_smart_90_for_video";
    private static final String ADAPTIVE_REFRESH_RATE_IDLE_TIMER_PROPERTY = "ro.surface_flinger.set_idle_timer_ms";
    private static final String ADAPTIVE_REFRESH_RATE_TOUCH_TIMER_PROPERTY = "ro.surface_flinger.set_touch_timer_ms";
    private static final String ADAPTIVE_REFRESH_RATE_DISPLAY_POWER_TIMER_PROPERTY = "ro.surface_flinger.set_display_power_timer_ms";

    private SharedPreferences mPrefs;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mPrefs = getContext().getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.refresh_rate_settings;
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        final Context c = getContext();
        final int[] availableRefreshRates = c.getResources().getIntArray(R.array.config_availableRefreshRates);

        List<RefreshRateCandidateInfo> candidates = new ArrayList<>();
        if (availableRefreshRates != null) {
            for (int refreshRate : availableRefreshRates) {
                if (refreshRate == REFRESH_RATE_ADAPTIVE) {
                    candidates.add(new RefreshRateCandidateInfo(
                                c.getText(R.string.refresh_rate_adaptive_title),
                                KEY_REFRESH_RATE_ADAPTIVE, true /* enabled */));
                } else if (refreshRate == REFRESH_RATE_NORMAL) {
                    candidates.add(new RefreshRateCandidateInfo(
                                c.getText(R.string.refresh_rate_normal_title),
                                KEY_REFRESH_RATE_NORMAL, true /* enabled */));
                } else if (refreshRate == REFRESH_RATE_HIGH) {
                    candidates.add(new RefreshRateCandidateInfo(
                                c.getText(R.string.refresh_rate_high_title),
                                KEY_REFRESH_RATE_HIGH, true /* enabled */));
                } else if (refreshRate == REFRESH_RATE_SUPER) {
                    candidates.add(new RefreshRateCandidateInfo(
                                c.getText(R.string.refresh_rate_super_title),
                                KEY_REFRESH_RATE_SUPER, true /* enabled */));
                }
            }
        }
        return candidates;
    }

    @Override
    protected String getDefaultKey() {
        final int refreshRate = getRefreshRate();
        if (refreshRate == REFRESH_RATE_ADAPTIVE) {
            return KEY_REFRESH_RATE_ADAPTIVE;
        } else if (refreshRate == REFRESH_RATE_HIGH) {
            return KEY_REFRESH_RATE_HIGH;
        } else if (refreshRate == REFRESH_RATE_SUPER) {
            return KEY_REFRESH_RATE_SUPER;
        }
        return KEY_REFRESH_RATE_NORMAL;
    }

    @Override
    protected boolean setDefaultKey(String key) {
        switch (key) {
            case KEY_REFRESH_RATE_ADAPTIVE:
                setRefreshRate(90, 60);
                break;
            case KEY_REFRESH_RATE_NORMAL:
                setRefreshRate(60, 60);
                break;
            case KEY_REFRESH_RATE_HIGH:
                setRefreshRate(90, 90);
                break;
            case KEY_REFRESH_RATE_SUPER:
                setRefreshRate(120, 120);
                break;
        }
        return true;
    }

    @Override
    public int getMetricsCategory() {
        return -1;
    }

    private int getRefreshRate() {
        boolean isUsingDefaultApdativeRefreshRate = SystemProperties.getBoolean(ADAPTIVE_REFRESH_RATE_PROPERTY, false);
        final int defaultPeakValue = getContext().getResources().getInteger(com.android.internal.R.integer.config_defaultPeakRefreshRate);
        int peakRefreshRate = Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.PEAK_REFRESH_RATE, defaultPeakValue);
        int minRefreshRate = Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.MIN_REFRESH_RATE, 60);
        if (peakRefreshRate == 90 && isUsingDefaultApdativeRefreshRate) {
            mPrefs.edit().putBoolean("use_smart_90_for_video", true).apply();
            mPrefs.edit().putInt("set_idle_timer_ms", SystemProperties.getInt(ADAPTIVE_REFRESH_RATE_IDLE_TIMER_PROPERTY, 0)).apply();
            mPrefs.edit().putInt("set_touch_timer_ms", SystemProperties.getInt(ADAPTIVE_REFRESH_RATE_TOUCH_TIMER_PROPERTY, 0)).apply();
            mPrefs.edit().putInt("set_display_power_timer_ms", SystemProperties.getInt(ADAPTIVE_REFRESH_RATE_DISPLAY_POWER_TIMER_PROPERTY, 0)).apply();
            return REFRESH_RATE_ADAPTIVE;
        } else if (peakRefreshRate == 60) {
            return REFRESH_RATE_NORMAL;
        } else if (peakRefreshRate == 90) {
            return REFRESH_RATE_HIGH;
        } else if (peakRefreshRate == 120) {
            return REFRESH_RATE_SUPER;
        }
        return REFRESH_RATE_NORMAL;
    }

    private void setRefreshRate(int peakValue, int minValue) {
        Settings.System.putInt(getContext().getContentResolver(),
                Settings.System.PEAK_REFRESH_RATE, peakValue);
        Settings.System.putInt(getContext().getContentResolver(),
                Settings.System.MIN_REFRESH_RATE, minValue);

        boolean isAdaptive = peakValue == 90 && minValue == 60;
        updateAdpativeValues(isAdaptive);
    }

    private void updateAdpativeValues(boolean useAdaptive) {
        SystemProperties.set(ADAPTIVE_REFRESH_RATE_PROPERTY, useAdaptive ? "true" : "false");
        SystemProperties.set(ADAPTIVE_REFRESH_RATE_IDLE_TIMER_PROPERTY, useAdaptive ? Integer.toString(getDefaultIdleTimer()) : "0");
        SystemProperties.set(ADAPTIVE_REFRESH_RATE_TOUCH_TIMER_PROPERTY, useAdaptive ? Integer.toString(getDefaultTouchTimer()) : "0");
        SystemProperties.set(ADAPTIVE_REFRESH_RATE_DISPLAY_POWER_TIMER_PROPERTY, useAdaptive ? Integer.toString(getDefaultDisplayPowerTimer()) : "0");
    }

    private int getDefaultIdleTimer() {
        return mPrefs.getInt("set_idle_timer_ms", 0);
    }

    private int getDefaultTouchTimer() {
        return mPrefs.getInt("set_touch_timer_ms", 0);
    }

    private int getDefaultDisplayPowerTimer() {
        return mPrefs.getInt("set_display_power_timer_ms", 0);
    }

    @VisibleForTesting
    static class RefreshRateCandidateInfo extends CandidateInfo {
        private final CharSequence mLabel;
        private final String mKey;

        RefreshRateCandidateInfo(CharSequence label, String key, boolean enabled) {
            super(enabled);
            mLabel = label;
            mKey = key;
        }

        @Override
        public CharSequence loadLabel() {
            return mLabel;
        }

        @Override
        public Drawable loadIcon() {
            return null;
        }

        @Override
        public String getKey() {
            return mKey;
        }
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.refresh_rate_settings;
                    return Arrays.asList(sir);
                }

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    final int[] availableRefreshRates = context.getResources().getIntArray(
                            R.array.config_availableRefreshRates);
                    return availableRefreshRates != null && availableRefreshRates.length >= 2;
                }
            };
}
