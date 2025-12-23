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

package com.android.settings.accessibility.shortcutssettings.ui

import android.app.settings.SettingsEnums
import com.android.settings.R
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.ShortcutsSettingsFragment
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Test for [ShortcutsSettingsScreen]. */
class ShortcutsSettingsScreenTest : SettingsCatalystTestCase() {
    override val flagName: String
        get() = Flags.FLAG_CATALYST_A11Y_SHORTCUTS_SETTINGS

    override val preferenceScreenCreator = ShortcutsSettingsScreen()

    @Test
    fun verifyKey() {
        assertThat(preferenceScreenCreator.key).isEqualTo(ShortcutsSettingsScreen.KEY)
    }

    @Test
    fun verifyTitleResource() {
        assertThat(preferenceScreenCreator.title)
            .isEqualTo(R.string.accessibility_shortcuts_settings_title)
    }

    @Test
    fun verifySummaryResource() {
        assertThat(preferenceScreenCreator.summary)
            .isEqualTo(R.string.accessibility_shortcuts_settings_subtext)
    }

    @Test
    fun verifyHighlightMenuKeyResource() {
        assertThat(preferenceScreenCreator.highlightMenuKey)
            .isEqualTo(R.string.menu_key_accessibility)
    }

    @Test
    fun verifyFragmentClassIsShortcutsSettingsFragment() {
        assertThat(preferenceScreenCreator.fragmentClass())
            .isEqualTo(ShortcutsSettingsFragment::class.java)
    }

    @Test
    fun indexable_returnsTrue() {
        assertThat(preferenceScreenCreator.indexable).isTrue()
    }

    @Test
    fun purpose_returnsCorrectStringResId() {
        assertThat(preferenceScreenCreator.purpose)
            .isEqualTo(R.string.a11y_shortcuts_settings_screen_purpose)
    }

    @Test
    fun getMetricsCategory_returnsA11yShortcutsSettings() {
        assertThat(preferenceScreenCreator.getMetricsCategory())
            .isEqualTo(SettingsEnums.ACCESSIBILITY_SHORTCUTS_SETTINGS)
    }
}
