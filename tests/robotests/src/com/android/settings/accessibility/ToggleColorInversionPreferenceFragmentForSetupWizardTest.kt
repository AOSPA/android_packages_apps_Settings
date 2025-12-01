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
package com.android.settings.accessibility

import android.app.settings.SettingsEnums
import android.content.ComponentName
import android.os.Bundle
import com.android.internal.accessibility.AccessibilityShortcutController.COLOR_INVERSION_COMPONENT_NAME
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests for [ToggleColorInversionPreferenceFragmentForSetupWizard]. */
@RunWith(RobolectricTestRunner::class)
class ToggleColorInversionPreferenceFragmentForSetupWizardTest :
    BaseShortcutInteractionsInSuwTestCases<
        ToggleColorInversionPreferenceFragmentForSetupWizard>() {

    @Test
    fun getMetricsCategory_returnsCorrectCategory() {
        assertThat(
            ToggleColorInversionPreferenceFragmentForSetupWizard().metricsCategory
        ).isEqualTo(SettingsEnums.SUW_ACCESSIBILITY_TOGGLE_SCREEN_COLOR_INVERSION)
    }

    @Test
    fun getHelpResource_shouldNotHaveHelpResource() {
        assertThat(ToggleColorInversionPreferenceFragmentForSetupWizard().getHelpResource())
            .isEqualTo(0)
    }

    override fun launchFragment(): ToggleColorInversionPreferenceFragmentForSetupWizard =
        super.launchFragment()


    override fun getFeatureComponent(): ComponentName = COLOR_INVERSION_COMPONENT_NAME

    override fun getFragmentArgs(): Bundle? = null

    override fun getSetupWizardTitle(): String =
        context.getString(R.string.accessibility_display_inversion_preference_title)

    override fun getSetupWizardDescription(): String =
        context.getString(R.string.accessibility_display_inversion_preference_intro_text)

    override fun getFragmentClazz(): Class<ToggleColorInversionPreferenceFragmentForSetupWizard> =
        ToggleColorInversionPreferenceFragmentForSetupWizard::class.java

    override fun getShortcutToggle(): ShortcutPreference? =
        fragment?.findPreference(SHORTCUT_PREF_KEY)

    companion object {
        private const val SHORTCUT_PREF_KEY = "color_inversion_shortcut_key"
    }
}
