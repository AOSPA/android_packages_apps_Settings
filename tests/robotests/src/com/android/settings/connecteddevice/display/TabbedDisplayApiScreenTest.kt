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

package com.android.settings.connecteddevice.display

import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.view.Display
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.FailedPreconditionException
import com.android.settings.testutils2.Parameters
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowDisplayManager

@RunWith(AndroidJUnit4::class)
class TabbedDisplayApiScreenTest {
    private var tester: ApiTester = ApiTester(TabbedDisplayApiScreen())

    @get:Rule val setFlagsRule = SetFlagsRule()

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagEnabled_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun screenLaunch_withParameter_producesCorrectExtras() {
        val displayId = ShadowDisplayManager.addDisplay("w1200dp-h800dp", Display.TYPE_EXTERNAL)

        tester.initializeScreenParameters(Parameters("display_id" to displayId.toString()))
        val extras = tester.getLaunchScreenExtras()

        assertThat(extras.getInt(ExternalDisplaySettingsConfiguration.DISPLAY_ID_ARG))
            .isEqualTo(displayId)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun screenLaunch_withoutParameter_throwsException() {
        // Because the parameter is missing, `prepareScreenExtras` will hit the requireNotNull check
        assertFailsWith<IllegalArgumentException> {
            tester.initializeScreenParameters(Parameters())
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun screenLaunch_invalidParameter_failsSchemaValidation() {
        // Since no external display is added to ShadowDisplayManager yet, "123" is an invalid
        // option.
        // Catalyst rejects it during initialization.
        assertFailsWith<IllegalArgumentException> {
            tester.initializeScreenParameters(Parameters("display_id" to "123"))
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun screenLaunch_noExternalDisplay_throwsFailedPreconditionException() {
        // 1. Add a display so the schema validation allows us to initialize the parameter
        val displayId = ShadowDisplayManager.addDisplay("w1200dp-h800dp", Display.TYPE_EXTERNAL)
        tester.initializeScreenParameters(Parameters("display_id" to displayId.toString()))

        // 2. Remove the display to simulate unplugging it.
        // Now `displayManager?.displays?.any` will return false!
        ShadowDisplayManager.removeDisplay(displayId)

        // 3. The preconditions block fails and returns `Custom`, which the tester throws as
        // FailedPreconditionException
        assertFailsWith<FailedPreconditionException> { tester.getLaunchIntent() }
    }
}
