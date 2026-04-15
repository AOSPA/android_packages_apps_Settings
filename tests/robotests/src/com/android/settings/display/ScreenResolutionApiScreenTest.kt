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

import android.Manifest.permission.MODIFY_USER_PREFERRED_DISPLAY_MODE
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
import com.android.settings.display.ScreenResolutionApiScreen.Companion.PREFERENCE_KEY
import com.android.settings.flags.Flags.FLAG_CATALYST_MIGRATION_26Q2
import com.android.settings.testutils.shadow.ShadowDisplay
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.HardwareUnsupportedException
import com.android.settings.testutils2.MissingPermissionException
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDisplayManager

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowDisplay::class])
class ScreenResolutionApiScreenTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val displayManager = context.getSystemService(DisplayManager::class.java)
    private val defaultDisplay = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
    private val tester = ApiTester(ScreenResolutionApiScreen())
    private val shadowDisplay = shadowOf(defaultDisplay) as ShadowDisplay

    private val modeHigh = Display.Mode(0, 1080, 1920, 60f)
    private val modeFull = Display.Mode(1, 1440, 2560, 60f)
    private val shadowApplication = shadowOf(context as Application)

    @get:Rule val setFlagsRule = SetFlagsRule()

    @Before
    fun setUp() {
        ShadowDisplayManager.reset()
        ShadowDisplayManager.setSupportedModes(Display.DEFAULT_DISPLAY, modeHigh, modeFull)
        // Set an initial mode for the display
        defaultDisplay.setUserPreferredDisplayMode(modeHigh)
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
    fun getLaunchIntent_notEnoughModes_throwsException() {
        // This overrides the modes set in setUp()
        ShadowDisplayManager.setSupportedModes(Display.DEFAULT_DISPLAY, modeHigh)

        assertThrows(HardwareUnsupportedException::class.java) { tester.getLaunchIntent() }
    }

    @Test
    fun getLaunchIntent_enoughModes_isNotNull() {
        assertThat(tester.getLaunchIntent()).isNotNull()
    }

    @Test
    fun getPreference_whenHighRes_returnsHigh() {
        defaultDisplay.setUserPreferredDisplayMode(modeHigh)

        assertThat(tester.get<Int>(PREFERENCE_KEY))
            .isEqualTo(ScreenResolutionOption.HIGH.asApiValue)
    }

    @Test
    fun getPreference_whenFullRes_returnsFull() {
        defaultDisplay.setUserPreferredDisplayMode(modeFull)

        assertThat(tester.get<Int>(PREFERENCE_KEY))
            .isEqualTo(ScreenResolutionOption.FULL.asApiValue)
    }

    @Test
    fun setPreference_toFullRes_setsFullMode() {
        // Start with HIGH resolution
        defaultDisplay.setUserPreferredDisplayMode(modeHigh)
        shadowApplication.grantPermissions(MODIFY_USER_PREFERRED_DISPLAY_MODE)

        tester.set<Int>(PREFERENCE_KEY, ScreenResolutionOption.FULL.asApiValue)

        assertThat(defaultDisplay.getMode()).isEqualTo(modeFull)
    }

    @Test
    fun setPreference_toHighRes_setsHighMode() {
        // Start with FULL resolution
        defaultDisplay.setUserPreferredDisplayMode(modeFull)
        shadowApplication.grantPermissions(MODIFY_USER_PREFERRED_DISPLAY_MODE)

        tester.set<Int>(PREFERENCE_KEY, ScreenResolutionOption.HIGH.asApiValue)

        assertThat(defaultDisplay.getMode()).isEqualTo(modeHigh)
    }

    @Test
    fun setPreference_noPermission_throwsException() {
        defaultDisplay.setUserPreferredDisplayMode(modeFull)
        shadowApplication.denyPermissions(MODIFY_USER_PREFERRED_DISPLAY_MODE)

        assertFailsWith<MissingPermissionException> {
            tester.set<Int>(PREFERENCE_KEY, ScreenResolutionOption.HIGH.asApiValue)
        }

        assertThat(defaultDisplay.getMode()).isEqualTo(modeFull)

    }
}
