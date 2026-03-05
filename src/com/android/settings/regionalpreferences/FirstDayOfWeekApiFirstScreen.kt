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
@ProvidePreferenceScreen(FirstDayOfWeekApiFirstScreen.KEY)
class FirstDayOfWeekApiFirstScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.SYSTEM,
        fragment = FirstDayOfWeekItemFragment::class,
        purpose = R.string.first_day_of_week_purpose,
    ) {

    init {
        flag { Flags.catalystMigration26q2() }

        preference(
            key = KEY_FIRST_DAY_OF_WEEK,
            purpose = R.string.first_day_of_week_item_preference_purpose,
            type = AnyString,
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)

            get {
                execute {
                    // Use the converter to convert the data to a human-readable value,
                    // for example: "Sunday", "Monday" and "Tuesday"...etc
                    RegionalPreferencesDataUtils.dayConverter(
                        context,
                        RegionalPreferencesDataUtils.getDefaultUnicodeExtensionData(
                            context,
                            ExtensionTypes.FIRST_DAY_OF_WEEK,
                        ),
                    )
                }
            }
            set {
                execute { value ->
                    val unitValues = context.resources.getStringArray(R.array.first_day_of_week)
                    for (item in unitValues) {
                        // If the human-readable value contains the input,
                        // 1. the human-readable value is Sunday and the input is sun
                        // 2. the human-readable value is Use default and the input is default
                        if (
                            RegionalPreferencesDataUtils.dayConverter(context, item)
                                .contains(value, ignoreCase = true)
                        ) {
                            RegionalPreferencesDataUtils.savePreference(
                                context, ExtensionTypes.FIRST_DAY_OF_WEEK,
                                item.takeIf { it != RegionalPreferencesDataUtils.DEFAULT_VALUE }
                            )
                            break
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val KEY = "regional_preference_first_day_of_week"
        const val KEY_FIRST_DAY_OF_WEEK = "first_day_of_week_item_preference"
    }
}
// LINT.ThenChange(FirstDayOfWeekItemFragment.java)
