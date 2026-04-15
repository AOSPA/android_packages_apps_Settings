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

import android.Manifest.permission.WRITE_SETTINGS
import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import com.android.settings.DisplaySettings
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported
import com.android.settingslib.metadata.preferencesapi.types.PercentageInt
import kotlin.math.roundToInt

// LINT.IfChange
@ProvidePreferenceScreen(DisplayApiScreen.KEY)
class DisplayApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.DISPLAY,
        fragment = DisplaySettings::class,
        purpose = R.string.display_settings_api_screen_purpose,
        alreadyPartiallyMigrated = DisplayScreen::class,
    ) {
    init {
        flag {
            Flags.catalystMigration26q2() &&
                com.android.server.display.feature.flags.Flags.displaySettingsApiScreenSupport()
        }

        // exclude this screen from api result since the adjust brightness preference (and more)
        // are in the display_settings_screen screen in the output. We can remove this tag when
        // we'll add other preferences.
        tags(UI_ONLY_PREFERENCE)

        preconditions(R.string.display_settings_screen_preconditions) {
            if (context.resources.getBoolean(R.bool.config_show_top_level_display)) {
                Allowed
            } else {
                HardwareUnsupported(R.string.display_settings_screen_unsupported)
            }
        }

        preference(
            key = BRIGHTNESS_LEVEL_KEY,
            purpose = R.string.display_settings_api_screen_brightness_level_purpose,
            type = PercentageInt,
        ) {
            preconditions(R.string.brightness_level_preconditions) {
                if (context.isBrightnessLevelSettingsAvailable) {
                    Allowed
                } else {
                    HardwareUnsupported(R.string.brightness_level_disabled)
                }
            }

            get { execute { context.getDefaultDisplayBrightnessLevel() } }

            set {
                permissions(WRITE_SETTINGS)
                execute { value -> context.setDefaultDisplayBrightnessLevel(value) }
            }
        }
    }

    private val Context.displayManager: DisplayManager
        get() = getSystemService(DisplayManager::class.java)!!

    private fun Context.getDefaultDisplayBrightnessLevel(): Int =
        displayManager
            .getBrightness(Display.DEFAULT_DISPLAY, DisplayManager.BRIGHTNESS_UNIT_PERCENTAGE)
            .roundToInt()

    private fun Context.setDefaultDisplayBrightnessLevel(value: Int) {
        displayManager.setBrightness(
            Display.DEFAULT_DISPLAY,
            value.toFloat(),
            DisplayManager.BRIGHTNESS_UNIT_PERCENTAGE,
        )
    }

    companion object {
        const val KEY = "api_display_settings_screen"
        const val BRIGHTNESS_LEVEL_KEY = "brightness"
    }
}
// LINT.ThenChange(com.android.settings.DisplaySettings.java, DisplayScreen.kt,
// BrightnessLevelPreference.kt, BrightnessLevelPreferenceController.java)
