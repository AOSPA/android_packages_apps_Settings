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

package com.android.settings.language

import android.content.Context
import android.os.LocaleList
import android.util.Log
import com.android.internal.app.LocalePicker
import com.android.internal.app.LocaleStore
import com.android.internal.app.SystemLocaleCollector
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.localepicker.LocaleUtils
import com.android.settings.regionalpreferences.RegionalPreferencesDataUtils.appendLocaleExtension
import com.android.settings.regionalpreferences.RegionalPreferencesDataUtils.sameLanguageAndScript
import com.android.settings.regionalpreferences.RegionalPreferencesDataUtils.updateSelectedLocale
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.types.GeneratedParameterType
import com.android.settingslib.metadata.preferencesapi.types.GeneratedType
import com.android.settingslib.metadata.preferencesapi.types.GeneratedValue
import java.util.Locale
import com.android.settingslib.metadata.preferencesapi.safe

// LINT.IfChange
@ProvidePreferenceScreen(LanguageAndRegionApiFirstScreen.KEY)
class LanguageAndRegionApiFirstScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.SYSTEM,
        fragment = LanguageAndRegionSettings::class,
        purpose = R.string.language_and_region_settings_purpose,
        alreadyPartiallyMigrated = LanguageAndRegionScreen::class,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }

        preference(
            key = KEY_PREFERENCE,
            purpose = R.string.default_system_language_purpose,
            type =
                GeneratedType(description = R.string.default_system_language_description) {
                    // value: language code/tag, for example, en-US, fr-CA
                    // description: Full language name (language with region), for example,
                    // English (United State), Français (Canada)
                    val localeInfoList = getLocaleInfoList(context)
                    // If a language has been added to the user's list, it will not appear in the
                    // supported list. We need to add it back to ensure the list is complete.
                    localeInfoList.addAll(
                        LocaleUtils.getUserLocaleList().map {
                            LocaleStore.getLocaleInfo(it.locale.stripExtensions())
                        }
                    )
                    localeInfoList.map {
                        GeneratedValue(it.locale.toLanguageTag().safe(), it.fullNameNative.safe())
                    }
                },
        ) {
            get { execute { Locale.getDefault().toLanguageTag() } }
            set {
                execute { value ->
                    val locale = appendLocaleExtension(Locale.forLanguageTag(value))
                    // Check whether the country that user inputs is in the user list already
                    val userLocaleList = LocaleUtils.getUserLocaleList()
                    val indexInUserList = userLocaleList.indexOfFirst { it?.locale == locale }
                    when {
                        // In the user list, move the user input to the first position
                        indexInUserList != -1 -> {
                            val existingLocaleInfo = userLocaleList.removeAt(indexInUserList)
                            userLocaleList.add(0, existingLocaleInfo)
                        }

                        // Update the region if the input language is the same as current default
                        // language. For example: current default language is en-US and the input is
                        // en-GB, then update the region to GB from US.
                        sameLanguageAndScript(locale, Locale.getDefault()) -> {
                            updateSelectedLocale(locale)
                            return@execute
                        }

                        else -> {
                            // It's not in the user list, check whether it is in the supported list
                            // If the language is supported, add it to the top of the user list.
                            val supportedLocaleInfo = getLocaleInfoList(context).find {
                                locale
                                    .toLanguageTag()
                                    .startsWith(it.locale.toLanguageTag(), ignoreCase = true)
                            }

                            supportedLocaleInfo?.let {
                                userLocaleList.add(0, LocaleStore.getLocaleInfo(locale))
                            }
                        }
                    }

                    val localeList = LocaleList(*userLocaleList.map { it?.locale }.toTypedArray())
                    // Update the device settings by the new locale list
                    LocaleList.setDefault(localeList)
                    LocalePicker.updateLocales(localeList)
                }
            }
        }
    }

    private fun getSupportedLanguageList(
        context: Context,
        parent: LocaleStore.LocaleInfo?,
        isForCountryMode: Boolean,
    ): Set<LocaleStore.LocaleInfo> {
        return SystemLocaleCollector(context, null)
            .getSupportedLocaleList(parent, false, isForCountryMode)
    }

    private fun getLocaleInfoList(context: Context): MutableList<LocaleStore.LocaleInfo> {
        // The language which is supported on the device, for example, en, es, fr...etc
        val languageList = getSupportedLanguageList(context, null, false)
        val localeInfoList: MutableList<LocaleStore.LocaleInfo> = ArrayList()
        for (language in languageList) {
            // Use the language to get its region, for example,
            // en: en-US, en-UK, en-IN...etc
            // es: es-US, es-ES, es-AR...etc
            // fr: fr-FR, fr-CA, fr-LU...etc
            localeInfoList.addAll(getSupportedLanguageList(context, language, true))
        }
        return localeInfoList
    }

    companion object {
        const val TAG = "LanguageAndRegionApiFirstScreen"
        const val KEY = "api_language_and_region_settings"
        const val KEY_PREFERENCE = "default_system_language_preference"
    }
}
// LINT.ThenChange(LanguageAndRegionSettings.java, LanguageAndRegionPreferenceController.java)
