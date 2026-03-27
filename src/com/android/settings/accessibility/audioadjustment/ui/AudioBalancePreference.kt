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
import android.view.View
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.accessibility.audioadjustment.data.AudioBalanceDataStore
import com.android.settings.accessibility.audioadjustment.data.AudioBalanceDataStore.Companion.BALANCE_CENTER_VALUE
import com.android.settings.accessibility.audioadjustment.data.AudioBalanceDataStore.Companion.BALANCE_MAX_VALUE
import com.android.settings.accessibility.audioadjustment.data.AudioBalanceDataStore.Companion.BALANCE_MIN_VALUE
import com.android.settings.accessibility.audioadjustment.data.AudioBalanceDataStore.Companion.SETTING_KEY
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.IntRangeValuePreference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.widget.SliderPreference
import com.android.settingslib.widget.SliderPreferenceBinding
import com.google.android.material.slider.Slider
import kotlin.math.abs
import kotlin.math.roundToInt

/** Preference for adjusting the audio balance between left and right channels. */
class AudioBalancePreference(context: Context) :
    IntRangeValuePreference, SliderPreferenceBinding {

    // b/494214799
    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    private val dataStore by lazy { AudioBalanceDataStore(context) }

    override fun getMinValue(context: Context): Int = BALANCE_MIN_VALUE

    override fun getMaxValue(context: Context): Int = BALANCE_MAX_VALUE

    override val key: String
        get() = KEY

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val supportsWrite = true

    override val purpose: Int
        get() = R.string.a11y_audio_balance_purpose

    override val title: Int
        get() = R.string.accessibility_toggle_primary_balance_title

    private var isSnappedToCenter = false

    override fun createWidget(context: Context): SliderPreference {
        return AudioBalanceSliderPreferenceWidget(context).apply {
            setTextStart(R.string.accessibility_toggle_primary_balance_left_label)
            setTextEnd(R.string.accessibility_toggle_primary_balance_right_label)
            setHapticFeedbackMode(SliderPreference.HAPTIC_FEEDBACK_MODE_ON_ENDS)
            updatesContinuously = true
            setExtraChangeListener { slider, newValue, fromUser ->
                handleSliderChange(context, this, slider, newValue, fromUser)
            }
        }
    }

    private fun handleSliderChange(
        context: Context,
        audioBalancePrefWidget: AudioBalanceSliderPreferenceWidget,
        slider: Slider,
        newValue: Float,
        fromUser: Boolean,
    ) {
        if (!fromUser) return

        val progress = newValue.roundToInt()
        if (shouldSnapToCenter(progress)) {
            audioBalancePrefWidget.value = BALANCE_CENTER_VALUE
            slider.value = BALANCE_CENTER_VALUE.toFloat()

            if (!isSnappedToCenter) {
                slider.performHapticFeedback(CLOCK_TICK)
                isSnappedToCenter = true
            }
        } else {
            isSnappedToCenter = false
        }

        audioBalancePrefWidget.setSliderStateDescription(
            createStateDescription(context, audioBalancePrefWidget.value)
        )
    }

    private fun shouldSnapToCenter(progress: Int): Boolean {
        val snapThreshold = BALANCE_MAX_VALUE * SNAP_TO_PERCENTAGE
        return abs(progress - BALANCE_CENTER_VALUE) < snapThreshold
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        if (preference is SliderPreference) {
            preference.setSliderStateDescription(
                createStateDescription(preference.context, preference.value)
            )
        }
    }

    private fun createStateDescription(context: Context, progress: Int): String {
        val rightPercent = (100f * progress / BALANCE_MAX_VALUE).toInt()
        val leftPercent = 100 - rightPercent

        val rightPercentString = Utils.formatPercentage(rightPercent)
        val leftPercentString = Utils.formatPercentage(leftPercent)

        val isLayoutRtl =
            context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

        // Determine if we should show Right or Left first based on dominance and layout direction
        val showRightFirst =
            rightPercent > leftPercent || (rightPercent == leftPercent && isLayoutRtl)

        return if (showRightFirst) {
            context.getString(
                R.string.audio_seek_bar_state_right_first,
                rightPercentString,
                leftPercentString,
            )
        } else {
            context.getString(
                R.string.audio_seek_bar_state_left_first,
                leftPercentString,
                rightPercentString,
            )
        }
    }

    override fun storage(context: Context): KeyValueStore = dataStore

    override val sensitivityLevel: Int
        get() = SensitivityLevel.NO_SENSITIVITY

    companion object {
        const val KEY = SETTING_KEY
        // Percentage of max to be used as a snap to threshold
        internal const val SNAP_TO_PERCENTAGE: Float = 0.03f
    }
}
