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

import androidx.core.text.util.LocalePreferences
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.types.CustomEnum
import com.android.settingslib.metadata.preferencesapi.types.EnumApiWithString

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
            type = CustomEnum(TemperatureUnitOptions::class, "Temperature unit"),
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)

            get {
                execute {
                    val value =
                        RegionalPreferencesDataUtils.getDefaultUnicodeExtensionData(
                            context,
                            ExtensionTypes.TEMPERATURE_UNIT,
                        )
                    if (value == null) {
                        TemperatureUnitOptions.DEFAULT
                    } else {
                        TemperatureUnitOptions.values().firstOrNull { it.asApiValue == value }
                            ?: error("Unknown temperature unit: $value")
                    }
                }
            }
            set {
                execute { value ->
                    RegionalPreferencesDataUtils.savePreference(
                        context,
                        ExtensionTypes.TEMPERATURE_UNIT,
                        value.takeIf { it != TemperatureUnitOptions.DEFAULT }?.asApiValue,
                    )
                }
            }
        }
    }

    enum class TemperatureUnitOptions(
        override val asApiValue: String,
        override val purpose: String,
    ) : EnumApiWithString<String> {
        DEFAULT(RegionalPreferencesDataUtils.DEFAULT_VALUE, "Default"),
        CELSIUS(LocalePreferences.TemperatureUnit.CELSIUS, "Celsius"),
        FAHRENHEIT(LocalePreferences.TemperatureUnit.FAHRENHEIT, "Fahrenheit"),
    }

    companion object {
        const val KEY = "regional_preference_temperature"
        const val KEY_TEMPERATURE_UNIT = "temperature_unit_preference"
    }
}
// LINT.ThenChange(TemperatureUnitFragment.java)
