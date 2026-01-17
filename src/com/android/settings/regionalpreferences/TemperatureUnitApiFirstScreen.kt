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
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.types.AnyString

// LINT.IfChange
@ProvidePreferenceScreen(TemperatureUnitApiFirstScreen.KEY)
class TemperatureUnitApiFirstScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.SYSTEM,
        fragment = TemperatureUnitFragment::class,
        purpose = R.string.regional_preference_temperature_purpose,
    ) {

    init {
        flag { Flags.catalystMigration26q2() }

        preference(
            key = KEY_TEMPERATURE_UNIT,
            purpose = R.string.regional_preference_temperature_unit_preference_purpose,
            type = AnyString,
        ) {
            get {
                execute {
                    // Use the converter to convert the data to a human-readable value,
                    // for example: "Use default", "Celsius" and "Fahrenheit"
                    RegionalPreferencesDataUtils.temperatureUnitsConverter(
                        context,
                        RegionalPreferencesDataUtils.getDefaultUnicodeExtensionData(
                            context,
                            ExtensionTypes.TEMPERATURE_UNIT,
                        ),
                    )
                }
            }
            set {
                execute { value ->
                    val unitValues = context.resources.getStringArray(R.array.temperature_units)
                    for (item in unitValues) {
                        // If the human-readable value contains the input,
                        // 1. the human-readable value is Fahrenheit and the input is fahrenhe
                        // 2. the human-readable value is Use default and the input is default
                        if (
                            RegionalPreferencesDataUtils.temperatureUnitsConverter(context, item)
                                .contains(value, ignoreCase = true)
                        ) {
                            RegionalPreferencesDataUtils.savePreference(
                                context,
                                ExtensionTypes.TEMPERATURE_UNIT,
                                item,
                            )
                            break
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val KEY = "regional_preference_temperature"
        const val KEY_TEMPERATURE_UNIT = "temperature_unit_preference"
    }
}
// LINT.ThenChange(TemperatureUnitFragment.java)
