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
import android.view.InputDevice
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.types.GeneratedParameterType
import com.android.settingslib.metadata.preferencesapi.types.GeneratedValue

// LINT.IfChange
@ProvidePreferenceScreen(StylusUsiDetailsApiScreen.KEY, parameterized = true)
class StylusUsiDetailsApiScreen : PreferencesApiScreen(
    key = KEY,
    topLevelSettingsCategory = Category.CONNECTED_DEVICES,
    fragment = StylusUsiDetailsFragment::class,
    purpose = R.string.api_stylus_screen_purpose
) {
    init {
        flag {
            Flags.catalystMigration26q2()
        }

        parameters {
            parameter(
                name = PARAM_KEY,
                purpose = R.string.api_stylus_device_input_id_purpose,
                required = true,
                type = GeneratedParameterType(
                    R.string.api_stylus_device_input_id_type_description
                ) {
                    val inputManager = context.getSystemService(InputManager::class.java)

                    inputManager?.inputDeviceIds?.toList()?.mapNotNull { id ->
                        val device = inputManager.getInputDevice(id)
                        if (device != null && device.supportsSource(InputDevice.SOURCE_STYLUS)) {
                            GeneratedValue(
                                value = id.toString(),
                                description = device.name ?: "Stylus $id"
                            )
                        } else {
                            null
                        }
                    }?.toSet() ?: emptySet()
                }
            )

            prepareScreenExtras { parameters, extras ->
                val deviceInputId = parameters[PARAM_KEY]
                if (deviceInputId != null) {
                    extras.putInt(PARAM_KEY, deviceInputId.toInt())
                }
            }
        }
    }

    companion object {
        const val KEY = "stylus_usi_details_api_screen"
        const val PARAM_KEY = "device_input_id"
    }
}
// LINT.ThenChange(StylusUsiDetailsFragment.kt)
