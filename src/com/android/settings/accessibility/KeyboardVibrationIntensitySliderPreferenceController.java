/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.VibrationAttributes;
import android.os.vibrator.Flags;
import android.provider.Settings;

import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/** Preference controller for keyboard vibration intensity slider */
// LINT.IfChange
public class KeyboardVibrationIntensitySliderPreferenceController
        extends VibrationIntensityPreferenceController {

    private final MetricsFeatureProvider mMetricsFeatureProvider;

    /** General configuration for keyboard vibration intensity settings. */
    public static final class KeyboardVibrationPreferenceConfig extends VibrationPreferenceConfig {

        public KeyboardVibrationPreferenceConfig(Context context) {
            super(context, Settings.System.KEYBOARD_VIBRATION_INTENSITY,
                    VibrationAttributes.USAGE_IME_FEEDBACK);
        }
    }

    public KeyboardVibrationIntensitySliderPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey, new KeyboardVibrationPreferenceConfig(context));
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    protected KeyboardVibrationIntensitySliderPreferenceController(Context context,
            String preferenceKey, int supportedIntensityLevels) {
        super(context, preferenceKey, new KeyboardVibrationPreferenceConfig(context),
                supportedIntensityLevels);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        final boolean isVibrationSupported = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_keyboardVibrationSettingsSupported);
        final boolean isIntensitySupported = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_keyboardVibrationSettingsIntensitySupported);
        final boolean isIntensityFlagEnabled = Flags.keyboardIntensitySliderEnabled();
        return (isVibrationSupported && isIntensitySupported && isIntensityFlagEnabled)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean setSliderPosition(int position) {
        final boolean success = super.setSliderPosition(position);
        if (success) {
            mMetricsFeatureProvider.action(mContext,
                    SettingsEnums.ACTION_KEYBOARD_VIBRATION_INTENSITY_CHANGED, position);
        }
        return success;
    }
}
// LINT.ThenChange(KeyboardVibrationIntensitySliderPreference.kt)
