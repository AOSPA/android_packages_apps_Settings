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

package com.android.settings.accessibility.shared.utils

import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.AccessibilityShortcutInfo
import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.view.accessibility.AccessibilityManager
import androidx.test.core.app.ApplicationProvider
import com.android.internal.accessibility.AccessibilityShortcutController.COLOR_INVERSION_COMPONENT_NAME
import com.android.settings.R
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.android.settings.testutils.AccessibilityTestUtils
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadow.api.Shadow

@RunWith(RobolectricTestRunner::class)
class AccessibilityFeaturesUtilsTest {
    private val appContext = ApplicationProvider.getApplicationContext<Context>()
    private val a11yManager: ShadowAccessibilityManager =
        Shadow.extract(appContext.getSystemService(AccessibilityManager::class.java))

    @Before
    fun setUp() {
        val mockInfo = createA11yActivity()
        val serviceInfo = createA11yServiceInfo()
        a11yManager.run {
            setInstalledAccessibilityShortcutListAsUser(listOf(mockInfo))
            setInstalledAccessibilityServiceList(listOf(serviceInfo))
        }
    }

    @After
    fun cleanUp() {
        AccessibilityRepositoryProvider.resetInstanceForTesting()
    }

    @Test
    fun getAccessibilityFeatureName_frameworkFeature_returnsCorrectName() {
        val feature = COLOR_INVERSION_COMPONENT_NAME.flattenToString()
        val expectedName =
            appContext.getString(R.string.accessibility_display_inversion_preference_title)

        val actualName = getAccessibilityFeatureName(appContext, feature)

        assertThat(actualName).isEqualTo(expectedName)
    }

    @Test
    fun getAccessibilityFeatureName_unknownFeature_returnsNull() {
        val feature = "unknown"

        val actualName = getAccessibilityFeatureName(appContext, feature)

        assertThat(actualName).isNull()
    }

    @Test
    fun getAccessibilityFeatureName_a11yServiceFeature_returnCorrectName() {
        val feature = A11Y_SERVICE_COMPONENT.flattenToString()
        val expectedName = A11Y_SERVICE_COMPONENT.shortClassName

        val actualName = getAccessibilityFeatureName(appContext, feature)

        assertThat(actualName.toString()).isEqualTo(expectedName)
    }

    @Test
    fun getAccessibilityFeatureName_a11yActivityFeature_returnCorrectName() {
        val feature = A11Y_ACTIVITY_COMPONENT.flattenToString()

        val actualName = getAccessibilityFeatureName(appContext, feature)

        assertThat(actualName.toString()).isEqualTo(DEFAULT_LABEL)
    }

    @Test
    fun getA11yServiceFeatureName_validFeature_returnsCorrectName() {
        val feature = A11Y_SERVICE_COMPONENT.flattenToString()
        val expectedName = A11Y_SERVICE_COMPONENT.shortClassName

        val actualName = getA11yServiceFeatureName(appContext, feature)

        assertThat(actualName.toString()).isEqualTo(expectedName)
    }

    @Test
    fun getA11yServiceFeatureName_unknownFeature_returnsNull() {
        val feature = A11Y_SERVICE_COMPONENT.flattenToString() + "2"

        val actualName = getA11yServiceFeatureName(appContext, feature)

        assertThat(actualName).isNull()
    }

    @Test
    fun getA11yActivityFeatureName_validFeature_returnsCorrectName() {
        val feature = A11Y_ACTIVITY_COMPONENT.flattenToString()
        val expectedName = DEFAULT_LABEL

        val actualName = getA11yActivityFeatureName(appContext, feature)

        assertThat(actualName.toString()).isEqualTo(expectedName)
    }

    @Test
    fun getA11yActivityFeatureName_unknownFeature_returnsNull() {
        val feature = A11Y_ACTIVITY_COMPONENT.flattenToString() + "2"

        val actualName = getA11yActivityFeatureName(appContext, feature)

        assertThat(actualName).isNull()
    }

    private fun createA11yServiceInfo(): AccessibilityServiceInfo {
        return AccessibilityTestUtils.createAccessibilityServiceInfo(
                appContext,
                A11Y_SERVICE_COMPONENT,
                false,
            )
            .apply { isAccessibilityTool = true }
    }

    private fun createA11yActivity(): AccessibilityShortcutInfo {
        val mockInfo: AccessibilityShortcutInfo = mock {
            on { componentName } doReturn A11Y_ACTIVITY_COMPONENT
        }
        val activityInfo =
            mock<ActivityInfo>().apply {
                packageName = PACKAGE_NAME
                name = A11Y_ACTIVITY_COMPONENT.className
                applicationInfo = ApplicationInfo()
            }
        whenever(activityInfo.loadLabel(any())).thenReturn(DEFAULT_LABEL)
        whenever(mockInfo.activityInfo).thenReturn(activityInfo)
        return mockInfo
    }

    companion object {
        private const val PACKAGE_NAME = "com.foo.bar"
        private val A11Y_SERVICE_COMPONENT = ComponentName(PACKAGE_NAME, "FakeA11yService")
        private val A11Y_ACTIVITY_COMPONENT = ComponentName(PACKAGE_NAME, "FakeA11yActivity")

        private const val DEFAULT_LABEL = "default label"
    }
}
