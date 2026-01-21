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

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.android.settings.EmptySetupWizardActivity
import com.android.settings.accessibility.a11yservice.ui.UseServicePreference
import com.android.settings.accessibility.setupwizard.items.MainSwitchItem
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadow.api.Shadow

/** Tests for [AccessibilityServiceMainSwitchItemController]. */
@RunWith(RobolectricTestRunner::class)
class AccessibilityServiceMainSwitchItemControllerTest {

    @get:Rule val activityScenarioRule = ActivityScenarioRule(EmptySetupWizardActivity::class.java)

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val mainSwitchItem: MainSwitchItem = MainSwitchItem()
    private val mockDataStore = mock<KeyValueStore>()
    private val a11yManager: ShadowAccessibilityManager =
        Shadow.extract(context.getSystemService(AccessibilityManager::class.java))

    private lateinit var serviceInfo: AccessibilityServiceInfo
    private lateinit var controller: AccessibilityServiceMainSwitchItemController

    @Before
    fun setUp() {
        serviceInfo =
            createMockServiceInfo(context, TEST_COMPONENT_NAME, TEST_LABEL, TEST_SUMMARY).stub {
                on { loadHtmlDescription(any()) } doReturn TEST_SUMMARY
            }
        a11yManager.setInstalledAccessibilityServiceList(listOf(serviceInfo))
        controller =
            AccessibilityServiceMainSwitchItemController(
                context,
                serviceInfo,
                mainSwitchItem,
                useServiceMetadataDataStore = mockDataStore,
            )
    }

    @Test
    fun onStart_registersObserverWithCorrectKey() {
        controller.onStart()

        verify(mockDataStore)
            .addObserver(eq(UseServicePreference.KEY), any<KeyedObserver<String>>(), any())
    }

    @Test
    fun onStop_removesRegisteredObserver() {
        val captor = argumentCaptor<KeyedObserver<String>>()
        controller.onStart()
        verify(mockDataStore).addObserver(any(), captor.capture(), any())
        val capturedObserver = captor.firstValue

        controller.onStop()

        verify(mockDataStore).removeObserver(eq(UseServicePreference.KEY), eq(capturedObserver))
    }

    @Test
    fun onStop_withoutStart_doesNotAttemptRemoval() {
        controller.onStop()
        verify(mockDataStore, never()).removeObserver(any(), any())
    }

    @Test
    fun bindData_setsInitialSwitchStateFromDataStore() {
        mockDataStore.stub { on { getBoolean(eq(UseServicePreference.KEY)) } doReturn true }

        controller.bindData(mainSwitchItem)

        assertThat(mainSwitchItem.isChecked).isTrue()
    }

    @Test
    fun onItemSelected_togglesSwitchState() {
        activityScenarioRule.scenario.onActivity { activity ->
            mainSwitchItem.isChecked = false

            controller.onItemSelected(activity)

            verify(mockDataStore).setBoolean(eq(UseServicePreference.KEY), eq(true))
        }
    }

    @Test
    fun dataStoreChange_triggersRebind() {
        val captor = argumentCaptor<KeyedObserver<String>>()
        controller.onStart()
        verify(mockDataStore).addObserver(any(), captor.capture(), any())

        captor.firstValue.onKeyChanged(UseServicePreference.KEY, 0)

        verify(mockDataStore, atLeastOnce()).getBoolean(eq(UseServicePreference.KEY))
    }

    companion object {
        private val TEST_COMPONENT_NAME = ComponentName("com.test.pkg", ".TestService")
        private const val TEST_LABEL = "Test Service"
        private const val TEST_SUMMARY = "This is a test summary description."
    }
}
