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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.shortcuts.ShortcutOptionPreference as ShortcutOptionWidget
import com.android.settings.testutils.AccessibilityTestUtils
import com.android.settings.testutils.AccessibilityTestUtils.setupMockAccessibilityManager
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.utils.StringUtil
import com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SETUP_FLOW
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@Config(shadows = [SettingsShadowResources::class])
@RunWith(RobolectricTestParameterInjector::class)
class QuickSettingsShortcutPreferenceTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private lateinit var appContext: Context
    private lateinit var preference: QuickSettingsShortcutPreference
    private var activityScenario: ActivityScenario<FragmentActivity>? = null
    private lateinit var a11yManager: AccessibilityManager

    @Before
    fun setUp() {
        SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_quickSettingsSupported,
            true,
        )
        appContext = spy(value = ApplicationProvider.getApplicationContext())
        a11yManager = setupMockAccessibilityManager(appContext)
        preference = QuickSettingsShortcutPreference(appContext, setOf(TARGET_FLATTEN))
    }

    @After
    fun tearDown() {
        activityScenario?.close()
    }

    @Test
    fun key_returnsCorrectValue() {
        assertThat(preference.key).isEqualTo("shortcut_quick_settings_pref")
    }

    @Test
    fun title_returnsCorrectValue() {
        assertThat(preference.title)
            .isEqualTo(R.string.accessibility_shortcut_edit_dialog_title_quick_settings)
    }

    @Test
    fun bind_setsIntroImage() {
        val widget = preference.createAndBindWidget<ShortcutOptionWidget>(appContext)

        val imageResId: Int = ReflectionHelpers.getField(widget, "mIntroImageResId")

        assertThat(imageResId).isEqualTo(R.drawable.accessibility_shortcut_type_quick_settings)
    }

    @TestParameters(
        value =
            [
                "{touchExplorationEnabled: false, expectedNumFingers: 1}",
                "{touchExplorationEnabled: true, expectedNumFingers: 2}",
            ]
    )
    @Test
    fun getSummary_notInSuw_usesSummaryWithCorrectNumOfFingers(
        touchExplorationEnabled: Boolean,
        expectedNumFingers: Int,
    ) {
        val context = spy(createContext(inSetupWizard = false))
        val expectedSummary =
            StringUtil.getIcuPluralsString(
                context,
                expectedNumFingers,
                R.string.accessibility_shortcut_edit_dialog_summary_quick_settings,
            )
        enableTouchExploration(touchExplorationEnabled, context)

        assertThat(preference.getSummary(context).toString()).isEqualTo(expectedSummary)
    }

    @TestParameters(
        value =
            [
                "{touchExplorationEnabled: false, expectedNumFingers: 1}",
                "{touchExplorationEnabled: true, expectedNumFingers: 2}",
            ]
    )
    @Test
    fun getSummary_inSuw_usesSummaryWithCorrectNumOfFingers(
        touchExplorationEnabled: Boolean,
        expectedNumFingers: Int,
    ) {
        val context = spy(createContext(inSetupWizard = true))
        val expectedSummary =
            StringUtil.getIcuPluralsString(
                context,
                expectedNumFingers,
                R.string.accessibility_shortcut_edit_dialog_summary_quick_settings_suw,
            )
        enableTouchExploration(touchExplorationEnabled, context)

        assertThat(preference.getSummary(context).toString()).isEqualTo(expectedSummary)
    }

    @Test
    fun isAvailable_qsNotSupported_returnsFalse() {
        SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_quickSettingsSupported,
            false,
        )

        assertThat(preference.isAvailable(appContext)).isFalse()
    }

    @Test
    fun isAvailable_targetHasNoTile_returnsFalse() {
        whenever(a11yManager.getA11yFeatureToTileMap(UserHandle.myUserId())).thenReturn(emptyMap())

        assertThat(preference.isAvailable(appContext)).isFalse()
    }

    @Test
    fun isAvailable_targetIsStandardA11yService_returnsFalse() {
        val mockStandardA11yService =
            AccessibilityTestUtils.createAccessibilityServiceInfo(
                appContext,
                TARGET,
                /* isAlwaysOnService= */ false,
            )
        whenever(a11yManager.getA11yFeatureToTileMap(UserHandle.myUserId()))
            .thenReturn(mapOf(TARGET to TARGET_TILE))

        // setup target as a standard a11y service
        whenever(a11yManager.getInstalledAccessibilityServiceList())
            .thenReturn(listOf(mockStandardA11yService))

        assertThat(preference.isAvailable(appContext)).isFalse()
    }

    @Test
    fun isAvailable_targetIsAlwaysOnA11yService_returnsTrue() {
        val mockAlwaysOnA11yService =
            AccessibilityTestUtils.createAccessibilityServiceInfo(
                appContext,
                TARGET,
                /* isAlwaysOnService= */ true,
            )
        whenever(a11yManager.getA11yFeatureToTileMap(UserHandle.myUserId()))
            .thenReturn(mapOf(TARGET to TARGET_TILE))

        // setup target as a always-on a11y service
        whenever(a11yManager.getInstalledAccessibilityServiceList())
            .thenReturn(listOf(mockAlwaysOnA11yService))

        assertThat(preference.isAvailable(appContext)).isTrue()
    }

    @Test
    fun isAvailable_multipleValidTargets_returnsTrue() {
        preference =
            QuickSettingsShortcutPreference(appContext, setOf(TARGET_FLATTEN, TARGET2_FLATTEN))
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
        whenever(a11yManager.getA11yFeatureToTileMap(UserHandle.myUserId()))
            .thenReturn(mapOf(TARGET to TARGET_TILE, TARGET2 to TARGET2_TILE))

        // setup target as a always-on a11y service
        whenever(a11yManager.getInstalledAccessibilityServiceList())
            .thenReturn(listOf(mockAlwaysOnA11yService, mockAlwaysOnA11yService2))

        assertThat(preference.isAvailable(appContext)).isTrue()
    }

    @Test
    fun isAvailable_multipleTargetsWithOneInvalidUseCase_returnsFalse() {
        preference =
            QuickSettingsShortcutPreference(appContext, setOf(TARGET_FLATTEN, TARGET2_FLATTEN))
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
        whenever(a11yManager.getA11yFeatureToTileMap(UserHandle.myUserId()))
            .thenReturn(mapOf(TARGET to TARGET_TILE, TARGET2 to TARGET2_TILE))

        // setup target as a always-on a11y service
        whenever(a11yManager.getInstalledAccessibilityServiceList())
            .thenReturn(listOf(mockAlwaysOnA11yService, mockStandardA11yService))

        assertThat(preference.isAvailable(appContext)).isFalse()
    }

    @Test
    fun lifecycle_onCreate_registersListener() {
        val lifecycleContext = getPreferenceLifecycleContext()
        whenever(lifecycleContext.getSystemService(AccessibilityManager::class.java))
            .thenReturn(a11yManager)

        preference.onCreate(lifecycleContext)

        verify(a11yManager).addTouchExplorationStateChangeListener(any())
    }

    @Test
    fun lifecycle_onDestroy_unregistersListener() {
        val lifecycleContext = getPreferenceLifecycleContext()
        whenever(lifecycleContext.getSystemService(AccessibilityManager::class.java))
            .thenReturn(a11yManager)
        val argumentCaptor = argumentCaptor<TouchExplorationStateChangeListener>()

        preference.run {
            onCreate(lifecycleContext)
            onDestroy(lifecycleContext)
        }

        assertThat(SettingsSecureStore.get(appContext).hasAnyObserver()).isFalse()
        verify(a11yManager).addTouchExplorationStateChangeListener(argumentCaptor.capture())
        verify(a11yManager).removeTouchExplorationStateChangeListener(argumentCaptor.lastValue)
    }

    @Test
    fun touchExplorationListener_onChange_notifiesPreferenceChange() {
        val lifecycleContext = getPreferenceLifecycleContext()
        whenever(lifecycleContext.getSystemService(AccessibilityManager::class.java))
            .thenReturn(a11yManager)
        val argumentCaptor = argumentCaptor<TouchExplorationStateChangeListener>()
        preference.onCreate(lifecycleContext)
        verify(a11yManager).addTouchExplorationStateChangeListener(argumentCaptor.capture())

        argumentCaptor.firstValue.onTouchExplorationStateChanged(/* enabled= */ true)

        verify(lifecycleContext).notifyPreferenceChange(preference.key)
    }

    private fun enableTouchExploration(enable: Boolean, context: Context) {
        val am: AccessibilityManager = setupMockAccessibilityManager(context)
        whenever(am.isTouchExplorationEnabled).thenReturn(enable)
    }

    private fun getPreferenceLifecycleContext(): PreferenceLifecycleContext {
        return mock { on { applicationContext }.thenReturn(appContext) }
    }

    private fun createContext(inSetupWizard: Boolean = false): Context {
        var startedActivity: Context? = null
        val shadowPackageManager = shadowOf(appContext.packageManager)
        shadowPackageManager.addActivityIfNotPresent(
            ComponentName(appContext, FragmentActivity::class.java)
        )
        val intent = Intent(appContext, FragmentActivity::class.java)
        intent.putExtra(EXTRA_IS_SETUP_FLOW, inSetupWizard)
        activityScenario = ActivityScenario.launch(intent)
        activityScenario!!.onActivity { activity -> startedActivity = activity }
        return startedActivity!!
    }

    companion object {
        private val TARGET: ComponentName = ComponentName("FakePackage", "FakeClass")
        private val TARGET_FLATTEN: String = TARGET.flattenToString()
        private val TARGET_TILE: ComponentName = ComponentName("FakePackage", "FakeTileClass")
        private val TARGET2: ComponentName = ComponentName("FakePackage", "FakeClass2")
        private val TARGET2_FLATTEN: String = TARGET2.flattenToString()
        private val TARGET2_TILE: ComponentName = ComponentName("FakePackage", "FakeTileClass2")
    }
}
