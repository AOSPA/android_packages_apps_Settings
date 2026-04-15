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

import android.content.Context
import com.android.internal.app.LocaleStore
import com.android.internal.app.SystemLocaleCollector
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.regionalpreferences.RegionalPreferencesDataUtils.updateSelectedLocale
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.types.GeneratedType
import com.android.settingslib.metadata.preferencesapi.types.GeneratedValue
import java.util.Locale
import com.android.settingslib.metadata.preferencesapi.safe

// LINT.IfChange
@ProvidePreferenceScreen(RegionPickerApiFirstScreen.KEY)
class RegionPickerApiFirstScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.SYSTEM,
        fragment = RegionPickerFragment::class,
        purpose = R.string.system_locale_region_picker_purpose,
    ) {

    init {
        flag { Flags.catalystMigration26q2() }

        preference(
            key = KEY_PREFERENCE,
            purpose = R.string.system_locale_region_picker_preference_purpose,
            type =
                GeneratedType(
                    description = R.string.default_system_language_region_description,
                    key = "SystemRegionCountryCode",
                ) {
                    val defaultLocaleInfo = LocaleStore.getLocaleInfo(Locale.getDefault())
                    val parentLocale = defaultLocaleInfo.parent
                    val parentLocaleInfo = LocaleStore.getLocaleInfo(parentLocale)
                    val localeInfoList = getSupportedLanguageList(context, parentLocaleInfo)
                    // Only show the default system language's region
                    // value: country code, for example, US, UK, IN
                    // description: full country name, for example, United State, United Kingdom,
                    // India
                    localeInfoList.add(defaultLocaleInfo)
                    localeInfoList.map {
                        GeneratedValue(it.locale.country.safe(), it.fullCountryNameNative.safe())
                    }
                },
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)

            get { execute { Locale.getDefault().country } }
            set {
                warning {
                    warn(R.string.change_region_warning)
                }
                execute { value ->
                    val locale =
                        Locale.Builder().setLocale(Locale.getDefault()).setRegion(value).build()
                    updateSelectedLocale(locale)
                }
            }
        }
    }

    private fun getSupportedLanguageList(
        context: Context,
        parent: LocaleStore.LocaleInfo?,
    ): MutableList<LocaleStore.LocaleInfo> =
        SystemLocaleCollector(context, null)
            .getSupportedLocaleList(parent, false, true)
            .toMutableList()

    companion object {
        const val TAG = "RegionPickerApiFirstScreen"
        const val KEY = "key_system_region_picker_page"
        const val KEY_PREFERENCE = "default_system_language_region_preference"
    }
}
// LINT.ThenChange(RegionPickerFragment.java)
