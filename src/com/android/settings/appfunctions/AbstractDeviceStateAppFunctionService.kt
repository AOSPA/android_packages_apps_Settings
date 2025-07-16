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

import android.app.appsearch.GenericDocument
import android.content.Context
import android.content.res.Configuration
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import android.os.Trace
import android.util.Log
import androidx.annotation.Keep
import com.android.extensions.appfunctions.AppFunctionException
import com.android.extensions.appfunctions.AppFunctionException.ERROR_FUNCTION_NOT_FOUND
import com.android.extensions.appfunctions.AppFunctionService
import com.android.extensions.appfunctions.ExecuteAppFunctionRequest
import com.android.extensions.appfunctions.ExecuteAppFunctionResponse
import com.android.settings.appfunctions.providers.DeviceStateProvider
import com.android.settings.appfunctions.providers.CatalystStateProvider
import com.android.settings.appfunctions.providers.AndroidApiStateProvider
import com.android.settings.appfunctions.DeviceStateAggregator
import com.android.settings.appfunctions.DeviceStateCategory
import com.android.settings.utils.getLocale
import java.util.Locale
import kotlinx.coroutines.runBlocking

/**
 * An abstract [AppFunctionService] that provides device state information.
 *
 * Subclasses must implement [providers] to define the data sources and transformations
 * for device state.
 */
@Keep
abstract class AbstractDeviceStateAppFunctionService : AppFunctionService() {
    protected lateinit var englishContext: Context
        private set

    /** The list of [DeviceStateProvider]s to query for device state information. */
    abstract val providers: List<DeviceStateProvider>

    private val aggregator: DeviceStateAggregator by lazy {
        DeviceStateAggregator(providers)
    }

    override fun onCreate() {
        super.onCreate()
        englishContext = createEnglishContext()
    }

    final override fun onExecuteFunction(
        request: ExecuteAppFunctionRequest,
        callingPackage: String, cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<ExecuteAppFunctionResponse, AppFunctionException>
    ) {
        val requestCategory = DeviceStateCategory.fromId(request.functionIdentifier)
        if (requestCategory == null) {
            callback.onError(
                AppFunctionException(
                    ERROR_FUNCTION_NOT_FOUND,
                    "${request.functionIdentifier} not supported."
                )
            )
            return
        }
        runBlocking {
            Trace.beginSection("DeviceStateAppFunction ${request.functionIdentifier}")
            Log.d(TAG, "device state app function ${request.functionIdentifier} called.")
            val responseData = aggregator.aggregate(
                requestCategory,
                applicationContext.getLocale().toString()
            )
            val response = buildResponse(responseData)
            callback.onResult(response)
            Log.d(TAG, "app function ${request.functionIdentifier} fulfilled.")
            Trace.endSection()
        }
    }

    private fun buildResponse(responseData: Any): ExecuteAppFunctionResponse {
        val jetpackDocument =
            androidx.appsearch.app.GenericDocument.fromDocumentClass(responseData)
        val platformDocument =
            GenericDocumentToPlatformConverter.toPlatformGenericDocument(jetpackDocument)
        val result =
            GenericDocument.Builder<GenericDocument.Builder<*>>("", "", "")
                .setPropertyDocument(
                    ExecuteAppFunctionResponse.PROPERTY_RETURN_VALUE,
                    platformDocument
                )
                .build()
        return ExecuteAppFunctionResponse(result)
    }

    private fun createEnglishContext(): Context {
        val configuration = Configuration(applicationContext.resources.configuration)
        configuration.setLocale(Locale.US)
        return applicationContext.createConfigurationContext(configuration)
    }

    companion object {
        private const val TAG = "SettingsDeviceStateAppFunctionService"
    }
}