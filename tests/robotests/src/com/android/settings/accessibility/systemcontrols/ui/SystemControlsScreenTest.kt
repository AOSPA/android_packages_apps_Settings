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
package com.android.settings.accessibility.systemcontrols.ui

import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemProperties

// LINT.IfChange
@Config(shadows = [ShadowSystemProperties::class])
class SystemControlsScreenTest : SettingsCatalystTestCase() {

    override val preferenceScreenCreator = SystemControlsScreen()

    override val flagName: String
        get() = Flags.FLAG_CATALYST_MIGRATION_26Q2

    @Test
    fun getSummary_oneHandedSupported_returnCorrectSummary() {
        ShadowSystemProperties.override(SUPPORT_ONE_HANDED_MODE, "true")

        assertThat(preferenceScreenCreator.summary)
            .isEqualTo(R.string.accessibility_system_controls_subtext)
    }

    @Test
    fun getSummary_oneHandedNotSupported_returnCorrectSummary() {
        ShadowSystemProperties.override(SUPPORT_ONE_HANDED_MODE, "false")

        assertThat(preferenceScreenCreator.summary)
            .isEqualTo(R.string.accessibility_system_controls_subtext_one_handed_not_supported)
    }

    companion object {
        private const val SUPPORT_ONE_HANDED_MODE = "ro.support_one_handed_mode"
    }
}
// LINT.ThenChange(SystemControlsFragmentTest.java, ../../SystemControlsPreferenceControllerTest.kt)
