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

package com.android.settings.accessibility.timingcontrols.ui

import android.app.settings.SettingsEnums
import com.android.settings.R
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.TapAssistanceFragment
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Test for [TimingControlsScreen]. */
class TimingControlsScreenTest : SettingsCatalystTestCase() {
    override val flagName: String
        get() = Flags.FLAG_CATALYST_TIMING_CONTROLS_SCREEN

    override val preferenceScreenCreator = TimingControlsScreen()

    @Test
    fun verifyTitleResource() {
        assertThat(preferenceScreenCreator.title)
            .isEqualTo(R.string.accessibility_tap_assistance_title)
    }

    @Test
    fun verifySummaryResource() {
        assertThat(preferenceScreenCreator.summary)
            .isEqualTo(R.string.accessibility_tap_assistance_subtext)
    }

    @Test
    fun verifyIconResource() {
        assertThat(preferenceScreenCreator.icon).isEqualTo(R.drawable.ic_tap_assistance)
    }

    @Test
    fun verifyHighlightMenuKeyResource() {
        assertThat(preferenceScreenCreator.highlightMenuKey)
            .isEqualTo(R.string.menu_key_accessibility)
    }

    @Test
    fun verifyKey() {
        assertThat(preferenceScreenCreator.key).isEqualTo(TimingControlsScreen.KEY)
    }

    @Test
    fun getMetricsCategory_returnsA11yTapAssistance() {
        assertThat(preferenceScreenCreator.getMetricsCategory())
            .isEqualTo(SettingsEnums.ACCESSIBILITY_TAP_ASSISTANCE)
    }

    @Test
    fun isIndexable_isTrue() {
        assertThat(preferenceScreenCreator.indexable).isTrue()
    }

    @Test
    fun getFragmentClass_returnsTapAssistanceFragment() {
        assertThat(preferenceScreenCreator.fragmentClass())
            .isEqualTo(TapAssistanceFragment::class.java)
    }
}
