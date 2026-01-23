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

import android.content.Context
import android.view.View
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.android.settings.R
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupdesign.GlifLayout
import com.google.android.setupdesign.GlifRecyclerLayout
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests for [TextReadingSetupWizardFragment]. */
@RunWith(RobolectricTestRunner::class)
class TextReadingSetupWizardFragmentTest {

    private val appContext: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun onViewCreated_hasCorrectTitle() {
        launchFragment().use { scenario ->
            scenario.onFragment { fragment ->
                val layout =
                    fragment
                        .requireView()
                        .findViewById<GlifRecyclerLayout>(R.id.text_reading_suw_screen_layout)

                val headerText = layout.headerTextView.text
                assertThat(headerText)
                    .isEqualTo(
                        appContext.getString(R.string.accessibility_text_reading_options_title)
                    )
            }
        }
    }

    @Test
    fun footerButtons_areInitializedCorrect() {
        launchFragment().use { scenario ->
            scenario.onFragment { fragment ->
                val layout =
                    fragment
                        .requireView()
                        .findViewById<GlifLayout>(R.id.text_reading_suw_screen_layout)
                val mixin = layout.getMixin(FooterBarMixin::class.java)

                assertThat(mixin.primaryButton.text).isEqualTo(appContext.getString(R.string.done))
                assertThat(mixin.primaryButtonView.visibility).isEqualTo(View.VISIBLE)
                assertThat(mixin.secondaryButton.text)
                    .isEqualTo(
                        appContext.getString(R.string.accessibility_text_reading_reset_button_title)
                    )
                assertThat(mixin.secondaryButtonView.visibility).isEqualTo(View.VISIBLE)
            }
        }
    }

    @Test
    fun clickDoneButton_popsBackStack() {
        launchFragment().use { scenario ->
            scenario.onFragment { fragment ->
                val fragmentManager = fragment.parentFragmentManager
                fragmentManager.beginTransaction().addToBackStack("test_state").commit()
                fragmentManager.executePendingTransactions()
                val initialCount = fragmentManager.backStackEntryCount

                val layout =
                    fragment
                        .requireView()
                        .findViewById<GlifLayout>(R.id.text_reading_suw_screen_layout)
                val mixin = layout.getMixin(FooterBarMixin::class.java)
                mixin.primaryButtonView.performClick()
                fragmentManager.executePendingTransactions()

                assertThat(fragmentManager.backStackEntryCount).isEqualTo(initialCount - 1)
            }
        }
    }

    @Test
    fun clickResetButton_showsConfirmDialogWithCorrectContent() {
        launchFragment().use {
            onView(withText(R.string.accessibility_text_reading_reset_button_title))
                .perform(click())

            onView(withText(R.string.accessibility_text_reading_confirm_dialog_title))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
            onView(withText(R.string.accessibility_text_reading_confirm_dialog_message))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
        }
    }

    private fun launchFragment(): FragmentScenario<TextReadingSetupWizardFragment> =
        launchFragmentInContainer<TextReadingSetupWizardFragment>(themeResId = R.style.GlifTheme)
}
