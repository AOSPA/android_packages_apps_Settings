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
import com.android.settings.R
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.google.android.setupdesign.items.Item
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowLooper

/** Tests for [AccessibilityServiceFooterItemController]. */
@RunWith(RobolectricTestRunner::class)
class AccessibilityServiceFooterItemControllerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val mockItem = mock<Item>()
    private val a11yManager: ShadowAccessibilityManager =
        Shadow.extract(context.getSystemService(AccessibilityManager::class.java))

    private lateinit var serviceInfo: AccessibilityServiceInfo
    private lateinit var controller: AccessibilityServiceFooterItemController

    @Before
    fun setUp() {
        serviceInfo =
            createMockServiceInfo(context, TEST_COMPONENT_NAME, TEST_LABEL, TEST_SUMMARY).stub {
                on { loadHtmlDescription(any()) } doReturn TEST_SUMMARY
            }
        a11yManager.setInstalledAccessibilityServiceList(listOf(serviceInfo))
        controller = AccessibilityServiceFooterItemController(context, serviceInfo, mockItem)
    }

    @Test
    fun bindData_setsSummaryAndContentDescription() {
        val expectedSummary = TEST_SUMMARY
        val introTitle = context.getString(R.string.accessibility_introduction_title, TEST_LABEL)
        val expectedContentDescription = "$introTitle\n\n$TEST_SUMMARY"

        controller.bindData(mockItem)
        ShadowLooper.idleMainLooper()

        val summaryCaptor = argumentCaptor<CharSequence>()
        verify(mockItem).summary = summaryCaptor.capture()
        assertThat(summaryCaptor.firstValue.toString()).isEqualTo(expectedSummary)
        val contentDescCaptor = argumentCaptor<CharSequence>()
        verify(mockItem).contentDescription = contentDescCaptor.capture()
        assertThat(contentDescCaptor.firstValue.toString()).isEqualTo(expectedContentDescription)
    }

    companion object {
        private val TEST_COMPONENT_NAME = ComponentName("com.test.pkg", ".TestService")
        private const val TEST_LABEL = "Test Service"
        private const val TEST_SUMMARY = "This is a test summary description."
    }
}
