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
import com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode
import com.android.settings.testutils.SettingsStoreRule
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector

/** Test for [MagnificationModeStorage] */
@RunWith(RobolectricTestParameterInjector::class)
class MagnificationModeStorageTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val storage = MagnificationModeStorage(context)

    @Test
    fun contains() {
        assertThat(storage.contains(FullScreenModeSelectorPreference.KEY)).isEqualTo(true)
        assertThat(storage.contains(WindowModeSelectorPreference.KEY)).isEqualTo(true)
        assertThat(storage.contains(AllModeSelectorPreference.KEY)).isEqualTo(true)
    }

    @Test
    @TestParameters(
        customName = "FullScreenMode",
        value =
            [
                "{mode: ${MagnificationMode.FULLSCREEN}, fullScreenModeExpectedValue: true, windowModeExpectedValue: false, allModeExpectedValue: false}"
            ],
    )
    @TestParameters(
        customName = "WindowMode",
        value =
            [
                "{mode: ${MagnificationMode.WINDOW}, fullScreenModeExpectedValue: false, windowModeExpectedValue: true, allModeExpectedValue: false}"
            ],
    )
    @TestParameters(
        customName = "AllMode",
        value =
            [
                "{mode: ${MagnificationMode.ALL}, fullScreenModeExpectedValue: false, windowModeExpectedValue: false, allModeExpectedValue: true}"
            ],
    )
    fun getValue(
        @MagnificationMode mode: Int,
        fullScreenModeExpectedValue: Boolean,
        windowModeExpectedValue: Boolean,
        allModeExpectedValue: Boolean,
    ) {
        storage.settingsStore.setInt(MagnificationModeStorage.SETTINGS_KEY, mode)

        assertThat(storage.getBoolean(FullScreenModeSelectorPreference.KEY))
            .isEqualTo(fullScreenModeExpectedValue)
        assertThat(storage.getBoolean(WindowModeSelectorPreference.KEY))
            .isEqualTo(windowModeExpectedValue)
        assertThat(storage.getBoolean(AllModeSelectorPreference.KEY))
            .isEqualTo(allModeExpectedValue)
    }

    @Test
    @TestParameters(
        customName = "FullScreenMode",
        value =
            [
                "{key: ${FullScreenModeSelectorPreference.KEY}, mode: ${MagnificationMode.FULLSCREEN}}"
            ],
    )
    @TestParameters(
        customName = "WindowMode",
        value = ["{key: ${WindowModeSelectorPreference.KEY}, mode: ${MagnificationMode.WINDOW}}"],
    )
    @TestParameters(
        customName = "AllMode",
        value = ["{key: ${AllModeSelectorPreference.KEY}, mode: ${MagnificationMode.ALL}}"],
    )
    fun setValue(key: String, @MagnificationMode mode: Int) {
        storage.setBoolean(key, true)

        assertThat(storage.settingsStore.getInt(MagnificationModeStorage.SETTINGS_KEY))
            .isEqualTo(mode)
    }
}
