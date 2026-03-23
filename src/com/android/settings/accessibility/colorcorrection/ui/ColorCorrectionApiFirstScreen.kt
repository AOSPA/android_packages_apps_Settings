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

package com.android.settings.accessibility.colorcorrection.ui

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.android.settings.R
import com.android.settings.accessibility.ToggleDaltonizerPreferenceFragment
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.InvalidPreference
import com.android.settingslib.metadata.preferencesapi.types.CustomEnum
import com.android.settingslib.metadata.preferencesapi.types.EnumApiWithRes

// LINT.IfChange
@ProvidePreferenceScreen(ColorCorrectionApiFirstScreen.KEY)
class ColorCorrectionApiFirstScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.ACCESSIBILITY,
        fragment = ToggleDaltonizerPreferenceFragment::class,
        purpose = R.string.daltonizer_preference_purpose,
        alreadyPartiallyMigrated = ColorCorrectionScreen::class,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }
        preference(
            key = RADIO_PREFERENCE_KEY,
            purpose = R.string.daltonizer_mode_selector_purpose,
            type =
                CustomEnum(
                    DaltonizerMode::class,
                    R.string.daltonizer_mode_selector_customenum_description,
                ),
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)
            preconditions(R.string.daltonizer_mode_selector_preconditions) {
                val isEnabled =
                    Settings.Secure.getInt(
                        context.contentResolver,
                        Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED,
                        OFF,
                    ) == ON
                if (isEnabled) {
                    Allowed
                } else {
                    InvalidPreference(
                        otherPreferenceScreenKey = ColorCorrectionScreen.KEY,
                        otherPreferenceKey = ColorCorrectionMainSwitchPreference.KEY,
                        reason = R.string.daltonizer_mode_selector_invalid_preference,
                    )
                }
            }
            get {
                execute {
                    val value =
                        Settings.Secure.getInt(
                            context.contentResolver,
                            Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER,
                            AccessibilityManager.DALTONIZER_CORRECT_DEUTERANOMALY,
                        )
                    val values = context.resources.getIntArray(R.array.daltonizer_type_values)
                    val index = values.indexOf(value)
                    when (index) {
                        PROTANOMALY_MODE -> DaltonizerMode.PROTANOMALY
                        TRITANOMALY_MODE -> DaltonizerMode.TRITANOMALY
                        GRAYSCALE_MODE -> DaltonizerMode.GRAYSCALE
                        else -> DaltonizerMode.DEUTERANOMALY
                    }
                }
            }
            set {
                permissions(WRITE_SECURE_SETTINGS)
                execute { value ->
                    var index = DEUTERANOMALY_MODE
                    when (value) {
                        DaltonizerMode.DEUTERANOMALY -> index = DEUTERANOMALY_MODE
                        DaltonizerMode.PROTANOMALY -> index = PROTANOMALY_MODE
                        DaltonizerMode.TRITANOMALY -> index = TRITANOMALY_MODE
                        DaltonizerMode.GRAYSCALE -> index = GRAYSCALE_MODE
                    }
                    val values = context.resources.getIntArray(R.array.daltonizer_type_values)
                    Settings.Secure.putInt(
                        context.contentResolver,
                        Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER,
                        values[index],
                    )
                }
            }
        }
    }

    companion object {
        const val KEY = "api_daltonizer_preference"
        private const val ON = 1
        private const val OFF = 0
        internal const val RADIO_PREFERENCE_KEY = "daltonizer_mode_selector"
        internal const val DEUTERANOMALY_MODE = 0
        internal const val PROTANOMALY_MODE = 1
        internal const val TRITANOMALY_MODE = 2
        internal const val GRAYSCALE_MODE = 3
    }
}

// LINT.ThenChange(ColorCorrectionScreen.kt, ModePreference.kt)

internal enum class DaltonizerMode(override val asApiValue: Int, override val purpose: Int) :
    EnumApiWithRes<Int> {
    DEUTERANOMALY(
        ColorCorrectionApiFirstScreen.DEUTERANOMALY_MODE,
        R.string.daltonizer_mode_deuteranomaly_title,
    ),
    PROTANOMALY(
        ColorCorrectionApiFirstScreen.PROTANOMALY_MODE,
        R.string.daltonizer_mode_protanomaly_title,
    ),
    TRITANOMALY(
        ColorCorrectionApiFirstScreen.TRITANOMALY_MODE,
        R.string.daltonizer_mode_tritanomaly_title,
    ),
    GRAYSCALE(
        ColorCorrectionApiFirstScreen.GRAYSCALE_MODE,
        R.string.daltonizer_mode_grayscale_title,
    ),
}
