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

import android.Manifest.permission.CONTROL_DISPLAY_COLOR_TRANSFORMS
import android.app.Application
import android.content.Context
import android.hardware.display.ColorDisplayManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.display.NightDisplayApiScreen.Companion.NIGHT_DISPLAY_ACTIVATED_KEY
import com.android.settings.display.NightDisplayApiScreen.Companion.NIGHT_DISPLAY_TEMPERATURE_KEY
import com.android.settings.flags.Flags.FLAG_CATALYST_MIGRATION_26Q2
import com.android.settings.testutils.shadow.SettingsShadowResources
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

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowApplication::class, SettingsShadowResources::class])
class NightDisplayApiScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val tester = ApiTester(NightDisplayApiScreen())
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val shadowApplication: ShadowApplication = shadowOf(context as Application)
    private val colorDisplayManager = context.getSystemService(ColorDisplayManager::class.java)
    private val minTemperature = ColorDisplayManager.getMinimumColorTemperature(context)
    private val maxTemperature = ColorDisplayManager.getMaximumColorTemperature(context)

    @Before
    fun setUp() {
        // This uses SettingsShadowResources to update config keys.
        NightDisplayTestUtils.setNightDisplayAvailable(true)
        NightDisplayTestUtils.setNightDisplaySettingsBlocked(false)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    fun getLaunchIntent_isNotNull() {
        assertThat(tester.getLaunchIntent()).isNotNull()
    }

    @Test
    fun getLaunchIntent_nightDisplayNotAvailable_fails() {
        NightDisplayTestUtils.setNightDisplayAvailable(false)
        val failure = assertFailsWith<HardwareUnsupportedException> { tester.getLaunchIntent() }
        assertThat(failure.reason).contains("not supported")
    }

    @Test
    fun getLaunchIntent_nightDisplaySettingsBlocked_fails() {
        NightDisplayTestUtils.setNightDisplaySettingsBlocked(true)
        val failure = assertFailsWith<HardwareUnsupportedException> { tester.getLaunchIntent() }
        assertThat(failure.reason).contains("not supported")
    }

    @Test
    fun getNightDisplayActivated_returnsSettingsValue() {
        colorDisplayManager.setNightDisplayActivated(false)
        assertThat(tester.get<Boolean>(NIGHT_DISPLAY_ACTIVATED_KEY)).isFalse()

        colorDisplayManager.setNightDisplayActivated(true)
        assertThat(tester.get<Boolean>(NIGHT_DISPLAY_ACTIVATED_KEY)).isTrue()
    }

    @Test
    fun setNightDisplayActivated_updatesSettings() {
        shadowApplication.grantPermissions(CONTROL_DISPLAY_COLOR_TRANSFORMS)
        tester.set(NIGHT_DISPLAY_ACTIVATED_KEY, false)
        assertThat(colorDisplayManager.isNightDisplayActivated).isFalse()

        tester.set(NIGHT_DISPLAY_ACTIVATED_KEY, true)
        assertThat(colorDisplayManager.isNightDisplayActivated).isTrue()
    }

    @Test
    fun setNightDisplayActivated_missingPermission_fails() {
        shadowApplication.denyPermissions(CONTROL_DISPLAY_COLOR_TRANSFORMS)
        colorDisplayManager.setNightDisplayActivated(true)
        val failure =
            assertFailsWith<MissingPermissionException> {
                tester.set(NIGHT_DISPLAY_ACTIVATED_KEY, false)
            }
        assertThat(colorDisplayManager.isNightDisplayActivated).isTrue()
    }

    @Test
    fun getNightDisplayTemperature_returnsSettingsValue() {
        colorDisplayManager.setNightDisplayActivated(true)

        // Max temperature should be 0%.
        colorDisplayManager.setNightDisplayColorTemperature(maxTemperature)
        assertThat(tester.get<Int>(NIGHT_DISPLAY_TEMPERATURE_KEY)).isEqualTo(0)

        // 25% should be 1/4 of the way from the max temperature.
        colorDisplayManager.setNightDisplayColorTemperature(
            maxTemperature - (maxTemperature - minTemperature) / 4
        )
        assertThat(tester.get<Int>(NIGHT_DISPLAY_TEMPERATURE_KEY)).isEqualTo(25)

        // 50% is the temperature halfway point.
        colorDisplayManager.setNightDisplayColorTemperature(
            minTemperature + (maxTemperature - minTemperature) / 2
        )
        assertThat(tester.get<Int>(NIGHT_DISPLAY_TEMPERATURE_KEY)).isEqualTo(50)

        // 80% should be 1/5 of the way from the min temperature.
        colorDisplayManager.setNightDisplayColorTemperature(
            minTemperature + (maxTemperature - minTemperature) / 5
        )
        assertThat(tester.get<Int>(NIGHT_DISPLAY_TEMPERATURE_KEY)).isEqualTo(80)

        // Min temperature should be 100%.
        colorDisplayManager.setNightDisplayColorTemperature(minTemperature)
        assertThat(tester.get<Int>(NIGHT_DISPLAY_TEMPERATURE_KEY)).isEqualTo(100)
    }

    @Test
    fun getNightDisplayTemperature_nightDisplayDeactivated_fails() {
        colorDisplayManager.setNightDisplayActivated(false)
        val failure =
            assertFailsWith<InvalidPreferenceException> {
                tester.get<Int>(NIGHT_DISPLAY_TEMPERATURE_KEY)
            }
        assertThat(failure.reason).contains("is disabled")
    }

    @Test
    fun setNightDisplayTemperature_updatesSettings() {
        shadowApplication.grantPermissions(CONTROL_DISPLAY_COLOR_TRANSFORMS)
        colorDisplayManager.setNightDisplayActivated(true)

        // 0% should be the max temperature.
        tester.set(NIGHT_DISPLAY_TEMPERATURE_KEY, 0)
        assertThat(colorDisplayManager.getNightDisplayColorTemperature()).isEqualTo(maxTemperature)

        // 25% should be 1/4 of the way from the max temperature.
        tester.set(NIGHT_DISPLAY_TEMPERATURE_KEY, 25)
        assertThat(colorDisplayManager.getNightDisplayColorTemperature())
            .isEqualTo(maxTemperature - (maxTemperature - minTemperature) / 4)

        // 50% is the temperature halfway point.
        tester.set(NIGHT_DISPLAY_TEMPERATURE_KEY, 50)
        assertThat(colorDisplayManager.getNightDisplayColorTemperature())
            .isEqualTo(minTemperature + (maxTemperature - minTemperature) / 2)

        // 80% should be 1/5 of the way from the min temperature.
        tester.set(NIGHT_DISPLAY_TEMPERATURE_KEY, 80)
        assertThat(colorDisplayManager.getNightDisplayColorTemperature())
            .isEqualTo(minTemperature + (maxTemperature - minTemperature) / 5)

        // 100% should be the min temperature.
        tester.set(NIGHT_DISPLAY_TEMPERATURE_KEY, 100)
        assertThat(colorDisplayManager.getNightDisplayColorTemperature()).isEqualTo(minTemperature)
    }

    @Test
    fun setNightDisplayTemperature_missingPermission_fails() {
        shadowApplication.denyPermissions(CONTROL_DISPLAY_COLOR_TRANSFORMS)
        colorDisplayManager.setNightDisplayActivated(true)
        colorDisplayManager.setNightDisplayColorTemperature(minTemperature)
        val failure =
            assertFailsWith<MissingPermissionException> {
                tester.set(NIGHT_DISPLAY_TEMPERATURE_KEY, 100)
            }
        assertThat(colorDisplayManager.getNightDisplayColorTemperature()).isEqualTo(minTemperature)
    }

    @Test
    fun setNightDisplayTemperature_nightDisplayDeactivated_fails() {
        shadowApplication.grantPermissions(CONTROL_DISPLAY_COLOR_TRANSFORMS)
        colorDisplayManager.setNightDisplayActivated(false)
        colorDisplayManager.setNightDisplayColorTemperature(minTemperature)
        val failure =
            assertFailsWith<InvalidPreferenceException> {
                tester.set(NIGHT_DISPLAY_TEMPERATURE_KEY, 100)
            }
        assertThat(colorDisplayManager.getNightDisplayColorTemperature()).isEqualTo(minTemperature)
    }
}
// LINT.ThenChange(NightDisplaySettingsTest.java, NightDisplayScreenTest.kt,
// NightDisplayActivationPreferenceControllerTest.java,
// NightDisplayIntensityPreferenceControllerTest.java)
