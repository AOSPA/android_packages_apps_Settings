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

package com.android.settings.accessibility.screenmagnification.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode
import com.android.settings.accessibility.shared.ui.SelectorWithImagePreference
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MagnificationModeSelectorPreferenceTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val storage = MagnificationModeStorage(context)
    private val fullScreenModeSelectorPreference = FullScreenModeSelectorPreference(storage)
    private val windowModeSelectorPreference = WindowModeSelectorPreference(storage)
    private val allModeSelectorPreference = AllModeSelectorPreference(storage)

    @Test
    fun key() {
        assertThat(fullScreenModeSelectorPreference.key).isEqualTo("full_screen_mode_preference")
        assertThat(windowModeSelectorPreference.key).isEqualTo("window_mode_preference")
        assertThat(allModeSelectorPreference.key).isEqualTo("all_mode_preference")
    }

    @Test
    fun title() {
        assertThat(fullScreenModeSelectorPreference.title)
            .isEqualTo(R.string.accessibility_magnification_mode_dialog_option_full_screen)
        assertThat(windowModeSelectorPreference.title)
            .isEqualTo(R.string.accessibility_magnification_mode_dialog_option_window)
        assertThat(allModeSelectorPreference.title)
            .isEqualTo(R.string.accessibility_magnification_mode_dialog_option_switch)
    }

    @Test
    fun summary() {
        assertThat(allModeSelectorPreference.summary)
            .isEqualTo(R.string.accessibility_magnification_area_settings_mode_switch_summary)
    }

    @Test
    fun isChecked_fullScreenModePreference() {
        val storage = getStorage() as MagnificationModeStorage
        storage.setBoolean(fullScreenModeSelectorPreference.key, true)
        assertThat(storage.settingsStore.getInt(MagnificationModeStorage.SETTINGS_KEY))
            .isEqualTo(MagnificationMode.FULLSCREEN)

        val fullScreenModeWidget: SelectorWithImagePreference =
            fullScreenModeSelectorPreference.createAndBindWidget(context)
        val windowModeWidget: SelectorWithImagePreference =
            windowModeSelectorPreference.createAndBindWidget(context)
        val allModeWidget: SelectorWithImagePreference =
            allModeSelectorPreference.createAndBindWidget(context)
        assertThat(fullScreenModeWidget.isChecked).isEqualTo(true)
        assertThat(windowModeWidget.isChecked).isEqualTo(false)
        assertThat(allModeWidget.isChecked).isEqualTo(false)
    }

    @Test
    fun isChecked_windowModePreference() {
        val storage = getStorage() as MagnificationModeStorage
        storage.setBoolean(windowModeSelectorPreference.key, true)
        assertThat(storage.settingsStore.getInt(MagnificationModeStorage.SETTINGS_KEY))
            .isEqualTo(MagnificationMode.WINDOW)

        val fullScreenModeWidget: SelectorWithImagePreference =
            fullScreenModeSelectorPreference.createAndBindWidget(context)
        val windowModeWidget: SelectorWithImagePreference =
            windowModeSelectorPreference.createAndBindWidget(context)
        val allModeWidget: SelectorWithImagePreference =
            allModeSelectorPreference.createAndBindWidget(context)
        assertThat(fullScreenModeWidget.isChecked).isEqualTo(false)
        assertThat(windowModeWidget.isChecked).isEqualTo(true)
        assertThat(allModeWidget.isChecked).isEqualTo(false)
    }

    @Test
    fun isChecked_allModePreference() {
        val storage = getStorage() as MagnificationModeStorage
        storage.setBoolean(allModeSelectorPreference.key, true)
        assertThat(storage.settingsStore.getInt(MagnificationModeStorage.SETTINGS_KEY))
            .isEqualTo(MagnificationMode.ALL)

        val fullScreenModeWidget: SelectorWithImagePreference =
            fullScreenModeSelectorPreference.createAndBindWidget(context)
        val windowModeWidget: SelectorWithImagePreference =
            windowModeSelectorPreference.createAndBindWidget(context)
        val allModeWidget: SelectorWithImagePreference =
            allModeSelectorPreference.createAndBindWidget(context)
        assertThat(fullScreenModeWidget.isChecked).isEqualTo(false)
        assertThat(windowModeWidget.isChecked).isEqualTo(false)
        assertThat(allModeWidget.isChecked).isEqualTo(true)
    }

    @Test
    fun performClick_fullScreenModePreference() {
        val storage = getStorage() as MagnificationModeStorage
        storage.setBoolean(windowModeSelectorPreference.key, true)
        val widget: SelectorWithImagePreference =
            fullScreenModeSelectorPreference.createAndBindWidget(context)
        assertThat(widget.isChecked).isEqualTo(false)
        assertThat(storage.settingsStore.getInt(MagnificationModeStorage.SETTINGS_KEY))
            .isEqualTo(MagnificationMode.WINDOW)

        widget.performClick()

        assertThat(widget.isChecked).isEqualTo(true)
        assertThat(storage.getBoolean(fullScreenModeSelectorPreference.key)).isEqualTo(true)
        assertThat(storage.settingsStore.getInt(MagnificationModeStorage.SETTINGS_KEY))
            .isEqualTo(MagnificationMode.FULLSCREEN)
    }

    @Test
    fun performClick_windowModePreference() {
        val storage = getStorage() as MagnificationModeStorage
        storage.setBoolean(fullScreenModeSelectorPreference.key, true)
        val widget: SelectorWithImagePreference =
            windowModeSelectorPreference.createAndBindWidget(context)
        assertThat(widget.isChecked).isEqualTo(false)
        assertThat(storage.settingsStore.getInt(MagnificationModeStorage.SETTINGS_KEY))
            .isEqualTo(MagnificationMode.FULLSCREEN)

        widget.performClick()

        assertThat(widget.isChecked).isEqualTo(true)
        assertThat(storage.getBoolean(windowModeSelectorPreference.key)).isEqualTo(true)
        assertThat(storage.settingsStore.getInt(MagnificationModeStorage.SETTINGS_KEY))
            .isEqualTo(MagnificationMode.WINDOW)
    }

    @Test
    fun performClick_allModePreference() {
        val storage = getStorage() as MagnificationModeStorage
        storage.setBoolean(fullScreenModeSelectorPreference.key, true)
        val widget: SelectorWithImagePreference =
            allModeSelectorPreference.createAndBindWidget(context)
        assertThat(widget.isChecked).isEqualTo(false)
        assertThat(storage.settingsStore.getInt(MagnificationModeStorage.SETTINGS_KEY))
            .isEqualTo(MagnificationMode.FULLSCREEN)

        widget.performClick()

        assertThat(widget.isChecked).isEqualTo(true)
        assertThat(storage.getBoolean(allModeSelectorPreference.key)).isEqualTo(true)
        assertThat(storage.settingsStore.getInt(MagnificationModeStorage.SETTINGS_KEY))
            .isEqualTo(MagnificationMode.ALL)
    }

    private fun getStorage(): KeyValueStore = fullScreenModeSelectorPreference.storage(context)
}
