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

package com.android.settings.development

import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import androidx.preference.PreferenceScreen
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.accessibility.TextCursorBlinkRateSliderPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class TextCursorBlinkRatePreferenceControllerTest {
    @get:Rule
    val setFlagsRule = SetFlagsRule()
    val context: Context = ApplicationProvider.getApplicationContext()

    val controller = TextCursorBlinkRatePreferenceController(context)

    val preference = TextCursorBlinkRateSliderPreference(context)
    val preferenceScreen = mock<PreferenceScreen>()

    private val noBlinkDurationMs = 0;
    private val minSliderValue = 0;

    private val slowBlinkDurationMs = 1000;
    private val slowBlinkSliderValue = 1;

    private val fastBlinkDurationMs = 333;
    private val maxSliderValue = 11;

    private val defaultDurationMs = 500;
    private val defaultSliderValue = 6;

    @Before
    fun setup() {
        whenever(preferenceScreen.findPreference<TextCursorBlinkRateSliderPreference>(
            controller.getPreferenceKey())).thenReturn(preference)
        controller.displayPreference(preferenceScreen)
    }

    @Test
    @DisableFlags(android.view.accessibility.Flags.FLAG_TEXT_CURSOR_BLINK_INTERVAL)
    fun getAvailabilityStatus_unavailableWhenFlagDisabled() {
        assertThat(controller.isAvailable()).isFalse()
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_TEXT_CURSOR_BLINK_INTERVAL)
    fun getAvailabilityStatus_availableWhenFlagEnabled() {
        assertThat(controller.isAvailable()).isTrue()
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_TEXT_CURSOR_BLINK_INTERVAL)
    fun onPreferenceChange_zeroValue_noBlink() {
        controller.onPreferenceChange(preference, minSliderValue)

        val value = getSecureSettingsValue()
        assertThat(value).isEqualTo(noBlinkDurationMs)
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_TEXT_CURSOR_BLINK_INTERVAL)
    fun onPreferenceChange_minNonZeroValue_slowBlink() {
        controller.onPreferenceChange(preference, slowBlinkSliderValue)

        val value = getSecureSettingsValue()
        assertThat(value).isEqualTo(slowBlinkDurationMs)
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_TEXT_CURSOR_BLINK_INTERVAL)
    fun onPreferenceChange_maxValue_fastBlink() {
        controller.onPreferenceChange(preference, maxSliderValue)

        val value = getSecureSettingsValue()
        assertThat(value).isEqualTo(fastBlinkDurationMs)
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_TEXT_CURSOR_BLINK_INTERVAL)
    fun onPreferenceChange_defaultValue_defaultBlink() {
        controller.onPreferenceChange(preference, defaultSliderValue)

        val value = getSecureSettingsValue()
        assertThat(value).isEqualTo(defaultDurationMs)
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_TEXT_CURSOR_BLINK_INTERVAL)
    fun updateState_noBlink_zeroValue() {
        setSecureSettingsValue(noBlinkDurationMs)
        controller.updateState(preference)

        assertThat(preference.value).isEqualTo(minSliderValue)
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_TEXT_CURSOR_BLINK_INTERVAL)
    fun updateState_slowBlink_minNonZeroValue() {
        setSecureSettingsValue(slowBlinkDurationMs)
        controller.updateState(preference)

        assertThat(preference.value).isEqualTo(slowBlinkSliderValue)
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_TEXT_CURSOR_BLINK_INTERVAL)
    fun updateState_fastBlink_maxValue() {
        setSecureSettingsValue(fastBlinkDurationMs)
        controller.updateState(preference)

        assertThat(preference.value).isEqualTo(maxSliderValue)
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_TEXT_CURSOR_BLINK_INTERVAL)
    fun updateState_defaultBlink_defaultValue() {
        setSecureSettingsValue(defaultDurationMs)
        controller.updateState(preference)

        assertThat(preference.value).isEqualTo(defaultSliderValue)
    }

    private fun getSecureSettingsValue(): Int {
        return Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_TEXT_CURSOR_BLINK_INTERVAL_MS,
            defaultDurationMs
        )
    }

    private fun setSecureSettingsValue(value: Int) {
        Settings.Secure.putInt(context.getContentResolver(),
            Settings.Secure.ACCESSIBILITY_TEXT_CURSOR_BLINK_INTERVAL_MS,
            value
        )
    }
}