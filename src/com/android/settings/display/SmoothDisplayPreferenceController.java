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
import android.provider.Settings;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import java.util.Collections;
import java.util.Set;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

public class SmoothDisplayPreferenceController extends TogglePreferenceController {

    private static final String TAG = "SmoothDisplayPreferenceController";
    private static final int DEFAULT_REFRESH_RATE = 60;

    private Set<Integer> mHighRefreshRates;
    private int mLeastHighRefreshRate, mMaxRefreshRate;
    private Preference mPreference;

    public SmoothDisplayPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mHighRefreshRates = SmoothDisplayFragment.getHighRefreshRates(context);
        mMaxRefreshRate = Collections.max(mHighRefreshRates);
        mLeastHighRefreshRate = Collections.min(mHighRefreshRates);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public CharSequence getSummary() {
        final boolean checked = isChecked();
        final String status = mContext.getString(checked
                ? R.string.switch_on_text : R.string.switch_off_text);
        final int refreshRate = checked ? getPeakRefreshRate() : DEFAULT_REFRESH_RATE;
        final String refreshRateString = String.format("(%d Hz)", refreshRate);
        return new StringBuilder()
                .append(status)
                .append(" ")
                .append(refreshRateString);
    }

    @Override
    public int getAvailabilityStatus() {
        return mHighRefreshRates.size() > 1 ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        return getPeakRefreshRate() >= mLeastHighRefreshRate;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.System.putFloat(mContext.getContentResolver(),
                Settings.System.PEAK_REFRESH_RATE,
                (float) (isChecked ? getSmoothRefreshRate() : DEFAULT_REFRESH_RATE));
        refreshSummary(mPreference);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        refreshSummary(preference);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
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

    private int getSmoothRefreshRate() {
        final int smoothRefreshRate = Math.round(Settings.System.getFloat(
                mContext.getContentResolver(),
                Settings.System.SMOOTH_REFRESH_RATE, (float) getPeakRefreshRate()));
        if (smoothRefreshRate == DEFAULT_REFRESH_RATE) {
            return Math.max(mLeastHighRefreshRate, getDefaultPeakRefreshRate());
        }
        return roundToNearestHighRefreshRate(smoothRefreshRate);
    }
}
