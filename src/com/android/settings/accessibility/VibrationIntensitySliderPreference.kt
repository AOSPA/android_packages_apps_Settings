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
package com.android.settings.accessibility

import android.content.Context
import android.os.VibrationAttributes.Usage
import android.os.Vibrator
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.IntRangeValuePreference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.widget.SliderPreference
import com.android.settingslib.widget.SliderPreferenceBinding
import kotlin.math.min

/**
 * SliderPreference for vibration intensity.
 *
 * This implementation uses VibrationIntensitySettingsStore to save the vibration intensity value,
 * also playing a haptic preview on slider changes.
 */
// LINT.IfChange
open class VibrationIntensitySliderPreference(
    override val key: String,
    @Usage val vibrationUsage: Int,
    @StringRes override val title: Int = 0,
    @StringRes override val summary: Int = 0,
) : IntRangeValuePreference, SliderPreferenceBinding, OnPreferenceChangeListener {

    override fun getMinValue(context: Context) = Vibrator.VIBRATION_INTENSITY_OFF

    override fun getMaxValue(context: Context) =
        min(Vibrator.VIBRATION_INTENSITY_HIGH, context.getSupportedVibrationIntensityLevels())

    override fun getIncrementStep(context: Context) = 1

    override fun storage(context: Context): KeyValueStore =
        VibrationIntensitySettingsStore(context, vibrationUsage)

    @CallSuper
    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.onPreferenceChangeListener = this
        (preference as SliderPreference).apply {
            // Haptics previews played by the Settings app don't bypass user settings to be played.
            // The sliders continuously updates the intensity value so the previews can apply them.
            updatesContinuously = true
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        if (newValue as Int != Vibrator.VIBRATION_INTENSITY_OFF) {
            preference.context.playVibrationSettingsPreview(vibrationUsage)
        }
        return true
    }
}
// LINT.ThenChange(VibrationIntensityPreferenceController.java)
