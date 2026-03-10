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
import android.content.Intent
import android.provider.Settings
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.core.BasePreferenceController

class TimeFormatTernaryPreferenceController(context: Context, key: String) :
    BasePreferenceController(context, key) {

    private var isFromSUW = false
    private var updateTimeAndDateCallback: UpdateTimeAndDateCallback = UpdateTimeAndDateCallback {}

    /** Set the Time and Date callback */
    fun setTimeAndDateCallback(
        callback: UpdateTimeAndDateCallback
    ): TimeFormatTernaryPreferenceController {
        updateTimeAndDateCallback = callback
        return this
    }

    /** Set if current fragment is launched via SUW */
    fun setFromSUW(isFromSUW: Boolean): TimeFormatTernaryPreferenceController {
        this.isFromSUW = isFromSUW
        return this
    }

    override fun getAvailabilityStatus(): Int {
        return if (isFromSUW) {
            DISABLED_DEPENDENT_SETTING
        } else {
            AVAILABLE
        }
    }

    override fun updateState(preference: Preference) {
        super.updateState(preference)
        preference.isEnabled = availabilityStatus == AVAILABLE
        refreshSummary(preference)
    }

    override fun getSummary(): CharSequence {
        val currentValue =
            Settings.System.getString(mContext.contentResolver, Settings.System.TIME_12_24)

        return when (currentValue) {
            null -> mContext.getString(R.string.time_format_automatic)
            HOURS_24 -> mContext.getString(R.string.time_format_24_hour)
            else -> mContext.getString(R.string.time_format_12_hour)
        }
    }

    companion object {
        const val HOURS_12 = "12"
        const val HOURS_24 = "24"

        // Update the signature to take String instead of Boolean for the 3-state logic
        fun update24HourFormat(context: Context, value: String?) {
            Settings.System.putString(context.contentResolver, Settings.System.TIME_12_24, value)

            // Notify the system that time format changed
            timeUpdated(context, value)
        }

        private fun timeUpdated(context: Context, value: String?) {
            val timeChanged = Intent(Intent.ACTION_TIME_CHANGED)
            timeChanged.addFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND)
            val timeFormatPreference: Int
            if (value == null) {
                timeFormatPreference = Intent.EXTRA_TIME_PREF_VALUE_USE_LOCALE_DEFAULT
            } else {
                timeFormatPreference =
                    if (value == HOURS_24) Intent.EXTRA_TIME_PREF_VALUE_USE_24_HOUR
                    else Intent.EXTRA_TIME_PREF_VALUE_USE_12_HOUR
            }
            timeChanged.putExtra(Intent.EXTRA_TIME_PREF_24_HOUR_FORMAT, timeFormatPreference)
            context.sendBroadcast(timeChanged)
        }
    }
}
