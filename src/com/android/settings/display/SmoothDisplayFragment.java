/*
 * Copyright (C) 2023 Paranoid Android
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
import android.graphics.drawable.Drawable;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;
import com.android.settingslib.widget.TopIntroPreference;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Preference fragment used for switching refresh rate */
@SearchIndexable
public class SmoothDisplayFragment extends RadioButtonPickerFragment {

    private static final String TAG = "SmoothDisplayFragment";
    private static final int DEFAULT_REFRESH_RATE = 60;

    private Context mContext;
    private Set<Integer> mHighRefreshRates;
    private int mLeastHighRefreshRate, mMaxRefreshRate;

    protected static Set<Integer> getHighRefreshRates(Context context) {
        return Arrays.stream(context.getDisplay().getSupportedModes())
                .mapToInt(m -> Math.round(m.getRefreshRate()))
                .filter(r -> r > DEFAULT_REFRESH_RATE)
                .boxed().collect(Collectors.toSet());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        mHighRefreshRates = getHighRefreshRates(context);
        mMaxRefreshRate = Collections.max(mHighRefreshRates);
        mLeastHighRefreshRate = Collections.min(mHighRefreshRates);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.smooth_display_settings;
    }

    @Override
    protected void addStaticPreferences(PreferenceScreen screen) {
        final TopIntroPreference introPref = new TopIntroPreference(screen.getContext());
        introPref.setTitle(R.string.smooth_display_intro);
        screen.addPreference(introPref);

        final MainSwitchPreference switchPref = new MainSwitchPreference(screen.getContext());
        switchPref.setTitle(R.string.smooth_display_switch);
        switchPref.setChecked(isSmoothDisplayEnabled());
        switchPref.addOnSwitchChangeListener((v, isChecked) -> {
            setPeakRefreshRate(isChecked ? getSmoothRefreshRate() : DEFAULT_REFRESH_RATE);
            updateCandidates();
        });
        screen.addPreference(switchPref);
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        return mHighRefreshRates.stream().sorted()
                .map(RefreshRateCandidateInfo::new)
                .collect(Collectors.toList());
    }

    private int roundToNearestHighRefreshRate(int refreshRate) {
        if (mHighRefreshRates.contains(refreshRate)) return refreshRate;
        int findRefreshRate = mLeastHighRefreshRate;
        for (Integer highRefreshRate : mHighRefreshRates) {
            if (highRefreshRate > refreshRate) break;
            findRefreshRate = highRefreshRate;
        }
        return findRefreshRate;
    }

    private int getDefaultPeakRefreshRate() {
        return mContext.getResources().getInteger(
                com.android.internal.R.integer.config_defaultPeakRefreshRate);
    }

    private int getPeakRefreshRate() {
        final int peakRefreshRate = Math.round(Settings.System.getFloat(
                mContext.getContentResolver(),
                Settings.System.PEAK_REFRESH_RATE, getDefaultPeakRefreshRate()));
        if (peakRefreshRate < DEFAULT_REFRESH_RATE) {
            return mMaxRefreshRate;
        } else if (peakRefreshRate < mLeastHighRefreshRate) {
            return DEFAULT_REFRESH_RATE;
        }
        return roundToNearestHighRefreshRate(peakRefreshRate);
    }

    private void setPeakRefreshRate(int refreshRate) {
        Settings.System.putFloat(mContext.getContentResolver(),
                Settings.System.PEAK_REFRESH_RATE, (float) refreshRate);
    }

    private int getSmoothRefreshRate() {
        final int smoothRefreshRate = Math.round(Settings.System.getFloat(
                mContext.getContentResolver(),
                Settings.System.SMOOTH_REFRESH_RATE, (float) getPeakRefreshRate()));
        if (smoothRefreshRate == DEFAULT_REFRESH_RATE) {
            return Math.max(mLeastHighRefreshRate, getDefaultPeakRefreshRate());
        }
        return roundToNearestHighRefreshRate(smoothRefreshRate);
    }

    private boolean isSmoothDisplayEnabled() {
        return getPeakRefreshRate() >= mLeastHighRefreshRate;
    }

    @Override
    protected String getDefaultKey() {
        return String.valueOf(getPeakRefreshRate());
    }

    @Override
    protected boolean setDefaultKey(final String key) {
        final int refreshRate = Integer.parseInt(key);
        setPeakRefreshRate(refreshRate);
        Settings.System.putFloat(mContext.getContentResolver(),
                Settings.System.SMOOTH_REFRESH_RATE, (float) refreshRate);
        return true;
    }

    @Override
    public int getMetricsCategory() {
        return -1;
    }

    private class RefreshRateCandidateInfo extends CandidateInfo {
        private final CharSequence mLabel;
        private final String mKey;

        RefreshRateCandidateInfo(Integer refreshRate) {
            super(isSmoothDisplayEnabled());
            mLabel = String.format("%d Hz", refreshRate.intValue());
            mKey = refreshRate.toString();
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

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.smooth_display_settings) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return getHighRefreshRates(context).size() > 1;
                }
            };
}
