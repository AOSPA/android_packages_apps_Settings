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
import android.hardware.display.ColorDisplayManager
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean

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
            get {
                execute {
                    context
                        .getSystemService(ColorDisplayManager::class.java)
                        .isNightDisplayActivated
                }
            }

            set {
                permissions(CONTROL_DISPLAY_COLOR_TRANSFORMS)
                execute { value ->
                    context.getSystemService(ColorDisplayManager::class.java)?.let {
                        it.setNightDisplayActivated(value)
                    }
                }
            }
        }
    }

    companion object {
        const val KEY = "api_night_display"
        const val NIGHT_DISPLAY_ACTIVATED_KEY = "night_display_activated"
    }
}
// LINT.ThenChange(NightDisplaySettings.java, NightDisplayScreen.kt,
// NightDisplayActivationPreferenceController.java)
