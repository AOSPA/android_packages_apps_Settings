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

import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener
import androidx.fragment.app.testing.EmptyFragmentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.android.internal.accessibility.AccessibilityShortcutController.COLOR_INVERSION_COMPONENT_NAME
import com.android.settings.R
import com.android.settings.accessibility.shortcuts.ShortcutOptionPreference as ShortcutOptionWidget
import com.android.settings.testutils.AccessibilityTestUtils
import com.android.settings.testutils.AccessibilityTestUtils.setupMockAccessibilityManager
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.utils.StringUtil
import com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SETUP_FLOW
import com.google.common.truth.Truth.assertThat
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class GestureShortcutPreferenceTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private lateinit var appContext: Context
    private var activityScenario: ActivityScenario<EmptyFragmentActivity>? = null
    private lateinit var preference: GestureShortcutPreference
    private val targets = setOf(COLOR_INVERSION_COMPONENT_NAME.flattenToString())

    @Before
    fun setUp() {
        appContext = spy(value = ApplicationProvider.getApplicationContext())
        AccessibilityTestUtils.setSoftwareShortcutMode(
            appContext,
            /* gestureNavEnabled= */ true,
            /* floatingButtonEnabled= */ false,
        )

        preference = GestureShortcutPreference(appContext, targets)
    }

    @After
    fun tearDown() {
        activityScenario?.close()
    }

    @Test
    fun key_returnsCorrectValue() {
        assertThat(preference.key).isEqualTo("shortcut_gesture_pref")
    }

    @Test
    fun title_returnsCorrectValue() {
        assertThat(preference.title)
            .isEqualTo(R.string.accessibility_shortcut_edit_dialog_title_software_by_gesture)
    }

    @Test
    fun bind_withTouchExploreEnabled_setsCorrectImage() {
        enableTouchExploration(true)
        val widget = preference.createAndBindWidget<ShortcutOptionWidget>(appContext)

        val imageResId: Int = ReflectionHelpers.getField(widget, "mIntroImageResId")

        assertThat(imageResId)
            .isEqualTo(R.drawable.accessibility_shortcut_type_gesture_touch_explore_on)
    }

    @Test
    fun bind_withTouchExploreDisabled_setsCorrectImage() {
        enableTouchExploration(false)
        val widget = preference.createAndBindWidget<ShortcutOptionWidget>(appContext)

        val imageResId: Int = ReflectionHelpers.getField(widget, "mIntroImageResId")

        assertThat(imageResId).isEqualTo(R.drawable.accessibility_shortcut_type_gesture)
    }

    @Test
    fun getSummary_withTouchExploreEnabled_usesThreeFingersSummary() {
        enableTouchExploration(true)
        val expectedSummary =
            StringUtil.getIcuPluralsString(
                appContext,
                3,
                R.string.accessibility_shortcut_edit_dialog_summary_gesture,
            )

        val summary = preference.getSummary(appContext)

        assertThat(summary).isEqualTo(expectedSummary)
    }

    @Test
    fun getSummary_withTouchExploreDisabled_usesTwoFingersSummary() {
        enableTouchExploration(false)
        val expectedSummary =
            StringUtil.getIcuPluralsString(
                appContext,
                2,
                R.string.accessibility_shortcut_edit_dialog_summary_gesture,
            )

        val summary = preference.getSummary(appContext)

        assertThat(summary).isEqualTo(expectedSummary)
    }

    @Test
    fun isAvailable_inSetupWizard_returnsFalse() {
        val context = createContext(inSetupWizard = true)
        AccessibilityTestUtils.setSoftwareShortcutMode(
            context,
            /* gestureNavEnabled= */ true,
            /* floatingButtonEnabled= */ false,
        )

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_gestureNavigationDisabled_returnsFalse() {
        val context = createContext(inSetupWizard = false)
        AccessibilityTestUtils.setSoftwareShortcutMode(
            context,
            /* gestureNavEnabled= */ false,
            /* floatingButtonEnabled= */ false,
        )

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_notInSuwAndGestureNavigationEnabled_returnsTrue() {
        val context = createContext(inSetupWizard = false)
        AccessibilityTestUtils.setSoftwareShortcutMode(
            context,
            /* gestureNavEnabled= */ true,
            /* floatingButtonEnabled= */ false,
        )

        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    fun lifecycle_onCreate_registersListeners() {
        val am: AccessibilityManager = setupMockAccessibilityManager(appContext)
        val lifecycleContext = getPreferenceLifecycleContext()
        whenever(lifecycleContext.getSystemService(AccessibilityManager::class.java)).thenReturn(am)

        preference.onCreate(lifecycleContext)

        assertThat(SettingsSecureStore.get(appContext).hasAnyObserver()).isTrue()
        verify(am).addTouchExplorationStateChangeListener(any())
    }

    @Test
    fun lifecycle_onDestroy_unregistersListeners() {
        val am: AccessibilityManager = setupMockAccessibilityManager(appContext)
        val lifecycleContext = getPreferenceLifecycleContext()
        whenever(lifecycleContext.getSystemService(AccessibilityManager::class.java)).thenReturn(am)
        val argumentCaptor = argumentCaptor<TouchExplorationStateChangeListener>()

        preference.run {
            onCreate(lifecycleContext)
            onDestroy(lifecycleContext)
        }

        assertThat(SettingsSecureStore.get(appContext).hasAnyObserver()).isFalse()
        verify(am).addTouchExplorationStateChangeListener(argumentCaptor.capture())
        verify(am).removeTouchExplorationStateChangeListener(argumentCaptor.lastValue)
    }

    @Test
    fun touchExplorationListener_onChange_notifiesPreferenceChange() {
        val am: AccessibilityManager = setupMockAccessibilityManager(appContext)
        val lifecycleContext = getPreferenceLifecycleContext()
        whenever(lifecycleContext.getSystemService(AccessibilityManager::class.java)).thenReturn(am)
        val argumentCaptor = argumentCaptor<TouchExplorationStateChangeListener>()
        preference.onCreate(lifecycleContext)
        verify(am).addTouchExplorationStateChangeListener(argumentCaptor.capture())

        argumentCaptor.firstValue.onTouchExplorationStateChanged(/* enabled= */ true)

        verify(lifecycleContext).notifyPreferenceChange(preference.key)
    }

    @Test
    fun settingsObserver_onChange_notifiesPreferenceChange() {
        AccessibilityTestUtils.setSoftwareShortcutMode(
            appContext,
            /* gestureNavEnabled= */ false,
            /* floatingButtonEnabled= */ true,
        )
        val lifecycleContext = getPreferenceLifecycleContext()

        preference.onCreate(lifecycleContext)
        AccessibilityTestUtils.setSoftwareShortcutMode(
            appContext,
            /* gestureNavEnabled= */ true,
            /* floatingButtonEnabled= */ true,
        )
        ShadowLooper.idleMainLooper()

        verify(lifecycleContext).notifyPreferenceChange(preference.key)
    }

    private fun enableTouchExploration(enable: Boolean) {
        val am: AccessibilityManager = setupMockAccessibilityManager(appContext)
        whenever(am.isTouchExplorationEnabled).thenReturn(enable)
    }

    private fun getPreferenceLifecycleContext(): PreferenceLifecycleContext {
        return mock { on { applicationContext }.thenReturn(appContext) }
    }

    private fun createContext(inSetupWizard: Boolean = false): Context {
        var startedActivity: Context? = null
        val intent = Intent(appContext, EmptyFragmentActivity::class.java)
        intent.putExtra(EXTRA_IS_SETUP_FLOW, inSetupWizard)
        activityScenario = ActivityScenario.launch(intent)
        activityScenario!!.onActivity { activity -> startedActivity = activity }
        return startedActivity!!
    }
}
