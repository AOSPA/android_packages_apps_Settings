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

import android.content.ComponentName
import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.android.settings.EmptySetupWizardActivity
import com.android.settings.R
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.google.android.setupdesign.items.Item
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowLooper

/** Tests for [AccessibilityServiceItemController]. */
@RunWith(RobolectricTestRunner::class)
class AccessibilityServiceItemControllerTest {

    @get:Rule val activityScenarioRule = ActivityScenarioRule(EmptySetupWizardActivity::class.java)

    private val mockItem = mock<Item>()
    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val a11yManager: ShadowAccessibilityManager =
        Shadow.extract(appContext.getSystemService(AccessibilityManager::class.java))
    private val controller: AccessibilityServiceItemController =
        AccessibilityServiceItemController(appContext, mockItem, TEST_COMPONENT_NAME)

    @Before
    fun setUp() {
        a11yManager.setInstalledAccessibilityServiceList(emptyList())
    }

    @After
    fun cleanUp() {
        AccessibilityRepositoryProvider.resetInstanceForTesting()
    }

    @Test
    fun bindData_serviceInstalled_setsTitleAndIcon() {
        val serviceInfo =
            createMockServiceInfo(
                appContext,
                ComponentName.unflattenFromString(TEST_COMPONENT_NAME)!!,
                TEST_LABEL,
                TEST_SUMMARY,
            )
        a11yManager.setInstalledAccessibilityServiceList(listOf(serviceInfo))

        controller.bindData(mockItem)
        ShadowLooper.idleMainLooper()

        verify(mockItem).title = argThat { this.toString() == TEST_LABEL }
        verify(mockItem).isVisible = true
        verify(mockItem).notifyItemChanged()
    }

    @Test
    fun bindData_noServicesInstalled_hidesItems() {
        controller.bindData(mockItem)
        ShadowLooper.idleMainLooper()

        verify(mockItem).isVisible = false
        verify(mockItem).notifyItemChanged()
    }

    @Test
    fun onItemSelected_navigatesToAccessibilityServiceSetupWizardFragmentWithCorrectArgs() {
        val testComponentName = ComponentName.unflattenFromString(TEST_COMPONENT_NAME)!!
        val serviceInfo =
            createMockServiceInfo(appContext, testComponentName, TEST_LABEL, TEST_SUMMARY)
        a11yManager.setInstalledAccessibilityServiceList(listOf(serviceInfo))
        mockItem.stub { on { summary } doReturn TEST_SUMMARY }
        controller.bindData(mockItem)
        ShadowLooper.idleMainLooper()
        activityScenarioRule.scenario.onActivity { activity ->
            activity.setContentView(R.layout.accessibility_suw_activity)

            controller.onItemSelected(activity)
            activity.supportFragmentManager.executePendingTransactions()

            val currentFragment =
                activity.supportFragmentManager.findFragmentById(R.id.fragment_container)
            assertThat(currentFragment)
                .isInstanceOf(AccessibilityServiceSetupWizardFragment::class.java)
            val args = currentFragment?.arguments
            assertThat(
                    args?.getParcelable(
                        AccessibilityServiceSetupWizardFragment.ARG_COMPONENT_NAME,
                        ComponentName::class.java,
                    )
                )
                .isEqualTo(testComponentName)
            assertThat(args?.getString(AccessibilityServiceSetupWizardFragment.ARG_DESCRIPTION))
                .isEqualTo(TEST_SUMMARY)
        }
    }

    companion object {
        private const val TEST_COMPONENT_NAME = "com.test/.Service"
        private const val TEST_LABEL = "Test Service"
        private const val TEST_SUMMARY = "This is a test summary description."
    }
}
