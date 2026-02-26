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

package com.android.settings.display

import android.Manifest.permission.CONTROL_DISPLAY_COLOR_TRANSFORMS
import android.content.Context
import android.hardware.display.ColorDisplayManager
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported
import com.android.settingslib.metadata.preferencesapi.preconditions.InvalidPreference
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean
import com.android.settingslib.metadata.preferencesapi.types.IntInRange
import kotlin.math.roundToInt

// LINT.IfChange
@ProvidePreferenceScreen(NightDisplayApiScreen.KEY)
class NightDisplayApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.DISPLAY,
        fragment = NightDisplaySettings::class,
        purpose = R.string.night_display_purpose,
        alreadyPartiallyMigrated = NightDisplayScreen::class,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }

        preconditions(R.string.night_display_preconditions) {
            if (context.isNightDisplaySettingsAvailable) {
                Allowed
            } else {
                HardwareUnsupported(R.string.night_display_unsupported)
            }
        }

        preference(
            key = NIGHT_DISPLAY_ACTIVATED_KEY,
            purpose = R.string.night_display_activated_purpose,
            type = AnyBoolean,
        ) {
            get { execute { context.colorDisplayManager.isNightDisplayActivated } }

            set {
                permissions(CONTROL_DISPLAY_COLOR_TRANSFORMS)
                execute { value ->
                    context.colorDisplayManager.let { it.setNightDisplayActivated(value) }
                }
            }
        }

        preference(
            key = NIGHT_DISPLAY_TEMPERATURE_KEY,
            purpose = R.string.night_display_temperature_purpose,
            type = IntInRange(min = 0, max = 100), // Use 0-100% in the API.
        ) {
            preconditions(R.string.night_display_temperature_preconditions) {
                if (context.colorDisplayManager.isNightDisplayActivated) {
                    Allowed
                } else {
                    InvalidPreference(
                        otherPreferenceScreenKey = KEY,
                        otherPreferenceKey = NIGHT_DISPLAY_ACTIVATED_KEY,
                        reason = R.string.night_display_disabled,
                    )
                }
            }

            get { execute { context.getNightDisplayIntensity() } }

            set {
                permissions(CONTROL_DISPLAY_COLOR_TRANSFORMS)
                execute { value -> context.setNightDisplayIntensity(value) }
            }
        }
    }

    private val Context.colorDisplayManager: ColorDisplayManager
        get() = getSystemService(ColorDisplayManager::class.java)!!

    private fun Context.getNightDisplayIntensity(): Int {
        val min = ColorDisplayManager.getMinimumColorTemperature(this)
        val max = ColorDisplayManager.getMaximumColorTemperature(this)
        val temperature = colorDisplayManager.getNightDisplayColorTemperature()
        val temperaturePercentage = (100 * (temperature - min).toFloat() / (max - min)).roundToInt()
        // Intensity range is inverted: 0% is max temperature, 100% is min temperature.
        return 100 - temperaturePercentage
    }

    private fun Context.setNightDisplayIntensity(intensity: Int) {
        val min = ColorDisplayManager.getMinimumColorTemperature(this)
        val max = ColorDisplayManager.getMaximumColorTemperature(this)
        // Intensity range is inverted: 0% is max temperature, 100% is min temperature.
        val temperaturePercentage = 100 - intensity
        val temperature = min + ((max - min).toFloat() * temperaturePercentage) / 100
        colorDisplayManager.setNightDisplayColorTemperature(temperature.roundToInt())
    }

    companion object {
        const val KEY = "api_night_display"
        const val NIGHT_DISPLAY_ACTIVATED_KEY = "night_display_activated"
        const val NIGHT_DISPLAY_TEMPERATURE_KEY = "night_display_temperature"
    }
}
// LINT.ThenChange(NightDisplaySettings.java, NightDisplayScreen.kt,
// NightDisplayActivationPreferenceController.java, NightDisplayIntensityPreferenceController.java)
