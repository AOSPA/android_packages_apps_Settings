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
import com.android.settings.flags.Flags.FLAG_CATALYST_MIGRATION_26Q2
import com.android.settings.testutils.shadow.SettingsShadowResources
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
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowApplication
import org.robolectric.shadows.ShadowColorDisplayManager

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowApplication::class, SettingsShadowResources::class])
class NightDisplayApiScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val tester = ApiTester(NightDisplayApiScreen())
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val shadowApplication: ShadowApplication = shadowOf(context as Application)
    private val colorDisplayManager = context.getSystemService(ColorDisplayManager::class.java)

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
        shadowApplication.grantPermissions(CONTROL_DISPLAY_COLOR_TRANSFORMS)
        colorDisplayManager.setNightDisplayActivated(false)
        assertThat(tester.get<Boolean>(NIGHT_DISPLAY_ACTIVATED_KEY)).isFalse()

        colorDisplayManager.setNightDisplayActivated(true)
        assertThat(tester.get<Boolean>(NIGHT_DISPLAY_ACTIVATED_KEY)).isTrue()
    }

    @Test
    fun getNightDisplayActivated_nightDisplayNotAvailable_fails() {
        NightDisplayTestUtils.setNightDisplayAvailable(false)
        val failure =
            assertFailsWith<HardwareUnsupportedException> {
                tester.get<Boolean>(NIGHT_DISPLAY_ACTIVATED_KEY)
            }
        assertThat(failure.reason).contains("not supported")
    }

    @Test
    fun getNightDisplayActivated_nightDisplaySettingsBlocked_fails() {
        NightDisplayTestUtils.setNightDisplaySettingsBlocked(true)
        val failure =
            assertFailsWith<HardwareUnsupportedException> {
                tester.get<Boolean>(NIGHT_DISPLAY_ACTIVATED_KEY)
            }
        assertThat(failure.reason).contains("not supported")
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
    fun setNightDisplayActivated_nightDisplayNotAvailable_fails() {
        shadowApplication.grantPermissions(CONTROL_DISPLAY_COLOR_TRANSFORMS)
        NightDisplayTestUtils.setNightDisplayAvailable(false)
        colorDisplayManager.setNightDisplayActivated(true)
        val failure =
            assertFailsWith<HardwareUnsupportedException> {
                tester.set(NIGHT_DISPLAY_ACTIVATED_KEY, false)
            }
        assertThat(failure.reason).contains("not supported")
        assertThat(colorDisplayManager.isNightDisplayActivated).isTrue()
    }

    @Test
    fun setNightDisplayActivated_nightDisplaySettingsBlocked_fails() {
        shadowApplication.grantPermissions(CONTROL_DISPLAY_COLOR_TRANSFORMS)
        NightDisplayTestUtils.setNightDisplaySettingsBlocked(true)
        colorDisplayManager.setNightDisplayActivated(true)
        val failure =
            assertFailsWith<HardwareUnsupportedException> {
                tester.set(NIGHT_DISPLAY_ACTIVATED_KEY, false)
            }
        assertThat(failure.reason).contains("not supported")
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
}
// LINT.ThenChange(NightDisplaySettingsTest.java, NightDisplayScreenTest.kt,
// NightDisplayActivationPreferenceControllerTest.java)
