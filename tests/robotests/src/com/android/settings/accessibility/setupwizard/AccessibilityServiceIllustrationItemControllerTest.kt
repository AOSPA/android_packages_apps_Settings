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
import android.net.Uri
import android.os.Looper
import android.view.accessibility.AccessibilityManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.setupwizard.items.IllustrationItem
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow

/** Tests for [AccessibilityServiceIllustrationItemController]. */
@RunWith(RobolectricTestRunner::class)
class AccessibilityServiceIllustrationItemControllerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val illustrationItem: IllustrationItem = IllustrationItem()
    private val a11yManager: ShadowAccessibilityManager =
        Shadow.extract(context.getSystemService(AccessibilityManager::class.java))

    private lateinit var serviceInfo: AccessibilityServiceInfo
    private lateinit var controller: AccessibilityServiceIllustrationItemController

    @Before
    fun setUp() {
        serviceInfo =
            createMockServiceInfo(context, TEST_COMPONENT_NAME, TEST_LABEL, TEST_SUMMARY).stub {
                on { animatedImageRes } doReturn TEST_IMAGE_RES
            }
        a11yManager.setInstalledAccessibilityServiceList(listOf(serviceInfo))
        controller =
            AccessibilityServiceIllustrationItemController(context, serviceInfo, illustrationItem)
    }

    @Test
    fun bindData_setsImageUriAndContentDescription() {
        val expectedSummary =
            context.getString(R.string.accessibility_illustration_content_description, TEST_LABEL)
        val expectedUri =
            Uri.Builder()
                .scheme("android.resource")
                .authority(TEST_COMPONENT_NAME.packageName)
                .appendPath(TEST_IMAGE_RES.toString())
                .build()

        controller.bindData(illustrationItem)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(illustrationItem.imageUri).isEqualTo(expectedUri)
        assertThat(illustrationItem.contentDescription?.toString()).isEqualTo(expectedSummary)
    }

    companion object {
        private val TEST_COMPONENT_NAME = ComponentName("com.test.pkg", ".TestService")
        private const val TEST_LABEL = "Test Service"
        private const val TEST_SUMMARY = "This is a test summary description."
        private val TEST_IMAGE_RES = 101
    }
}
