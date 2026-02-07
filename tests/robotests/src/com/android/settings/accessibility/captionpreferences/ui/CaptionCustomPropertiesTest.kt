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
import android.view.accessibility.CaptioningManager
import androidx.preference.ListPreference
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.captionpreferences.data.CaptionFontFamilyDataStore
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.datastore.SettingsSecureStore
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CaptionCustomPropertiesTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()

    private lateinit var context: Context
    private lateinit var captioningManager: CaptioningManager
    private lateinit var settingsSecureStore: SettingsSecureStore

    @Before
    fun setUp() {
        captioningManager = mock()
        context =
            spy(ApplicationProvider.getApplicationContext()) {
                on { getSystemService(CaptioningManager::class.java) } doReturn captioningManager
            }
        settingsSecureStore = SettingsSecureStore.get(context)
    }

    @Test
    fun fontFamilyPreference_metadata() {
        val preference = CaptionFontFamilyPreference(context)

        assertThat(preference.key).isEqualTo("captioning_typeface")
        assertThat(preference.title).isEqualTo(R.string.captioning_typeface)
        assertThat(preference.values).isEqualTo(R.array.captioning_typeface_selector_values)
        assertThat(preference.valueType).isEqualTo(String::class.javaObjectType)
    }

    @Test
    fun fontFamilyPreference_storage() {
        val preference = CaptionFontFamilyPreference(context)
        assertThat(preference.storage(context)).isInstanceOf(CaptionFontFamilyDataStore::class.java)
    }

    @Test
    fun fontFamilyPreference_createWidget() {
        val preference = CaptionFontFamilyPreference(context)
        assertThat(preference.createWidget(context)).isInstanceOf(ListPreference::class.java)
    }

    @Test
    fun fontFamilyPreference_getSummary() {
        val preference = CaptionFontFamilyPreference(context)
        val typefaceKey = Settings.Secure.ACCESSIBILITY_CAPTIONING_TYPEFACE
        val values = context.resources.getStringArray(R.array.captioning_typeface_selector_values)
        val titles = context.resources.getStringArray(R.array.captioning_typeface_selector_titles)

        settingsSecureStore.setString(typefaceKey, values[0])
        assertThat(preference.getSummary(context)).isEqualTo(titles[0])

        settingsSecureStore.setString(typefaceKey, values[1])
        assertThat(preference.getSummary(context)).isEqualTo(titles[1])
    }

    @Test
    fun edgeTypePreference_metadata() {
        val preference = CaptionEdgeTypePreference(context)

        assertThat(preference.key).isEqualTo("captioning_edge_type")
        assertThat(preference.title).isEqualTo(R.string.captioning_edge_type)
        assertThat(preference.values).isEqualTo(R.array.captioning_edge_type_selector_values)
        assertThat(preference.valueType).isEqualTo(Int::class.javaObjectType)
    }

    @Test
    fun edgeTypePreference_getSummary() {
        val preference = CaptionEdgeTypePreference(context)
        val edgeTypeKey = Settings.Secure.ACCESSIBILITY_CAPTIONING_EDGE_TYPE
        val values = context.resources.getIntArray(R.array.captioning_edge_type_selector_values)
        val titles = context.resources.getStringArray(R.array.captioning_edge_type_selector_titles)

        settingsSecureStore.setInt(edgeTypeKey, values[1])
        assertThat(preference.getSummary(context)).isEqualTo(titles[1])

        settingsSecureStore.setInt(edgeTypeKey, values[2])
        assertThat(preference.getSummary(context)).isEqualTo(titles[2])
    }

    @Test
    fun edgeColorPreference_metadata() {
        val preference = CaptionEdgeColorPreference(context)

        assertThat(preference.key).isEqualTo("captioning_edge_color")
        assertThat(preference.title).isEqualTo(R.string.captioning_edge_color)
        assertThat(preference.values).isEqualTo(R.array.captioning_color_selector_values)
        assertThat(preference.valueType).isEqualTo(Int::class.javaObjectType)
    }

    @Test
    fun edgeColorPreference_isEnabled_returnsFalseWhenEdgeTypeNone() {
        val preference = CaptionEdgeColorPreference(context)
        settingsSecureStore.setInt(
            Settings.Secure.ACCESSIBILITY_CAPTIONING_EDGE_TYPE,
            CaptioningManager.CaptionStyle.EDGE_TYPE_NONE,
        )

        assertThat(preference.isEnabled(context)).isFalse()
    }

    @Test
    fun edgeColorPreference_isEnabled_returnsTrueWhenEdgeTypeNotNone() {
        val preference = CaptionEdgeColorPreference(context)
        settingsSecureStore.setInt(
            Settings.Secure.ACCESSIBILITY_CAPTIONING_EDGE_TYPE,
            CaptioningManager.CaptionStyle.EDGE_TYPE_DROP_SHADOW,
        )

        assertThat(preference.isEnabled(context)).isTrue()
    }

    @Test
    fun edgeColorPreference_dependencies() {
        val preference = CaptionEdgeColorPreference(context)

        assertThat(preference.dependencies(context).toList())
            .contains(CaptionEdgeTypePreference.KEY)
    }
}
