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

package com.android.settings.display.darkmode

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import com.android.settings.R
import com.android.settings.accessibility.Flags as AccFlags
import com.android.settings.flags.Flags
import com.android.settings.fuelgauge.batterysaver.BatterySaverScreen
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.InvalidPreference
import com.android.settingslib.metadata.preferencesapi.types.CustomEnum
import com.android.settingslib.metadata.preferencesapi.types.EnumApiWithRes

// LINT.IfChange
@ProvidePreferenceScreen(DarkModeApiFirstScreen.KEY)
class DarkModeApiFirstScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.DISPLAY,
        fragment = DarkModeSettingsFragment::class,
        purpose = R.string.dark_ui_mode_purpose,
        alreadyPartiallyMigrated = DarkModeScreen::class,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }
        preconditions(R.string.api_dark_theme_screen_preconditions) {
            val isScreenPrecondition =
                if (AccFlags.allowToEnterDarkThemeSettingsWhenBatterySaver()) true
                else !context.isPowerSaveMode()
            if (isScreenPrecondition) {
                Allowed
            } else {
                InvalidPreference(
                    otherPreferenceScreenKey = BatterySaverScreen.KEY,
                    otherPreferenceKey = "battery_saver",
                    reason = R.string.api_dark_theme_screen_invalid_preference,
                )
            }
        }
        preference(
            key = RADIO_PREFERENCE_KEY,
            purpose = R.string.dark_theme_mode_selector_purpose,
            type =
                CustomEnum(
                    DarkThemeMode::class,
                    R.string.dark_theme_mode_selector_customenum_description,
                ),
        ) {
            get {
                executeEnum {
                    val isExpanded =
                        Settings.Secure.getInt(
                            context.contentResolver,
                            Settings.Secure.ACCESSIBILITY_FORCE_INVERT_COLOR_ENABLED,
                        ) == EXPANDED_DARK_THEME

                    if (isExpanded) {
                        DarkThemeMode.EXPANDED
                    } else {
                        DarkThemeMode.STANDARD
                    }
                }
            }

            set {
                permissions(WRITE_SECURE_SETTINGS)
                executeEnum { value ->
                    var darkThemeMode = STANDARD_DARK_THEME
                    when (value) {
                        DarkThemeMode.STANDARD -> darkThemeMode = STANDARD_DARK_THEME
                        DarkThemeMode.EXPANDED -> darkThemeMode = EXPANDED_DARK_THEME
                    }
                    Settings.Secure.putInt(
                        context.contentResolver,
                        Settings.Secure.ACCESSIBILITY_FORCE_INVERT_COLOR_ENABLED,
                        darkThemeMode,
                    )
                }
            }
        }
    }

    companion object {
        const val KEY = "api_dark_theme_screen"
        internal const val RADIO_PREFERENCE_KEY = "dark_theme_mode_selector"
        internal const val STANDARD_DARK_THEME = 0
        internal const val EXPANDED_DARK_THEME = 1

        private fun Context.isPowerSaveMode() =
            getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
    }
}

// LINT.ThenChange(DarkModeSettingsFragment.java,
//                 ../DarkUIPreferenceController.java,
//                 ForceInvertPreferenceController.java)

internal enum class DarkThemeMode(override val asApiValue: Int, override val purpose: Int) :
    EnumApiWithRes<Int> {
    STANDARD(
        DarkModeApiFirstScreen.STANDARD_DARK_THEME,
        R.string.accessibility_standard_dark_theme_title,
    ),
    EXPANDED(
        DarkModeApiFirstScreen.EXPANDED_DARK_THEME,
        R.string.accessibility_expanded_dark_theme_title,
    ),
}
