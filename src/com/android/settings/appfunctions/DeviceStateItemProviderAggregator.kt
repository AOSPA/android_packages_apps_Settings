/*
 * Copyright 2026 The Android Open Source Project
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

import android.app.appsearch.GenericDocument
import com.android.settings.appfunctions.executors.DeviceStateExecutor
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItemResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Aggregates results from multiple [DeviceStateExecutor]s that provide a single
 * [DeviceStateItemResponse].
 */
class DeviceStateItemProviderAggregator(private val executors: List<DeviceStateExecutor>) :
    DeviceStateAggregator(executors) {
    override suspend fun aggregate(
        appFunctionType: DeviceStateAppFunctionType,
        params: GenericDocument,
        deviceLocale: String,
    ): DeviceStateItemResponse {
        val executorResults = coroutineScope {
            executors
                .map { executor ->
                    async {
                        executor.execute(appFunctionType, params)
                            as DeviceStateItemProviderExecutorResult
                    }
                }
                .awaitAll()
        }

        val validResults = executorResults.mapNotNull { it.result }

        return when (validResults.size) {
            0 -> throw IllegalStateException("No valid executor found for $appFunctionType")
            1 -> validResults.first()
            else -> throw IllegalStateException("Multiple executors found for $appFunctionType")
        }
    }
}
