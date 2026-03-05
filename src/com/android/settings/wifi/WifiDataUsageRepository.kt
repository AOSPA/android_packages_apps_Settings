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

package com.android.settings.wifi

import android.content.Context
import android.net.NetworkTemplate
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.android.settings.datausage.DataUsageUtils
import com.android.settingslib.net.DataUsageController

/** Repository for Wi-Fi data usage information and navigation within Android Settings. */
class WifiDataUsageRepository(private val context: Context) {

    @set:VisibleForTesting
    var dataUsageController: DataUsageController = DataUsageController(context)

    /** Identifies if the Wi-Fi data usage is available. */
    val isAvailable: Boolean
        get() = hasFeatureWifi && historicalUsageLevel > 0

    /** Identifies if the device hardware supports Wi-Fi. Initialized once. */
    val hasFeatureWifi: Boolean = hasWifiRadio(context)

    /**
     * Provides the current historical data usage in bytes. Fetched dynamically to ensure data
     * freshness.
     */
    val historicalUsageLevel: Long
        get() {
            val usage = dataUsageController.getHistoricalUsageLevel(WIFI_TEMPLATE)
            if (usage <= 0) Log.w(TAG, "No Wi-Fi data usage! usage:$usage")
            return usage
        }

    companion object {
        private const val TAG = "WifiDataUsageRepository"
        private var cachedHasFeatureWifi: Boolean? = null

        /**
         * Global check for Wi-Fi radio availability.
         *
         * This is cached in the companion object because it requires a Context dependency
         * (preventing a simple static 'lazy' property) and should only be performed once per
         * process to avoid redundant system calls during repository re-instantiation.
         */
        @JvmStatic
        fun hasWifiRadio(context: Context): Boolean {
            return cachedHasFeatureWifi
                ?: DataUsageUtils.hasWifiRadio(context).also {
                    cachedHasFeatureWifi = it
                    if (!it) Log.w(TAG, "hasWifiRadio:false")
                }
        }

        /** Network Template to query just Wi-Fi network usage. */
        val WIFI_TEMPLATE: NetworkTemplate by lazy {
            NetworkTemplate.Builder(NetworkTemplate.MATCH_WIFI).build()
        }
    }
}
