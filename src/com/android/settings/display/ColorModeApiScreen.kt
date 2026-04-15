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

import android.content.Context
import android.hardware.display.ColorDisplayManager
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.preferencesapi.safe
import com.android.settingslib.metadata.preferencesapi.types.GeneratedType
import com.android.settingslib.metadata.preferencesapi.types.EType
import com.android.settingslib.metadata.preferencesapi.types.GeneratedValue

// LINT.IfChange
@ProvidePreferenceScreen(ColorModeApiScreen.KEY)
class ColorModeApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.DISPLAY,
        fragment = ColorModePreferenceFragment::class,
        purpose = R.string.color_mode_purpose,
        alreadyPartiallyMigrated = ColorModeScreen::class,
    ) {

    // Initialized in the preference() init block.
    private lateinit var colorModesToSummaries: Map<Int, CharSequence>

    init {
        flag {
            Flags.catalystMigration26q2() &&
                com.android.server.display.feature.flags.Flags.displaySettingsApiScreenSupport()
        }

        preconditions(R.string.color_mode_preconditions) {
            if (!context.colorDisplayManager.isDeviceColorManaged) {
                HardwareUnsupported("The device does not have a wide color gamut display")
            } else if (ColorDisplayManager.areAccessibilityTransformsEnabled(context)) {
                Custom(R.string.color_mode_unsupported, stability = PreconditionStability.UNSTABLE)
            } else {
                Allowed
            }
        }

        preference(
            key = COLOR_MODE_KEY,
            type =
                GeneratedType(R.string.color_mode_options_purpose) {
                    colorModesToSummaries = ColorModeUtils.getColorModeMapping(context.resources)
                    ColorModeUtils.getAvailableColorModes(context)
                        .map { colorMode ->
                            GeneratedValue(
                                value = colorMode.safe(),
                                description =
                                    colorModesToSummaries.get(colorMode)?.toString()?.safe()
                                        ?: colorMode.toString().safe(),
                            )
                        }
                        .toSet()
                },
            purpose = R.string.color_mode_purpose,
        ) {
            get { execute { ColorModeUtils.getColorMode(context) } }
            set { execute { value -> context.colorDisplayManager.let { it.setColorMode(value) } } }
        }
    }

    companion object {
        const val KEY = "api_color_mode_settings"
        const val COLOR_MODE_KEY = "color_mode"

        private val Context.colorDisplayManager: ColorDisplayManager
            get() = getSystemService(ColorDisplayManager::class.java)!!
    }
}
// LINT.ThenChange(ColorModePreferenceFragment.java, ColorModePreferenceController.java,
// ColorModeScreen.kt, ColorModeScreenTest.kt)
