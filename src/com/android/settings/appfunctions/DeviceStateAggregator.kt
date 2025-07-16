/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.appfunctions

import androidx.annotation.Keep
import com.android.settings.appfunctions.providers.DeviceStateProvider
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Orchestrates the collection and transformation of device state information.
 *
 * This class fetches data from multiple [DeviceStateProvider]s in parallel, aggregates the
 * results to produce the final [DeviceStateResponse].
*
* @property providers The list of [DeviceStateProvider]s to query for device state.
*/
@Keep
class DeviceStateAggregator(
    private val providers: List<DeviceStateProvider>
) {
    /**
     * Aggregates device state from all registered providers.
     *
     * This function performs the following steps:
     * 1. Calls all [DeviceStateProvider]s concurrently to gather device state information.
     * 2. Combines the states and hint text from all provider results.
     * 3. Constructs and returns the final [DeviceStateResponse].
     *
     * @param requestCategory The category of device state to fetch, passed to each provider.
     * @param deviceLocale The current locale of the device, included in the final response.
     * @return A [DeviceStateResponse] containing the fully aggregated device state.
     */
     suspend fun aggregate(
        requestCategory: DeviceStateCategory,
        deviceLocale: String
    ): DeviceStateResponse {
        val providerResults = coroutineScope {
            providers.map { provider ->
                async { provider.provide(requestCategory) }
            }.map { it.await() }
        }

        val allStates = providerResults.flatMap { it.states }
        val allHintText = providerResults.mapNotNull { it.hintText }.joinToString(separator = "\n")

        return DeviceStateResponse(
            perScreenDeviceStates = allStates,
            deviceLocale = deviceLocale
        )
    }
}