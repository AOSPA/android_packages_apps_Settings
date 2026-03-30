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

package com.android.settings.datetime

import android.content.Context
import android.icu.text.DateFormat
import android.provider.Settings
import android.text.TextUtils
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.core.BasePreferenceController
import com.android.settingslib.widget.SelectorWithWidgetPreference
import java.util.Calendar

class TimeFormatOptionsController(context: Context, preferenceKey: String) :
    BasePreferenceController(context, preferenceKey) {

    override fun getAvailabilityStatus(): Int {
        return AVAILABLE
    }

    override fun getSummary(): CharSequence? {
        return when (preferenceKey) {
            KEY_12_HOUR -> FORMATTER_12_HOUR.format(SAMPLE_TIME)
            KEY_24_HOUR -> FORMATTER_24_HOUR.format(SAMPLE_TIME)
            else -> super.getSummary()
        }
    }

    override fun updateState(preference: Preference?) {
        super.updateState(preference)
        val pref = preference as? SelectorWithWidgetPreference ?: return
        updateCheckedState(pref)
    }

    private fun updateCheckedState(pref: SelectorWithWidgetPreference) {
        val value = Settings.System.getString(mContext.contentResolver, Settings.System.TIME_12_24)

        pref.isChecked =
            when (pref.key) {
                KEY_AUTO -> TextUtils.isEmpty(value)
                KEY_12_HOUR -> TimeFormatTernaryPreferenceController.HOURS_12 == value
                KEY_24_HOUR -> TimeFormatTernaryPreferenceController.HOURS_24 == value
                else -> false
            }
    }

    override fun handlePreferenceTreeClick(preference: Preference): Boolean {
        if (!TextUtils.equals(preference.key, preferenceKey)) {
            return super.handlePreferenceTreeClick(preference)
        }

        val key = preference.key
        val newValueToSave =
            when (key) {
                KEY_AUTO -> null
                KEY_12_HOUR -> TimeFormatTernaryPreferenceController.HOURS_12
                KEY_24_HOUR -> TimeFormatTernaryPreferenceController.HOURS_24
                else -> return false
            }

        // Use the centralized update method to ensure Broadcasts are sent correctly
        TimeFormatTernaryPreferenceController.update24HourFormat(mContext, newValueToSave)

        val screen = preference.preferenceManager?.preferenceScreen ?: return true
        TIME_FORMAT_OPTIONS.forEach { updatePreferenceState(screen, it) }
        return true
    }

    private fun updatePreferenceState(screen: PreferenceScreen, key: String) {
        screen.findPreference<SelectorWithWidgetPreference>(key)?.let { updateCheckedState(it) }
    }

    companion object {
        private const val KEY_AUTO = "time_format_automatic"
        private const val KEY_12_HOUR = "time_format_12_hour"
        private const val KEY_24_HOUR = "time_format_24_hour"

        private val TIME_FORMAT_OPTIONS = listOf(KEY_AUTO, KEY_12_HOUR, KEY_24_HOUR)

        private val SAMPLE_TIME =
            Calendar.getInstance()
                .apply {
                    set(Calendar.HOUR_OF_DAY, 13)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                .time

        private val FORMATTER_12_HOUR = DateFormat.getInstanceForSkeleton("hm")
        private val FORMATTER_24_HOUR = DateFormat.getInstanceForSkeleton("Hm")
    }
}
