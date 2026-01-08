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

package com.android.settings.accessibility.buttonshortcutsetting.ui

import android.content.Context
import android.provider.Settings
import android.provider.Settings.Secure
import androidx.preference.ListPreference
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.shared.data.StringToIntDataStoreWrapper
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

/** Test for [ButtonSizePreference] */
@RunWith(RobolectricTestRunner::class)
class ButtonSizePreferenceTest {

    @get:Rule val settingsStoreRule = SettingsStoreRule()
    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()
    @Mock lateinit var mockPrefLifecycleContext: PreferenceLifecycleContext
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference = ButtonSizePreference(context)

    @Test
    fun key() {
        assertThat(preference.key).isEqualTo(Settings.Secure.ACCESSIBILITY_FLOATING_MENU_SIZE)
    }

    @Test
    fun purpose() {
        assertThat(preference.purpose).isEqualTo(R.string.a11y_button_shortcut_size_purpose)
    }

    @Test
    fun title() {
        assertThat(preference.title).isEqualTo(R.string.accessibility_button_size_title)
    }

    @Test
    fun values() {
        assertThat(preference.values)
            .isEqualTo(R.array.accessibility_button_size_selector_int_values)
    }

    @Test
    fun valuesDescription() {
        assertThat(preference.valuesDescription)
            .isEqualTo(R.array.accessibility_button_size_selector_titles)
    }

    @Test
    fun valueType() {
        assertThat(preference.valueType).isEqualTo(Int::class.javaObjectType)
    }

    @Test
    fun storage_returnsStringToIntDataStoreWrapper() {
        assertThat(preference.storage(context))
            .isInstanceOf(StringToIntDataStoreWrapper::class.java)
    }

    @Test
    fun createWidget_returnsListPreference() {
        val widget = preference.createWidget(context)
        assertThat(widget).isInstanceOf(ListPreference::class.java)
    }

    @Test
    fun bindWidget_largeFloatingMenuEnabled_returnLarge() {
        setFloatingMenuEnabled(true)
        setFloatingButtonSize(large = true)

        val widget = preference.createAndBindWidget(context) as ListPreference

        assertThat(widget.summary.toString())
            .isEqualTo(context.getString(R.string.accessibility_button_size_selector_large))
    }

    @Test
    fun bindWidget_smallFloatingMenuEnabled_returnSmall() {
        setFloatingMenuEnabled(true)
        setFloatingButtonSize(large = false)

        val widget = preference.createAndBindWidget(context) as ListPreference

        assertThat(widget.summary.toString())
            .isEqualTo(context.getString(R.string.accessibility_button_size_selector_small))
    }

    @Test
    fun bindWidget_floatingMenuDisabled_returnsDisabledSummary() {
        setFloatingMenuEnabled(false)
        setFloatingButtonSize(large = false)

        val widget = preference.createAndBindWidget(context) as ListPreference

        assertThat(widget.summary.toString())
            .isEqualTo(
                context.getString(R.string.accessibility_button_disabled_button_mode_summary)
            )
    }

    @Test
    fun isEnabled_floatingMenuEnabled_isTrue() {
        setFloatingMenuEnabled(true)
        assertThat(preference.isEnabled(context)).isTrue()
    }

    @Test
    fun isEnabled_floatingMenuDisabled_isFalse() {
        setFloatingMenuEnabled(false)
        assertThat(preference.isEnabled(context)).isFalse()
    }

    @Test
    fun onCreate_buttonModeSettingChanged_callsNotifyPreferenceChange() {
        setFloatingMenuEnabled(false)

        preference.onCreate(mockPrefLifecycleContext)
        setFloatingMenuEnabled(true)

        verify(mockPrefLifecycleContext).notifyPreferenceChange(preference.key)
    }

    @Test
    fun onDestroy_buttonModeSettingChanged_shouldNotCallNotifyPreferenceChange() {
        setFloatingMenuEnabled(false)

        preference.run {
            onCreate(mockPrefLifecycleContext)
            onDestroy(mockPrefLifecycleContext)
        }
        setFloatingMenuEnabled(true)

        verify(mockPrefLifecycleContext, never()).notifyPreferenceChange(preference.key)
    }

    private fun setFloatingMenuEnabled(enabled: Boolean) {
        SettingsSecureStore.get(context)
            .setInt(
                Secure.ACCESSIBILITY_BUTTON_MODE,
                if (enabled) Secure.ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU
                else Secure.ACCESSIBILITY_BUTTON_MODE_NAVIGATION_BAR,
            )
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    }

    private fun setFloatingButtonSize(large: Boolean) {
        SettingsSecureStore.get(context)
            .setInt(Secure.ACCESSIBILITY_FLOATING_MENU_SIZE, if (large) 1 else 0)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    }
}
