/**
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

package com.android.settings.regionalpreferences

import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.types.AnyString

// LINT.IfChange
@ProvidePreferenceScreen(MeasurementSystemApiFirstScreen.KEY)
class MeasurementSystemApiFirstScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.SYSTEM,
        fragment = MeasurementSystemItemFragment::class,
        purpose = R.string.regional_preference_measurement_system_purpose,
    ) {

    init {
        flag { Flags.catalystMigration26q2() }

        preference(
            key = KEY_MEASUREMENT_SYSTEM_ITEM,
            purpose = R.string.regional_preference_measurement_system_item_preference_purpose,
            type = AnyString,
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)

            get {
                execute {
                    // Use the converter to convert the data to a human-readable value,
                    // for example: "Metric", "US" and "UK"
                    RegionalPreferencesDataUtils.measurementSystemConverter(
                        context,
                        RegionalPreferencesDataUtils.getDefaultUnicodeExtensionData(
                            context,
                            ExtensionTypes.MEASUREMENT_SYSTEM,
                        ),
                    )
                }
            }
            set {
                execute { value ->
                    val unitValues = context.resources.getStringArray(R.array.measurement_system)
                    val defaultName =
                        context.getString(R.string.default_string_of_regional_preference)
                    for (item in unitValues) {
                        // If the human-readable value contains the input,
                        // 1. the human-readable value is UK and the input is uk
                        // 2. the human-readable value is Use default and the input is default
                        val systemName =
                            RegionalPreferencesDataUtils.measurementSystemConverter(context, item)
                        if (isMeasurementSystemMatch(defaultName, systemName, value)) {
                            RegionalPreferencesDataUtils.savePreference(
                                context,
                                ExtensionTypes.MEASUREMENT_SYSTEM,
                                item.takeIf { it != RegionalPreferencesDataUtils.DEFAULT_VALUE },
                            )
                            break
                        }
                    }
                }
            }
        }
    }

    private fun isMeasurementSystemMatch(
        defaultName: String,
        systemName: String,
        inputValue: String,
    ): Boolean {
        val inputValue = inputValue.lowercase()
        val systemName = systemName.lowercase()

        return when (inputValue) {
            // case 1: when input is "default" or "use default"
            INPUT_VALUE_DEFAULT,
            INPUT_VALUE_USE_DEFAULT -> {
                systemName == defaultName.lowercase()
            }
            // case 2: when input is "us", "uk", "metrics"
            else -> {
                systemName == inputValue
            }
        }
    }

    companion object {
        const val KEY = "regional_preference_measurement_system"
        const val KEY_MEASUREMENT_SYSTEM_ITEM = "measurement_system_item_preference"
        const val INPUT_VALUE_DEFAULT = "default"
        const val INPUT_VALUE_USE_DEFAULT = "use default"
    }
}
// LINT.ThenChange(MeasurementSystemItemFragment.java)
