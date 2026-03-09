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

import android.content.Context
import android.hardware.display.ColorDisplayManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.server.display.feature.flags.Flags.FLAG_DISPLAY_SETTINGS_API_SCREEN_SUPPORT
import com.android.settings.display.ColorModeApiScreen.Companion.COLOR_MODE_KEY
import com.android.settings.flags.Flags.FLAG_CATALYST_MIGRATION_26Q2
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.FailedPreconditionException
import com.android.settingslib.testutils.shadow.ShadowColorDisplayManager
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowColorDisplayManager::class, SettingsShadowResources::class])
class ColorModeApiScreenTest {
    private val tester = ApiTester(ColorModeApiScreen())
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val CONFIG_AVAILABLE_COLOR_MODES_RES_ID =
        com.android.internal.R.array.config_availableColorModes

    @get:Rule val setFlagsRule = SetFlagsRule()

    private lateinit var shadowColorDisplayManager: ShadowColorDisplayManager
    private var originalAccessibilityTransformsEnabledValue: Int = 0

    @Before
    fun setUp() {
        originalAccessibilityTransformsEnabledValue =
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED,
                0,
            )
        SettingsShadowResources.overrideResource(
            CONFIG_AVAILABLE_COLOR_MODES_RES_ID,
            intArrayOf(
                ColorDisplayManager.COLOR_MODE_NATURAL,
                ColorDisplayManager.COLOR_MODE_BOOSTED,
                ColorDisplayManager.COLOR_MODE_SATURATED,
                ColorDisplayManager.COLOR_MODE_AUTOMATIC,
            ),
        )
        shadowColorDisplayManager =
            Shadow.extract(context.getSystemService(ColorDisplayManager::class.java))
        shadowColorDisplayManager.setDeviceColorManaged(true)
        setAccessibilityTransformsEnabled(false)
    }

    @Test
    fun getLaunchIntent_isNotNull() {
        assertThat(tester.getLaunchIntent()).isNotNull()
    }

    @Test
    fun getLaunchIntent_accessibilityTransformsEnabled_disallowed() {
        shadowColorDisplayManager.setDeviceColorManaged(true)
        setAccessibilityTransformsEnabled(true)

        assertThrows(FailedPreconditionException::class.java) { tester.getLaunchIntent() }
    }

    @Test
    fun getLaunchIntent_deviceColorManaged_disallowed() {
        shadowColorDisplayManager.setDeviceColorManaged(false)
        setAccessibilityTransformsEnabled(false)

        assertThrows(FailedPreconditionException::class.java) { tester.getLaunchIntent() }
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
    fun getSetting() {
        shadowColorDisplayManager.setColorMode(ColorDisplayManager.COLOR_MODE_BOOSTED)
        shadowColorDisplayManager.setDeviceColorManaged(true)
        setAccessibilityTransformsEnabled(false)

        assertThat(tester.get<Int>(COLOR_MODE_KEY))
            .isEqualTo(ColorDisplayManager.COLOR_MODE_BOOSTED)
    }

    @Test
    fun getSetting_notSet_returnsDefault() {
        assertThat(tester.get<Int>(COLOR_MODE_KEY))
            .isEqualTo(ColorDisplayManager.COLOR_MODE_NATURAL)
    }

    @Test
    fun setSetting() {
        shadowColorDisplayManager.setColorMode(ColorDisplayManager.COLOR_MODE_BOOSTED)
        shadowColorDisplayManager.setDeviceColorManaged(true)
        setAccessibilityTransformsEnabled(false)

        tester.set(COLOR_MODE_KEY, ColorDisplayManager.COLOR_MODE_SATURATED)

        assertThat(shadowColorDisplayManager.colorMode)
            .isEqualTo(ColorDisplayManager.COLOR_MODE_SATURATED)
    }

    @Test
    fun setSetting_accessibilityTransformsEnabled_disallowed() {
        shadowColorDisplayManager.setDeviceColorManaged(true)
        setAccessibilityTransformsEnabled(true)

        assertThrows(FailedPreconditionException::class.java) {
            tester.set(COLOR_MODE_KEY, ColorDisplayManager.COLOR_MODE_SATURATED)
        }
    }

    @Test
    fun setSetting_deviceColorManaged_disallowed() {
        shadowColorDisplayManager.setDeviceColorManaged(false)
        setAccessibilityTransformsEnabled(false)

        assertThrows(FailedPreconditionException::class.java) {
            tester.set(COLOR_MODE_KEY, ColorDisplayManager.COLOR_MODE_SATURATED)
        }
    }

    fun setAccessibilityTransformsEnabled(enabled: Boolean) {
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED,
            if (enabled) 1 else 0,
        )
    }
}
// LINT.ThenChange(ColorModePreferenceFragmentTest.java, ColorModeScreenTest.kt)
