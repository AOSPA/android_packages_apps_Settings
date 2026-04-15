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

package com.android.settings.connecteddevice.stylus

import android.hardware.input.InputManager
import android.provider.Settings
import android.view.InputDevice
import android.view.inputmethod.InputMethodManager
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.preferencesapi.safe
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean
import com.android.settingslib.metadata.preferencesapi.types.GeneratedParameterType
import com.android.settingslib.metadata.preferencesapi.types.GeneratedValue
import com.android.settingslib.metadata.preferencesapi.unsafe

// LINT.IfChange
@ProvidePreferenceScreen(StylusUsiDetailsApiScreen.KEY, parameterized = true)
class StylusUsiDetailsApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.CONNECTED_DEVICES,
        fragment = StylusUsiDetailsFragment::class,
        purpose = R.string.api_stylus_screen_purpose,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }

        parameters {
            parameter(
                name = PARAM_KEY,
                purpose = R.string.api_stylus_device_input_id_purpose,
                required = true,
                type =
                    GeneratedParameterType(
                        R.string.api_stylus_device_input_id_type_description,
                        key = "StylusDeviceInputId",
                    ) {
                        val inputManager = context.getSystemService(InputManager::class.java)

                        inputManager
                            ?.inputDeviceIds
                            ?.toList()
                            ?.mapNotNull { id ->
                                val device = inputManager.getInputDevice(id)
                                if (
                                    device != null &&
                                        device.supportsSource(InputDevice.SOURCE_STYLUS)
                                ) {
                                    GeneratedValue(
                                        value = id.toString().safe(),
                                        description = device.name.unsafe() ?: "Stylus $id".safe(),
                                    )
                                } else {
                                    null
                                }
                            }
                            ?.toSet() ?: emptySet()
                    },
            )

            prepareScreenExtras { parameters, extras ->
                val deviceInputId = parameters[PARAM_KEY]
                if (deviceInputId != null) {
                    extras.putInt(PARAM_KEY, deviceInputId.toInt())
                }
            }
        }

        preference(
            key = HANDWRITING_SWITCH_KEY,
            purpose = R.string.api_stylus_handwriting_purpose,
            type = AnyBoolean,
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)
            preconditions(R.string.stylus_handwriting_precondition) {
                val imm = context.getSystemService(InputMethodManager::class.java)
                val inputMethod = imm?.currentInputMethodInfo

                if (inputMethod != null && inputMethod.supportsStylusHandwriting()) {
                    Allowed
                } else {
                    Custom(
                        R.string.stylus_handwriting_precondition_failed,
                        stability = PreconditionStability.UNSTABLE,
                    )
                }
            }

            get {
                execute {
                    // Default value is 1 (true) according to
                    // Secure.STYLUS_HANDWRITING_DEFAULT_VALUE
                    Settings.Secure.getInt(
                        context.contentResolver,
                        Settings.Secure.STYLUS_HANDWRITING_ENABLED,
                        STYLUS_HANDWRITING_ENABLED,
                    ) == STYLUS_HANDWRITING_ENABLED
                }
            }

            set {
                execute { enabled ->
                    Settings.Secure.putInt(
                        context.contentResolver,
                        Settings.Secure.STYLUS_HANDWRITING_ENABLED,
                        if (enabled) STYLUS_HANDWRITING_ENABLED else STYLUS_HANDWRITING_DISABLED,
                    )
                }
            }
        }

        preference(
            key = IGNORE_BUTTON_KEY,
            purpose = R.string.api_stylus_ignore_button_purpose,
            type = AnyBoolean,
        ) {
            sensitivityLevel(SensitivityLevel.DEEP_LINK_ONLY)
            get {
                execute {
                    // Logic is inverted: 0 means buttons are disabled (ignore is TRUE)
                    Settings.Secure.getInt(
                        context.contentResolver,
                        Settings.Secure.STYLUS_BUTTONS_ENABLED,
                        STYLUS_BUTTONS_ENABLED,
                    ) == STYLUS_BUTTONS_DISABLED
                }
            }

            set {
                execute { disabled ->
                    // Logic is inverted: If API passes true (ignore buttons), write 0 to DB
                    Settings.Secure.putInt(
                        context.contentResolver,
                        Settings.Secure.STYLUS_BUTTONS_ENABLED,
                        if (disabled) STYLUS_BUTTONS_DISABLED else STYLUS_BUTTONS_ENABLED,
                    )
                }
            }
        }
    }

    companion object {
        const val KEY = "stylus_usi_details_api_screen"
        const val PARAM_KEY = "device_input_id"
        const val HANDWRITING_SWITCH_KEY = "handwriting_switch"
        const val IGNORE_BUTTON_KEY = "ignore_button"

        const val STYLUS_HANDWRITING_ENABLED = 1
        const val STYLUS_HANDWRITING_DISABLED = 0

        const val STYLUS_BUTTONS_ENABLED = 1
        const val STYLUS_BUTTONS_DISABLED = 0
    }
}
// LINT.ThenChange(StylusUsiDetailsFragment.kt)
