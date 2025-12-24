/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.accessibility.buttonshortcutsetting.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.datastore.SettingsSecureStore
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Test for [FloatingMenuTransparencyDataStore] */
@RunWith(AndroidJUnit4::class)
class FloatingMenuTransparencyDataStoreTest {

    @get:Rule val settingsStoreRule = SettingsStoreRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dataStore = FloatingMenuTransparencyDataStore(context)
    private val settingsSecureStore = SettingsSecureStore.get(context)

    @Test
    fun getDefaultValue_returnsDefaultTransparencyProgress() {
        val defaultValue =
            dataStore.getDefaultValue(
                FloatingMenuTransparencyDataStore.SETTING_KEY,
                Int::class.java,
            )

        assertThat(defaultValue)
            .isEqualTo(FloatingMenuTransparencyDataStore.DEFAULT_TRANSPARENCY_PROGRESS)
    }

    @Test
    fun getValue_noValueStored_returnsDefaultTransparencyProgress() {
        // Ensure no value is stored
        settingsSecureStore.setFloat(FloatingMenuTransparencyDataStore.SETTING_KEY, null)

        val transparencyProgress =
            dataStore.getValue(FloatingMenuTransparencyDataStore.SETTING_KEY, Int::class.java)
        assertThat(transparencyProgress)
            .isEqualTo(FloatingMenuTransparencyDataStore.DEFAULT_TRANSPARENCY_PROGRESS)
    }

    @Test
    fun getValue_valueStored_returnsCorrectTransparencyProgress() {
        val opacity = 0.25f // Corresponds to 75% transparency
        settingsSecureStore.setFloat(FloatingMenuTransparencyDataStore.SETTING_KEY, opacity)

        val transparencyProgress =
            dataStore.getValue(FloatingMenuTransparencyDataStore.SETTING_KEY, Int::class.java)

        // transparency = MAX_TRANSPARENCY - opacity = 1 - 0.25 = 0.75
        // transparencyProgress = (transparency * PRECISION).toInt() = (0.75 * 100).toInt() = 75
        assertThat(transparencyProgress).isEqualTo(75)
    }

    @Test
    fun setValue_validTransparencyProgress_storesCorrectOpacity() {
        val transparencyProgress = 60 // Corresponds to 40% opacity
        dataStore.setValue(
            FloatingMenuTransparencyDataStore.SETTING_KEY,
            Int::class.java,
            transparencyProgress,
        )

        val storedOpacity =
            settingsSecureStore.getFloat(FloatingMenuTransparencyDataStore.SETTING_KEY)

        // transparency = transparencyProgress / PRECISION = 60 / 100 = 0.6
        // opacity = MAX_TRANSPARENCY - transparency = 1 - 0.6 = 0.4
        assertThat(storedOpacity).isWithin(tolerance).of(0.4f)
    }

    @Test
    fun setValue_minTransparencyProgress_storesMaxOpacity() {
        dataStore.setValue(
            FloatingMenuTransparencyDataStore.SETTING_KEY,
            Int::class.java,
            FloatingMenuTransparencyDataStore.MIN_TRANSPARENCY_PROGRESS,
        )

        val storedOpacity =
            settingsSecureStore.getFloat(FloatingMenuTransparencyDataStore.SETTING_KEY)

        // transparency = 0 / 100 = 0
        // opacity = 1 - 0 = 1
        assertThat(storedOpacity).isWithin(tolerance).of(1.0f)
    }

    @Test
    fun setValue_maxTransparencyProgress_storesMinOpacity() {
        dataStore.setValue(
            FloatingMenuTransparencyDataStore.SETTING_KEY,
            Int::class.java,
            FloatingMenuTransparencyDataStore.MAX_TRANSPARENCY_PROGRESS,
        )

        val storedOpacity =
            settingsSecureStore.getFloat(FloatingMenuTransparencyDataStore.SETTING_KEY)

        // transparency = 90 / 100 = 0.9
        // opacity = 1 - 0.9 = 0.1
        assertThat(storedOpacity).isWithin(tolerance).of(0.1f)
    }

    @Test
    fun setValue_transparencyProgressBelowMin_doesNotStoreValue() {
        val initialOpacity = 0.5f
        settingsSecureStore.setFloat(FloatingMenuTransparencyDataStore.SETTING_KEY, initialOpacity)

        dataStore.setValue(
            FloatingMenuTransparencyDataStore.SETTING_KEY,
            Int::class.java,
            -1, // Below min
        )

        val storedOpacity =
            settingsSecureStore.getFloat(FloatingMenuTransparencyDataStore.SETTING_KEY)
        assertThat(storedOpacity).isWithin(tolerance).of(initialOpacity)
    }

    @Test
    fun setValue_transparencyProgressAboveMax_doesNotStoreValue() {
        val initialOpacity = 0.5f
        settingsSecureStore.setFloat(FloatingMenuTransparencyDataStore.SETTING_KEY, initialOpacity)

        dataStore.setValue(
            FloatingMenuTransparencyDataStore.SETTING_KEY,
            Int::class.java,
            100, // Above max
        )

        val storedOpacity =
            settingsSecureStore.getFloat(FloatingMenuTransparencyDataStore.SETTING_KEY)
        assertThat(storedOpacity).isWithin(tolerance).of(initialOpacity)
    }

    @Test
    fun setValue_unsupportedType_doesNotStoreValue() {
        val initialOpacity = 0.5f
        settingsSecureStore.setFloat(FloatingMenuTransparencyDataStore.SETTING_KEY, initialOpacity)

        dataStore.setValue(
            FloatingMenuTransparencyDataStore.SETTING_KEY,
            String::class.java,
            "test",
        )

        val storedOpacity =
            settingsSecureStore.getFloat(FloatingMenuTransparencyDataStore.SETTING_KEY)
        assertThat(storedOpacity).isWithin(tolerance).of(initialOpacity)
    }

    companion object {
        private const val tolerance = 1e-5f
    }
}
