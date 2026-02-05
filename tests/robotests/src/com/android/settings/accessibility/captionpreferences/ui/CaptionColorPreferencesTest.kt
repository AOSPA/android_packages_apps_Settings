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
import android.graphics.Color
import android.provider.Settings
import android.view.accessibility.CaptioningManager
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
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
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CaptionColorPreferencesTest {
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
    fun customCaptionPreference_isAvailable_returnsTrueWhenCustom() {
        val customPreference =
            object : CustomCaptionPreference {
                override val key = "test_key"
                override val purpose = 0
            }
        whenever(captioningManager.rawUserStyle)
            .thenReturn(CaptioningManager.CaptionStyle.PRESET_CUSTOM)

        assertThat(customPreference.isAvailable(context)).isTrue()
    }

    @Test
    fun customCaptionPreference_isAvailable_returnsFalseWhenNotCustom() {
        val customPreference =
            object : CustomCaptionPreference {
                override val key = "test_key"
                override val purpose = 0
            }
        // Use 0 as a representative non-custom preset value
        whenever(captioningManager.rawUserStyle).thenReturn(0)

        assertThat(customPreference.isAvailable(context)).isFalse()
    }

    @Test
    fun customCaptionPreference_dependencies() {
        val customPreference =
            object : CustomCaptionPreference {
                override val key = "test_key"
                override val purpose = 0
            }
        assertThat(customPreference.dependencies(context).toList())
            .isEqualTo(listOf(CustomCaptionOptionsPreference.KEY))
    }

    @Test
    fun textColorPreference_metadata() {
        val preference = CaptionTextColorPreference(context)

        assertThat(preference.key).isEqualTo(CaptionTextColorPreference.KEY)
        assertThat(preference.title).isEqualTo(R.string.captioning_foreground_color)
        assertThat(preference.values).isEqualTo(R.array.captioning_color_selector_values)
        assertThat(preference.valueType).isEqualTo(Int::class.javaObjectType)
    }

    @Test
    fun textOpacityPreference_metadata() {
        val preference = CaptionTextOpacityPreference(context)

        assertThat(preference.key).isEqualTo(CaptionTextOpacityPreference.KEY)
        assertThat(preference.title).isEqualTo(R.string.captioning_foreground_opacity)
        assertThat(preference.values).isEqualTo(R.array.captioning_opacity_selector_values)
    }

    @Test
    fun textOpacityPreference_isEnabled_returnsTrueWhenAlphaNotZero() {
        val preference = CaptionTextOpacityPreference(context)
        val colorKey = Settings.Secure.ACCESSIBILITY_CAPTIONING_FOREGROUND_COLOR

        settingsSecureStore.setInt(colorKey, Color.RED)
        assertThat(preference.isEnabled(context)).isTrue()
    }

    @Test
    fun textOpacityPreference_isEnabled_returnsFalseWhenAlphaIsZero() {
        val preference = CaptionTextOpacityPreference(context)
        val colorKey = Settings.Secure.ACCESSIBILITY_CAPTIONING_FOREGROUND_COLOR

        settingsSecureStore.setInt(
            colorKey,
            Color.argb(0, Color.WHITE.red, Color.WHITE.green, Color.WHITE.blue),
        )
        assertThat(preference.isEnabled(context)).isFalse()
    }

    @Test
    fun textOpacityPreference_dependencies() {
        val preference = CaptionTextOpacityPreference(context)
        assertThat(preference.dependencies(context).toList())
            .contains(CaptionTextColorPreference.KEY)
    }
}
