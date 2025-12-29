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

package com.android.settings.accessibility.actiontimeout.ui

import android.app.settings.SettingsEnums
import android.content.Context
import com.android.settings.R
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.actiontimeout.data.ActionTimeoutOptionDataStore
import com.android.settings.accessibility.actiontimeout.data.ActionTimeoutOptionDataStore.Companion.DISCRETE_TIMEOUT_OPTIONS
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import com.google.testing.junit.testparameterinjector.TestParametersValuesProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestParameterInjector::class)
class ActionTimeoutSettingsScreenTest : SettingsCatalystTestCase() {

    override val preferenceScreenCreator = ActionTimeoutSettingsScreen(appContext)

    override val flagName = Flags.FLAG_CATALYST_TIME_TO_TAKE_ACTION_SCREEN

    @Test
    fun getMetricsCategory_returnsCorrectValue() {
        assertThat(preferenceScreenCreator.getMetricsCategory())
            .isEqualTo(SettingsEnums.ACCESSIBILITY_TIMEOUT)
    }

    @Test
    fun highlightMenuKey_returnsAccessibilityMenuKey() {
        assertThat(preferenceScreenCreator.highlightMenuKey)
            .isEqualTo(R.string.menu_key_accessibility)
    }

    @Test
    fun key_returnsActionTimeoutSettingsScreenKey() {
        assertThat(preferenceScreenCreator.key).isEqualTo(ActionTimeoutSettingsScreen.KEY)
    }

    @Test
    fun title_returnsCorrectStringResId() {
        assertThat(preferenceScreenCreator.title)
            .isEqualTo(R.string.accessibility_setting_item_control_timeout_title)
    }

    @Test
    fun screenTitle_returnsCorrectStringResId() {
        assertThat(preferenceScreenCreator.screenTitle)
            .isEqualTo(R.string.accessibility_control_timeout_preference_title)
    }

    @Test
    fun keywords_returnsAccessibilityTimeoutKeywords() {
        assertThat(preferenceScreenCreator.keywords)
            .isEqualTo(R.string.keywords_accessibility_timeout)
    }

    @Test
    fun purpose_returnsCorrectStringResId() {
        assertThat(preferenceScreenCreator.purpose)
            .isEqualTo(R.string.a11y_action_timeout_setting_screen_purpose)
    }

    @Test
    fun indexable_returnsTrue() {
        assertThat(preferenceScreenCreator.indexable).isTrue()
    }

    @TestParameters(valuesProvider = TimeoutOptionsProvider::class)
    @Test
    fun getSummary_returnsCorrespondingSummaryForSelectedTimeoutValue(
        timeoutValue: Int,
        expectedStringRes: Int,
    ) {
        SettingsSecureStore.get(appContext)
            .setInt(ActionTimeoutOptionDataStore.INTERACTIVE_UI_TIMEOUT_SETTING_KEY, timeoutValue)
        val expectedSummary = appContext.getString(expectedStringRes)

        assertThat(preferenceScreenCreator.getSummary(appContext)).isEqualTo(expectedSummary)
    }

    @Test
    fun getSummary_unknownValue_returnsNull() {
        SettingsSecureStore.get(appContext)
            .setInt(ActionTimeoutOptionDataStore.INTERACTIVE_UI_TIMEOUT_SETTING_KEY, -100)

        assertThat(preferenceScreenCreator.getSummary(appContext)).isNull()
    }

    @Test
    fun onCreate_shownAsEntrypoint_settingChanges_notifyPrefChange() {
        val mockLifecycleContext =
            mock<PreferenceLifecycleContext> {
                on { preferenceScreenKey } doReturn "other_screen_key"
            }

        preferenceScreenCreator.onCreate(mockLifecycleContext)
        SettingsSecureStore.get(appContext)
            .setInt(ActionTimeoutOptionDataStore.INTERACTIVE_UI_TIMEOUT_SETTING_KEY, 10000)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        verify(mockLifecycleContext, times(1))
            .notifyPreferenceChange(preferenceScreenCreator.bindingKey)
    }

    @Test
    fun onCreate_shownAsContainer_settingChanges_doNotNotifyPrefChange() {
        val mockLifecycleContext =
            mock<PreferenceLifecycleContext> {
                on { preferenceScreenKey } doReturn preferenceScreenCreator.key
            }

        preferenceScreenCreator.onCreate(mockLifecycleContext)
        SettingsSecureStore.get(appContext)
            .setInt(ActionTimeoutOptionDataStore.INTERACTIVE_UI_TIMEOUT_SETTING_KEY, 10000)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        verify(mockLifecycleContext, never()).notifyPreferenceChange(any())
    }

    @Test
    fun onDestroy_shownAsEntryPoint_settingChanges_doNotNotifyPrefChange() {
        val mockLifecycleContext =
            mock<PreferenceLifecycleContext> {
                on { preferenceScreenKey } doReturn "other_screen_key"
            }
        preferenceScreenCreator.onCreate(mockLifecycleContext)
        preferenceScreenCreator.onDestroy(mockLifecycleContext)

        SettingsSecureStore.get(appContext)
            .setInt(ActionTimeoutOptionDataStore.INTERACTIVE_UI_TIMEOUT_SETTING_KEY, 10000)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        verify(mockLifecycleContext, never()).notifyPreferenceChange(any())
    }

    @Test
    fun onDestroy_shownAsContainer_settingChanges_doNotNotifyPrefChange() {
        val mockLifecycleContext =
            mock<PreferenceLifecycleContext> {
                on { preferenceScreenKey } doReturn preferenceScreenCreator.key
            }
        preferenceScreenCreator.onCreate(mockLifecycleContext)
        preferenceScreenCreator.onDestroy(mockLifecycleContext)

        SettingsSecureStore.get(appContext)
            .setInt(ActionTimeoutOptionDataStore.INTERACTIVE_UI_TIMEOUT_SETTING_KEY, 10000)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        verify(mockLifecycleContext, never()).notifyPreferenceChange(any())
    }

    private object TimeoutOptionsProvider : TestParametersValuesProvider() {
        override fun provideValues(context: Context): List<TestParameters.TestParametersValues> {
            return DISCRETE_TIMEOUT_OPTIONS.map { entry ->
                TestParameters.TestParametersValues.builder()
                    .name("Accessibility Timeout: ${entry.key}")
                    .addParameter("timeoutValue", entry.key)
                    .addParameter("expectedStringRes", entry.value.optionDescriptionRes)
                    .build()
            }
        }
    }
}
