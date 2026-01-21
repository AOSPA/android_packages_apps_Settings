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
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import androidx.annotation.OpenForTesting
import androidx.preference.PreferenceViewHolder
import com.android.settings.R
import com.android.settingslib.widget.SliderPreference

@OpenForTesting
open class TextCursorBlinkRateSliderPreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    SliderPreference(context, attrs, defStyleAttr) {
    private var resetButton: Button? = null
    private var clickListener: View.OnClickListener? = null

    init {
        setTickVisible(true)
        sliderIncrement = 1
        setIconStart(R.drawable.ic_remove_24dp)
        setIconStartContentDescription(R.string.accessibility_text_cursor_blink_rate_slower_desc)
        setIconEnd(R.drawable.ic_add_24dp)
        setIconEndContentDescription(R.string.accessibility_text_cursor_blink_rate_faster_desc)
        layoutResource = R.layout.text_cursor_blink_rate_slider_preference
    }

    /** Set the click listener for the reset button. */
    open fun setResetClickListener(listener: View.OnClickListener?) {
        clickListener = listener
    }

    /** Return Button */
    fun getButton(): Button? {
        return resetButton
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        resetButton = holder.findViewById(R.id.reset_button) as Button?
        resetButton?.setOnClickListener(clickListener)
    }
}
