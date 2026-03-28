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
import android.content.Context
import android.service.settings.preferences.SetValueResult
import android.util.Log
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.appfunctions.DeviceStateSetterExecutorResult
import com.android.settings.appfunctions.GenericDeviceStateItemSetterParams
import com.android.settings.appfunctions.utils.DEEP_LINK_ONLY
import com.android.settings.appfunctions.utils.DO_NOT_EXPOSE
import com.android.settings.appfunctions.utils.REQUIRES_CONFIRMATION
import com.android.settings.appfunctions.utils.determineParamName
import com.android.settings.appfunctions.utils.getPreference
import com.android.settings.appfunctions.utils.setPreference
import com.android.settings.appfunctions.utils.settingsPreferenceValueToString
import com.android.settings.appfunctions.utils.toSettingsPreferenceValue
import com.android.settingslib.metadata.KeyParameters
import com.android.settingslib.service.transformCatalystSetValueResponse
import com.google.android.appfunctions.schema.common.v1.devicestate.SetDeviceStateItemResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A [DeviceStateExecutor] that sets device state for Settings that are exposed using Catalyst
 * framework. Configured in [CatalystStateProviderConfig].
 */
class CatalystStateSetterExecutor(private val context: Context) : DeviceStateExecutor {

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
        val params = genericParams.getSetDeviceStateItemParams()
        val fullKey = params.key
        val (screenKey, key) = splitFullKey(fullKey)

        val keyParameters = buildKeyParameters(screenKey, params.itemizationKeys)

        val currentValue = getPreference(context, screenKey, key, keyParameters)
        var failureReason: String? = null

        if (currentValue == null) {
            failureReason = "Unsupported value type or value"
        }

        if (
            currentValue?.sensitivityLevel == DO_NOT_EXPOSE ||
                currentValue?.sensitivityLevel == REQUIRES_CONFIRMATION ||
                currentValue?.sensitivityLevel == DEEP_LINK_ONLY
        ) {
            failureReason = "Set unavailable due to sensitivity level restrictions"

            if (
                currentValue.sensitivityLevel == DO_NOT_EXPOSE &&
                    toSettingsPreferenceValue(
                        params.value,
                        currentValue.settingsPreferenceValue?.type,
                    ) != null
            ) {
                Log.e(
                    "UNEXPECTED",
                    "Get current value should not be possible on DO_NOT_EXPOSE preferences",
                )

                return SetDeviceStateItemResponse(
                    isSuccessful = false,
                    currentValue = "N/A",
                    failureReason = failureReason,
                )
            }
        }

        if (currentValue != null) {
            if (!currentValue.isAvailable) {
                failureReason = "Preference unavailable"
            } else if (!currentValue.isEnabled) {
                failureReason = "Preference not enabled"
            } else if (!currentValue.isWriteable) {
                failureReason = "Set is not supported for this preference"
            }
        }

        if (failureReason != null) {
            return SetDeviceStateItemResponse(
                isSuccessful = false,
                currentValue = "N/A",
                failureReason = failureReason,
            )
        }

        val settingsPreferenceValue =
            toSettingsPreferenceValue(params.value, currentValue?.settingsPreferenceValue?.type)
                ?: return SetDeviceStateItemResponse(
                    isSuccessful = false,
                    currentValue =
                        settingsPreferenceValueToString(currentValue?.settingsPreferenceValue),
                    failureReason = failureReason,
                )

        if (currentValue != null && currentValue.hasError) {
            return SetDeviceStateItemResponse(
                isSuccessful = false,
                currentValue = "N/A",
                failureReason = settingsPreferenceValueToString(currentValue.settingsPreferenceValue),
            )
        }

        return try {
            val setterResponse =
                setPreference(context, screenKey, key, settingsPreferenceValue, keyParameters)
            val setValueResult = transformCatalystSetValueResponse(setterResponse)

            return if (setValueResult.resultCode == SetValueResult.RESULT_OK) {
                SetDeviceStateItemResponse(
                    isSuccessful = true,
                    currentValue = settingsPreferenceValueToString(settingsPreferenceValue),
                )
            } else {
                val failureReasonMessage =
                    setterResponse.failureReason?.let { " due to $it" } ?: ""

                SetDeviceStateItemResponse(
                    isSuccessful = false,
                    currentValue =
                        settingsPreferenceValueToString(currentValue?.settingsPreferenceValue),
                    failureReason = "Error with code ${setterResponse.errorCode}$failureReasonMessage",
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting preference value", e)
            SetDeviceStateItemResponse(
                isSuccessful = false,
                currentValue =
                    settingsPreferenceValueToString(currentValue?.settingsPreferenceValue),
                failureReason = "Error: ${e.message}",
            )
        }
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

    private fun splitFullKey(fullKey: String): Pair<String, String> {
        val keyParts = fullKey.split("/", limit = 2)
        val screenKey = keyParts[0]
        val key = keyParts.getOrElse(1) { fullKey }
        return screenKey to key
    }

    private fun buildKeyParameters(
        screenKey: String,
        itemizationKeys: List<String>,
    ): KeyParameters? {
        if (itemizationKeys.isEmpty()) return null
        val paramName = determineParamName(screenKey)
        if (paramName != null) {
            Log.d(TAG, "Determined paramName: $paramName for screen: $screenKey")
            return KeyParameters(mapOf(paramName to itemizationKeys.joinToString(",")))
        }
        return null
    }

    companion object {
        private const val TAG = "CatalystStateSetterExecutor"
    }
}
