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
import com.android.settings.R
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupdesign.GlifLayout
import com.google.android.setupdesign.GlifRecyclerLayout
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests for [ColorInversionSetupWizardFragment]. */
@RunWith(RobolectricTestRunner::class)
class ColorInversionSetupWizardFragmentTest {

    private val appContext: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun onViewCreated_hasCorrectTitle() {
        launchFragment().use { scenario ->
            scenario.onFragment { fragment ->
                val layout =
                    fragment
                        .requireView()
                        .findViewById<GlifRecyclerLayout>(R.id.color_inversion_suw_screen_layout)

                val headerText = layout.headerTextView.text
                assertThat(headerText)
                    .isEqualTo(
                        appContext.getString(
                            R.string.accessibility_display_inversion_preference_title
                        )
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
                        .findViewById<GlifLayout>(R.id.color_inversion_suw_screen_layout)

                val mixin = layout.getMixin(FooterBarMixin::class.java)
                assertThat(mixin.primaryButton.text).isEqualTo(appContext.getString(R.string.done))
                assertThat(mixin.primaryButtonView.visibility).isEqualTo(View.VISIBLE)
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
                        .findViewById<GlifLayout>(R.id.color_inversion_suw_screen_layout)
                val mixin = layout.getMixin(FooterBarMixin::class.java)

                mixin.primaryButtonView.performClick()
                fragmentManager.executePendingTransactions()

                assertThat(fragmentManager.backStackEntryCount).isEqualTo(initialCount - 1)
            }
        }
    }

    private fun launchFragment(): FragmentScenario<ColorInversionSetupWizardFragment> =
        launchFragmentInContainer<ColorInversionSetupWizardFragment>(themeResId = R.style.GlifTheme)
}
