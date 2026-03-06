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
import android.icu.util.TimeZone
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.text.TextUtils
import java.util.Date
import java.util.stream.Collectors
import java.util.stream.Stream

/** Provide time zone suggestions based on phone state. */
open class HomeTimeZoneSuggestor(context: Context) {

    private val mSubscriptionManager: SubscriptionManager =
        context.getSystemService(SubscriptionManager::class.java)
    private val mTimeZoneInfoFormatter: TimeZoneInfo.Formatter

    init {
        mTimeZoneInfoFormatter =
            TimeZoneInfo.Formatter(context.resources.configuration.locales.get(0), Date())
    }

    /** @return A list of {@link TimeZoneInfo} suggestions. */
    open fun getTimeZoneSuggestions(): List<TimeZoneInfo> {
        return getCountryCodesFromSim()
            .flatMap { getTimeZoneInfosForCountry(it) }
            .distinct()
            .collect(Collectors.toList())
    }

    private fun getCountryCodesFromSim(): Stream<String> {
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

        if (subscriptionInfoList == null) {
            return Stream.empty()
        }

        return subscriptionInfoList
            .stream()
            .map { obj: SubscriptionInfo -> obj.countryIso }
            .filter { iso: String? -> !TextUtils.isEmpty(iso) }
            .map { obj: String -> obj.uppercase() }
            .distinct()
    }

    private fun getTimeZoneInfosForCountry(countryCode: String): Stream<TimeZoneInfo> {
        return TimeZone.getAvailableIDs(TimeZone.SystemTimeZoneType.ANY, countryCode, null)
            .stream()
            .map { id: String? -> TimeZone.getCanonicalID(id) }
            .filter { id: String? -> !TextUtils.isEmpty(id) }
            .distinct()
            .map { id: String? -> TimeZone.getTimeZone(id) }
            .map { timeZone: TimeZone? -> mTimeZoneInfoFormatter.format(timeZone) }
    }
}
