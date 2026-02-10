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

package com.android.settings.accessibility.autoclick.dialogs

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.testutils.SettingsStoreRule
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

/** Tests for [AutoclickCursorAreaSizeDialogFragment]. */
@RunWith(RobolectricTestRunner::class)
class AutoclickCursorAreaSizeDialogFragmentTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private lateinit var fragmentScenario: FragmentScenario<AutoclickCursorAreaSizeDialogFragment>

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_AUTOCLICK_CURSOR_AREA_SIZE,
            AccessibilityManager.AUTOCLICK_CURSOR_AREA_SIZE_DEFAULT,
        )

        fragmentScenario =
            FragmentScenario.launch(
                AutoclickCursorAreaSizeDialogFragment::class.java,
                Bundle(),
                R.style.Theme_AlertDialog_SettingsLib,
                Lifecycle.State.INITIALIZED,
            )
    }

    @After
    fun cleanUp() {
        fragmentScenario.close()
    }

    @Test
    fun onCreateDialog_reflectsCurrentSetting() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_AUTOCLICK_CURSOR_AREA_SIZE,
            CURSOR_AREA_SIZE_LARGE,
        )

        fragmentScenario.moveToState(Lifecycle.State.RESUMED)

        fragmentScenario.onFragment { fragment ->
            val dialog = fragment.requireDialog() as AlertDialog
            val radioGroup =
                dialog.findViewById<RadioGroup>(R.id.autoclick_cursor_area_size_value_group)
            assertThat(radioGroup?.checkedRadioButtonId)
                .isEqualTo(R.id.autoclick_cursor_area_size_value_large)
        }
    }

    @Test
    fun positiveButton_updatesSetting() {
        fragmentScenario.moveToState(Lifecycle.State.RESUMED)

        fragmentScenario.onFragment { fragment ->
            val dialog = fragment.requireDialog() as AlertDialog
            val radioGroup =
                dialog.findViewById<RadioGroup>(R.id.autoclick_cursor_area_size_value_group)

            radioGroup?.check(R.id.autoclick_cursor_area_size_value_extra_large)

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            ShadowLooper.idleMainLooper()

            val context = ApplicationProvider.getApplicationContext<Context>()
            val updatedSize =
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_AUTOCLICK_CURSOR_AREA_SIZE,
                )
            assertThat(updatedSize).isEqualTo(CURSOR_AREA_SIZE_EXTRA_LARGE)
        }
    }

    @Test
    fun negativeButton_dismissesDialogWithoutUpdatingSetting() {
        val initialSize = AccessibilityManager.AUTOCLICK_CURSOR_AREA_SIZE_DEFAULT
        fragmentScenario.moveToState(Lifecycle.State.RESUMED)

        fragmentScenario.onFragment { fragment ->
            val dialog = fragment.requireDialog() as AlertDialog
            val radioGroup =
                dialog.findViewById<RadioGroup>(R.id.autoclick_cursor_area_size_value_group)

            radioGroup?.check(R.id.autoclick_cursor_area_size_value_extra_large)

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
            ShadowLooper.idleMainLooper()

            assertThat(dialog.isShowing).isFalse()
            val context = ApplicationProvider.getApplicationContext<Context>()
            val updatedSize =
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_AUTOCLICK_CURSOR_AREA_SIZE,
                )
            assertThat(updatedSize).isEqualTo(initialSize)
        }
    }

    companion object {
        private const val CURSOR_AREA_SIZE_EXTRA_LARGE = 100
        private const val CURSOR_AREA_SIZE_LARGE = 80
    }
}
