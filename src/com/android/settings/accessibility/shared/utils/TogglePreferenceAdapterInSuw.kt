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

package com.android.settings.accessibility.shared.utils

import android.view.View
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceViewHolder
import com.android.settings.accessibility.PreferenceAdapterInSuw
import com.android.settings.widget.FocusIndicatorDrawable

/**
 * An adapter that adds a focus ring indicator to the main switch bar for toggle preferences in the
 * setup wizard.
 */
class TogglePreferenceAdapterInSuw(preferenceGroup: PreferenceGroup) :
    PreferenceAdapterInSuw(preferenceGroup) {

    override fun onBindViewHolder(holder: PreferenceViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val context = holder.itemView.context
        if (!shouldShowFocusRingsInSuw(context)) {
            return
        }

        val view = holder.itemView
        view
            .findViewById<View>(
                com.android.settingslib.widget.mainswitch.R.id.settingslib_main_switch_bar
            )
            ?.let { mainSwitchBar ->
                view.isFocusable = false
                view.foreground = null
                mainSwitchBar.isFocusable = true
                mainSwitchBar.foreground =
                    FocusIndicatorDrawable.Builder(context)
                        .withHorizontalPaddingAdjustment(
                            FOCUS_INDICATOR_HORIZONTAL_PADDING_ADJUSTMENT_DP
                        )
                        .withVerticalPaddingAdjustment(
                            FOCUS_INDICATOR_VERTICAL_PADDING_ADJUSTMENT_DP
                        )
                        .withCornerRadius(FOCUS_INDICATOR_CORNER_RADIUS_DP)
                        .build()
            }

        view
            .findViewById<View>(
                com.android.settingslib.widget.preference.illustration.R.id.illustration_frame
            )
            ?.let { illustrationFrame ->
                illustrationFrame.foreground = FocusIndicatorDrawable.Builder(context).build()
            }
    }

    companion object {
        private const val FOCUS_INDICATOR_HORIZONTAL_PADDING_ADJUSTMENT_DP = -3
        private const val FOCUS_INDICATOR_VERTICAL_PADDING_ADJUSTMENT_DP = 13
        private const val FOCUS_INDICATOR_CORNER_RADIUS_DP = 999 // Fully rounded.
    }
}
