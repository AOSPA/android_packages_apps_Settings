/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.accessibility.colorandmotion.ui

import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.colorandmotion.data.TextCursorBlinkRateDataStore
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TextCursorBlinkRatePreferenceTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val appContext: Context = ApplicationProvider.getApplicationContext()

    protected val preference: TextCursorBlinkRatePreference =
        TextCursorBlinkRatePreference(appContext)

    private val noBlinkDurationMs = 0
    private val minSliderValue = 0
    private val noBlinkLabel = "Don\'t blink"

    private val slowBlinkDurationMs = 1000
    private val slowBlinkSliderValue = 1
    private val slowBlinkLabel = "50%"

    private val fastBlinkDurationMs = 333
    private val maxSliderValue = 11
    private val fastBlinkLabel = "150%"

    private val defaultDurationMs = 500
    private val defaultSliderValue = 6
    private val defaultBlinkLabel = "100% (default)"

    @Test
    @DisableFlags(Flags.FLAG_TEXT_CURSOR_BLINKING_USER_SETTING)
    fun isAvailable_returnsFalseWhenFlagDisabled() {
        assertThat(preference.isAvailable(appContext)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_TEXT_CURSOR_BLINKING_USER_SETTING)
    fun isAvailable_returnsTrueWhenFlagEnabled() {
        assertThat(preference.isAvailable(appContext)).isTrue()
    }

    @Test
    fun value_default() {
        getStorage().setInt(preference.key, defaultSliderValue)
        val widget = createWidget()
        assertThat(widget.value).isEqualTo(defaultSliderValue)
        assertThat(widget.slider?.stateDescription).isEqualTo(defaultBlinkLabel)
        assertThat(getSecureSettingsValue()).isEqualTo(defaultDurationMs)
    }

    @Test
    fun value_noBlink() {
        setValue(minSliderValue)
        val widget = createWidget()
        assertThat(widget.value).isEqualTo(minSliderValue)
        assertThat(widget.slider?.stateDescription).isEqualTo(noBlinkLabel)
        assertThat(getSecureSettingsValue()).isEqualTo(noBlinkDurationMs)
    }

    @Test
    fun value_slowBlink() {
        setValue(slowBlinkSliderValue)
        val widget = createWidget()
        assertThat(widget.value).isEqualTo(slowBlinkSliderValue)
        assertThat(widget.slider?.stateDescription).isEqualTo(slowBlinkLabel)
        assertThat(getSecureSettingsValue()).isEqualTo(slowBlinkDurationMs)
    }

    @Test
    fun value_fastBlink() {
        setValue(maxSliderValue)
        val widget = createWidget()
        assertThat(widget.value).isEqualTo(maxSliderValue)
        assertThat(widget.slider?.stateDescription).isEqualTo(fastBlinkLabel)
        assertThat(getSecureSettingsValue()).isEqualTo(fastBlinkDurationMs)
    }

    protected fun createWidget(): TextCursorBlinkRateSliderPreference =
        preference.createAndBindWidget<TextCursorBlinkRateSliderPreference>(appContext).apply {
            inflateViewHolder()
        }

    private fun getStorage(): TextCursorBlinkRateDataStore = preference.storage(appContext)

    private fun setValue(index: Int?) = getStorage().setInt(preference.key, index)

    private fun getSecureSettingsValue(): Int {
        return Settings.Secure.getInt(
            appContext.contentResolver,
            Settings.Secure.ACCESSIBILITY_TEXT_CURSOR_BLINK_INTERVAL_MS,
            defaultDurationMs,
        )
    }
}
