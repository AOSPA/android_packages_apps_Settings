/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.input.gamecontroller

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.android.settings.R

/** A Preference that overrides the content description of the summary view. */
class GameControllerRemappingPreference(context: Context, attrs: AttributeSet?) :
    Preference(context, attrs) {

    @VisibleForTesting
    var summaryView: TextView? = null
        private set

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        summaryView = holder.findViewById(android.R.id.summary) as? TextView
        updateSummaryContentDescription()
    }

    fun updateSummaryContentDescription() {
        summaryView?.let {
            val summaryText = it.text
            it.contentDescription =
                context.getString(
                    R.string.game_controller_remapping_preference_content_description,
                    summaryText,
                )
        }
    }
}
