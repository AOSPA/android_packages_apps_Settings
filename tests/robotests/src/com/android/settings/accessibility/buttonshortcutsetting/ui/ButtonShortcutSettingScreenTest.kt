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

package com.android.settings.accessibility.buttonshortcutsetting.ui

import android.app.settings.SettingsEnums
import android.view.accessibility.AccessibilityManager
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.SOFTWARE
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityButtonFragment
import com.android.settings.accessibility.Flags
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow

/** Test for [ButtonShortcutSettingScreen]. */
@Config(shadows = [SettingsShadowResources::class, ShadowAccessibilityManager::class])
class ButtonShortcutSettingScreenTest : SettingsCatalystTestCase() {
    override val flagName: String
        get() = Flags.FLAG_CATALYST_A11Y_BUTTON_SHORTCUT_SETTING

    override val preferenceScreenCreator = ButtonShortcutSettingScreen()
    private val shadowA11yManager: ShadowAccessibilityManager =
        Shadow.extract(appContext.getSystemService(AccessibilityManager::class.java))

    override fun migration() {
        shadowA11yManager.setAccessibilityShortcutTargets(SOFTWARE, listOf("Foo"))
        super.migration()
    }

    @Test
    fun verifyTitleResource() {
        assertThat(preferenceScreenCreator.title).isEqualTo(R.string.accessibility_button_title)
    }

    @Test
    fun verifyHighlightMenuKeyResource() {
        assertThat(preferenceScreenCreator.highlightMenuKey)
            .isEqualTo(R.string.menu_key_accessibility)
    }

    @Test
    fun verifyFragmentClassIsAccessibilityButtonFragment() {
        assertThat(preferenceScreenCreator.fragmentClass())
            .isEqualTo(AccessibilityButtonFragment::class.java)
    }

    @Test
    fun verifyKey() {
        assertThat(preferenceScreenCreator.key).isEqualTo("accessibility_button_preference")
    }

    @Test
    fun getMetricsCategory_returnsA11yButtonSettingsCategory() {
        assertThat(preferenceScreenCreator.metricsCategory)
            .isEqualTo(SettingsEnums.ACCESSIBILITY_BUTTON_SETTINGS)
    }

    @Test
    fun isEnabled_hasButtonShortcutTargets_returnTrue() {
        shadowA11yManager.setAccessibilityShortcutTargets(SOFTWARE, listOf("Foo"))

        assertThat(preferenceScreenCreator.isEnabled(appContext)).isEqualTo(true)
    }

    @Test
    fun isEnabled_noButtonShortcutTargetsAssigned_returnFalse() {
        shadowA11yManager.setAccessibilityShortcutTargets(SOFTWARE, emptyList())

        assertThat(preferenceScreenCreator.isEnabled(appContext)).isEqualTo(false)
    }

    @Test
    fun isIndexable_hasButtonShortcutTargets_returnTrue() {
        shadowA11yManager.setAccessibilityShortcutTargets(SOFTWARE, listOf("Foo"))

        assertThat(preferenceScreenCreator.isIndexable(appContext)).isEqualTo(true)
    }

    @Test
    fun isIndexable_noButtonShortcutTargetsAssigned_returnFalse() {
        shadowA11yManager.setAccessibilityShortcutTargets(SOFTWARE, emptyList())

        assertThat(preferenceScreenCreator.isIndexable(appContext)).isEqualTo(false)
    }

    @Test
    fun getSummary_hasButtonShortcutTargets_returnsEmpty() {
        shadowA11yManager.setAccessibilityShortcutTargets(SOFTWARE, listOf("Foo"))

        assertThat(preferenceScreenCreator.getSummary(appContext)).isEqualTo("")
    }

    @Test
    fun getSummary_noButtonShortcutTargetsAssigned_returnExpectedSummary() {
        shadowA11yManager.setAccessibilityShortcutTargets(SOFTWARE, emptyList())

        assertThat(preferenceScreenCreator.getSummary(appContext).toString())
            .isEqualTo(
                appContext.getString(
                    R.string.a11y_button_shortcut_unassigned_setting_unavailable_summary
                )
            )
    }
}
