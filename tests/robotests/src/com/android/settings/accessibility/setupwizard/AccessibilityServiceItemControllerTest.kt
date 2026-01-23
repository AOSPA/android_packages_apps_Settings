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

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Looper
import android.view.accessibility.AccessibilityManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.testutils.AccessibilityTestUtils
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.google.android.setupdesign.items.Item
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow

/** Tests for [AccessibilityServiceItemController]. */
@RunWith(RobolectricTestRunner::class)
class AccessibilityServiceItemControllerTest {

    private val mockItem = mock<Item>()

    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val a11yManager: ShadowAccessibilityManager =
        Shadow.extract(appContext.getSystemService(AccessibilityManager::class.java))
    private lateinit var controller: AccessibilityServiceItemController

    @Before
    fun setUp() {
        a11yManager.setInstalledAccessibilityServiceList(emptyList())
        controller = AccessibilityServiceItemController(appContext, mockItem, TEST_COMPONENT_NAME)
    }

    @Test
    fun bindData_serviceInstalled_setsTitleAndIcon() {
        val serviceInfo =
            createA11yServiceInfo(
                serviceComponent = ComponentName.unflattenFromString(TEST_COMPONENT_NAME)!!,
                label = TEST_LABEL,
            )
        a11yManager.setInstalledAccessibilityServiceList(listOf(serviceInfo))

        controller.bindData(mockItem)
        shadowOf(Looper.getMainLooper()).idle()

        verify(mockItem).title = TEST_LABEL
        verify(mockItem).isVisible = true
        verify(mockItem).notifyItemChanged()
    }

    @Test
    fun bindData_noServicesInstalled_hidesItems() {
        controller.bindData(mockItem)
        shadowOf(Looper.getMainLooper()).idle()

        verify(mockItem).isVisible = false
        verify(mockItem).notifyItemChanged()
    }

    private fun createA11yServiceInfo(
        isAlwaysOnService: Boolean = false,
        serviceComponent: ComponentName,
        label: String,
    ): AccessibilityServiceInfo =
        AccessibilityTestUtils.createAccessibilityServiceInfo(
                appContext,
                serviceComponent,
                isAlwaysOnService,
            )
            .apply {
                isAccessibilityTool = true
                resolveInfo =
                    resolveInfo?.let {
                        spy(it).apply {
                            doReturn(label).whenever(this).loadLabel(any())
                            doReturn(ColorDrawable(0)).whenever(this).loadIcon(any())
                        }
                    }
            }

    companion object {
        private const val TEST_COMPONENT_NAME = "com.test/.Service"
        private const val TEST_LABEL = "Test Service"
    }
}
