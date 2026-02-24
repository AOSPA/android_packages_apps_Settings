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

import android.app.settings.SettingsEnums
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_COMPONENT_NAME
import com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME
import com.android.settings.R
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupdesign.GlifLayout
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests for [EditShortcutSetupWizardFragment]. */
@RunWith(RobolectricTestRunner::class)
class EditShortcutSetupWizardFragmentTest {

    private val appContext: Context = ApplicationProvider.getApplicationContext()

    @After
    fun cleanUp() {
        AccessibilityRepositoryProvider.resetInstanceForTesting()
    }

    @Test
    fun newInstance_withMagnification_setsCorrectTarget() {
        val fragment =
            EditShortcutSetupWizardFragment.newInstance(
                METRICS_CATEGORY,
                TEST_TITLE,
                MAGNIFICATION_COMPONENT_NAME,
            )

        val targets =
            fragment.arguments?.getStringArray(
                EditShortcutSetupWizardFragment.ARG_KEY_SHORTCUT_TARGETS
            )
        assertThat(targets).isEqualTo(arrayOf(MAGNIFICATION_CONTROLLER_NAME))
    }

    @Test
    fun onViewCreated_hasCorrectTitle() {
        val args = createTestArgs()
        launchFragment(args).use { scenario ->
            scenario.onFragment { fragment ->
                val layout =
                    fragment
                        .requireView()
                        .findViewById<GlifLayout>(R.id.edit_shortcut_suw_screen_layout)

                assertThat(layout.headerTextView.text).isEqualTo(TEST_TITLE)
            }
        }
    }

    @Test
    fun footerButtons_areInitializedCorrect() {
        val args = createTestArgs()
        launchFragment(args).use { scenario ->
            scenario.onFragment { fragment ->
                val layout =
                    fragment
                        .requireView()
                        .findViewById<GlifLayout>(R.id.edit_shortcut_suw_screen_layout)

                val mixin = layout.getMixin(FooterBarMixin::class.java)
                assertThat(mixin.primaryButton.text).isEqualTo(appContext.getString(R.string.done))
                assertThat(mixin.primaryButtonView.visibility).isEqualTo(View.VISIBLE)
            }
        }
    }

    @Test
    fun clickDoneButton_popsBackStack() {
        val args = createTestArgs()
        launchFragment(args).use { scenario ->
            scenario.onFragment { fragment ->
                val fragmentManager = fragment.parentFragmentManager
                fragmentManager.beginTransaction().addToBackStack("test_state").commit()
                fragmentManager.executePendingTransactions()
                val initialCount = fragmentManager.backStackEntryCount

                val layout =
                    fragment
                        .requireView()
                        .findViewById<GlifLayout>(R.id.edit_shortcut_suw_screen_layout)
                val mixin = layout.getMixin(FooterBarMixin::class.java)
                mixin.primaryButtonView.performClick()
                fragmentManager.executePendingTransactions()

                assertThat(fragmentManager.backStackEntryCount).isEqualTo(initialCount - 1)
            }
        }
    }

    private fun createTestArgs() =
        EditShortcutSetupWizardFragment.newInstance(
                METRICS_CATEGORY,
                TEST_TITLE,
                TEST_COMPONENT_NAME,
            )
            .arguments

    private fun launchFragment(args: Bundle?): FragmentScenario<EditShortcutSetupWizardFragment> =
        launchFragmentInContainer<EditShortcutSetupWizardFragment>(
            fragmentArgs = args,
            // Using the same theme pattern as your success case
            themeResId = R.style.GlifTheme,
        )

    companion object {
        private const val METRICS_CATEGORY = SettingsEnums.ACCESSIBILITY_MAGNIFICATION_SETTINGS
        private const val TEST_TITLE = "Edit Shortcut"
        private val TEST_COMPONENT_NAME = ComponentName("pkg", "cls")
        private const val EXTRA_TEST_KEY = "test_key"
        private const val EXTRA_TEST_VALUE = "test_value"
    }
}
