/*
 * Copyright (C) 2026 The Android Open Source Project
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

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityManager
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupdesign.GlifLayout
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadow.api.Shadow

/** Tests for [AccessibilityServiceSetupWizardFragment]. */
@RunWith(RobolectricTestRunner::class)
class AccessibilityServiceSetupWizardFragmentTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val a11yManager: ShadowAccessibilityManager =
        Shadow.extract(context.getSystemService(AccessibilityManager::class.java))

    @Before
    fun setUp() {
        val serviceInfo =
            createMockServiceInfo(context, TEST_COMPONENT_NAME, TEST_LABEL, TEST_SUMMARY)
        a11yManager.setInstalledAccessibilityServiceList(listOf(serviceInfo))
    }

    @After
    fun cleanUp() {
        AccessibilityRepositoryProvider.resetInstanceForTesting()
    }

    @Test
    fun onCreateView_setsHeaderAndDescriptionWithArgs() {
        launchFragment(createTestArgs()).use { scenario ->
            scenario.onFragment { fragment ->
                val layout = fragment.findGlifLayout()
                assertThat(layout.headerText.toString()).isEqualTo(TEST_LABEL)
                assertThat(layout.descriptionText.toString()).isEqualTo(TEST_SUMMARY)
            }
        }
    }

    @Test
    fun footerButtons_areInitializedCorrectly() {
        launchFragment(createTestArgs()).use { scenario ->
            scenario.onFragment { fragment ->
                val mixin = fragment.findGlifLayout().getMixin(FooterBarMixin::class.java)
                assertThat(mixin.primaryButton?.text?.toString())
                    .isEqualTo(context.getString(R.string.done))
            }
        }
    }

    @Test
    fun clickDoneButton_popsBackStack() {
        launchFragment(createTestArgs()).use { scenario ->
            scenario.onFragment { fragment ->
                val fragmentManager = fragment.parentFragmentManager
                fragmentManager.beginTransaction().addToBackStack("test_state").commit()
                fragmentManager.executePendingTransactions()
                val initialCount = fragmentManager.backStackEntryCount

                val mixin = fragment.findGlifLayout().getMixin(FooterBarMixin::class.java)
                mixin.primaryButtonView.performClick()
                fragmentManager.executePendingTransactions()

                assertThat(fragmentManager.backStackEntryCount).isEqualTo(initialCount - 1)
            }
        }
    }

    private fun createTestArgs() =
        Bundle().apply {
            putParcelable(
                AccessibilityServiceSetupWizardFragment.ARG_COMPONENT_NAME,
                TEST_COMPONENT_NAME,
            )
            putString(AccessibilityServiceSetupWizardFragment.ARG_DESCRIPTION, TEST_SUMMARY)
        }

    private fun AccessibilityServiceSetupWizardFragment.findGlifLayout(): GlifLayout =
        requireView().findViewById(R.id.accessibility_service_suw_screen_layout)

    private fun launchFragment(args: Bundle? = null) =
        launchFragmentInContainer<AccessibilityServiceSetupWizardFragment>(
            fragmentArgs = args,
            themeResId = R.style.GlifTheme,
        )

    companion object {
        private val TEST_COMPONENT_NAME = ComponentName("com.test.pkg", ".TestService")
        private const val TEST_LABEL = "Test Service"
        private const val TEST_SUMMARY = "This is a test summary description."
    }
}
