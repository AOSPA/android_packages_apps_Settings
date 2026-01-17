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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.internal.app.LocalePicker
import com.android.settings.R
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.datastore.SettingsSecureStore
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [CaptionLocalePreference]. */
@RunWith(AndroidJUnit4::class)
class CaptionLocalePreferenceTest {

    @get:Rule val settingsStoreRule = SettingsStoreRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference: CaptionLocalePreference = CaptionLocalePreference(context)
    private val store = SettingsSecureStore.get(context)

    private val localeInfos =
        LocalePicker.getAllAssetLocales(context, /* isInDeveloperMode= */ false)

    @Test
    fun valueType_isString() {
        assertThat(preference.valueType).isEqualTo(String::class.javaObjectType)
    }

    @Test
    fun key_isCorrect() {
        assertThat(preference.key).isEqualTo(Settings.Secure.ACCESSIBILITY_CAPTIONING_LOCALE)
    }

    @Test
    fun purpose_isCorrect() {
        assertThat(preference.purpose).isEqualTo(R.string.caption_preferences_locale_purpose)
    }

    @Test
    fun title_isCorrect() {
        assertThat(preference.title).isEqualTo(R.string.captioning_locale)
    }

    @Test
    fun storage_isSettingsSecureStore_withDefaultValue() {
        val storage = preference.storage(context)
        assertThat(storage).isInstanceOf(SettingsSecureStore::class.java)
        assertThat(storage.getDefaultValue(preference.key, String::class.javaObjectType))
            .isEqualTo("")
    }

    @Test
    fun createWidget_returnsListPreference_withCorrectEntries() {
        val widget = preference.createWidget(context)
        val expectedEntries = buildList {
            add(context.getString(R.string.locale_default))
            addAll(localeInfos.map { localeInfo -> localeInfo.label })
        }

        val expectedEntryValues = buildList {
            add("")
            addAll(localeInfos.map { localeInfo -> localeInfo.locale.toString() })
        }

        assertThat(widget).isInstanceOf(ListPreference::class.java)
        assertThat(widget.entries.toList()).containsExactlyElementsIn(expectedEntries)
        assertThat(widget.entryValues.toList()).containsExactlyElementsIn(expectedEntryValues)
    }

    @Test
    fun getSummary_noLocaleSet_returnsDefault() {
        store.setString(preference.key, "")
        assertThat(preference.getSummary(context))
            .isEqualTo(context.getString(R.string.locale_default))
    }

    @Test
    fun getSummary_invalidLocale_returnsNull() {
        store.setString(preference.key, "invalid_locale")
        assertThat(preference.getSummary(context)).isNull()
    }

    @Test
    fun getSummary_noDefaultLocale_returnLocaleLabel() {
        assumeTrue(
            "There should be at least one locale to make this test effective",
            localeInfos.isNotEmpty(),
        )
        val newLocale = localeInfos[localeInfos.size - 1]
        store.setString(preference.key, newLocale.locale.toString())

        assertThat(preference.getSummary(context).toString()).isEqualTo(newLocale.label)
    }
}
