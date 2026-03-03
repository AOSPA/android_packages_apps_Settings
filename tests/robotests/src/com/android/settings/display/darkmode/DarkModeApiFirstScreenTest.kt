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

package com.android.settings.display.darkmode

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.app.Application
import android.content.Context
import android.os.PowerManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import android.provider.Settings.Secure.ACCESSIBILITY_FORCE_INVERT_COLOR_ENABLED
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.accessibility.Flags as AccFlags
import com.android.settings.flags.Flags.FLAG_CATALYST_MIGRATION_26Q2
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.InvalidPreferenceException
import com.android.settings.testutils2.MissingPermissionException
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class DarkModeApiFirstScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val tester = ApiTester(DarkModeApiFirstScreen())
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val shadowPowerManager = shadowOf(context.getSystemService(PowerManager::class.java))
    private val shadowApplication = shadowOf(context as Application)

    @Before
    fun setUp() {
        shadowPowerManager.setIsPowerSaveMode(false)
        shadowApplication.grantPermissions(WRITE_SECURE_SETTINGS)
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
    fun getLaunchIntent_hasIntent() {
        assertThat(tester.getLaunchIntent()).isNotNull()
    }

    @Test
    @DisableFlags(AccFlags.FLAG_ALLOW_TO_ENTER_DARK_THEME_SETTINGS_WHEN_BATTERY_SAVER)
    fun getLaunchIntent_disabledFlag_enabledPowerSaverMode_throwsInvalidPreferenceException() {
        shadowPowerManager.setIsPowerSaveMode(true)

        assertFailsWith<InvalidPreferenceException> { tester.getLaunchIntent() }
    }

    @Test
    fun getRadioPreference_defaultStandard_returnStandardMode() {
        Settings.Secure.putInt(
            context.contentResolver,
            ACCESSIBILITY_FORCE_INVERT_COLOR_ENABLED,
            DarkModeApiFirstScreen.STANDARD_DARK_THEME,
        )

        assertThat(tester.get<DarkThemeMode>(DarkModeApiFirstScreen.RADIO_PREFERENCE_KEY))
            .isEqualTo(DarkThemeMode.STANDARD.asApiValue)
    }

    @Test
    fun getRadioPreference_defaultExpended_returnExpendedMode() {
        Settings.Secure.putInt(
            context.contentResolver,
            ACCESSIBILITY_FORCE_INVERT_COLOR_ENABLED,
            DarkModeApiFirstScreen.EXPANDED_DARK_THEME,
        )

        assertThat(tester.get<DarkThemeMode>(DarkModeApiFirstScreen.RADIO_PREFERENCE_KEY))
            .isEqualTo(DarkThemeMode.EXPANDED.asApiValue)
    }

    @Test
    fun setRadioPreference_asStandardMode_returnStandardMode() {
        tester.set(DarkModeApiFirstScreen.RADIO_PREFERENCE_KEY, DarkThemeMode.STANDARD.asApiValue)

        assertThat(
                Settings.Secure.getInt(
                    context.contentResolver,
                    ACCESSIBILITY_FORCE_INVERT_COLOR_ENABLED,
                )
            )
            .isEqualTo(DarkModeApiFirstScreen.STANDARD_DARK_THEME)
    }

    @Test
    fun setRadioPreference_asExpandedMode_returnExpandedMode() {
        tester.set(DarkModeApiFirstScreen.RADIO_PREFERENCE_KEY, DarkThemeMode.EXPANDED.asApiValue)

        assertThat(
                Settings.Secure.getInt(
                    context.contentResolver,
                    ACCESSIBILITY_FORCE_INVERT_COLOR_ENABLED,
                )
            )
            .isEqualTo(DarkModeApiFirstScreen.EXPANDED_DARK_THEME)
    }

    @Test
    fun setRadioPreference_noPermission_throwsException() {
        shadowApplication.denyPermissions(WRITE_SECURE_SETTINGS)
        assertFailsWith<MissingPermissionException> {
            tester.set(
                DarkModeApiFirstScreen.RADIO_PREFERENCE_KEY,
                DarkThemeMode.STANDARD.asApiValue,
            )
        }
    }
}
