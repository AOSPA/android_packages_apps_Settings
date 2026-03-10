/*
 * Copyright (C) 2026 The Android Open Source Project
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
package com.android.settings.display

import android.Manifest.permission.WRITE_SETTINGS
import android.app.Application
import android.content.Context
import android.hardware.display.DisplayManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.view.Display
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.server.display.feature.flags.Flags.FLAG_DISPLAY_SETTINGS_API_SCREEN_SUPPORT
import com.android.settings.R
import com.android.settings.display.DisplayApiScreen.Companion.BRIGHTNESS_LEVEL_KEY
import com.android.settings.flags.Flags.FLAG_CATALYST_MIGRATION_26Q2
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settings.testutils.shadow.ShadowDisplayManager
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.HardwareUnsupportedException
import com.android.settings.testutils2.MissingPermissionException
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
@Config(
    shadows =
        [ShadowApplication::class, ShadowDisplayManager::class, SettingsShadowResources::class]
)
class DisplayApiScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val tester = ApiTester(DisplayApiScreen())
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val shadowApplication: ShadowApplication = shadowOf(context as Application)
    private val displayManager = context.getSystemService(DisplayManager::class.java)

    @Before
    fun setUp() {
        setShowTopLevelDisplaySettings(true)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2, FLAG_DISPLAY_SETTINGS_API_SCREEN_SUPPORT)
    fun getScreen_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(FLAG_CATALYST_MIGRATION_26Q2, FLAG_DISPLAY_SETTINGS_API_SCREEN_SUPPORT)
    fun getScreen_flagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    fun getLaunchIntent_isNotNull() {
        assertThat(tester.getLaunchIntent()).isNotNull()
    }

    @Test
    fun getLaunchIntent_topLevelDisplayNotAvailable_fails() {
        setShowTopLevelDisplaySettings(false)
        val failure = assertFailsWith<HardwareUnsupportedException> { tester.getLaunchIntent() }
        assertThat(failure.reason).contains("does not support")
    }

    @Test
    fun getBrightnessLevel_returnsSettingsValue() {
        setDefaultDisplayBrightnessPercentage(0.0f)
        assertThat(tester.get<Int>(BRIGHTNESS_LEVEL_KEY)).isEqualTo(0)

        setDefaultDisplayBrightnessPercentage(25.36f)
        assertThat(tester.get<Int>(BRIGHTNESS_LEVEL_KEY)).isEqualTo(25)

        setDefaultDisplayBrightnessPercentage(100.0f)
        assertThat(tester.get<Int>(BRIGHTNESS_LEVEL_KEY)).isEqualTo(100)
    }

    @Test
    fun setBrightnessLevel_updatesSettings() {
        shadowApplication.grantPermissions(WRITE_SETTINGS)
        tester.set(BRIGHTNESS_LEVEL_KEY, 0)
        assertThat(getDefaultDisplayBrightnessPercentage()).isEqualTo(0.0f)

        tester.set(BRIGHTNESS_LEVEL_KEY, 50)
        assertThat(getDefaultDisplayBrightnessPercentage()).isEqualTo(50.0f)

        tester.set(BRIGHTNESS_LEVEL_KEY, 100)
        assertThat(getDefaultDisplayBrightnessPercentage()).isEqualTo(100.0f)
    }

    @Test
    fun setBrightnessLevel_missingPermission_fails() {
        shadowApplication.denyPermissions(WRITE_SETTINGS)
        setDefaultDisplayBrightnessPercentage(0.0f)
        val failure =
            assertFailsWith<MissingPermissionException> { tester.set(BRIGHTNESS_LEVEL_KEY, 100) }
        assertThat(getDefaultDisplayBrightnessPercentage()).isEqualTo(0.0f)
    }

    private fun setShowTopLevelDisplaySettings(value: Boolean) {
        SettingsShadowResources.overrideResource(R.bool.config_show_top_level_display, value)
    }

    private fun getDefaultDisplayBrightnessPercentage(): Float =
        displayManager.getBrightness(
            Display.DEFAULT_DISPLAY,
            DisplayManager.BRIGHTNESS_UNIT_PERCENTAGE,
        )

    private fun setDefaultDisplayBrightnessPercentage(value: Float) {
        displayManager.setBrightness(
            Display.DEFAULT_DISPLAY,
            value,
            DisplayManager.BRIGHTNESS_UNIT_PERCENTAGE,
        )
    }
}
// LINT.ThenChange(com.android.settings.DisplaySettingsTest.java, DisplayScreen.kt,
// BrightnessLevelPreferenceControllerTest.java, BrightnessLevelPreferenceTest.kt)
