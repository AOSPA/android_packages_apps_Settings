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

package com.android.settings.accessibility.a11yservice.data

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.view.accessibility.AccessibilityManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import com.android.settingslib.metadata.preferencesapi.safe
import com.android.settingslib.metadata.preferencesapi.unsafe

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowAccessibilityManager::class])
class AccessibilityServiceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var a11yManager: ShadowAccessibilityManager

    @Before
    fun setUp() {
        a11yManager = Shadow.extract(context.getSystemService(AccessibilityManager::class.java))
    }

    @After
    fun tearDown() {
        AccessibilityRepositoryProvider.resetInstanceForTesting()
    }

    @Test
    fun getOptions_returnsExpectedList() = runTest {
        val componentName = ComponentName("com.example", "com.example.Service")
        val info = createMockAccessibilityServiceInfo(componentName)
        a11yManager.setInstalledAccessibilityServiceList(listOf(info))

        val options = AccessibilityService.getOptions(context)

        assertThat(options).hasSize(1)
        assertThat(options[0].first).isEqualTo(componentName.flattenToString().safe())
        assertThat(options[0].second).isEqualTo("An intro".unsafe())
    }

    @Test
    fun getType_returnsStringClass() {
        assertThat(AccessibilityService.getType()).isEqualTo(String::class.java)
    }

    @Test
    fun getKey_returnsCorrectKey() {
        assertThat(AccessibilityService.getKey()).isEqualTo("AccessibilityService")
    }

    @Test
    fun getDescription_returnsCorrectDescription() {
        assertThat(AccessibilityService.getDescription(context))
            .isEqualTo("The flattened string representation of an AccessibilityService implementing an accessibility feature")
    }

    private fun createMockAccessibilityServiceInfo(componentName: ComponentName): AccessibilityServiceInfo {
        val info = mock<AccessibilityServiceInfo>()
        val resolveInfo = ResolveInfo()
        val serviceInfo = ServiceInfo()
        serviceInfo.packageName = componentName.packageName
        serviceInfo.name = componentName.className
        serviceInfo.applicationInfo = ApplicationInfo()
        resolveInfo.serviceInfo = serviceInfo
        whenever(info.resolveInfo).thenReturn(resolveInfo)
        whenever(info.componentName).thenReturn(componentName)
        whenever(info.loadIntro(any())).thenReturn("An intro")
        return info
    }
}
