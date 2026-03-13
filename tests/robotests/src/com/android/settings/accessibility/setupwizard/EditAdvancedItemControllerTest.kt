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

package com.android.settings.accessibility.setupwizard

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME
import com.android.settings.Settings.TestingSettingsActivity
import com.google.android.setupdesign.items.Item
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests for [EditAdvancedItemController]. */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class EditAdvancedItemControllerTest {

    @get:Rule val activityScenarioRule = ActivityScenarioRule(TestingSettingsActivity::class.java)
    private val appContext: Application = ApplicationProvider.getApplicationContext()
    private val item = Item()
    private val testDispatcher = StandardTestDispatcher()
    private val expandableState = MutableStateFlow(false)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun bindData_setsVisible() {
        val controller = createController(setOf(MAGNIFICATION_CONTROLLER_NAME))

        controller.bindData(item)

        assertThat(item.isVisible).isTrue()
    }

    @Test
    fun onItemSelected_setsExpandableStateToTrue() = runTest {
        activityScenarioRule.scenario.onActivity { activity ->
            val controller = createController(setOf(MAGNIFICATION_CONTROLLER_NAME))
            controller.bindData(item)

            controller.onItemSelected(activity)

            assertThat(expandableState.value).isTrue()
        }
    }

    @Test
    fun onItemSelected_triggersVisibilityChange() = runTest {
        activityScenarioRule.scenario.onActivity { activity ->
            val controller = createController(setOf(MAGNIFICATION_CONTROLLER_NAME))
            controller.bindData(item)

            controller.onItemSelected(activity)
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(item.isVisible).isFalse()
        }
    }

    /** Creates the controller and its associated store. */
    private fun createController(targets: Set<String>) =
        EditAdvancedItemController.create(appContext, item, targets) { expandableState }
}
