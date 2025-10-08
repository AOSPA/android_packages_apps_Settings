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

package com.android.settings.accessibility.shortcuts.ui

import android.hardware.input.InputManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.testing.TestableContext
import android.view.KeyEvent
import androidx.test.platform.app.InstrumentationRegistry
import com.android.internal.accessibility.AccessibilityShortcutController.COLOR_INVERSION_COMPONENT_NAME
import com.android.settings.R
import com.android.settings.testutils.AccessibilityTestUtils
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TopRowKeyShortcutPreferenceTest {
    @get:Rule val setFlagsRule = SetFlagsRule()
    private val context: TestableContext =
        TestableContext(InstrumentationRegistry.getInstrumentation().context)
    private lateinit var preference: TopRowKeyShortcutPreference
    private val targets = setOf(COLOR_INVERSION_COMPONENT_NAME.flattenToString())
    private val inputManager: InputManager = mock()

    @Before
    fun setUp() {
        context.addMockSystemService(InputManager::class.java, inputManager)
        AccessibilityTestUtils.setSoftwareShortcutMode(
            context,
            /* gestureNavEnabled= */ false,
            /* floatingButtonEnabled= */ false,
        )
        preference = TopRowKeyShortcutPreference(context, targets)
    }

    @Test
    fun key_returnsCorrectValue() {
        assertThat(preference.key).isEqualTo("shortcut_top_row_key_pref")
    }

    @Test
    fun title_returnsCorrectValue() {
        assertThat(preference.title)
            .isEqualTo(R.string.accessibility_shortcut_edit_dialog_title_top_row_key)
    }

    @Test
    @EnableFlags(
        android.view.accessibility.Flags.FLAG_ENABLE_A11Y_TOP_ROW_SHORTCUT,
        com.android.hardware.input.Flags.FLAG_ENABLE_NEW_26Q2_KEYCODES,
    )
    fun hasAccessibilityKey_isAvailable() {
        whenever(inputManager.deviceHasKeys(intArrayOf(KeyEvent.KEYCODE_ACCESSIBILITY)))
            .thenReturn(booleanArrayOf(true))

        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    @EnableFlags(
        android.view.accessibility.Flags.FLAG_ENABLE_A11Y_TOP_ROW_SHORTCUT,
        com.android.hardware.input.Flags.FLAG_ENABLE_NEW_26Q2_KEYCODES,
    )
    fun hasNoAccessibilityKey_isNotAvailable() {
        whenever(inputManager.deviceHasKeys(intArrayOf(KeyEvent.KEYCODE_ACCESSIBILITY)))
            .thenReturn(booleanArrayOf(false))

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    @DisableFlags(
        android.view.accessibility.Flags.FLAG_ENABLE_A11Y_TOP_ROW_SHORTCUT,
        com.android.hardware.input.Flags.FLAG_ENABLE_NEW_26Q2_KEYCODES,
    )
    fun hasAccessibilityKey_flagOff_isNotAvailable() {
        whenever(inputManager.deviceHasKeys(intArrayOf(KeyEvent.KEYCODE_ACCESSIBILITY)))
            .thenReturn(booleanArrayOf(true))

        assertThat(preference.isAvailable(context)).isFalse()
    }
}
