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
import android.os.SystemProperties
import android.os.UserHandle
import android.provider.Settings.Secure
import android.provider.Settings.Secure.DOZE_TAP_SCREEN_GESTURE
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
@ProvidePreferenceScreen(TapScreenGestureApiScreen.KEY)
class TapScreenGestureApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.SYSTEM,
        fragment = TapScreenGestureSettings::class,
        purpose = R.string.tap_screen_gesture_purpose,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }
        preconditions(R.string.tap_screen_gesture_preconditions) {
            if (AmbientDisplayConfiguration(context).tapSensorAvailable()) {
                Allowed
            } else {
                HardwareUnsupported(R.string.tap_screen_gesture_hardware_unsupported)
            }
        }
        preference(
            key = MAIN_SWITCH_KEY,
            purpose = R.string.gesture_tap_purpose,
            type = AnyBoolean,
        ) {
            sensitivityLevel(SensitivityLevel.DEEP_LINK_ONLY)

            get {
                execute {
                    AmbientDisplayConfiguration(context).tapGestureEnabled(UserHandle.myUserId())
                }
            }
            set {
                permissions(WRITE_SECURE_SETTINGS)
                execute { value ->
                    Secure.putInt(
                        context.getContentResolver(),
                        DOZE_TAP_SCREEN_GESTURE,
                        if (value) ON else OFF,
                    )
                    SystemProperties.set("persist.sys.tap_gesture", if (value) "1" else "0")
                }
            }
        }
    }

    companion object {
        const val KEY = "tap_screen_gesture"
        const val MAIN_SWITCH_KEY = "gesture_tap"
        const val ON = 1
        const val OFF = 0
    }
}
// LINT.ThenChange(TapScreenGestureSettings.java,
//                 TapScreenGesturePreferenceController.java)
