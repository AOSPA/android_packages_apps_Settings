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

package com.android.settings.display.darkmode

import android.content.Context
import android.os.PowerManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.Flags
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class DarkModeMainSwitchPreferenceTest {
    @get:Rule val mSetFlagsRule = SetFlagsRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val shadowPowerManager = shadowOf(context.getSystemService(PowerManager::class.java))!!
    private val mainSwitchPreference = DarkModeMainSwitchPreference(DarkModeStorage(context), false)

    @Test
    fun key() {
        assertThat(mainSwitchPreference.key).isEqualTo(DarkModeMainSwitchPreference.KEY)
    }

    @Test
    fun isIndexable() {
        assertThat(mainSwitchPreference.indexable).isFalse()
    }

    @Test
    fun getTitle() {
        assertThat(mainSwitchPreference.title).isEqualTo(R.string.dark_theme_main_switch_title)
    }

    @Test
    @EnableFlags(Flags.FLAG_ALLOW_TO_ENTER_DARK_THEME_SETTINGS_WHEN_BATTERY_SAVER)
    fun isEnabled_flagOn_isTrue() {
        shadowPowerManager.setIsPowerSaveMode(true)

        assertThat(mainSwitchPreference.isEnabled(context)).isTrue()
    }

    @Test
    @DisableFlags(Flags.FLAG_ALLOW_TO_ENTER_DARK_THEME_SETTINGS_WHEN_BATTERY_SAVER)
    fun isEnabled_flagOff_PowerSaveFalse_isTrue() {
        shadowPowerManager.setIsPowerSaveMode(false)

        assertThat(mainSwitchPreference.isEnabled(context)).isTrue()
    }

    @Test
    @DisableFlags(Flags.FLAG_ALLOW_TO_ENTER_DARK_THEME_SETTINGS_WHEN_BATTERY_SAVER)
    fun isEnabled_flagOff_PowerSaveTrue_isFalse() {
        shadowPowerManager.setIsPowerSaveMode(true)

        assertThat(mainSwitchPreference.isEnabled(context)).isFalse()
    }
}
