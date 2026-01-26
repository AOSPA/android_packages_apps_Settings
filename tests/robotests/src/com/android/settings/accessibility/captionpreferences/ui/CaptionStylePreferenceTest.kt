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
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.PresetPreference
import com.android.settings.accessibility.captionpreferences.data.CaptionStyleDataStore
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class CaptionStylePreferenceTest {

    @get:Rule val settingsStoreRule = SettingsStoreRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val preference = CaptionStylePreference(context)

    @Test
    fun key_isCorrect() {
        assertThat(preference.key).isEqualTo("captioning_preset")
    }

    @Test
    fun title_isCorrect() {
        assertThat(preference.title).isEqualTo(R.string.captioning_preset)
    }

    @Test
    fun storage_isCaptionStyleDataStore() {
        assertThat(preference.storage(context)).isInstanceOf(CaptionStyleDataStore::class.java)
    }

    @Test
    fun createWidget_returnsPresetPreference() {
        assertThat(preference.createWidget(context)).isInstanceOf(PresetPreference::class.java)
    }

    @Test
    fun bind_initializesPresetPreference() {
        val expectedValues =
            context.resources.getIntArray(R.array.captioning_preset_selector_values)
        val expectedTitles =
            context.resources.getStringArray(R.array.captioning_preset_selector_titles)
        val widget = preference.createAndBindWidget<PresetPreference>(context)

        assertThat(widget.isPersistent).isTrue()
        assertThat(ReflectionHelpers.getField<IntArray>(widget, "mEntryValues"))
            .isEqualTo(expectedValues)
        assertThat(ReflectionHelpers.getField<Array<CharSequence>>(widget, "mEntryTitles"))
            .isEqualTo(expectedTitles)
    }

    @Test
    fun getSummary_returnsCorrectString() {
        val values = context.resources.getIntArray(R.array.captioning_preset_selector_values)
        val titles = context.resources.getStringArray(R.array.captioning_preset_selector_titles)

        val testValue = values[0]
        val expectedTitle = titles[0]

        preference.storage(context).setInt(preference.key, testValue)

        assertThat(preference.getSummary(context)).isEqualTo(expectedTitle)
    }
}
