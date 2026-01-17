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
                    for (item in unitValues) {
                        // If the human-readable value contains the input,
                        // 1. the human-readable value is UK and the input is uk
                        // 2. the human-readable value is Use default and the input is default
                        if (
                            RegionalPreferencesDataUtils.measurementSystemConverter(context, item)
                                .contains(value, ignoreCase = true)
                        ) {
                            RegionalPreferencesDataUtils.savePreference(
                                context,
                                ExtensionTypes.MEASUREMENT_SYSTEM,
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
        const val KEY = "regional_preference_measurement_system"
        const val KEY_MEASUREMENT_SYSTEM_ITEM = "measurement_system_item_preference"
    }
}
// LINT.ThenChange(MeasurementSystemItemFragment.java)
