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

import android.app.Application
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
class TripleTapShortcutPreferenceTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()

    private val appContext: Application = ApplicationProvider.getApplicationContext()
    private lateinit var preference: TripleTapShortcutPreference

    @Before
    fun setUp() {
        preference = TripleTapShortcutPreference(appContext, setOf(MAGNIFICATION_CONTROLLER_NAME))
    }

    @Test
    fun key_returnsCorrectValue() {
        assertThat(preference.key).isEqualTo("shortcut_triple_tap_pref")
    }

    @Test
    fun title_returnsCorrectValue() {
        assertThat(preference.title)
            .isEqualTo(R.string.accessibility_shortcut_edit_screen_title_triple_tap)
    }

    @Test
    fun getSummary_returnsCorrectFormattedString() {
        val expectedSummary =
            appContext.getString(R.string.accessibility_shortcut_edit_screen_summary_triple_tap, 3)

        assertThat(preference.getSummary(appContext)).isEqualTo(expectedSummary)
    }

    @Test
    fun bind_setsIntroImage() {
        val widget = preference.createAndBindWidget<ShortcutOptionWidget>(appContext)

        val imageRawResId: Int = ReflectionHelpers.getField(widget, "mIntroImageRawResId")

        assertThat(imageRawResId).isEqualTo(R.raw.accessibility_shortcut_type_tripletap)
    }

    @Test
    fun isAvailable_whenNoTargets_returnsFalse() {
        preference = TripleTapShortcutPreference(appContext, emptySet())

        assertThat(preference.isAvailable(appContext)).isFalse()
    }

    @Test
    fun isAvailable_whenWrongTarget_returnsFalse() {
        preference = TripleTapShortcutPreference(appContext, setOf("some.other.target"))

        assertThat(preference.isAvailable(appContext)).isFalse()
    }

    @Test
    fun isAvailable_whenMultipleTargets_returnsFalse() {
        preference =
            TripleTapShortcutPreference(
                appContext,
                setOf(MAGNIFICATION_CONTROLLER_NAME, "some.other.target"),
            )

        assertThat(preference.isAvailable(appContext)).isFalse()
    }

    @Test
    fun isAvailable_whenTargetIsMagnification_returnsTrue() {
        assertThat(preference.isAvailable(appContext)).isTrue()
    }
}
