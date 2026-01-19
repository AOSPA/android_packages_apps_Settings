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
import android.service.settings.preferences.GetValueRequest
import android.service.settings.preferences.GetValueResult
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
                val result =
                    when (appFunctionType) {
                        DeviceStateAppFunctionType.SET_DEVICE_STATE ->
                            executeSetDeviceStateRequest(appFunctionType, params)
                        else ->
                            SetDeviceStateItemResponse(
                                isSuccessful = false,
                                currentValue = "Not implemented",
                            )
                    }

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

        val currentValue = getPreference(screenKey, key)
        val settingsPreferenceValue =
            toSettingsPreferenceValue(params.value, currentValue?.type)
                ?: return SetDeviceStateItemResponse(
                    isSuccessful = false,
                    currentValue = settingsPreferenceValueToString(currentValue),
                    failureReason = "Unsupported value type or value",
                )

        val request = SetValueRequest.Builder(screenKey, key, settingsPreferenceValue).build()

        return suspendCancellableCoroutine { continuation ->
            client.setPreferenceValue(
                request,
                Dispatchers.Default.asExecutor(),
                object : OutcomeReceiver<SetValueResult, Exception> {
                    override fun onResult(result: SetValueResult) {
                        continuation.resume(
                            SetDeviceStateItemResponse(
                                isSuccessful = result.resultCode == SetValueResult.RESULT_OK,
                                currentValue =
                                    settingsPreferenceValueToString(settingsPreferenceValue),
                            )
                        )
                    }

                    override fun onError(error: Exception) {
                        Log.e(TAG, "Error setting preference value", error)
                        continuation.resume(
                            // TODO(461469319): set the failure reason
                            SetDeviceStateItemResponse(
                                isSuccessful = false,
                                currentValue = settingsPreferenceValueToString(currentValue),
                                failureReason = "Error: ${error.message}",
                            )
                        )
                    }
                },
            )
        }
    }

    // This should probably be moved to a common file when getDeviceStateItem is implemented in
    // order to reuse this.
    private suspend fun getPreference(screenKey: String, key: String): SettingsPreferenceValue? {
        val client = SettingsPreferenceServiceClientManager.client ?: return null
        val request = GetValueRequest.Builder(screenKey, key).build()
        return suspendCancellableCoroutine { continuation ->
            client.getPreferenceValue(
                request,
                Dispatchers.Default.asExecutor(),
                object : OutcomeReceiver<GetValueResult, Exception> {
                    override fun onResult(result: GetValueResult) {
                        continuation.resume(result.value)
                    }

                    override fun onError(error: Exception) {
                        Log.e(TAG, "Error getting preference value", error)
                        continuation.resume(null)
                    }
                },
            )
        }
    }

    private fun toSettingsPreferenceValue(value: String, type: Int?): SettingsPreferenceValue? {
        return when (type) {
            SettingsPreferenceValue.TYPE_BOOLEAN ->
                SettingsPreferenceValue.Builder(SettingsPreferenceValue.TYPE_BOOLEAN)
                    .setBooleanValue(value.toBooleanStrictOrNull() ?: return null)
                    .build()
            SettingsPreferenceValue.TYPE_INT ->
                SettingsPreferenceValue.Builder(SettingsPreferenceValue.TYPE_INT)
                    .setIntValue(value.toIntOrNull() ?: return null)
                    .build()
            SettingsPreferenceValue.TYPE_STRING ->
                SettingsPreferenceValue.Builder(SettingsPreferenceValue.TYPE_STRING)
                    .setStringValue(value)
                    .build()
            SettingsPreferenceValue.TYPE_LONG ->
                SettingsPreferenceValue.Builder(SettingsPreferenceValue.TYPE_LONG)
                    .setLongValue(value.toLongOrNull() ?: return null)
                    .build()
            SettingsPreferenceValue.TYPE_DOUBLE ->
                SettingsPreferenceValue.Builder(SettingsPreferenceValue.TYPE_DOUBLE)
                    .setDoubleValue(value.toDoubleOrNull() ?: return null)
                    .build()
            else -> {
                Log.e(TAG, "Unsupported value type from preference: $type")
                null
            }
        }
    }

    private fun offsetNumericDeviceStateByValue(
        genericParams: GenericDeviceStateItemSetterParams
    ): SetDeviceStateItemResponse? {
        val params = genericParams.getOffsetNumericDeviceStateItemByValueParams()
        // TODO: call into appropriate setter APIs

        return null
    }

    private fun settingsPreferenceValueToString(value: SettingsPreferenceValue?): String {
        return when (value?.type) {
            SettingsPreferenceValue.TYPE_BOOLEAN -> value.booleanValue.toString()
            SettingsPreferenceValue.TYPE_INT -> value.intValue.toString()
            SettingsPreferenceValue.TYPE_STRING -> value.stringValue ?: ""
            else -> ""
        }
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
