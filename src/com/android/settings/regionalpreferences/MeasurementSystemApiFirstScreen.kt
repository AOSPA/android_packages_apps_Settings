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
import com.android.settingslib.metadata.preferencesapi.types.CustomEnum
import com.android.settingslib.metadata.preferencesapi.types.EnumApiWithString

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
            type = CustomEnum(MeasurementSystemOptions::class, "Measurement system"),
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)

            get {
                execute {
                    val value =
                        RegionalPreferencesDataUtils.getDefaultUnicodeExtensionData(
                            context,
                            ExtensionTypes.MEASUREMENT_SYSTEM,
                        )
                    if (value == null) {
                        MeasurementSystemOptions.DEFAULT
                    } else {
                        MeasurementSystemOptions.values().firstOrNull { it.asApiValue == value }
                            ?: error("Unknown measurement system: $value")
                    }
                }
            }
            set {
                execute { value ->
                    RegionalPreferencesDataUtils.savePreference(
                        context,
                        ExtensionTypes.MEASUREMENT_SYSTEM,
                        value.takeIf { it != MeasurementSystemOptions.DEFAULT }?.asApiValue,
                    )
                }
            }
        }
    }

    enum class MeasurementSystemOptions(
        override val asApiValue: String,
        override val purpose: String,
    ) : EnumApiWithString<String> {
        DEFAULT(RegionalPreferencesDataUtils.DEFAULT_VALUE, "Default"),
        METRIC(RegionalPreferencesDataUtils.MEASUREMENT_SYSTEM_METRIC, "Metric"),
        USSYSTEM(RegionalPreferencesDataUtils.MEASUREMENT_SYSTEM_US, "US"),
        UKSYSTEM(RegionalPreferencesDataUtils.MEASUREMENT_SYSTEM_UK, "UK"),
    }

    companion object {
        const val KEY = "regional_preference_measurement_system"
        const val KEY_MEASUREMENT_SYSTEM_ITEM = "measurement_system_item_preference"
        const val INPUT_VALUE_DEFAULT = "default"
        const val INPUT_VALUE_USE_DEFAULT = "use default"
    }
}
// LINT.ThenChange(MeasurementSystemItemFragment.java)
