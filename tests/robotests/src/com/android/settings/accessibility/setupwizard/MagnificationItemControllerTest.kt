/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.accessibility.setupwizard

import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.android.settings.EmptySetupWizardActivity
import com.android.settings.R
import com.google.android.setupdesign.items.Item
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

/** Tests for [MagnificationItemController]. */
@RunWith(RobolectricTestRunner::class)
class MagnificationItemControllerTest {

    private val mockItem = mock<Item>()

    @get:Rule val activityScenarioRule = ActivityScenarioRule(EmptySetupWizardActivity::class.java)

    @Test
    fun onItemSelected_navigatesToMagnificationFragment() {
        activityScenarioRule.scenario.onActivity { activity ->
            activity.setContentView(R.layout.accessibility_suw_activity)
            val controller = MagnificationItemController(mockItem)

            controller.onItemSelected(activity)
            activity.supportFragmentManager.executePendingTransactions()

            val currentFragment =
                activity.supportFragmentManager.findFragmentById(R.id.fragment_container)
            assertThat(currentFragment).isInstanceOf(MagnificationSetupWizardFragment::class.java)
        }
    }
}
