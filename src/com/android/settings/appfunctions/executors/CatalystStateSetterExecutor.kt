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

package com.android.settings.appfunctions.executors

import android.app.appsearch.GenericDocument
import android.os.OutcomeReceiver
import android.service.settings.preferences.SetValueRequest
import android.service.settings.preferences.SetValueResult
import android.service.settings.preferences.SettingsPreferenceValue
import android.util.Log
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.appfunctions.DeviceStateSetterExecutorResult
import com.android.settings.appfunctions.GenericDeviceStateItemSetterParams
import com.android.settings.appfunctions.SettingsPreferenceServiceClientManager
import com.google.android.appfunctions.schema.common.v1.devicestate.SetDeviceStateItemResponse
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * A [DeviceStateExecutor] that sets device state for Settings that are exposed using Catalyst
 * framework. Configured in [CatalystStateProviderConfig].
 */
class CatalystStateSetterExecutor() : DeviceStateExecutor {
    /**
     * Asynchronously executes the device state set request.
     *
     * @param appFunctionType The app function type requested by the caller. The executor will only
     *   execute if it matches the requested type.
     * @param params The required params to execute the set request.
     * @return A [DeviceStateSetterExecutorResult] containing the outcome of the set operation.
     */
    override suspend fun execute(
        appFunctionType: DeviceStateAppFunctionType,
        params: GenericDocument?,
    ): DeviceStateSetterExecutorResult =
        withContext(Dispatchers.IO) {
            try {
                if (params == null) {
                    throw IllegalArgumentException("Provided params are null.")
                }
                val result = executeSetDeviceStateRequest(appFunctionType, params)

                DeviceStateSetterExecutorResult(result = result)
            } catch (e: Exception) {
                Log.e(TAG, "error executing $appFunctionType", e)
                DeviceStateSetterExecutorResult(result = null)
            }
        }

    private suspend fun executeSetDeviceStateRequest(
        appFunctionType: DeviceStateAppFunctionType,
        params: GenericDocument,
    ): SetDeviceStateItemResponse? {
        Log.i(TAG, "Executing a setDeviceStateRequest with appFunctionType: $appFunctionType")
        val parsedParams = GenericDeviceStateItemSetterParams(appFunctionType, params)
        return when (appFunctionType) {
            DeviceStateAppFunctionType.SET_DEVICE_STATE -> setDeviceState(parsedParams)
            DeviceStateAppFunctionType.OFFSET_DEVICE_STATE_BY_VALUE ->
                offsetNumericDeviceStateByValue(parsedParams)
            DeviceStateAppFunctionType.ADJUST_DEVICE_STATE_BY_PERCENTAGE ->
                adjustNumericDeviceStateByPercentage(parsedParams)
            else -> {
                Log.i(TAG, "Unrecognised appFunctionType: $appFunctionType")
                null
            }
        }
    }

    private suspend fun setDeviceState(
        genericParams: GenericDeviceStateItemSetterParams
    ): SetDeviceStateItemResponse {
        val client = SettingsPreferenceServiceClientManager.client
        if (client == null) {
            Log.e(TAG, "SettingsPreferenceServiceClient is not available.")
            return SetDeviceStateItemResponse(
                isSuccessful = false,
                currentValue = "",
                failureReason = "Service client not available",
            )
        }

        val params = genericParams.getSetDeviceStateItemParams()
        val settingsPreferenceValue =
            toSettingsPreferenceValue(params.value)
                ?: return SetDeviceStateItemResponse(
                    isSuccessful = false,
                    currentValue = "",
                    failureReason = "Unsupported value type",
                )

        val fullKey = params.key
        val keyParts = fullKey.split("/", limit = 2)
        val screenKey = keyParts.getOrNull(0)
        val key = keyParts.getOrElse(1) { fullKey }

        if (screenKey == null) {
            Log.e(TAG, "Unsupported key: ${params.key}")
            return SetDeviceStateItemResponse(
                isSuccessful = false,
                currentValue = "",
                failureReason = "Unsupported key value",
            )
        }
        val request =
            SetValueRequest.Builder(
                    screenKey,
                    key,
                    settingsPreferenceValue, // mPreferenceValue
                )
                .build()

        return suspendCancellableCoroutine { continuation ->
            client.setPreferenceValue(
                request,
                Dispatchers.Default.asExecutor(),
                object : OutcomeReceiver<SetValueResult, Exception> {
                    override fun onResult(result: SetValueResult) {
                        continuation.resume(
                            SetDeviceStateItemResponse(
                                isSuccessful = result.resultCode == SetValueResult.RESULT_OK,
                                // TODO(b/461469319): Set the current value
                                currentValue = "",
                            )
                        )
                    }

                    override fun onError(error: Exception) {
                        Log.e(TAG, "Error setting preference value", error)
                        continuation.resume(
                            // TODO(461469319): set the failure reason and the current value
                            SetDeviceStateItemResponse(
                                isSuccessful = false,
                                currentValue = "",
                                failureReason = "Error: ${error.message}",
                            )
                        )
                    }
                },
            )
        }
    }

    private fun toSettingsPreferenceValue(value: String): SettingsPreferenceValue? =
        when {
            value.equals("true", ignoreCase = true) || value.equals("false", ignoreCase = true) -> {

                SettingsPreferenceValue.Builder(SettingsPreferenceValue.TYPE_BOOLEAN)
                    .setBooleanValue(value.toBoolean())
                    .build()
            }
            value.toIntOrNull() != null -> {
                SettingsPreferenceValue.Builder(SettingsPreferenceValue.TYPE_INT)
                    .setIntValue(value.toInt())
                    .build()
            }
            else -> {
                Log.e(TAG, "Unsupported value type: $value")
                null
            } // Unsupported type
        }

    private fun offsetNumericDeviceStateByValue(
        genericParams: GenericDeviceStateItemSetterParams
    ): SetDeviceStateItemResponse? {
        val params = genericParams.getOffsetNumericDeviceStateItemByValueParams()
        // TODO: call into appropriate setter APIs

        return null
    }

    private fun adjustNumericDeviceStateByPercentage(
        genericParams: GenericDeviceStateItemSetterParams
    ): SetDeviceStateItemResponse? {
        val params = genericParams.getAdjustNumericDeviceStateItemByPercentageParams()
        // TODO: call into appropriate setter APIs

        return null
    }

    companion object {
        private const val TAG = "CatalystStateSetterExecutor"
    }
}
