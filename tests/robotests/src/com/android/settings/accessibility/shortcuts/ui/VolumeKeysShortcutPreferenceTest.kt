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

package com.android.settings.accessibility.shortcuts.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME
import com.android.settings.R
import com.android.settings.accessibility.shortcuts.ShortcutOptionPreference as ShortcutOptionWidget
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class VolumeKeysShortcutPreferenceTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()

    private lateinit var appContext: Context
    private lateinit var preference: VolumeKeysShortcutPreference

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()
        preference =
            VolumeKeysShortcutPreference(
                context = appContext,
                targets = setOf(MAGNIFICATION_CONTROLLER_NAME),
            )
    }

    @Test
    fun key_returnsCorrectValue() {
        assertThat(preference.key).isEqualTo("shortcut_volume_keys_pref")
    }

    @Test
    fun title_returnsCorrectStringResource() {
        assertThat(preference.title)
            .isEqualTo(R.string.accessibility_shortcut_edit_dialog_title_hardware)
    }

    @Test
    fun bind_setsCorrectImageResource() {
        val widget = preference.createAndBindWidget<ShortcutOptionWidget>(appContext)
        val imageResId = ReflectionHelpers.getField<Int>(widget, "mIntroImageResId")

        assertThat(imageResId).isEqualTo(R.drawable.accessibility_shortcut_type_volume_keys)
    }
}
