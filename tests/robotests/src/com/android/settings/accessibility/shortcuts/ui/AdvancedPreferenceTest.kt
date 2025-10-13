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

import android.app.Application
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import androidx.test.core.app.ApplicationProvider
import com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME
import com.android.settings.R
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AdvancedPreferenceTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()

    private val appContext: Application = ApplicationProvider.getApplicationContext()
    @OptIn(ExperimentalCoroutinesApi::class) private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val magnificationShortcutTarget = setOf(MAGNIFICATION_CONTROLLER_NAME)
    private lateinit var preference: AdvancedPreference

    @Before
    fun setUp() {
        preference = AdvancedPreference(magnificationShortcutTarget)
    }

    @After
    fun cleanUp() {
        testScope.cancel()
    }

    @Test
    fun key_returnsCorrectValue() {
        assertThat(preference.key).isEqualTo("advanced_shortcuts_collapsed")
    }

    @Test
    fun icon_returnsCorrectValue() {
        assertThat(preference.icon).isEqualTo(R.drawable.ic_keyboard_arrow_down)
    }

    @Test
    fun title_returnsCorrectValue() {
        assertThat(preference.title)
            .isEqualTo(R.string.accessibility_shortcut_edit_dialog_title_advance)
    }

    @Test
    fun isAvailable_notExpandedAndMagnificationTarget_returnsTrue() {
        // Default state is not expanded
        assertThat(preference.isAvailable(appContext)).isTrue()
    }

    @Test
    fun isAvailable_isExpanded_returnsFalse() =
        testScope.runTest {
            val widget = preference.createAndBindWidget<Preference>(appContext)

            // Click to expand
            widget.performClick()

            assertThat(preference.isAvailable(appContext)).isFalse()
        }

    @Test
    fun isAvailable_noTargets_returnsFalse() {
        preference = AdvancedPreference(emptySet())
        assertThat(preference.isAvailable(appContext)).isFalse()
    }

    @Test
    fun isAvailable_wrongTarget_returnsFalse() {
        preference = AdvancedPreference(setOf("some.other.target"))
        assertThat(preference.isAvailable(appContext)).isFalse()
    }

    @Test
    fun isAvailable_multipleTargets_returnsFalse() {
        preference = AdvancedPreference(setOf(MAGNIFICATION_CONTROLLER_NAME, "some.other.target"))
        assertThat(preference.isAvailable(appContext)).isFalse()
    }

    @Test
    fun bind_onClick_togglesExpandableState() =
        testScope.runTest {
            val widget = preference.createAndBindWidget<Preference>(appContext)

            // Initial state is false
            assertThat(preference.expandableState.value).isFalse()

            // First click -> true
            widget.performClick()
            assertThat(preference.expandableState.value).isTrue()

            // Second click -> false
            widget.performClick()
            assertThat(preference.expandableState.value).isFalse()
        }

    @Test
    fun onCreate_tripleTapIsChecked_expandsStateIsTrue() =
        testScope.runTest {
            val tripleTapWidget = CheckBoxPreference(appContext)
            tripleTapWidget.isChecked = true
            val advancedPreferenceWidget = preference.createAndBindWidget<Preference>(appContext)
            val lifecycleContext =
                getPreferenceLifecycleContext(advancedPreferenceWidget, tripleTapWidget)

            preference.onCreate(lifecycleContext)

            assertThat(preference.expandableState.value).isTrue()
        }

    @Test
    fun onCreate_advancedWidgetIsNotVisible_expandsStateIsTrue() =
        testScope.runTest {
            val tripleTapWidget = CheckBoxPreference(appContext)
            tripleTapWidget.isChecked = true
            val advancedPreferenceWidget = preference.createAndBindWidget<Preference>(appContext)
            advancedPreferenceWidget.isVisible = false
            val lifecycleContext =
                getPreferenceLifecycleContext(advancedPreferenceWidget, tripleTapWidget)

            preference.onCreate(lifecycleContext)

            assertThat(preference.expandableState.value).isTrue()
        }

    @Test
    fun onCreate_defaultState_doesNotExpand() =
        testScope.runTest {
            val tripleTapWidget = CheckBoxPreference(appContext)
            val advancedPreferenceWidget = preference.createAndBindWidget<Preference>(appContext)
            val lifecycleContext =
                getPreferenceLifecycleContext(advancedPreferenceWidget, tripleTapWidget)

            preference.onCreate(lifecycleContext)

            assertThat(preference.expandableState.value).isFalse()
        }

    @Test
    fun onCreate_collectsStateAndNotifiesChange() =
        testScope.runTest {
            val tripleTapWidget = CheckBoxPreference(appContext)
            val advancedPreferenceWidget = preference.createAndBindWidget<Preference>(appContext)
            val lifecycleContext =
                getPreferenceLifecycleContext(advancedPreferenceWidget, tripleTapWidget)

            // Now call onCreate to start collection
            preference.onCreate(lifecycleContext)
            clearInvocations(lifecycleContext)

            // Change the state
            advancedPreferenceWidget.performClick()

            // Verify the change was collected and notified
            verify(lifecycleContext).notifyPreferenceChange(preference.key)
        }

    private fun getPreferenceLifecycleContext(
        advancedPreferenceWidget: Preference,
        tripleTapWidget: CheckBoxPreference,
    ): PreferenceLifecycleContext {
        return mock {
            on { applicationContext }.thenReturn(appContext)
            on { lifecycleScope }.thenReturn(testScope.backgroundScope)
            on { findPreference<Preference>(preference.key) }.thenReturn(advancedPreferenceWidget)
            on { findPreference<TwoStatePreference>(TripleTapShortcutPreference.KEY) }
                .thenReturn(tripleTapWidget)
        }
    }
}
