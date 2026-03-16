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
import android.os.UserHandle
import android.view.accessibility.AccessibilityManager
import androidx.test.core.app.ApplicationProvider
import com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME
import com.android.settings.R
import com.android.settings.accessibility.setupwizard.items.IllustrationCheckBoxItem
import com.android.settings.testutils.AccessibilityTestUtils
import com.android.settings.testutils.AccessibilityTestUtils.setupMockAccessibilityManager
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.robolectric.RobolectricTestRunner

/** Tests for [EditQuickSettingsShortcutController]. */
@RunWith(RobolectricTestRunner::class)
class EditQuickSettingsShortcutControllerTest {

    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private val appContext: Context = spy(value = ApplicationProvider.getApplicationContext())
    private val a11yManager: AccessibilityManager = setupMockAccessibilityManager(appContext)
    private val item = IllustrationCheckBoxItem()

    @Before
    fun setUp() {
        SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_quickSettingsSupported,
            true,
        )
    }

    @Test
    fun bindData_setsSummary() {
        val controller = createController(setOf(MAGNIFICATION_CONTROLLER_NAME))

        controller.bindData(item)

        assertThat(item.summary).isNotNull()
    }

    @Test
    fun bindData_setsExpectedResId() {
        val controller = createController(setOf(MAGNIFICATION_CONTROLLER_NAME))

        controller.bindData(item)

        assertThat(item.imageResId).isEqualTo(R.drawable.accessibility_shortcut_type_quick_settings)
    }

    @Test
    fun bindData_qsNotSupported_hidesItem() {
        SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_quickSettingsSupported,
            false,
        )
        val controller = createController(setOf(TARGET_FLATTEN))

        controller.bindData(item)

        assertThat(item.isVisible).isFalse()
    }

    @Test
    fun bindData_targetHasNoTile_hidesItem() {
        a11yManager.stub {
            on { getA11yFeatureToTileMap(UserHandle.myUserId()) } doReturn emptyMap()
        }
        val controller = createController(setOf(TARGET_FLATTEN))

        controller.bindData(item)

        assertThat(item.isVisible).isFalse()
    }

    @Test
    fun bindData_targetIsStandardA11yService_hidesItem() {
        val mockStandardA11yService =
            AccessibilityTestUtils.createAccessibilityServiceInfo(
                appContext,
                TARGET,
                /* isAlwaysOnService= */ false,
            )
        a11yManager.stub {
            on { getA11yFeatureToTileMap(UserHandle.myUserId()) } doReturn
                mapOf(TARGET to TARGET_TILE)
            on { getInstalledAccessibilityServiceList() } doReturn listOf(mockStandardA11yService)
        }
        val controller = createController(setOf(TARGET_FLATTEN))

        controller.bindData(item)

        assertThat(item.isVisible).isFalse()
    }

    @Test
    fun bindData_targetIsAlwaysOnA11yService_showsItem() {
        val mockAlwaysOnA11yService =
            AccessibilityTestUtils.createAccessibilityServiceInfo(
                appContext,
                TARGET,
                /* isAlwaysOnService= */ true,
            )
        a11yManager.stub {
            on { getA11yFeatureToTileMap(UserHandle.myUserId()) } doReturn
                mapOf(TARGET to TARGET_TILE)
            on { getInstalledAccessibilityServiceList() } doReturn listOf(mockAlwaysOnA11yService)
        }
        val controller = createController(setOf(TARGET_FLATTEN))

        controller.bindData(item)

        assertThat(item.isVisible).isTrue()
    }

    @Test
    fun bindData_multipleValidTargets_showsItem() {
        val mockAlwaysOnA11yService =
            AccessibilityTestUtils.createAccessibilityServiceInfo(
                appContext,
                TARGET,
                /* isAlwaysOnService= */ true,
            )
        val mockAlwaysOnA11yService2 =
            AccessibilityTestUtils.createAccessibilityServiceInfo(
                appContext,
                TARGET2,
                /* isAlwaysOnService= */ true,
            )
        a11yManager.stub {
            on { getA11yFeatureToTileMap(UserHandle.myUserId()) } doReturn
                mapOf(TARGET to TARGET_TILE, TARGET2 to TARGET2_TILE)
            on { getInstalledAccessibilityServiceList() } doReturn
                listOf(mockAlwaysOnA11yService, mockAlwaysOnA11yService2)
        }
        val controller = createController(setOf(TARGET2_FLATTEN))

        controller.bindData(item)

        assertThat(item.isVisible).isTrue()
    }

    @Test
    fun bindData_multipleTargetsWithOneInvalidUseCase_hidesItem() {
        val mockAlwaysOnA11yService =
            AccessibilityTestUtils.createAccessibilityServiceInfo(
                appContext,
                TARGET,
                /* isAlwaysOnService= */ true,
            )
        val mockStandardA11yService =
            AccessibilityTestUtils.createAccessibilityServiceInfo(
                appContext,
                TARGET2,
                /* isAlwaysOnService= */ false,
            )

        a11yManager.stub {
            on { getA11yFeatureToTileMap(UserHandle.myUserId()) } doReturn
                mapOf(TARGET to TARGET_TILE, TARGET2 to TARGET2_TILE)
            on { getInstalledAccessibilityServiceList() } doReturn
                listOf(mockAlwaysOnA11yService, mockStandardA11yService)
        }
        val controller = createController(setOf(TARGET_FLATTEN, TARGET2_FLATTEN))

        controller.bindData(item)

        assertThat(item.isVisible).isFalse()
    }

    /** Creates the controller and its associated store. */
    private fun createController(targets: Set<String>) =
        EditQuickSettingsShortcutController.create(appContext, item, targets)

    companion object {
        private val TARGET: ComponentName = ComponentName("FakePackage", "FakeClass")
        private val TARGET_FLATTEN: String = TARGET.flattenToString()
        private val TARGET_TILE: ComponentName = ComponentName("FakePackage", "FakeTileClass")
        private val TARGET2: ComponentName = ComponentName("FakePackage", "FakeClass2")
        private val TARGET2_FLATTEN: String = TARGET2.flattenToString()
        private val TARGET2_TILE: ComponentName = ComponentName("FakePackage", "FakeTileClass2")
    }
}
