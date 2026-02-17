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
import android.util.AttributeSet
import android.view.View
import androidx.preference.PreferenceViewHolder
import com.android.settingslib.widget.SliderPreference
import com.android.settingslib.widget.preference.slider.R as SettingsLibSliderR
import com.google.android.material.slider.Slider

/**
 * [SliderPreference] for audio balance adjustment. The slider is centered and always use LTR layout
 * on the slider and the slider's label.
 */
internal class AudioBalanceSliderPreferenceWidget
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    SliderPreference(context, attrs, defStyleAttr) {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        // Prevent RTL mirroring: the labels "Left" and "Right" must remain in their literal
        // physical positions. See b/190354990.
        holder.findViewById(SettingsLibSliderR.id.slider_frame)?.layoutDirection =
            View.LAYOUT_DIRECTION_LTR
        holder.findViewById(SettingsLibSliderR.id.label_frame)?.layoutDirection =
            View.LAYOUT_DIRECTION_LTR

        // Needs to call setCentered prior to calling onBindViewHolder
        // because calling Slider#setCentered would reset the Slider's value to middle.
        // super.onBindViewHolder will apply the correct persisted value.
        (holder.findViewById(SettingsLibSliderR.id.slider) as? Slider)?.isCentered = true

        super.onBindViewHolder(holder)
    }
}
