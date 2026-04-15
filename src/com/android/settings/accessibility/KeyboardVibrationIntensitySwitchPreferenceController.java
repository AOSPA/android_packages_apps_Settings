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

import android.content.Context;
import android.os.vibrator.Flags;

import com.android.settings.accessibility.KeyboardVibrationIntensitySliderPreferenceController.KeyboardVibrationPreferenceConfig;

/** Preference controller for keyboard vibration with only a switch for on/off states. */
// LINT.IfChange
public class KeyboardVibrationIntensitySwitchPreferenceController
        extends VibrationTogglePreferenceController {

    public KeyboardVibrationIntensitySwitchPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey, new KeyboardVibrationPreferenceConfig(context));
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
}
// LINT.ThenChange(KeyboardVibrationIntensitySwitchPreference.kt)
