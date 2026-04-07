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
@ProvidePreferenceScreen(PickupGestureApiScreen.KEY)
class PickupGestureApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.SYSTEM,
        fragment = PickupGestureSettings::class,
        purpose = R.string.pickup_gesture_screen_purpose,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }
        preconditions(R.string.pickup_gesture_screen_preconditions) {
            if (AmbientDisplayConfiguration(context).dozePickupSensorAvailable()) {
                Allowed
            } else {
                HardwareUnsupported(R.string.pickup_gesture_screen_hardware_unsupported)
            }
        }
        preference(
            key = MAIN_SWITCH_KEY,
            purpose = R.string.gesture_pick_up_purpose,
            type = AnyBoolean,
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)
            get {
                execute {
                    AmbientDisplayConfiguration(context).pickupGestureEnabled(UserHandle.myUserId())
                }
            }
            set {
                permissions(WRITE_SECURE_SETTINGS)
                execute { value ->
                    Secure.putInt(
                        context.getContentResolver(),
                        Secure.DOZE_PICK_UP_GESTURE,
                        if (value) ON else OFF,
                    )
                }
            }
        }
    }

    companion object {
        const val KEY = "pickup_gesture_screen"
        const val MAIN_SWITCH_KEY = "gesture_pick_up"
        const val ON = 1
        const val OFF = 0
    }
}
// LINT.ThenChange(PickupGestureSettings.java,
//                 PickupGesturePreferenceController.java)
