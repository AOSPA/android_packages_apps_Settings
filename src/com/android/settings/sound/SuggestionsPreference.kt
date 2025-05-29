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

package com.android.settings.sound

import android.content.Context
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider

/** Preference for media device suggestions. */
class SuggestionsPreference() :
    PreferenceMetadata, PreferenceSummaryProvider, PreferenceAvailabilityProvider {

    override val key
        get() = KEY

    override val title
        get() = R.string.media_controls_suggestions_title

    override fun intent(context: Context) = SuggestionsPreferenceUtils.getSuggestionIntent(context)

    override fun isAvailable(context: Context) = intent(context) != null

    override fun getSummary(context: Context) =
        SuggestionsPreferenceUtils.getSuggestionDescription(context)

    companion object {
        const val KEY = "media_suggestions"
    }
}
