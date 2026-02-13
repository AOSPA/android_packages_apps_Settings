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

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.provider.Settings
import com.android.settings.R
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean

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

        preference(
            key = HDR_BRIGHTNESS_ENABLED_KEY,
            purpose = R.string.hdr_brightness_enabled_purpose,
            type = AnyBoolean,
        ) {
            get {
                execute {
                    Settings.Secure.getInt(
                        context.contentResolver,
                        Settings.Secure.HDR_BRIGHTNESS_ENABLED,
                        ON,
                    ) == ON
                }
            }

            set {
                permissions(WRITE_SECURE_SETTINGS)
                execute { value ->
                    Settings.Secure.putInt(
                        context.contentResolver,
                        Settings.Secure.HDR_BRIGHTNESS_ENABLED,
                        if (value) ON else OFF,
                    )
                }
            }
        }
    }

    companion object {
        const val KEY = "hdr_brightness_detail"
        internal const val HDR_BRIGHTNESS_ENABLED_KEY = "hdr_brightness_enabled"
        internal const val ON = 1
        internal const val OFF = 0
    }
}
// LINT.ThenChange(HdrBrightnessSettings.java, HdrBrightnessPreferenceController.java)
