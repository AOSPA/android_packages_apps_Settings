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
import android.os.Process
import android.os.UserManager
import android.os.UserManager.DISALLOW_CONFIG_BRIGHTNESS
import android.provider.Settings
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
import android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
import com.android.settings.R
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.EnterpriseRestriction
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean

// LINT.IfChange
@ProvidePreferenceScreen(AutoBrightnessApiScreen.KEY)
class AutoBrightnessApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.DISPLAY,
        fragment = AutoBrightnessSettings::class,
        purpose = R.string.auto_brightness_entry_purpose,
        alreadyPartiallyMigrated = AutoBrightnessScreen::class,
    ) {
    init {
        flag {
            Flags.catalystMigration26q2() &&
                com.android.server.display.feature.flags.Flags.displaySettingsApiScreenSupport()
        }

        preconditions(R.string.auto_brightness_preconditions) {
            if (context.autoBrightnessAvailabilityStatus == AVAILABLE) {
                Allowed
            } else {
                HardwareUnsupported(R.string.auto_brightness_unavailable)
            }
        }

        preference(
            key = PREFERENCE_KEY,
            type = AnyBoolean,
            purpose = R.string.auto_brightness_purpose,
        ) {
            get {
                execute {
                    Settings.System.getInt(
                        context.contentResolver,
                        SCREEN_BRIGHTNESS_MODE,
                        SCREEN_BRIGHTNESS_MODE_MANUAL,
                    ) == SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                }
            }
            set {
                permissions(WRITE_SETTINGS)
                preconditions(R.string.auto_brightness_preconditions) {
                    if (
                        UserManager.get(context)
                            .hasBaseUserRestriction(
                                UserManager.DISALLOW_CONFIG_BRIGHTNESS,
                                Process.myUserHandle(),
                            )
                    ) {
                        EnterpriseRestriction(R.string.auto_brightness_disallowed)
                    } else {
                        Allowed
                    }
                }

                execute { value ->
                    Settings.System.putInt(
                        context.contentResolver,
                        SCREEN_BRIGHTNESS_MODE,
                        if (value) SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                        else SCREEN_BRIGHTNESS_MODE_MANUAL,
                    )
                }
            }
        }
    }

    companion object {
        const val KEY = "api_auto_brightness_entry"
        const val PREFERENCE_KEY = "auto_brightness_entry_preference"
    }
}
// LINT.ThenChange(AutoBrightnessSettings.java, AutoBrightnessPreferenceController.java)
