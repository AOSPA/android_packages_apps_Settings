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

package com.android.settings.accessibility.shortcuts.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.AccessibilityShortcutInfo
import android.app.settings.SettingsEnums
import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.accessibility.AccessibilityManager
import com.android.internal.accessibility.AccessibilityShortcutController.COLOR_INVERSION_COMPONENT_NAME
import com.android.internal.accessibility.AccessibilityShortcutController.DALTONIZER_COMPONENT_NAME
import com.android.settings.R
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.android.settings.accessibility.extensions.getFeatureName
import com.android.settings.accessibility.shortcuts.EditShortcutsPreferenceFragment
import com.android.settings.accessibility.shortcuts.ui.EditShortcutsScreen.Companion.FEATURE_SHORTCUT_TITLE_MAP
import com.android.settings.testutils.AccessibilityTestUtils
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.shadow.api.Shadow

/** Tests for [EditShortcutsScreen]. */
class EditShortcutsScreenTest : SettingsCatalystTestCase() {
    @get:Rule val settingStoreRule = SettingsStoreRule()
    private val a11yManager: ShadowAccessibilityManager =
        Shadow.extract(appContext.getSystemService(AccessibilityManager::class.java))

    override val flagName: String
        get() = Flags.FLAG_CATALYST_EDIT_SHORTCUTS

    private val arguments =
        Bundle().apply {
            putStringArray(
                EditShortcutsPreferenceFragment.ARG_KEY_SHORTCUT_TARGETS,
                arrayOf(COLOR_INVERSION_COMPONENT_NAME.flattenToString()),
            )
        }

    override val preferenceScreenCreator: EditShortcutsScreen by lazy {
        EditShortcutsScreen(appContext, arguments)
    }

    @After
    fun cleanUp() {
        AccessibilityRepositoryProvider.resetInstanceForTesting()
    }

    override fun migration() {
        // TODO(b/440383851): temporarily don't run the test until we're fully migrated.
    }

    @Test
    fun highlightMenuKey() {
        assertThat(preferenceScreenCreator.highlightMenuKey)
            .isEqualTo(R.string.menu_key_accessibility)
    }

    @Test
    fun getMetricsCategory() {
        assertThat(preferenceScreenCreator.metricsCategory)
            .isEqualTo(SettingsEnums.DIALOG_ACCESSIBILITY_SERVICE_EDIT_SHORTCUT)
    }

    @Test
    fun getLaunchIntent_returnNull() {
        assertThat(preferenceScreenCreator.getLaunchIntent(appContext, null)).isNull()
    }

    @Test
    fun getScreenTitle_frameworkFeature_returnCorrectShortcutTitle() {
        // Unable to use parameterized test, due to the framework features' component names are
        // store in the internal class that can't be accessed with the parameterized test where its
        // initialized before the Robolectric test environment are setup. Hence it can't access
        // internal hidden fields. You'll see NoClassDefFoundError.
        for (entry in FEATURE_SHORTCUT_TITLE_MAP) {
            assertEditShortcutScreenTitleForSingleFeature(
                target = entry.key,
                expectedTitle = appContext.getString(entry.value),
            )
        }
    }

    @Test
    fun getScreenTitle_multipleTargets_returnGeneralTitle() {
        val screen =
            createScreen(
                arrayOf(
                    COLOR_INVERSION_COMPONENT_NAME.flattenToString(),
                    DALTONIZER_COMPONENT_NAME.flattenToString(),
                )
            )
        assertThat(screen.getScreenTitle(appContext))
            .isEqualTo(appContext.getString(R.string.accessibility_shortcut_edit_screen_title))
    }

    @Test
    fun getScreenTitle_accessibilityService_returnFeatureTitle() {
        val serviceInfo = createA11yServiceInfo()
        a11yManager.setInstalledAccessibilityServiceList(listOf(serviceInfo))

        val screen = createScreen(arrayOf(A11Y_SERVICE_COMPONENT.flattenToString()))

        assertThat(screen.getScreenTitle(appContext))
            .isEqualTo(
                appContext.getString(
                    R.string.accessibility_shortcut_title,
                    serviceInfo.getFeatureName(appContext),
                )
            )
    }

    @Test
    fun getScreenTitle_accessibilityActivity_returnFeatureTitle() {
        val mockInfo = createA11yActivity()
        a11yManager.setInstalledAccessibilityShortcutListAsUser(listOf(mockInfo))

        val screen = createScreen(arrayOf(A11Y_ACTIVITY_COMPONENT.flattenToString()))

        assertThat(screen.getScreenTitle(appContext))
            .isEqualTo(appContext.getString(R.string.accessibility_shortcut_title, DEFAULT_LABEL))
    }

    private fun assertEditShortcutScreenTitleForSingleFeature(
        target: String,
        expectedTitle: String,
    ) {
        val screen = createScreen(arrayOf(target))
        assertThat(screen.getScreenTitle(appContext)).isEqualTo(expectedTitle)
    }

    private fun createScreen(targets: Array<String>): EditShortcutsScreen {
        return EditShortcutsScreen(
            appContext,
            Bundle().apply {
                putStringArray(EditShortcutsPreferenceFragment.ARG_KEY_SHORTCUT_TARGETS, targets)
            },
        )
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
        whenever(mockInfo.loadSummary(any())).thenReturn(DEFAULT_SUMMARY)
        return mockInfo
    }

    companion object {
        private const val PACKAGE_NAME = "com.foo.bar"
        private val A11Y_SERVICE_COMPONENT = ComponentName(PACKAGE_NAME, "FakeA11yService")
        private val A11Y_ACTIVITY_COMPONENT = ComponentName(PACKAGE_NAME, "FakeA11yActivity")

        private const val DEFAULT_LABEL = "default label"
        private const val DEFAULT_SUMMARY = "default summary"
    }
}
