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

package com.android.settings.accessibility.audioadjustment.ui

import android.app.settings.SettingsEnums
import com.android.settings.R
import com.android.settings.accessibility.AudioAdjustmentFragment
import com.android.settings.accessibility.Flags
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Test for [AudioAdjustmentScreen]. */
class AudioAdjustmentScreenTest : SettingsCatalystTestCase() {
    override val flagName: String = Flags.FLAG_CATALYST_AUDIO_ADJUSTMENT_SCREEN
    override val preferenceScreenCreator = AudioAdjustmentScreen()

    override fun migration() {
        // Temporarily disabled until we finished migrating all items on the screen
    }

    @Test
    fun verifyTitleResource() {
        assertThat(preferenceScreenCreator.title)
            .isEqualTo(R.string.accessibility_audio_adjustment_title)
    }

    @Test
    fun verifyHighlightMenuKeyResource() {
        assertThat(preferenceScreenCreator.highlightMenuKey)
            .isEqualTo(R.string.menu_key_accessibility)
    }

    @Test
    fun verifyFragmentClassIsAudioAdjustmentFragment() {
        assertThat(preferenceScreenCreator.fragmentClass())
            .isEqualTo(AudioAdjustmentFragment::class.java)
    }

    @Test
    fun verifyKey() {
        assertThat(preferenceScreenCreator.key).isEqualTo(AudioAdjustmentScreen.KEY)
    }

    @Test
    fun getMetricsCategory_returnsA11yAudioAdjustment() {
        assertThat(preferenceScreenCreator.getMetricsCategory())
            .isEqualTo(SettingsEnums.ACCESSIBILITY_AUDIO_ADJUSTMENT)
    }

    @Test
    fun isIndexable_returnsTrue() {
        assertThat(preferenceScreenCreator.indexable).isTrue()
    }

    @Test
    fun verifySummaryResource() {
        assertThat(preferenceScreenCreator.summary)
            .isEqualTo(R.string.accessibility_audio_adjustment_subtext)
    }
}
