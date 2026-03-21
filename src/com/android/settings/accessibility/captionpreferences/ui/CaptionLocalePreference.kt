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

package com.android.settings.accessibility.captionpreferences.ui

import android.content.Context
import android.provider.Settings
import androidx.preference.ListPreference
import com.android.internal.app.LocalePicker
import com.android.settings.R
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.preference.PreferenceBinding

class CaptionLocalePreference(context: Context) :
    PersistentPreference<String>, PreferenceBinding, PreferenceSummaryProvider {
    override val valueType: Class<String>
        get() = String::class.javaObjectType

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.caption_preferences_locale_purpose

    override val title: Int
        get() = R.string.captioning_locale

    override fun storage(context: Context) =
        SettingsSecureStore.get(context).apply { setDefaultValue(KEY, "") }

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val supportsWrite = true

    private val localeData: List<LocalePicker.LocaleInfo> by lazy {
        LocalePicker.getAllAssetLocales(context, /* isInDeveloperMode= */ false)
    }

    private val valuesDescription: Array<CharSequence> by lazy {
        buildList {
                add(context.getString(R.string.locale_default))
                addAll(localeData.map { localeInfo -> localeInfo.label })
            }
            .toTypedArray()
    }

    private val values: Array<CharSequence> by lazy {
        buildList {
                add("") // default locale
                addAll(localeData.map { localeInfo -> localeInfo.locale.toString() })
            }
            .toTypedArray()
    }

    override fun createWidget(context: Context) =
        ListPreference(context).apply {
            entries = valuesDescription
            entryValues = values
        }

    override fun getSummary(context: Context): CharSequence? {
        val locale = storage(context).getString(KEY) ?: return null
        val index = values.indexOf(locale)
        return if (index != -1) valuesDescription[index] else null
    }

    companion object {
        private const val KEY = Settings.Secure.ACCESSIBILITY_CAPTIONING_LOCALE
    }
}
