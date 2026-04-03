/*
 * Copyright (C) 2025 The Android Open Source Project
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
import android.os.UserManager
import android.provider.Settings
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean

// LINT.IfChange
@ProvidePreferenceScreen(DoubleTwistGestureApiFirstScreen.KEY)
class DoubleTwistGestureApiFirstScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.SYSTEM,
        fragment = DoubleTwistGestureSettings::class,
        purpose = R.string.double_twist_gesture_screen_purpose,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }
        // This preconditions detect whether this screen will be available or not.
        preconditions(R.string.double_twist_gesture_screen_preconditions) {
            if (DoubleTwistPreferenceController.isGestureAvailable(context)) {
                Allowed
            } else {
                HardwareUnsupported(R.string.double_twist_gesture_screen_hardware_unsupported)
            }
        }

        preference(
            key = MAIN_SWITCH_KEY,
            purpose = R.string.gesture_double_twist_purpose,
            type = AnyBoolean,
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)

            get {
                execute {
                    Settings.Secure.getInt(
                        context.contentResolver,
                        Settings.Secure.CAMERA_DOUBLE_TWIST_TO_FLIP_ENABLED,
                        ON,
                    ) != OFF
                }
            }
            set {
                permissions(WRITE_SECURE_SETTINGS)
                preconditions(R.string.double_twist_gesture_screen_set_preconditions) {
                    val userManager: UserManager? =
                        context.getSystemService(UserManager::class.java)
                    if (userManager != null) {
                        Allowed
                    } else {
                        Custom(
                            R.string.double_twist_gesture_screen_usermanager_not_available,
                            stability = PreconditionStability.UNSTABLE,
                        )
                    }
                }
                execute { value ->
                    val userManager =
                        context.getSystemService(UserManager::class.java)
                            ?: error("UserManager is not available.")
                    DoubleTwistPreferenceController.setDoubleTwistPreference(
                        context,
                        userManager,
                        if (value) ON else OFF,
                    )
                }
            }
        }
    }

    companion object {
        const val KEY = "double_twist_gesture_screen"
        const val MAIN_SWITCH_KEY = "gesture_double_twist"
        const val ON = 1
        const val OFF = 0
    }
}
// LINT.ThenChange(DoubleTwistGestureSettings.java,
//                 DoubleTwistPreferenceController.java)
