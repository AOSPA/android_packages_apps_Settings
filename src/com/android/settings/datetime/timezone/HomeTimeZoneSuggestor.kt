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
import android.icu.text.Collator
import android.icu.util.TimeZone
import android.telephony.SubscriptionManager
import android.util.TimeUtils
import androidx.annotation.VisibleForTesting
import java.util.Date

/** Provide time zone suggestions based on phone state. */
open class HomeTimeZoneSuggestor(context: Context) {

    private val mSubscriptionManager: SubscriptionManager =
        context.getSystemService(SubscriptionManager::class.java)
    private val mTimeZoneInfoFormatter: TimeZoneInfo.Formatter
    private val mCollator: Collator

    init {
        val locale = context.resources.configuration.locales.get(0)
        mTimeZoneInfoFormatter = TimeZoneInfo.Formatter(locale, Date())
        mCollator = Collator.getInstance(locale)
    }

    /** @return A list of {@link TimeZoneInfo} suggestions. */
    open fun getTimeZoneSuggestions(): List<TimeZoneInfo> {
        // Get the current default time zone
        val currentTimeZoneInfo = mTimeZoneInfoFormatter.format(TimeZone.getDefault())

        // Get suggestions based on SIM country codes
        val simTimeZoneInfos = getCountryCodesFromSim().flatMap { getTimeZoneInfosForCountry(it) }

        return (listOfNotNull(currentTimeZoneInfo) + simTimeZoneInfos)
            .filterNotNull()
            .filter { it.timeZone.id != TimeZone.UNKNOWN_ZONE_ID }
            .distinct()
            .sortedWith(TimeZoneInfoComparator(mCollator, Date()))
    }

    private fun getCountryCodesFromSim(): List<String> {
        val subscriptionInfoList =
            try {
                mSubscriptionManager.activeSubscriptionInfoList
            } catch (e: Exception) {
                // Catch SecurityException or telephony dead-object exceptions
                android.util.Log.w(
                    "HomeTimeZoneSuggestor",
                    "Failed to fetch active subscriptions",
                    e,
                )
                null
            }

        return subscriptionInfoList
            .orEmpty() // Handles the null case safely and cleanly
            .map { it.countryIso }
            .filter { !it.isNullOrEmpty() }
            .map { it.uppercase() }
            .distinct()
    }

    private fun getTimeZoneInfosForCountry(countryCode: String): List<TimeZoneInfo> {
        return TimeUtils.getTimeZoneIdsForCountryCode(countryCode)
            .orEmpty()
            .map { id -> TimeZone.getTimeZone(id) }
            .map { timeZone -> mTimeZoneInfoFormatter.format(timeZone) }
    }

    @VisibleForTesting
    internal class TimeZoneInfoComparator(private val mCollator: Collator, private val mNow: Date) :
        Comparator<TimeZoneInfo> {

        override fun compare(tzi1: TimeZoneInfo, tzi2: TimeZoneInfo): Int {
            var result =
                tzi1.timeZone.getOffset(mNow.time).compareTo(tzi2.timeZone.getOffset(mNow.time))
            if (result == 0) {
                result = tzi1.timeZone.rawOffset.compareTo(tzi2.timeZone.rawOffset)
            }
            if (result == 0) {
                result = mCollator.compare(tzi1.exemplarLocation ?: "", tzi2.exemplarLocation ?: "")
            }
            if (result == 0 && tzi1.genericName != null && tzi2.genericName != null) {
                result = mCollator.compare(tzi1.genericName, tzi2.genericName)
            }
            return result
        }
    }
}
