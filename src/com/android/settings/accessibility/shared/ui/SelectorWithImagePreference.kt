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

package com.android.settings.accessibility.shared.ui

import android.content.Context
import android.widget.ImageView
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceViewHolder
import com.android.settings.R

/**
 * A custom radio button preference that displays an image, a title, and an optional summary.
 *
 * @param context The Context the preference is running in.
 * @param image The resource ID of the image to display.
 */
class SelectorWithImagePreference(private val context: Context, private val image: Int) :
    CheckBoxPreference(context) {
    private var mListener: OnClickListener? = null

    init {
        widgetLayoutResource =
            com.android.settingslib.widget.preference.selector.R.layout
                .settingslib_preference_widget_radiobutton
        layoutResource = R.layout.selector_with_image_preference
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        (holder.findViewById(R.id.image) as? ImageView)?.setImageResource(image)
    }

    /** Processes a click on the preference. */
    override fun onClick() {
        mListener?.onRadioButtonClicked(this)
    }

    /**
     * Sets the callback to be invoked when this preference is clicked by the user.
     *
     * @param listener The callback to be invoked
     */
    fun setOnClickListener(listener: OnClickListener?) {
        mListener = listener
    }

    /** Interface definition for a callback to be invoked when the preference is clicked. */
    interface OnClickListener {
        /**
         * Called when a preference has been clicked.
         *
         * @param emitter The clicked preference
         */
        fun onRadioButtonClicked(emitter: SelectorWithImagePreference)
    }
}
