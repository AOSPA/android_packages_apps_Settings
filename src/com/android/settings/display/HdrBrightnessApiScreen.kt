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
@ProvidePreferenceScreen(HdrBrightnessApiScreen.KEY)
class HdrBrightnessApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.DISPLAY,
        fragment = HdrBrightnessSettings::class,
        purpose = R.string.hdr_brightness_screen_purpose,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }

        preconditions(R.string.hdr_brightness_screen_preconditions) {
            if (HdrBrightnessUtils.getAvailabilityStatus(context) == AVAILABLE) {
                Allowed
            } else {
                HardwareUnsupported(R.string.hdr_brightness_screen_unsupported)
            }
        }
    }

    companion object {
        const val KEY = "hdr_brightness_detail"
    }
}
// LINT.ThenChange(HdrBrightnessSettings.java, HdrBrightnessPreferenceController.java)
