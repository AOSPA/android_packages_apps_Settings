/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.accessibility.setupwizard

import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.android.settings.R
import com.android.settings.accessibility.Flags
import com.android.settings.testutils.SystemProperty
import com.google.common.truth.Truth.assertThat
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests for [AccessibilitySetupWizardActivity]. */
@RunWith(RobolectricTestRunner::class)
@RequiresFlagsEnabled(Flags.FLAG_ENABLE_SETUPLIB_IN_A11Y_SETUP_PAGE)
class AccessibilitySetupWizardActivityTest {

    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(AccessibilitySetupWizardActivity::class.java)

    @Test
    fun onCreate_loadsFragment() {
        activityScenarioRule.scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentById(R.id.fragment_container)
            assertThat(fragment).isInstanceOf(AccessibilitySetupWizardFragment::class.java)
        }
    }

    @Test
    fun onCreate_appliesTheme() {
        activityScenarioRule.scenario.onActivity { activity ->
            assertThat(activity.theme).isNotNull()
        }
    }

    companion object {
        private var systemProperty: SystemProperty? = null

        @BeforeClass
        @JvmStatic
        fun setup() {
            // Robolectric by default creates Activity contexts without an associated Display.
            // This property ensures the Activity context is attached to a simulated display,
            // preventing UnsupportedOperationException when code calls Context#getDisplay().
            systemProperty =
                SystemProperty().apply { override("robolectric.createActivityContexts", "true") }
        }

        @AfterClass
        @JvmStatic
        fun cleanUp() {
            systemProperty?.let {
                it.close()
                systemProperty = null
            }
        }
    }
}
