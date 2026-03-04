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

package com.android.settings.datetime.timezone

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import com.android.settings.core.BasePreferenceController
import com.android.settingslib.datetime.ZoneGetter
import com.android.settingslib.utils.ThreadUtils
import java.util.Date
import java.util.TimeZone

class SuggestionsTimeZonePreferenceController(context: Context, preferenceKey: String) :
    BasePreferenceController(context, preferenceKey) {

    private var mHomeTimeZoneSettings: HomeTimeZoneSettings? = null

    fun setParentFragment(fragment: HomeTimeZoneSettings) {
        mHomeTimeZoneSettings = fragment
    }

    @VisibleForTesting
    internal var homeTimeZoneSuggestor: HomeTimeZoneSuggestor = HomeTimeZoneSuggestor(mContext)

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        val suggestionCategory = screen.findPreference<PreferenceCategory>(preferenceKey)
        suggestionCategory?.removeAll()

        loadSuggestionsAsync(suggestionCategory)
    }

    private fun loadSuggestionsAsync(category: PreferenceCategory?) {
        if (category == null) return

        category.isVisible = false

        ThreadUtils.postOnBackgroundThread {
            val suggestions = homeTimeZoneSuggestor.getTimeZoneSuggestions()

            ThreadUtils.postOnMainThread {
                if (suggestions.isNotEmpty()) {
                    category.removeAll()
                    suggestions.forEach { category.addPreference(createTimeZonePreference(it)) }

                    category.isVisible = mHomeTimeZoneSettings?.isHomeTimeZoneEnabled() ?: false
                }
            }
        }
    }

    override fun updateState(preference: Preference) {
        super.updateState(preference)
        preference.isVisible = mHomeTimeZoneSettings?.isHomeTimeZoneEnabled() ?: false
    }

    private fun createTimeZonePreference(timeZoneInfo: TimeZoneInfo): Preference {
        val preference = Preference(mContext)
        val displayName = timeZoneInfo.exemplarLocation ?: timeZoneInfo.genericName
        val summary =
            ZoneGetter.getTimeZoneOffsetAndName(
                    mContext,
                    TimeZone.getTimeZone(timeZoneInfo.id),
                    Date(),
                )
                ?.toString() ?: timeZoneInfo.gmtOffset.toString()

        preference.title = displayName
        preference.summary = summary
        preference.key = timeZoneInfo.id
        preference.setOnPreferenceClickListener { p ->
            mHomeTimeZoneSettings?.setHomeTimeZone(p.key)
            true
        }
        return preference
    }

    override fun getAvailabilityStatus(): Int {
        return AVAILABLE
    }
}
