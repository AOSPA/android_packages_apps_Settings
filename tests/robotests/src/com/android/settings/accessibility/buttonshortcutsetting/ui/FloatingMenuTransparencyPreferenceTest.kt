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
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.buttonshortcutsetting.data.FloatingMenuTransparencyDataStore
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.SliderPreference
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import java.text.NumberFormat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.shadows.ShadowLooper

/** Test for [FloatingMenuTransparencyPreference] */
@RunWith(RobolectricTestParameterInjector::class)
class FloatingMenuTransparencyPreferenceTest {

    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    @get:Rule val setFlagsRule = SetFlagsRule()

    @Mock private lateinit var mockPrefLifecycleContext: PreferenceLifecycleContext

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference = FloatingMenuTransparencyPreference(context)
    private val settingsSecureStore = SettingsSecureStore.get(context)

    @Test
    fun key() {
        assertThat(preference.key).isEqualTo(FloatingMenuTransparencyPreference.KEY)
    }

    @Test
    fun purpose_returnsCorrectResourceId() {
        assertThat(preference.purpose).isEqualTo(R.string.a11y_button_shortcut_transparency_purpose)
    }

    @Test
    fun title_returnsCorrectResourceId() {
        assertThat(preference.title).isEqualTo(R.string.accessibility_button_opacity_title)
    }

    @TestParameters(
        value =
            [
                "{floatingMenuEnabled: true, fadeEnabled: true, expected: true}",
                "{floatingMenuEnabled: false, fadeEnabled: true, expected: false}",
                "{floatingMenuEnabled: true, fadeEnabled: false, expected: false}",
                "{floatingMenuEnabled: false, fadeEnabled: false, expected: false}",
            ]
    )
    @Test
    fun isEnabled_returnsExpectedResult(
        floatingMenuEnabled: Boolean,
        fadeEnabled: Boolean,
        expected: Boolean,
    ) {
        setFloatingMenuEnabled(floatingMenuEnabled)
        setFadeEnabled(fadeEnabled)

        assertThat(preference.isEnabled(context)).isEqualTo(expected)
    }

    @Test
    fun createWidget_returnsSliderPreference() {
        val widget = preference.createWidget(context)

        assertThat(widget).isInstanceOf(SliderPreference::class.java)
    }

    @Test
    fun createWidget_verifySliderUpdatesContinuouslyIsTrue() {
        val widget = preference.createWidget(context)

        assertThat(widget.updatesContinuously).isTrue()
    }

    @Test
    fun getMinValue_returnsMinProgress() {
        assertThat(preference.getMinValue(context))
            .isEqualTo(FloatingMenuTransparencyDataStore.MIN_TRANSPARENCY_PROGRESS)
    }

    @Test
    fun getMaxValue_returnsMaxProgress() {
        assertThat(preference.getMaxValue(context))
            .isEqualTo(FloatingMenuTransparencyDataStore.MAX_TRANSPARENCY_PROGRESS)
    }

    @Test
    fun storage_returnsFloatingMenuTransparencyDataStore() {
        assertThat(preference.storage(context))
            .isInstanceOf(FloatingMenuTransparencyDataStore::class.java)
    }

    @Test
    @DisableFlags(Flags.FLAG_FLOATING_MENU_TRANSPARENCY_SLIDER_ANNOUNCES_DISABLED)
    fun bind_flagOff_setsStateDescription() {
        // sets transparency progress
        preference.storage(context).setInt(preference.key, 27)
        val numberFormat =
            NumberFormat.getPercentInstance(context.resources.configuration.getLocales().get(0))
        val expectedDescription = numberFormat.format(0.27)

        val widget =
            preference.createAndBindWidget<SliderPreference>(context).apply { inflateViewHolder() }

        assertThat(widget.slider!!.stateDescription).isEqualTo(expectedDescription)
    }

    @Test
    @EnableFlags(Flags.FLAG_FLOATING_MENU_TRANSPARENCY_SLIDER_ANNOUNCES_DISABLED)
    fun bind_onEnabled_setsPercentageStateDescription() {
        setFloatingMenuEnabled(true)
        setFadeEnabled(true)

        // sets transparency progress
        preference.storage(context).setInt(preference.key, 27)
        val numberFormat =
            NumberFormat.getPercentInstance(context.resources.configuration.getLocales().get(0))
        val expectedDescription = numberFormat.format(0.27)

        val widget =
            preference.createAndBindWidget<SliderPreference>(context).apply { inflateViewHolder() }

        assertThat(widget.slider!!.stateDescription).isEqualTo(expectedDescription)
    }

    @Test
    @EnableFlags(Flags.FLAG_FLOATING_MENU_TRANSPARENCY_SLIDER_ANNOUNCES_DISABLED)
    fun bind_onDisabled_setsDisabledStateDescription() {
        setFloatingMenuEnabled(true)
        setFadeEnabled(false)

        val widget =
            preference.createAndBindWidget<SliderPreference>(context).apply { inflateViewHolder() }
        val expectedDescription = context.getString(com.android.settingslib.R.string.disabled)

        assertThat(widget.slider!!.stateDescription).isEqualTo(expectedDescription)
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

    @Test
    fun onCreate_fadeEnabledSettingChanged_callsNotifyPreferenceChange() {
        setFadeEnabled(false)

        preference.onCreate(mockPrefLifecycleContext)
        setFadeEnabled(true)

        verify(mockPrefLifecycleContext).notifyPreferenceChange(preference.key)
    }

    @Test
    fun onDestroy_fadeEnabledSettingChanged_shouldNotCallNotifyPreferenceChange() {
        setFadeEnabled(false)

        preference.onCreate(mockPrefLifecycleContext)
        preference.onDestroy(mockPrefLifecycleContext)
        setFadeEnabled(true)

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

    private fun setFadeEnabled(enabled: Boolean) {
        settingsSecureStore.setBoolean(
            Settings.Secure.ACCESSIBILITY_FLOATING_MENU_FADE_ENABLED,
            enabled,
        )
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    }
}
