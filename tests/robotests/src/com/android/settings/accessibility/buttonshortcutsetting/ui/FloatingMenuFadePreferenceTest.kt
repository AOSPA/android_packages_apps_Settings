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
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

/** Test for [FloatingMenuFadePreference] */
@RunWith(RobolectricTestRunner::class)
class FloatingMenuFadePreferenceTest {

    @get:Rule val settingsStoreRule = SettingsStoreRule()

    private lateinit var mockPrefLifecycleContext: PreferenceLifecycleContext

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference = FloatingMenuFadePreference()
    private val settingsSecureStore = SettingsSecureStore.get(context)

    @Before
    fun setUp() {
        mockPrefLifecycleContext = mock { on { applicationContext } doReturn context }
    }

    @Test
    fun key() {
        assertThat(preference.key).isEqualTo(FloatingMenuFadePreference.KEY)
    }

    @Test
    fun purpose_returnsCorrectResourceId() {
        assertThat(preference.purpose)
            .isEqualTo(R.string.a11y_floating_menu_shortcut_fade_enabled_purpose)
    }

    @Test
    fun title_returnsCorrectResourceId() {
        assertThat(preference.title).isEqualTo(R.string.accessibility_button_fade_title)
    }

    @Test
    fun isEnabled_floatingMenuOn_returnsTrue() {
        setFloatingMenuEnabled(true)

        assertThat(preference.isEnabled(context)).isTrue()
    }

    @Test
    fun isEnabled_floatingMenuOff_returnsFalse() {
        setFloatingMenuEnabled(false)

        assertThat(preference.isEnabled(context)).isFalse()
    }

    @Test
    fun getSummary_enabled_returnsFadeSummary() {
        setFloatingMenuEnabled(true)

        assertThat(preference.getSummary(context))
            .isEqualTo(context.getString(R.string.accessibility_button_fade_summary))
    }

    @Test
    fun getSummary_disabled_returnsDisabledSummary() {
        setFloatingMenuEnabled(false)

        assertThat(preference.getSummary(context))
            .isEqualTo(
                context.getString(R.string.accessibility_button_disabled_button_mode_summary)
            )
    }

    @Test
    fun storage_returnsSettingsSecureStoreWithDefaultToTrue() {
        val store = preference.storage(context)
        assertThat(store).isInstanceOf(SettingsSecureStore::class.java)

        val defaultValue = store.getBoolean(FloatingMenuFadePreference.KEY)

        assertThat(defaultValue).isEqualTo(true)
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

        preference.onCreate(mockPrefLifecycleContext)
        preference.onDestroy(mockPrefLifecycleContext)
        setFloatingMenuEnabled(true)

        verify(mockPrefLifecycleContext, never()).notifyPreferenceChange(preference.key)
    }

    private fun setFloatingMenuEnabled(enabled: Boolean) {
        settingsSecureStore.setInt(
            Settings.Secure.ACCESSIBILITY_BUTTON_MODE,
            if (enabled) Settings.Secure.ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU
            else Settings.Secure.ACCESSIBILITY_BUTTON_MODE_NAVIGATION_BAR,
        )

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    }
}
