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
import android.os.UserManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.internal.R
import com.android.server.display.feature.flags.Flags.FLAG_DISPLAY_SETTINGS_API_SCREEN_SUPPORT
import com.android.settings.flags.Flags.FLAG_CATALYST_MIGRATION_26Q2
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settings.testutils.shadow.ShadowUserManager
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.FailedPreconditionException
import com.android.settings.testutils2.MissingPermissionException
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowApplication

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
@Config(
    shadows = [SettingsShadowResources::class, ShadowUserManager::class]
)
class AutoBrightnessApiScreenTest {
    private val tester = ApiTester(AutoBrightnessApiScreen())
    @get:Rule val setFlagsRule = SetFlagsRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val shadowApplication: ShadowApplication = shadowOf(context as Application)
    private lateinit var shadowUserManager: ShadowUserManager

    @Before
    fun setUp() {
        SettingsShadowResources.overrideResource(R.bool.config_automatic_brightness_available, true)
        shadowApplication.grantPermissions(WRITE_SETTINGS)
        shadowUserManager = Shadow.extract(context.getSystemService(Context.USER_SERVICE))
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
    fun getLaunchIntent_preconditionNotMet_isNull() {
        SettingsShadowResources.overrideResource(
            R.bool.config_automatic_brightness_available,
            false,
        )

        assertThrows(FailedPreconditionException::class.java) { tester.getLaunchIntent() }
    }

    @Test
    fun getLaunchIntent_preconditionMet_isNotNull() {

        assertThat(tester.getLaunchIntent()).isNotNull()
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getSetting_manualMode() {
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
        )

        val isAutomatic = false

        assertThat(tester.get<Boolean>(AutoBrightnessApiScreen.PREFERENCE_KEY))
            .isEqualTo(isAutomatic)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getSetting_automaticMode() {

        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC,
        )
        // verify whether setting is set to automatic mode
        assertThat(tester.get<Boolean>(AutoBrightnessApiScreen.PREFERENCE_KEY)).isTrue()
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun setSetting_manualMode() {
        val isAutomatic = false

        tester.set<Boolean>(AutoBrightnessApiScreen.PREFERENCE_KEY, isAutomatic)

        // verify whether setting is set to manual mode
        assertThat(
                Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                )
            )
            .isEqualTo(Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun setSetting_automaticMode() {
        val isAutomatic = true

        tester.set<Boolean>(AutoBrightnessApiScreen.PREFERENCE_KEY, isAutomatic)

        // verify whether setting is set to automatic mode
        assertThat(
                Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                )
            )
            .isEqualTo(Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun autoBrightnessConfigDisabled_throwsException() {
        SettingsShadowResources.overrideResource(
            R.bool.config_automatic_brightness_available,
            false,
        )

        assertThrows(FailedPreconditionException::class.java) {
            tester.get<Int>(AutoBrightnessApiScreen.PREFERENCE_KEY)
        }

        assertThrows(FailedPreconditionException::class.java) { tester.getLaunchIntent() }

        assertThrows(FailedPreconditionException::class.java) {
            val isAutomatic = true
            tester.set<Boolean>(AutoBrightnessApiScreen.PREFERENCE_KEY, isAutomatic)
        }
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun userRestrictionDisallowConfigBrightness_set_throwsException() {
        shadowUserManager.addBaseUserRestriction(UserManager.DISALLOW_CONFIG_BRIGHTNESS)

        assertThrows(FailedPreconditionException::class.java) {
            val isAutomatic = true
            tester.set<Boolean>(AutoBrightnessApiScreen.PREFERENCE_KEY, isAutomatic)
        }
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun setSetting_failPermission() {
        shadowApplication.denyPermissions(WRITE_SETTINGS)
        val isAutomatic = true

        assertThrows(MissingPermissionException::class.java) {
            tester.set<Boolean>(AutoBrightnessApiScreen.PREFERENCE_KEY, isAutomatic)
        }
    }
}
// LINT.ThenChange(AutoBrightnessPreferenceControllerTest.java, AutoBrightnessScreenTest.kt)
