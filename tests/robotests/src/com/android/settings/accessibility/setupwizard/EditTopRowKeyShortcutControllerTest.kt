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

import android.hardware.input.InputManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.testing.TestableContext
import android.view.KeyEvent
import android.view.accessibility.Flags.FLAG_ENABLE_A11Y_TOP_ROW_SHORTCUT
import androidx.test.platform.app.InstrumentationRegistry
import com.android.hardware.input.Flags.FLAG_ENABLE_NEW_26Q2_KEYCODES
import com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME
import com.android.settings.R
import com.android.settings.accessibility.setupwizard.items.IllustrationCheckBoxItem
import com.android.settings.testutils.AccessibilityTestUtils
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.RobolectricTestRunner

/** Tests for [EditTopRowKeyShortcutController]. */
@RunWith(RobolectricTestRunner::class)
class EditTopRowKeyShortcutControllerTest {

    @get:Rule val setFlagsRule = SetFlagsRule()
    private val context: TestableContext =
        TestableContext(InstrumentationRegistry.getInstrumentation().context)
    private val item = IllustrationCheckBoxItem()
    private val inputManager: InputManager = mock()

    @Before
    fun setUp() {
        context.addMockSystemService(InputManager::class.java, inputManager)
        AccessibilityTestUtils.setSoftwareShortcutMode(
            context,
            /* gestureNavEnabled= */ false,
            /* floatingButtonEnabled= */ false,
        )
        stubKeyCheck(exists = true)
    }

    @Test
    fun bindData_setsSummary() {
        val controller = createController(setOf(MAGNIFICATION_CONTROLLER_NAME))

        controller.bindData(item)

        assertThat(item.summary)
            .isEqualTo(
                context.getString(R.string.accessibility_shortcut_edit_dialog_summary_top_row_key)
            )
    }

    @Test
    fun bindData_setsExpectedResId() {
        val controller = createController(setOf(MAGNIFICATION_CONTROLLER_NAME))

        controller.bindData(item)

        assertThat(item.imageResId).isEqualTo(R.drawable.accessibility_shortcut_type_top_row)
    }

    @Test
    @EnableFlags(FLAG_ENABLE_A11Y_TOP_ROW_SHORTCUT, FLAG_ENABLE_NEW_26Q2_KEYCODES)
    fun bindData_showsItem() {
        val controller = createController(setOf(MAGNIFICATION_CONTROLLER_NAME))

        controller.bindData(item)

        assertThat(item.isVisible).isTrue()
    }

    @Test
    @EnableFlags(FLAG_ENABLE_A11Y_TOP_ROW_SHORTCUT, FLAG_ENABLE_NEW_26Q2_KEYCODES)
    fun bindData_hasNoAccessibilityKey_hidesItem() {
        stubKeyCheck(exists = false)
        val controller = createController(setOf(MAGNIFICATION_CONTROLLER_NAME))

        controller.bindData(item)

        assertThat(item.isVisible).isFalse()
    }

    @Test
    @DisableFlags(FLAG_ENABLE_A11Y_TOP_ROW_SHORTCUT, FLAG_ENABLE_NEW_26Q2_KEYCODES)
    fun bindData_flagOff_hidesItem() {
        val controller = createController(setOf(MAGNIFICATION_CONTROLLER_NAME))

        controller.bindData(item)

        assertThat(item.isVisible).isFalse()
    }

    /** Creates the controller and its associated store. */
    private fun createController(targets: Set<String>) =
        EditTopRowKeyShortcutController.create(context, item, targets)

    private fun stubKeyCheck(exists: Boolean) {
        inputManager.stub {
            on { deviceHasKeys(intArrayOf(KeyEvent.KEYCODE_ACCESSIBILITY)) } doReturn
                booleanArrayOf(exists)
        }
    }
}
