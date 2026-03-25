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
import com.android.settingslib.metadata.preferencesapi.types.AnyString
import com.android.settingslib.metadata.preferencesapi.types.CustomEnum
import com.android.settingslib.metadata.preferencesapi.types.EType
import com.android.settingslib.metadata.preferencesapi.types.EnumApiWithString

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
            type = CustomEnum(FirstDayOfWeekOptions::class, "A day of the week"),
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)

            get {
                execute {
                    val value =RegionalPreferencesDataUtils.getDefaultUnicodeExtensionData(
                        context,
                        ExtensionTypes.FIRST_DAY_OF_WEEK,
                    )
                    if (value == null) {
                        FirstDayOfWeekOptions.DEFAULT
                    } else {
                        FirstDayOfWeekOptions.values().firstOrNull { it.asApiValue == value } ?: error("Unknown first day of week: $value")
                    }
                }
            }
            set {
                execute { value ->
                    RegionalPreferencesDataUtils.savePreference(
                        context, ExtensionTypes.FIRST_DAY_OF_WEEK,
                            value.takeIf { it != FirstDayOfWeekOptions.DEFAULT }?.asApiValue
                    )
                }
            }
        }
    }

    enum class FirstDayOfWeekOptions(
        override val asApiValue: String,
        override val purpose: String,
    ) : EnumApiWithString<String> {
        DEFAULT("default", "Default"),
        SUNDAY(LocalePreferences.FirstDayOfWeek.SUNDAY, "Sunday"),
        MONDAY(LocalePreferences.FirstDayOfWeek.MONDAY, "Monday"),
        TUESDAY(LocalePreferences.FirstDayOfWeek.TUESDAY, "Tuesday"),
        WEDNESDAY(LocalePreferences.FirstDayOfWeek.WEDNESDAY, "Wednesday"),
        THURSDAY(LocalePreferences.FirstDayOfWeek.THURSDAY, "Thursday"),
        FRIDAY(LocalePreferences.FirstDayOfWeek.FRIDAY, "Friday"),
        SATURDAY(LocalePreferences.FirstDayOfWeek.SATURDAY, "Saturday")
    }

    companion object {
        const val KEY = "regional_preference_first_day_of_week"
        const val KEY_FIRST_DAY_OF_WEEK = "first_day_of_week_item_preference"
    }
}
// LINT.ThenChange(FirstDayOfWeekItemFragment.java)
