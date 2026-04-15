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

package com.android.settings.localepicker

import android.content.Context
import android.text.TextUtils
import com.android.internal.app.AppLocaleCollector
import com.android.internal.app.LocaleStore
import com.android.settings.R
import com.android.settings.applications.InstalledPackageName
import com.android.settings.flags.Flags
import com.android.settings.localepicker.AppLocalePickerFragment.ARG_PACKAGE_NAME
import com.android.settings.localepicker.LocaleUtils.canDisplayLocaleUi
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.types.GeneratedParameterType
import com.android.settingslib.metadata.preferencesapi.types.GeneratedType
import com.android.settingslib.metadata.preferencesapi.types.GeneratedValue
import java.util.Locale
import com.android.settingslib.metadata.preferencesapi.unsafe
import com.android.settingslib.metadata.preferencesapi.safe
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability

// LINT.IfChange
@ProvidePreferenceScreen(AppLocalePickerApiFirstScreen.KEY, parameterized = true)
class AppLocalePickerApiFirstScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.SYSTEM,
        fragment = AppLocalePickerFragment::class,
        purpose = R.string.app_locale_picker_purpose,
    ) {

    init {
        flag { Flags.catalystMigration26q2() }

        parameters {
            parameter(
                name = ARG_PACKAGE_NAME,
                purpose = R.string.app_locale_picker_parameter_purpose,
                type = InstalledPackageName
            )

            prepareScreenExtras { parameters, extras ->
                extras.putString(ARG_PACKAGE_NAME, parameters[ARG_PACKAGE_NAME])
            }
        }

        preconditions(R.string.app_locale_picker_screen_preconditions) {
            val packageName = keyParameters?.get(ARG_PACKAGE_NAME) ?: ""
            if (!TextUtils.isEmpty(packageName) && canDisplayLocaleUi(context, packageName)) {
                Allowed
            } else {
                Custom(R.string.app_locale_picker_screen_unavailable, stability = PreconditionStability.UNSTABLE)
            }
        }

        preference(
            key = KEY_PREFERENCE,
            purpose = R.string.app_locale_picker_preference_purpose,
            type =
                GeneratedType(description = R.string.app_locale_picker_description) {
                    // value: language code/tag, for example, en-US, fr-CA
                    // description: Full language name (language with region), for example,
                    // English (United State), Français (Canada)
                    val localeInfoList =
                        getLocaleInfoList(context, keyParameters?.get(ARG_PACKAGE_NAME) ?: "")
                    localeInfoList.map {
                        GeneratedValue(it.locale.toLanguageTag().safe(), it.fullNameNative.safe())
                    }
                },
        ) {
            get {
                execute {
                    val localeInfoList =
                        getLocaleInfoList(context, keyParameters?.get(ARG_PACKAGE_NAME) ?: "")
                    val localeCode =
                        localeInfoList.find { it.isAppCurrentLocale }?.locale?.toLanguageTag() ?: ""
                    localeCode
                }
            }
            set {
                execute { value ->
                    val localeInfo = LocaleStore.getLocaleInfo(Locale.forLanguageTag(value))
                    LocaleUtils.onLocaleSelected(
                        context,
                        localeInfo,
                        keyParameters?.get(ARG_PACKAGE_NAME) ?: ""
                    )
                }
            }
        }
    }

    private fun getSupportedLanguageList(
        context: Context,
        appPackageName: String,
        parent: LocaleStore.LocaleInfo?,
        isForCountryMode: Boolean,
    ): Set<LocaleStore.LocaleInfo> {
        return AppLocaleCollector(context, appPackageName)
            .getSupportedLocaleList(parent, false, isForCountryMode)
    }

    private fun getLocaleInfoList(
        context: Context,
        appPackageName: String,
    ): List<LocaleStore.LocaleInfo> {
        val languageList = getSupportedLanguageList(context, appPackageName, null, false)
        val localeInfoList = languageList.flatMap { language ->
            if (language.isSuggested) {
                listOf(language)
            } else {
                getSupportedLanguageList(context, appPackageName, language, true)
            }
        }
        return localeInfoList
    }

    companion object {
        const val KEY = "key_app_language_picker_page"
        const val KEY_PREFERENCE = "app_language_picker_preference"
    }
}
// LINT.ThenChange(AppLocalePickerFragment.java)
