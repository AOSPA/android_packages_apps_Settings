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
import android.os.LocaleList
import com.android.internal.app.LocalePicker
import com.android.internal.app.LocaleStore
import com.android.internal.app.SystemLocaleCollector
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.types.GeneratedType
import com.android.settingslib.metadata.preferencesapi.types.GeneratedValue
import java.util.Locale

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
                GeneratedType(description = R.string.default_system_language_region_description) {
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
                        GeneratedValue(it.locale.country, it.fullCountryNameNative)
                    }
                },
        ) {
            get { execute { Locale.getDefault().country } }
            set {
                execute { value ->
                    val locale =
                        Locale.Builder()
                            .setLocale(Locale.getDefault())
                            .setRegion(value)
                            .build()
                    updateRegion(locale)
                }
            }
        }
    }

    private fun getSupportedLanguageList(
        context: Context,
        parent: LocaleStore.LocaleInfo?,
    ): MutableList<LocaleStore.LocaleInfo> {
        return SystemLocaleCollector(context, null)
            .getSupportedLocaleList(parent, false, true)
            .toMutableList()
    }

    private fun updateRegion(selectedLocale: Locale) {
        val newLocales = getUpdatedLocales(selectedLocale)
        val localeList = LocaleList(*newLocales)
        LocaleList.setDefault(localeList)
        LocalePicker.updateLocales(localeList)
    }

    private fun getUpdatedLocales(selectedLocale: Locale): Array<Locale?> {
        val localeList = LocaleList.getDefault()
        val newLocales = arrayOfNulls<Locale>(localeList.size())
        for (i in 0..<localeList.size()) {
            val target = localeList.get(i)
            if (sameLanguageAndScript(selectedLocale, target!!)) {
                newLocales[i] = appendLocaleExtension(selectedLocale)
            } else {
                newLocales[i] = localeList.get(i)
            }
        }
        return newLocales
    }

    private fun appendLocaleExtension(selectedLocale: Locale): Locale {
        val systemLocale = Locale.getDefault()
        val extensionKeys = systemLocale.getExtensionKeys()
        val builder = Locale.Builder()
        builder.setLocale(selectedLocale)
        if (!extensionKeys.isEmpty()) {
            for (extKey in extensionKeys) {
                builder.setExtension(extKey, systemLocale.getExtension(extKey))
            }
        }
        return builder.build()
    }

    private fun sameLanguageAndScript(source: Locale, target: Locale): Boolean {
        val sourceLanguage = source.language
        val targetLanguage = target.language
        val sourceLocaleScript = source.script
        val targetLocaleScript = target.script
        if (sourceLanguage == targetLanguage) {
            if (!sourceLocaleScript.isEmpty() && !targetLocaleScript.isEmpty()) {
                return sourceLocaleScript == targetLocaleScript
            }
            return true
        }
        return false
    }

    companion object {
        const val TAG = "RegionPickerApiFirstScreen"
        const val KEY = "key_system_region_picker_page"
        const val KEY_PREFERENCE = "default_system_language_region_preference"
    }
}
// LINT.ThenChange(RegionPickerFragment.java)
