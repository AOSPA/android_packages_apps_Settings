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
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.internal.R
import com.android.settings.flags.Flags.FLAG_CATALYST_MIGRATION_26Q2
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.FailedPreconditionException
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
@Config(shadows = [SettingsShadowResources::class])
class AutoBrightnessApiScreenTest {
    private val tester = ApiTester(AutoBrightnessApiScreen())
    @get:Rule val setFlagsRule = SetFlagsRule()

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
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getLaunchIntent_preconditionNotMet_isNull() {
        SettingsShadowResources.overrideResource(
            R.bool.config_automatic_brightness_available,
            false,
        )

        assertThrows(FailedPreconditionException::class.java) { tester.getLaunchIntent() }
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getLaunchIntent_preconditionMet_isNotNull() {
        SettingsShadowResources.overrideResource(R.bool.config_automatic_brightness_available, true)

        assertThat(tester.getLaunchIntent()).isNotNull()
    }
}
// LINT.ThenChange(AutoBrightnessPreferenceControllerTest.java, AutoBrightnessScreenTest.kt)
