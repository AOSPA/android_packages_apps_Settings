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

package com.android.settings.bluetooth

import android.bluetooth.AudioInputControl.MUTE_DISABLED
import android.bluetooth.AudioInputControl.MUTE_MUTED
import android.bluetooth.AudioInputControl.MUTE_NOT_MUTED
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.preference.PreferenceViewHolder
import com.android.settings.R
import com.android.settingslib.bluetooth.hearingdevices.ui.AmbientVolumeUi.AMBIENT_VOLUME_LEVEL_NUMBER
import com.android.settingslib.widget.SliderPreference
import kotlin.math.ceil

/**
 * A [SliderPreference] specifically designed for controlling ambient sound volume.
 *
 * This preference extends the standard slider with an additional mute icon at the end. It manages
 * three states for the mute functionality: not muted, muted, and disabled. The slider's value is
 * linked to the mute state; for example, when muted, the slider value is automatically set to its
 * minimum. It also provides a method to convert the slider's raw value into a discrete volume
 * level.
 */
class AmbientVolumeSliderPreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    SliderPreference(context, attrs, defStyleAttr) {

    private var onMuteIconClickListener: View.OnClickListener? = null
    private var muteState: Int = MUTE_NOT_MUTED
    private var muteIconResId: Int = 0
    private var muteIconContentDescriptionId: Int = 0

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val iconEndId = com.android.settingslib.widget.preference.slider.R.id.icon_end
        val muteIcon = holder.findViewById(iconEndId) as? ImageView
        val muteIconFrame = muteIcon?.parent as? ViewGroup
        if (muteIconFrame != null) {
            muteIcon.isEnabled = true
            muteIconFrame.isEnabled = true
            muteIconFrame.setBackgroundResource(R.drawable.ambient_icon_background_expressive)

            muteIconFrame.isVisible = muteState != MUTE_DISABLED
            muteIconFrame.setOnClickListener { v ->
                if (muteState == MUTE_DISABLED) {
                    return@setOnClickListener
                }
                val newMuteState = if (muteState == MUTE_MUTED) MUTE_NOT_MUTED else MUTE_MUTED
                setMuteState(newMuteState)
                onMuteIconClickListener?.onClick(v)
            }
            if (muteIconContentDescriptionId != 0) {
                muteIconFrame.setContentDescription(
                    muteIconFrame.context.getString(muteIconContentDescriptionId)
                )
            }
            if (muteIconResId != 0) {
                muteIcon.setImageResource(muteIconResId)
            }
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        if (!enabled) {
            value = min
        }
    }

    /**
     * Sets a click listener for the mute icon at the end of the slider.
     *
     * The listener will be invoked after the mute state has been internally updated, allowing
     * external components to react to the mute state change initiated by the user.
     *
     * @param listener The [View.OnClickListener] to be attached to the mute icon.
     */
    fun setOnMuteIconClickListener(listener: View.OnClickListener) {
        onMuteIconClickListener = listener
        notifyChanged()
    }

    /**
     * Sets the mute state for the preference and updates the UI accordingly.
     *
     * This method controls the visibility and icon of the mute button, sets the content description
     * for accessibility, and adjusts the slider's value based on the state.
     * - [MUTE_MUTED]: The slider value is set to the minimum, and the icon shows muted.
     * - [MUTE_NOT_MUTED]: The icon shows unmuted.
     * - [MUTE_DISABLED]: The mute icon is hidden.
     *
     * @param muteState The new mute state, which should be one of [MUTE_MUTED], [MUTE_NOT_MUTED],
     *   or [MUTE_DISABLED].
     */
    fun setMuteState(muteState: Int) {
        this.muteState = muteState
        when (muteState) {
            MUTE_MUTED -> {
                value = min
                muteIconResId = com.android.settingslib.R.drawable.ic_ambient_mute
                muteIconContentDescriptionId = R.string.bluetooth_ambient_volume_unmute
            }
            MUTE_NOT_MUTED -> {
                muteIconResId = com.android.settingslib.R.drawable.ic_ambient_unmute
                muteIconContentDescriptionId = R.string.bluetooth_ambient_volume_mute
            }
            else -> {
                muteIconResId = 0
                muteIconContentDescriptionId = 0
            }
        }
        notifyChanged()
    }

    /**
     * Retrieves the current mute state of the preference.
     *
     * @return The current mute state, which is one of [MUTE_MUTED], [MUTE_NOT_MUTED], or
     *   [MUTE_DISABLED].
     */
    fun getMuteState(): Int {
        return muteState
    }

    /**
     * Calculates and returns the discrete volume level based on the slider's current value.
     *
     * The slider's continuous value (from [getMin] to [getMax]) is mapped to a discrete integer
     * level ranging from 0 up to [AMBIENT_VOLUME_LEVEL_NUMBER] - 1. If the preference is disabled
     * or muted, the volume level is considered 0.
     *
     * @return A discrete integer representing the current volume level, or 0 if mute disabled.
     */
    fun getVolumeLevel(): Int {
        if (!isEnabled || muteState != MUTE_NOT_MUTED) {
            return 0
        }
        val levelGap = (max - min).toDouble() / (AMBIENT_VOLUME_LEVEL_NUMBER - 1).toDouble()
        return ceil((value - min).toDouble() / levelGap).toInt()
    }
}
