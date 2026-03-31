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
import android.os.Vibrator;
import android.os.vibrator.Flags;

import com.android.settings.accessibility.KeyboardVibrationIntensitySliderPreferenceController.KeyboardVibrationPreferenceConfig;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/** Preference controller for keyboard vibration with only a switch for on/off states. */
// LINT.IfChange
public class KeyboardVibrationIntensitySwitchPreferenceController
        extends VibrationTogglePreferenceController {

    private final MetricsFeatureProvider mMetricsFeatureProvider;

    public KeyboardVibrationIntensitySwitchPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey, new KeyboardVibrationPreferenceConfig(context));
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        final boolean isVibrationSupported = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_keyboardVibrationSettingsSupported);
        final boolean isIntensitySupported = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_keyboardVibrationSettingsIntensitySupported)
                && Flags.keyboardIntensitySliderEnabled();
        return (isVibrationSupported && isIntensitySupported) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        final boolean success = super.setChecked(isChecked);
        if (success) {
            final int intensity = isChecked
                    ? mPreferenceConfig.getDefaultIntensity()
                    : Vibrator.VIBRATION_INTENSITY_OFF;
            mMetricsFeatureProvider.action(mContext,
                    SettingsEnums.ACTION_KEYBOARD_VIBRATION_INTENSITY_CHANGED, intensity);
        }
        return success;
    }
}
// LINT.ThenChange(KeyboardVibrationIntensitySwitchPreference.kt)
