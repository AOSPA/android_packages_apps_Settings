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
import com.android.internal.app.LocaleHelper
import com.android.internal.app.LocalePicker
import com.android.internal.app.LocaleStore
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.regionalpreferences.NumberingSystemItemController.ARG_KEY_REGIONAL_PREFERENCE
import com.android.settings.regionalpreferences.NumberingSystemItemController.ARG_VALUE_NUMBERING_SYSTEM_SELECT
import com.android.settings.regionalpreferences.NumberingSystemItemController.KEY_SELECTED_LANGUAGE
import com.android.settings.regionalpreferences.RegionalPreferencesDataUtils.getNumberingSystemLocales
import com.android.settings.regionalpreferences.RegionalPreferencesDataUtils.updateSelectedLocale
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.types.GeneratedParameterType
import com.android.settingslib.metadata.preferencesapi.types.GeneratedType
import com.android.settingslib.metadata.preferencesapi.types.GeneratedValue
import java.util.Locale
import com.android.settingslib.metadata.preferencesapi.safe
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability

// LINT.IfChange
@ProvidePreferenceScreen(NumberingSystemLocaleListApiFirstScreen.KEY, parameterized = true)
class NumberingSystemLocaleListApiFirstScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.SYSTEM,
        fragment = NumberingSystemFormatSelectionFragment::class,
        purpose = R.string.regional_preference_numbering_system_purpose,
    ) {

    init {
        flag { Flags.catalystMigration26q2() }

        parameters {
            parameter(
                name = KEY_SELECTED_LANGUAGE,
                purpose = R.string.regional_preference_numbering_system_parameter_purpose,
                type =
                    GeneratedParameterType(
                        R.string.numbering_system_parameter_description,
                        key = "NumberingSystemLanguageCode",
                    ) {
                        getNumberingSystemLocales().map {
                            GeneratedValue(it.toLanguageTag().safe(), getLocaleNameWithNumberingSystem(it).safe())
                        }
                    },
            )

            prepareScreenExtras { parameters, extras ->
                extras.putString(KEY_SELECTED_LANGUAGE, parameters[KEY_SELECTED_LANGUAGE])
                extras.putString(ARG_KEY_REGIONAL_PREFERENCE, ARG_VALUE_NUMBERING_SYSTEM_SELECT)
            }
        }

        preconditions(R.string.numbering_system_screen_preconditions) {
            if (getNumberingSystemLocales().isNotEmpty()) {
                Allowed
            } else {
                Custom(R.string.numbering_system_screen_unavailable, stability = PreconditionStability.UNSTABLE)
            }
        }

        preference(
            key = KEY_PREFERENCE,
            purpose = R.string.regional_preference_numbering_system_preference_purpose,
            type =
                GeneratedType(description = R.string.numbering_system_description) {
                    // Step1: Get all languages that have numbering system by selected language.
                    // Step2: Show all numbering system of the language, for example,
                    // 1) Selected language is ar-EG: ar-EG and ar-EG-u-nu-latn
                    // 2) Selected language is mzn-IR: mzn-IR and mzn-IR-u-nu-latn
                    // Step3: Set the code to the value and the combination of the language name
                    // and numbering system to the description, for example:
                    // value: ar-EG, mzn-IR-u-nu-latn
                    // description: Arabic (Egypt), Mazanderani (Iran, Western Digits).
                    val selectedLocale =
                        Locale.forLanguageTag(keyParameters?.get(KEY_SELECTED_LANGUAGE) ?: "")
                    val localeList = getLocaleListWithNumberingSystem(context, selectedLocale)
                    localeList.map { locale ->
                        val code = locale.toLanguageTag()
                        val name = getLocaleNameWithNumberingSystem(locale)
                        GeneratedValue(code.safe(), name.safe())
                    }
                },
        ) {
            get {
                execute {
                    val locales = LocaleList.getDefault()
                    val selectedLocale =
                        Locale.forLanguageTag(keyParameters?.get(KEY_SELECTED_LANGUAGE) ?: "")
                            .toLanguageTag()
                    var localeCode = ""
                    for (i in 0 until locales.size()) {
                        val targetLocale = locales.get(i)
                        // Get the selected language's current numbering system from the locale list
                        if (targetLocale.toLanguageTag().startsWith(selectedLocale)) {
                            localeCode = targetLocale.toLanguageTag()
                            break
                        }
                    }
                    localeCode
                }
            }
            set {
                execute { value ->
                    // Update the numbering system for the selected language then update the locale
                    // list
                    updateSelectedLocale(Locale.forLanguageTag(value))
                }
            }
        }
    }

    private fun getLocaleListWithNumberingSystem(
        context: Context,
        targetLocale: Locale,
    ): List<Locale> {
        if (!LocaleStore.getLocaleInfo(targetLocale).hasNumberingSystems()) {
            return emptyList()
        }

        val supportedLocaleTags = LocalePicker.getSupportedLocales(context)
        return supportedLocaleTags
            .map { localeTag -> Locale.forLanguageTag(localeTag) }
            .filter { supportedLocale -> isSameBaseLocale(targetLocale, supportedLocale) }
    }

    private fun getLocaleNameWithNumberingSystem(locale: Locale): String {
        val displayLocale = Locale.getDefault()
        return LocaleHelper.toSentenceCase(
            LocaleHelper.getDisplayName(locale, displayLocale, false),
            displayLocale,
        )
    }

    private fun isSameBaseLocale(locale1: Locale, locale2: Locale): Boolean {
        return locale1.stripExtensions() == locale2.stripExtensions()
    }

    companion object {
        const val KEY = "regional_preference_numbering_system"
        const val KEY_PREFERENCE: String = "numbering_system_preference"
    }
}
// LINT.ThenChange(NumberingSystemFormatSelectionFragment.java)
