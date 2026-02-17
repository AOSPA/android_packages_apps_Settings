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
import android.provider.Settings
import com.android.internal.app.LocaleStore
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Locale

/** Utils for each regional preference unit test.  */
object RegionalPreferenceTestUtils {
    /** Set language tag to Settings Provider  */
    fun setSettingsProviderContent(context: Context, languageTag: String?) {
        Settings.System.putString(
            context.getContentResolver(),
            Settings.System.LOCALE_PREFERENCES, languageTag
        )
    }

    fun setLocaleConditions(): LocaleList {
        val localeInfoEnUs = mock<LocaleStore.LocaleInfo>().apply {
            whenever(fullNameNative).thenReturn("English (United States)")
            whenever(isTranslated).thenReturn(true)
            whenever(locale).thenReturn(Locale.forLanguageTag("en-US"))
        }

        val localeInfoEsUS = mock<LocaleStore.LocaleInfo>().apply {
            whenever(fullNameNative).thenReturn("Español (Estados Unidos)")
            whenever(isTranslated).thenReturn(true)
            whenever(locale).thenReturn(Locale.forLanguageTag("es-US"))
        }

        val locales = listOf(localeInfoEnUs, localeInfoEsUS)
            .map { it.locale }
            .toTypedArray()

        return LocaleList(*locales)
    }
}
