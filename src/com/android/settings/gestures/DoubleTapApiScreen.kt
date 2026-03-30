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

package com.android.settings.gestures

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.hardware.display.AmbientDisplayConfiguration
import android.os.UserHandle
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
@ProvidePreferenceScreen(DoubleTapApiScreen.KEY)
class DoubleTapApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.SYSTEM,
        fragment = DoubleTapScreenSettings::class,
        purpose = R.string.double_tap_screen_purpose,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }
        preconditions(R.string.double_tap_screen_preconditions) {
            if (AmbientDisplayConfiguration(context).doubleTapSensorAvailable()) {
                Allowed
            } else {
                HardwareUnsupported(R.string.double_tap_screen_hardware_unsupported)
            }
        }
        preference(
            key = MAIN_SWITCH_KEY,
            purpose = R.string.gesture_double_tap_screen_purpose,
            type = AnyBoolean,
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)

            get {
                execute {
                    AmbientDisplayConfiguration(context)
                        .doubleTapGestureEnabled(UserHandle.myUserId())
                }
            }
            set {
                permissions(WRITE_SECURE_SETTINGS)
                execute { value ->
                    Secure.putInt(
                        context.contentResolver,
                        Secure.DOZE_DOUBLE_TAP_GESTURE,
                        if (value) ON else OFF,
                    )
                }
            }
        }
    }

    companion object {
        const val KEY = "double_tap_screen"
        internal const val MAIN_SWITCH_KEY = "gesture_double_tap_screen"
        internal const val ON = 1
        internal const val OFF = 0
    }
}
// LINT.ThenChange(DoubleTapScreenSettings.java,
//                 DoubleTapScreenPreferenceController.java)
