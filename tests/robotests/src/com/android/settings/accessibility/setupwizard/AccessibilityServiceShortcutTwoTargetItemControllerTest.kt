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

import android.content.ComponentName
import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.android.settings.accessibility.setupwizard.items.TwoTargetItem
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowLooper

/** Tests for [AccessibilityServiceShortcutTwoTargetItemController]. */
@RunWith(RobolectricTestRunner::class)
class AccessibilityServiceShortcutTwoTargetItemControllerTest {

    private val item = TwoTargetItem()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val a11yManager: ShadowAccessibilityManager =
        Shadow.extract(context.getSystemService(AccessibilityManager::class.java))

    @Before
    fun setUp() {
        a11yManager.setInstalledAccessibilityServiceList(emptyList())
    }

    @After
    fun cleanUp() {
        AccessibilityRepositoryProvider.resetInstanceForTesting()
    }

    @Test
    fun bindData_setsItemPropertiesFromMetadata() {
        val serviceInfo =
            createMockServiceInfo(
                context,
                ComponentName.unflattenFromString(TEST_COMPONENT_NAME)!!,
                TEST_LABEL,
                UNUSED_SUMMARY,
            )
        a11yManager.setInstalledAccessibilityServiceList(listOf(serviceInfo))
        val controller =
            AccessibilityServiceShortcutTwoTargetItemController.create(context, item, serviceInfo)

        controller.bindData(item)
        ShadowLooper.idleMainLooper()

        assertThat(item.title.toString()).contains(TEST_LABEL)
    }

    companion object {
        private const val TEST_COMPONENT_NAME = "com.test/.Service"
        private const val TEST_LABEL = "Test Service"
        private const val UNUSED_SUMMARY = "unused"
    }
}
