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

import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.view.Display
import android.view.Display.HdrCapabilities
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags.FLAG_CATALYST_MIGRATION_26Q2
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.HardwareUnsupportedException
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDisplay

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowDisplay::class])
class HdrBrightnessApiScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val tester = ApiTester(HdrBrightnessApiScreen())
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
    fun getLaunchIntent_hardwareUnsupported_fails() {
        disableHdrSupport()
        val failure = assertFailsWith<HardwareUnsupportedException> { tester.getLaunchIntent() }
        assertThat(failure.reason).contains("not supported")
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
}
// LINT.ThenChange(HdrBrightnessSettingsTest.java, HdrBrightnessPreferenceControllerTest.java)
