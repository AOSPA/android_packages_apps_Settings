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

package com.android.settings.notification

import android.content.Context
import android.os.Vibrator
import android.provider.Settings.Secure
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean

// LINT.IfChange
@ProvidePreferenceScreen(SoundApiScreen.KEY)
class SoundApiScreen : PreferencesApiScreen(
    key = KEY,
    topLevelSettingsCategory = Category.SOUND,
    fragment = SoundSettings::class,
    purpose = R.string.sound_screen_purpose_api,
    alreadyPartiallyMigrated = SoundScreen::class,
) {
    init {
        flag { Flags.catalystMigration26q2() }

        preference(
            key = "vibrate_icon",
            purpose = R.string.sound_always_show_vibration_icon_purpose,
            type = AnyBoolean,
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)

            preconditions(R.string.sound_vibration_precondition) {
                if (context.isVibrationAvailable()) {
                    Allowed
                } else {
                    HardwareUnsupported(R.string.sound_vibration_hardware_unsupported)
                }
            }

            get {
                execute {
                    Secure.getInt(context.contentResolver, Secure.STATUS_BAR_SHOW_VIBRATE_ICON, 0 /*default off*/) != 0
                }
            }

            set {
                execute { value ->
                    Secure.putInt(context.contentResolver, Secure.STATUS_BAR_SHOW_VIBRATE_ICON, if (value) 1 else 0)
                }
            }
        }
    }

    private fun Context.isVibrationAvailable(): Boolean {
        return getSystemService(Vibrator::class.java)?.hasVibrator() ?: false
    }

    companion object {
        const val KEY = "api_sound_screen"
    }
}
// LINT.ThenChange(VibrateIconPreferenceController.java)