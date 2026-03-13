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
import com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME
import com.android.settings.R
import com.android.settings.accessibility.setupwizard.items.IllustrationCheckBoxItem
import com.android.settings.testutils.AccessibilityTestUtils
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector

/** Tests for [EditNavButtonShortcutController]. */
@RunWith(RobolectricTestParameterInjector::class)
class EditNavButtonShortcutControllerTest {

    private val appContext: Application = ApplicationProvider.getApplicationContext()
    private val item = IllustrationCheckBoxItem()

    @Test
    fun bindData_setsSummary() {
        val controller = createController(setOf(MAGNIFICATION_CONTROLLER_NAME))

        controller.bindData(item)

        assertThat(item.summary).isNotNull()
    }

    @Test
    fun bindData_setsExpectedResId() {
        val controller = createController(setOf(MAGNIFICATION_CONTROLLER_NAME))

        controller.bindData(item)

        assertThat(item.imageResId).isEqualTo(R.drawable.accessibility_shortcut_type_navbar)
    }

    @TestParameters(
        value =
            [
                "{gestureNavEnabled: false, floatingButtonEnabled: false, expectedValue: true}",
                "{gestureNavEnabled: false, floatingButtonEnabled: true, expectedValue: false}",
                "{gestureNavEnabled: true, floatingButtonEnabled: false, expectedValue: false}",
                "{gestureNavEnabled: true, floatingButtonEnabled: true, expectedValue: false}",
            ]
    )
    @Test
    fun bindData_updatesItemVisibility(
        gestureNavEnabled: Boolean,
        floatingButtonEnabled: Boolean,
        expectedValue: Boolean,
    ) {
        AccessibilityTestUtils.setSoftwareShortcutMode(
            appContext,
            gestureNavEnabled,
            floatingButtonEnabled,
        )
        val controller = createController(setOf(MAGNIFICATION_CONTROLLER_NAME))

        controller.bindData(item)

        assertThat(item.isVisible).isEqualTo(expectedValue)
    }

    /** Creates the controller and its associated store. */
    private fun createController(targets: Set<String>) =
        EditNavButtonShortcutController.create(appContext, item, targets)
}
