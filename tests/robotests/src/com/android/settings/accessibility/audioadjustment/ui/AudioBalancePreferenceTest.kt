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

package com.android.settings.accessibility.audioadjustment.ui

import android.content.Context
import android.view.HapticFeedbackConstants.CLOCK_TICK
import android.view.HapticFeedbackConstants.NO_HAPTICS
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.accessibility.audioadjustment.data.AudioBalanceDataStore
import com.android.settings.accessibility.audioadjustment.data.AudioBalanceDataStore.Companion.BALANCE_CENTER_VALUE
import com.android.settings.accessibility.audioadjustment.data.AudioBalanceDataStore.Companion.BALANCE_MAX_VALUE
import com.android.settings.accessibility.audioadjustment.data.AudioBalanceDataStore.Companion.BALANCE_MIN_VALUE
import com.android.settings.accessibility.audioadjustment.ui.AudioBalancePreference.Companion.SNAP_TO_PERCENTAGE
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.inflateViewHolder
import com.android.settings.testutils.setValueFromUser
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.preference.createAndBindWidget
import com.google.android.material.slider.Slider
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/** Tests for [AudioBalancePreference] */
@RunWith(AndroidJUnit4::class)
class AudioBalancePreferenceTest {

    @get:Rule val settingsStoreRule = SettingsStoreRule()

    private lateinit var context: Context
    private lateinit var preference: AudioBalancePreference

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        preference = AudioBalancePreference(context)
    }

    @Test
    fun staticProperties_areCorrect() {
        assertThat(preference.key).isEqualTo(AudioBalanceDataStore.SETTING_KEY)
        assertThat(preference.title).isEqualTo(R.string.accessibility_toggle_primary_balance_title)
        assertThat(preference.purpose).isEqualTo(R.string.a11y_audio_balance_purpose)
        assertThat(preference.getMinValue(context)).isEqualTo(BALANCE_MIN_VALUE)
        assertThat(preference.getMaxValue(context)).isEqualTo(BALANCE_MAX_VALUE)
    }

    @Test
    fun storage_isAudioBalanceDataStore() {
        assertThat(preference.storage(context)).isInstanceOf(AudioBalanceDataStore::class.java)
    }

    @Test
    fun createWidget_returnsAudioBalanceSliderPreferenceWidget() {
        val widget = preference.createWidget(context)

        assertThat(widget).isInstanceOf(AudioBalanceSliderPreferenceWidget::class.java)
    }

    @Test
    fun createAndInflateWidget_verifyLabelsAreSet() {
        val prefViewHolder = preference.createWidget(context).inflateViewHolder()
        val startLabel = prefViewHolder.findViewById(android.R.id.text1) as TextView
        val endLabel = prefViewHolder.findViewById(android.R.id.text2) as TextView

        assertThat(startLabel.text.toString())
            .isEqualTo(context.getString(R.string.accessibility_toggle_primary_balance_left_label))
        assertThat(endLabel.text.toString())
            .isEqualTo(context.getString(R.string.accessibility_toggle_primary_balance_right_label))
    }

    @Test
    fun createAndInflateWidget_sliderReflectsValueAndDoesNotVibrate() {
        // set to center
        SettingsSystemStore.get(context).setFloat(AudioBalanceDataStore.SETTING_KEY, 0f)

        val (widget, slider) = createAndInflateWidget()
        val shadowSlider = shadowOf(slider)

        assertThat(widget.value).isEqualTo(BALANCE_CENTER_VALUE)
        assertThat(slider.value).isWithin(TOLERANCE).of(BALANCE_CENTER_VALUE.toFloat())
        assertThat(shadowSlider.lastHapticFeedbackPerformed()).isEqualTo(NO_HAPTICS)
    }

    @Test
    fun handleSliderChange_fromUser_closeToCenter_snapsToCenterAndVibrates() {
        val (widget, slider) = createAndInflateWidget()
        val shadowSlider = shadowOf(slider)

        // Inside snap threshold
        val newValue = BALANCE_CENTER_VALUE + SNAP_THRESHOLD - 1
        slider.setValueFromUser(newValue)

        assertThat(widget.value).isEqualTo(BALANCE_CENTER_VALUE)
        assertThat(shadowSlider.lastHapticFeedbackPerformed()).isEqualTo(CLOCK_TICK)
    }

    @Test
    fun handleSliderChange_fromUser_closeToCenter_alreadySnapped_doesNotVibrate() {
        val (widget, slider) = createAndInflateWidget()
        val shadowSlider = shadowOf(slider)

        // Snap first time
        var newValue = BALANCE_CENTER_VALUE + SNAP_THRESHOLD - 1
        slider.setValueFromUser(newValue)
        // clear the haptic feedback if there was any
        slider.performHapticFeedback(NO_HAPTICS)

        // Snap second time, should not vibrate again
        newValue = BALANCE_CENTER_VALUE - SNAP_THRESHOLD + 1
        slider.setValueFromUser(newValue)
        assertThat(widget.value).isEqualTo(BALANCE_CENTER_VALUE)
        assertThat(shadowSlider.lastHapticFeedbackPerformed()).isEqualTo(NO_HAPTICS)
    }

    @Test
    fun handleSliderChange_fromUser_farFromCenter_doesNotSnap() {
        val (widget, slider) = createAndInflateWidget()
        val shadowSlider = shadowOf(slider)

        // Outside snap threshold
        val newValue = BALANCE_CENTER_VALUE + SNAP_THRESHOLD + 10
        slider.setValueFromUser(newValue)

        assertThat(widget.value).isEqualTo(newValue.roundToInt())
        assertThat(shadowSlider.lastHapticFeedbackPerformed()).isEqualTo(NO_HAPTICS)
    }

    private fun createAndInflateWidget(): Pair<AudioBalanceSliderPreferenceWidget, Slider> {
        val widget = preference.createAndBindWidget<AudioBalanceSliderPreferenceWidget>(context)
        widget.inflateViewHolder()

        val slider = widget.slider ?: throw AssertionError("Widget did not contain a slider.")

        return widget to slider
    }

    companion object {
        private const val TOLERANCE = 1e-5f
        private const val SNAP_THRESHOLD = SNAP_TO_PERCENTAGE * BALANCE_MAX_VALUE
    }
}
