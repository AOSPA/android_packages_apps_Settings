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
import androidx.preference.ListPreference
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.captionpreferences.data.CaptionFontSizeDataStore
import com.android.settings.testutils.SettingsStoreRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CaptionFontSizePreferenceTest {

    @get:Rule val settingsStoreRule = SettingsStoreRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val preference = CaptionFontSizePreference(context)

    @Test
    fun key_isCorrect() {
        assertThat(preference.key).isEqualTo(CaptionFontSizePreference.KEY)
    }

    @Test
    fun title_isCorrect() {
        assertThat(preference.title).isEqualTo(R.string.captioning_text_size)
    }

    @Test
    fun storage_isCaptionFontSizeDataStore() {
        assertThat(preference.storage(context)).isInstanceOf(CaptionFontSizeDataStore::class.java)
    }

    @Test
    fun createWidget_returnsListPreference() {
        assertThat(preference.createWidget(context)).isInstanceOf(ListPreference::class.java)
    }

    @Test
    fun getSummary_returnsCorrectString() {
        val values = context.resources.getStringArray(R.array.captioning_font_size_selector_values)
        val titles = context.resources.getStringArray(R.array.captioning_font_size_selector_titles)
        val testValue = values[0]
        val expectedTitle = titles[0]

        preference.storage(context).setString(preference.key, testValue)

        assertThat(preference.getSummary(context)).isEqualTo(expectedTitle)
    }
}
