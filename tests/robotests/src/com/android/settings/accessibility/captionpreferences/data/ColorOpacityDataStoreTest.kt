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

package com.android.settings.accessibility.captionpreferences.data

import android.content.Context
import android.graphics.Color
import android.provider.Settings
import android.view.accessibility.CaptioningManager
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.test.core.app.ApplicationProvider
import com.android.settings.accessibility.CaptionUtils
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.datastore.SettingsSecureStore
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ColorOpacityDataStoreTest {

    @get:Rule val settingsStoreRule = SettingsStoreRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val settingsSecureStore = SettingsSecureStore.get(context)

    @Test
    fun captionTextColorDataStore_getValue_returnsFromSettings() {
        val dataStore = CaptionTextColorDataStore(context, isColor = true)
        settingsSecureStore.setInt(
            Settings.Secure.ACCESSIBILITY_CAPTIONING_FOREGROUND_COLOR,
            Color.RED,
        )
        assertThat(dataStore.getValue(KEY, Int::class.javaObjectType)).isEqualTo(Color.RED)
    }

    @Test
    fun captionBackgroundColorDataStore_getValue_returnsFromSettings() {
        val dataStore = CaptionBackgroundColorDataStore(context, isColor = true)
        settingsSecureStore.setInt(
            Settings.Secure.ACCESSIBILITY_CAPTIONING_BACKGROUND_COLOR,
            Color.BLUE,
        )
        assertThat(dataStore.getValue(KEY, Int::class.javaObjectType)).isEqualTo(Color.BLUE)
    }

    @Test
    fun captionWindowColorDataStore_getValue_returnsFromSettings() {
        val dataStore = CaptionWindowColorDataStore(context, isColor = true)
        settingsSecureStore.setInt(
            Settings.Secure.ACCESSIBILITY_CAPTIONING_WINDOW_COLOR,
            Color.GREEN,
        )
        assertThat(dataStore.getValue(KEY, Int::class.javaObjectType)).isEqualTo(Color.GREEN)
    }

    @Test
    fun hasValidColor_returnsTrueForOpaque() {
        val dataStore = CaptionTextColorDataStore(context, isColor = true)
        settingsSecureStore.setInt(
            Settings.Secure.ACCESSIBILITY_CAPTIONING_FOREGROUND_COLOR,
            Color.BLACK,
        )
        assertThat(dataStore.hasValidColor()).isTrue()
    }

    @Test
    fun hasValidColor_returnsFalseForTransparent() {
        val dataStore = CaptionTextColorDataStore(context, isColor = true)
        settingsSecureStore.setInt(
            Settings.Secure.ACCESSIBILITY_CAPTIONING_FOREGROUND_COLOR,
            Color.TRANSPARENT,
        )
        assertThat(dataStore.hasValidColor()).isFalse()
    }

    @Test
    fun setValue_colorMode_updatesColorAndPreservesOpacity() {
        val colorDataStore = CaptionTextColorDataStore(context, isColor = true)
        val colorKey = Settings.Secure.ACCESSIBILITY_CAPTIONING_FOREGROUND_COLOR

        // Initial state: Blue color, 50% opacity
        val initialOpacity = 128
        val initialColor = Color.BLUE
        val initialFullColor =
            Color.argb(initialOpacity, initialColor.red, initialColor.green, initialColor.blue)
        settingsSecureStore.setInt(colorKey, initialFullColor)

        // Set to Red color
        colorDataStore.setValue(KEY, Int::class.javaObjectType, Color.RED)

        val finalFullColor = settingsSecureStore.getInt(colorKey)!!
        assertThat(Color.rgb(finalFullColor.red, finalFullColor.green, finalFullColor.blue))
            .isEqualTo(Color.RED)
        assertThat(Color.alpha(finalFullColor)).isEqualTo(initialOpacity)
    }

    @Test
    fun setValue_colorMode_unspecifiedColor_cachesOpacity() {
        val colorDataStore = CaptionTextColorDataStore(context, isColor = true)
        val opacityDataStore = CaptionTextColorDataStore(context, isColor = false)
        val colorKey = Settings.Secure.ACCESSIBILITY_CAPTIONING_FOREGROUND_COLOR

        // 1. Start with red and 50% opacity
        val opacity50 = Color.argb(128, 255, 255, 255)
        colorDataStore.setValue(KEY, Int::class.javaObjectType, Color.RED)
        opacityDataStore.setValue(KEY, Int::class.javaObjectType, opacity50)

        // 2. Set color to unspecified (Default)
        colorDataStore.setValue(
            KEY,
            Int::class.javaObjectType,
            CaptioningManager.CaptionStyle.COLOR_UNSPECIFIED,
        )

        // 3. Set color to Green. It should restore the cached 50% opacity.
        colorDataStore.setValue(KEY, Int::class.javaObjectType, Color.GREEN)

        val finalFullColor = settingsSecureStore.getInt(colorKey)!!
        assertThat(CaptionUtils.parseColor(finalFullColor)).isEqualTo(Color.GREEN)
        assertThat(CaptionUtils.parseOpacity(finalFullColor)).isEqualTo(opacity50)
    }

    @Test
    fun setValue_opacityMode_updatesOpacityAndPreservesColor() {
        val opacityDataStore = CaptionTextColorDataStore(context, isColor = false)
        val colorKey = Settings.Secure.ACCESSIBILITY_CAPTIONING_FOREGROUND_COLOR

        // Initial state: Green color, 100% opacity
        val initialColor = Color.GREEN
        settingsSecureStore.setInt(colorKey, initialColor)

        // Set to 25% opacity
        val newOpacity = Color.argb(64, 255, 255, 255)
        opacityDataStore.setValue(KEY, Int::class.javaObjectType, newOpacity)

        val finalFullColor = settingsSecureStore.getInt(colorKey)!!
        assertThat(
                Color.rgb(
                    Color.red(finalFullColor),
                    Color.green(finalFullColor),
                    Color.blue(finalFullColor),
                )
            )
            .isEqualTo(initialColor)
        assertThat(Color.alpha(finalFullColor)).isEqualTo(64)
    }

    @Test
    fun setValue_enablesCaptioning() {
        val dataStore = CaptionTextColorDataStore(context, isColor = true)
        settingsSecureStore.setBoolean(Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED, false)

        dataStore.setValue(KEY, Int::class.javaObjectType, Color.RED)

        assertThat(settingsSecureStore.getBoolean(Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED))
            .isTrue()
    }

    companion object {
        private const val KEY = "any_key"
    }
}
