/*
 * Copyright (C) 2025 The Android Open Source Project
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

import static android.provider.Settings.Secure.HDR_BRIGHTNESS_ENABLED;
import static android.provider.Settings.Secure.HDR_BRIGHTNESS_BOOST_LEVEL;

import android.content.Context;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.SliderPreferenceController;
import com.android.settingslib.display.BrightnessUtils;
import com.android.settingslib.widget.SliderPreference;

public class HdrBrightnessLevelPreferenceController extends SliderPreferenceController {

    public HdrBrightnessLevelPreferenceController(@NonNull Context context, @NonNull String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return HdrBrightnessUtils.getAvailabilityStatus(mContext);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        SliderPreference preference = screen.findPreference(getPreferenceKey());
        preference.setUpdatesContinuously(true);
        preference.setMax(getMax());
        preference.setMin(getMin());
        preference.setHapticFeedbackMode(SliderPreference.HAPTIC_FEEDBACK_MODE_ON_ENDS);
    }

    @Override
    public final void updateState(Preference preference) {
        super.updateState(preference);
        boolean hdrBrightnessEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                HDR_BRIGHTNESS_ENABLED, /* def= */ 1) == 1;
        preference.setEnabled(hdrBrightnessEnabled);
    }

    @Override
    public int getSliderPosition() {
        return Math.round(Settings.Secure.getFloat(mContext.getContentResolver(),
                HDR_BRIGHTNESS_BOOST_LEVEL, /* def= */ 1) * getMax());
    }

    @Override
    public boolean setSliderPosition(int position) {
        return Settings.Secure.putFloat(mContext.getContentResolver(), HDR_BRIGHTNESS_BOOST_LEVEL,
                ((float) position) / getMax());
    }

    @Override
    public int getMax() {
        return BrightnessUtils.GAMMA_SPACE_MAX;
    }

    @Override
    public int getMin() {
        return 0;
    }
}
