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
import android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON
import android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL
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

/** Test for [ButtonLocationPreference] */
@RunWith(RobolectricTestRunner::class)
class ButtonLocationPreferenceTest {

    @get:Rule val settingsStoreRule = SettingsStoreRule()
    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()
    @Mock lateinit var mockPrefLifecycleContext: PreferenceLifecycleContext
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference = ButtonLocationPreference(context)

    @Test
    fun key() {
        assertThat(preference.key).isEqualTo(Settings.Secure.ACCESSIBILITY_BUTTON_MODE)
    }

    @Test
    fun title() {
        assertThat(preference.title).isEqualTo(R.string.accessibility_button_location_title)
    }

    @Test
    fun purpose() {
        assertThat(preference.purpose).isEqualTo(R.string.a11y_button_shortcut_location_purpose)
    }

    @Test
    fun values() {
        assertThat(preference.values)
            .isEqualTo(R.array.accessibility_button_location_selector_int_values)
    }

    @Test
    fun valuesDescription() {
        assertThat(preference.valuesDescription)
            .isEqualTo(R.array.accessibility_button_location_selector_titles)
    }

    @Test
    fun createWidget_returnsListPreference() {
        val widget = preference.createWidget(context)
        assertThat(widget).isInstanceOf(ListPreference::class.java)
    }

    @Test
    fun bindWidget_useNavButton_verifySummary() {
        setButtonMode(useNavButton = true)

        val widget = preference.createAndBindWidget(context) as ListPreference

        assertThat(widget.summary.toString())
            .isEqualTo(
                context.getString(R.string.accessibility_button_location_selector_navigation_bar)
            )
    }

    @Test
    fun bindWidget_useFloatingButton_verifySummary() {
        setButtonMode(useNavButton = false)

        val widget = preference.createAndBindWidget(context) as ListPreference

        assertThat(widget.summary.toString())
            .isEqualTo(context.getString(R.string.accessibility_button_location_selector_floating))
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
    fun isAvailable_gestureNavigationEnabled_isFalse() {
        setGestureNavigation(enabled = true)
        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_gestureNavigationDisabled_isTrue() {
        setGestureNavigation(enabled = false)
        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    fun onCreate_navigationModeSettingChanged_callsNotifyPreferenceChange() {
        setGestureNavigation(enabled = false)

        preference.onCreate(mockPrefLifecycleContext)
        setGestureNavigation(enabled = true)

        verify(mockPrefLifecycleContext).notifyPreferenceChange(preference.key)
    }

    @Test
    fun onDestroy_navigationModeSettingChanged_shouldNotCallNotifyPreferenceChange() {
        setGestureNavigation(enabled = false)

        preference.run {
            onCreate(mockPrefLifecycleContext)
            onDestroy(mockPrefLifecycleContext)
        }
        setGestureNavigation(enabled = true)

        verify(mockPrefLifecycleContext, never()).notifyPreferenceChange(preference.key)
    }

    private fun setGestureNavigation(enabled: Boolean) {
        val navMode = if (enabled) NAV_BAR_MODE_GESTURAL else NAV_BAR_MODE_3BUTTON
        SettingsSecureStore.get(context).setInt(Settings.Secure.NAVIGATION_MODE, navMode)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    }

    private fun setButtonMode(useNavButton: Boolean) {
        val buttonMode =
            if (useNavButton) Settings.Secure.ACCESSIBILITY_BUTTON_MODE_NAVIGATION_BAR
            else Settings.Secure.ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU
        SettingsSecureStore.get(context)
            .setInt(Settings.Secure.ACCESSIBILITY_BUTTON_MODE, buttonMode)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    }
}
