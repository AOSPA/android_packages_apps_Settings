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

import com.android.settings.R
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported

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
        flag { Flags.catalystMigration26q2() }

        preconditions(R.string.auto_brightness_preconditions) {
            if (context.autoBrightnessAvailabilityStatus == AVAILABLE) {
                Allowed
            } else {
                HardwareUnsupported(R.string.auto_brightness_unavailable)
            }
        }
    }

    companion object {
        const val KEY = "api_auto_brightness_entry"
    }
}
// LINT.ThenChange(AutoBrightnessSettings.java, AutoBrightnessPreferenceController.java)
