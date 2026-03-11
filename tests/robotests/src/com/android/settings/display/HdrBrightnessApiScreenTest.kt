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

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.app.Application
import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import android.view.Display
import android.view.Display.HdrCapabilities
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.server.display.feature.flags.Flags.FLAG_DISPLAY_SETTINGS_API_SCREEN_SUPPORT
import com.android.settings.display.HdrBrightnessApiScreen.Companion.HDR_BRIGHTNESS_BOOST_LEVEL_KEY
import com.android.settings.display.HdrBrightnessApiScreen.Companion.HDR_BRIGHTNESS_ENABLED_KEY
import com.android.settings.display.HdrBrightnessApiScreen.Companion.OFF
import com.android.settings.display.HdrBrightnessApiScreen.Companion.ON
import com.android.settings.flags.Flags.FLAG_CATALYST_MIGRATION_26Q2
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.HardwareUnsupportedException
import com.android.settings.testutils2.InvalidPreferenceException
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
import org.robolectric.shadows.ShadowDisplay

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowApplication::class, ShadowDisplay::class])
class HdrBrightnessApiScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val TOLERANCE = 1e-5f

    private val tester = ApiTester(HdrBrightnessApiScreen())
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val shadowApplication: ShadowApplication = shadowOf(context as Application)
    private val shadowDisplay: ShadowDisplay = shadowOf(ShadowDisplay.getDefaultDisplay())

    @Before
    fun setUp() {
        // Change the default display to have HDR support and HDR-SDR ratio available.
        shadowDisplay.setDisplayHdrCapabilities(
            /* displayId= */ Display.DEFAULT_DISPLAY,
            /* maxLuminance= */ 1000f,
            /* maxAverageLuminance= */ 1000f,
            /* minLuminance= */ 0f,
            /* supportedHdrTypes= */ HdrCapabilities.HDR_TYPE_HDR10,
        )
        shadowDisplay.setHdrSdrRatio(5.0f)
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
    fun getLaunchIntent_hardwareUnsupported_fails() {
        disableHdrSupport()
        val failure = assertFailsWith<HardwareUnsupportedException> { tester.getLaunchIntent() }
        assertThat(failure.reason).contains("not supported")
    }

    @Test
    fun getHdrBrightnessEnabled_returnsSettingsValue() {
        // Default value is ON.
        assertThat(tester.get<Boolean>(HDR_BRIGHTNESS_ENABLED_KEY)).isTrue()

        setHdrBrightnessEnabledInSettingsProvider(OFF)
        assertThat(tester.get<Boolean>(HDR_BRIGHTNESS_ENABLED_KEY)).isFalse()

        setHdrBrightnessEnabledInSettingsProvider(ON)
        assertThat(tester.get<Boolean>(HDR_BRIGHTNESS_ENABLED_KEY)).isTrue()
    }

    @Test
    fun getHdrBrightnessEnabled_hardwareUnsupported_fails() {
        disableHdrSupport()
        val failure =
            assertFailsWith<HardwareUnsupportedException> {
                tester.get<Boolean>(HDR_BRIGHTNESS_ENABLED_KEY)
            }
        assertThat(failure.reason).contains("not supported")
    }

    @Test
    fun setHdrBrightnessEnabled_updatesSettings() {
        shadowApplication.grantPermissions(WRITE_SECURE_SETTINGS)
        tester.set(HDR_BRIGHTNESS_ENABLED_KEY, false)
        assertThat(getHdrBrightnessEnabledSettingsValue()).isEqualTo(OFF)

        tester.set(HDR_BRIGHTNESS_ENABLED_KEY, true)
        assertThat(getHdrBrightnessEnabledSettingsValue()).isEqualTo(ON)
    }

    @Test
    fun setHdrBrightnessEnabled_hardwareUnsupported_fails() {
        shadowApplication.grantPermissions(WRITE_SECURE_SETTINGS)
        disableHdrSupport()
        setHdrBrightnessEnabledInSettingsProvider(ON)
        val failure =
            assertFailsWith<HardwareUnsupportedException> {
                tester.set(HDR_BRIGHTNESS_ENABLED_KEY, false)
            }
        assertThat(failure.reason).contains("not supported")
        assertThat(getHdrBrightnessEnabledSettingsValue()).isEqualTo(ON)
    }

    @Test
    fun setHdrBrightnessEnabled_missingPermission_fails() {
        shadowApplication.denyPermissions(WRITE_SECURE_SETTINGS)
        setHdrBrightnessEnabledInSettingsProvider(ON)
        val failure =
            assertFailsWith<MissingPermissionException> {
                tester.set(HDR_BRIGHTNESS_ENABLED_KEY, false)
            }
        assertThat(getHdrBrightnessEnabledSettingsValue()).isEqualTo(ON)
    }

    @Test
    fun getHdrBrightnessBoostLevel_returnsSettingsValue() {
        setHdrBrightnessEnabledInSettingsProvider(ON)
        // Default value is 100%.
        assertThat(tester.get<Int>(HDR_BRIGHTNESS_BOOST_LEVEL_KEY)).isEqualTo(100)

        setHdrBrightnessBoostLevelInSettingsProvider(0.0f)
        assertThat(tester.get<Int>(HDR_BRIGHTNESS_BOOST_LEVEL_KEY)).isEqualTo(0)

        setHdrBrightnessBoostLevelInSettingsProvider(0.521f)
        assertThat(tester.get<Int>(HDR_BRIGHTNESS_BOOST_LEVEL_KEY)).isEqualTo(52)
    }

    @Test
    fun getHdrBrightnessBoostLevel_hardwareUnsupported_fails() {
        setHdrBrightnessEnabledInSettingsProvider(ON)
        disableHdrSupport()
        val failure =
            assertFailsWith<HardwareUnsupportedException> {
                tester.get<Int>(HDR_BRIGHTNESS_BOOST_LEVEL_KEY)
            }
        assertThat(failure.reason).contains("not supported")
    }

    @Test
    fun getHdrBrightnessBoostLevel_hdrBrightnessDisabled_fails() {
        setHdrBrightnessEnabledInSettingsProvider(OFF)
        val failure =
            assertFailsWith<InvalidPreferenceException> {
                tester.get<Int>(HDR_BRIGHTNESS_BOOST_LEVEL_KEY)
            }
        assertThat(failure.reason).contains("is disabled")
    }

    @Test
    fun setHdrBrightnessBoostLevel_updatesSettings() {
        shadowApplication.grantPermissions(WRITE_SECURE_SETTINGS)
        setHdrBrightnessEnabledInSettingsProvider(ON)
        tester.set(HDR_BRIGHTNESS_BOOST_LEVEL_KEY, 0)
        assertThat(getHdrBrightnessBoostLevelSettingsValue()).isEqualTo(0.0f)

        tester.set(HDR_BRIGHTNESS_BOOST_LEVEL_KEY, 11)
        assertThat(getHdrBrightnessBoostLevelSettingsValue()).isWithin(TOLERANCE).of(0.11f)

        tester.set(HDR_BRIGHTNESS_BOOST_LEVEL_KEY, 100)
        assertThat(getHdrBrightnessBoostLevelSettingsValue()).isWithin(TOLERANCE).of(1.0f)
    }

    @Test
    fun setHdrBrightnessBoostLevel_hardwareUnsupported_fails() {
        shadowApplication.grantPermissions(WRITE_SECURE_SETTINGS)
        setHdrBrightnessEnabledInSettingsProvider(ON)
        disableHdrSupport()
        setHdrBrightnessBoostLevelInSettingsProvider(1.0f)
        val failure =
            assertFailsWith<HardwareUnsupportedException> {
                tester.set(HDR_BRIGHTNESS_BOOST_LEVEL_KEY, 10)
            }
        assertThat(failure.reason).contains("not supported")
        assertThat(getHdrBrightnessBoostLevelSettingsValue()).isEqualTo(1.0f)
    }

    @Test
    fun setHdrBrightnessBoostLevel_hdrBrightnessDisabled_fails() {
        shadowApplication.grantPermissions(WRITE_SECURE_SETTINGS)
        setHdrBrightnessEnabledInSettingsProvider(OFF)
        setHdrBrightnessBoostLevelInSettingsProvider(1.0f)
        val failure =
            assertFailsWith<InvalidPreferenceException> {
                tester.set(HDR_BRIGHTNESS_BOOST_LEVEL_KEY, 10)
            }
        assertThat(failure.reason).contains("is disabled")
        assertThat(getHdrBrightnessBoostLevelSettingsValue()).isEqualTo(1.0f)
    }

    @Test
    fun setHdrBrightnessBoostLevel_missingPermission_fails() {
        shadowApplication.denyPermissions(WRITE_SECURE_SETTINGS)
        setHdrBrightnessEnabledInSettingsProvider(ON)
        setHdrBrightnessBoostLevelInSettingsProvider(1.0f)
        val failure =
            assertFailsWith<MissingPermissionException> {
                tester.set(HDR_BRIGHTNESS_BOOST_LEVEL_KEY, 10)
            }
        assertThat(getHdrBrightnessBoostLevelSettingsValue()).isEqualTo(1.0f)
    }

    private fun disableHdrSupport() {
        shadowDisplay.setDisplayHdrCapabilities(
            /* displayId= */ Display.DEFAULT_DISPLAY,
            /* maxLuminance= */ 0f,
            /* maxAverageLuminance= */ 0f,
            /* minLuminance= */ 0f,
        )
        shadowDisplay.setHdrSdrRatio(Float.NaN)
    }

    private fun setHdrBrightnessEnabledInSettingsProvider(value: Int) {
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.HDR_BRIGHTNESS_ENABLED,
            value,
        )
    }

    private fun getHdrBrightnessEnabledSettingsValue(): Int =
        Settings.Secure.getInt(context.contentResolver, Settings.Secure.HDR_BRIGHTNESS_ENABLED, -1)

    private fun setHdrBrightnessBoostLevelInSettingsProvider(value: Float) {
        Settings.Secure.putFloat(
            context.contentResolver,
            Settings.Secure.HDR_BRIGHTNESS_BOOST_LEVEL,
            value,
        )
    }

    private fun getHdrBrightnessBoostLevelSettingsValue(): Float =
        Settings.Secure.getFloat(
            context.contentResolver,
            Settings.Secure.HDR_BRIGHTNESS_BOOST_LEVEL,
            -1.0f,
        )
}
// LINT.ThenChange(HdrBrightnessSettingsTest.java, HdrBrightnessPreferenceControllerTest.java,
// HdrBrightnessLevelPreferenceControllerTest.java)
