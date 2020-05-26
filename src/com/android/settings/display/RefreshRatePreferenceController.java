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
import android.os.SystemProperties;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import java.util.ArrayList;

public class RefreshRatePreferenceController extends BasePreferenceController {

    private static final String ADAPTIVE_REFRESH_RATE_PROPERTY = "ro.surface_flinger.use_smart_90_for_video";

    public RefreshRatePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        final int[] availableRefreshRates = mContext.getResources().getIntArray(R.array.config_availableRefreshRates);
        return availableRefreshRates != null && availableRefreshRates.length >= 2 
                ? AVAILABLE_UNSEARCHABLE : DISABLED_FOR_USER;
    }

    @Override
    public CharSequence getSummary() {
        final boolean isUsingDefaultApdativeRefreshRate = SystemProperties.getBoolean(ADAPTIVE_REFRESH_RATE_PROPERTY, false);
        final int defaultPeakValue = mContext.getResources().getInteger(com.android.internal.R.integer.config_defaultPeakRefreshRate);
        int peakRefreshRate = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.PEAK_REFRESH_RATE, defaultPeakValue);
        if (peakRefreshRate == 90 && isUsingDefaultApdativeRefreshRate) {
            return mContext.getText(R.string.refresh_rate_adaptive_title);
        } else if (peakRefreshRate == 60) {
            return mContext.getText(R.string.refresh_rate_normal_title);
        } else if (peakRefreshRate == 90) {
            return mContext.getText(R.string.refresh_rate_high_title);
        } else if (peakRefreshRate == 120) {
            return mContext.getText(R.string.refresh_rate_super_title);
        }
        return mContext.getText(R.string.refresh_rate_normal_title);
    }
}
