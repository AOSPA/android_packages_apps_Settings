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

package com.android.settings.appfunctions.providers

import android.app.appsearch.GenericDocument
import android.content.Context
import android.util.Log
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.appfunctions.DeviceStateSetterExecutorResult
import com.google.android.appfunctions.schema.common.v1.devicestate.AdjustNumericDeviceStateByPercentageParams
import com.google.android.appfunctions.schema.common.v1.devicestate.OffsetNumericDeviceStateByValueParams
import com.google.android.appfunctions.schema.common.v1.devicestate.SetDeviceStateParams
import com.google.android.appfunctions.schema.common.v1.devicestate.SetDeviceStateResponse
import com.google.android.appfunctions.schema.common.v1.devicestate.ToggleDeviceStateParams

/* A [DeviceStateExecutor] that provides device state metadata information for Settings that are
exposed using Catalyst framework. Configured in [CatalystStateProviderConfig]. */
class CatalystStateSetterExecutor(
    private val context: Context,
    private val englishContext: Context,
) : DeviceStateExecutor {

    override suspend fun execute(
        appFunctionType: DeviceStateAppFunctionType,
        params: GenericDocument?,
    ): DeviceStateSetterExecutorResult {
        try {
            var result = executeSetDeviceStateRequest(appFunctionType, params)

            // TODO: replace with actual result
            var dummyResult =
                SetDeviceStateResponse(isSuccessful = true, currentValue = "currentValue")
            return DeviceStateSetterExecutorResult(result = dummyResult)
        } catch (e: Exception) {
            Log.e(TAG, "error executing $appFunctionType", e)
            return DeviceStateSetterExecutorResult(result = null)
        }
    }

    private fun executeSetDeviceStateRequest(
        appFunctionType: DeviceStateAppFunctionType,
        params: GenericDocument?,
    ): SetDeviceStateResponse? {
        Log.i(TAG, "Executing a setDeviceStateRequest with appFunctionType: $appFunctionType")
        return when (appFunctionType) {
            DeviceStateAppFunctionType.SET_DEVICE_STATE -> setDeviceState(params)
            DeviceStateAppFunctionType.OFFSET_DEVICE_STATE_BY_VALUE ->
                offsetNumericDeviceStateByValue(params)
            DeviceStateAppFunctionType.ADJUST_DEVICE_STATE_BY_PERCENTAGE ->
                adjustNumericDeviceStateByPercentage(params)
            DeviceStateAppFunctionType.TOGGLE_DEVICE_STATE -> toggleDeviceStateParams(params)
            else -> {
                Log.i(TAG, "Unrecognised appFunctionType: $appFunctionType")
                return null
            }
        }
    }

    private fun setDeviceState(unparsedParams: GenericDocument?): SetDeviceStateResponse? {
        val params = parseSetDeviceStateParams(unparsedParams)
        // TODO: call into appropriate setter APIs

        return null
    }

    private fun offsetNumericDeviceStateByValue(
        unparsedParams: GenericDocument?
    ): SetDeviceStateResponse? {
        val params = parseOffsetNumericDeviceStateByValueParams(unparsedParams)
        // TODO: call into appropriate setter APIs

        return null
    }

    private fun adjustNumericDeviceStateByPercentage(
        unparsedParams: GenericDocument?
    ): SetDeviceStateResponse? {
        val params = parseAdjustNumericDeviceStateByPercentageParams(unparsedParams)
        // TODO: call into appropriate setter APIs

        return null
    }

    private fun toggleDeviceStateParams(unparsedParams: GenericDocument?): SetDeviceStateResponse? {
        val params = parseToggleDeviceStateParams(unparsedParams)
        // TODO: call into appropriate setter APIs

        return null
    }

    private fun parseSetDeviceStateParams(params: GenericDocument?): SetDeviceStateParams? {
        val setDeviceStateParams: SetDeviceStateParams? = null

        val unparsedParams = params?.getPropertyDocument("setDeviceStateParams")
        if (unparsedParams == null) {
            throw IllegalArgumentException("Missing setDeviceStateParams in the request: $params")
        } else {
            Log.i(TAG, "Found setDeviceStateParams: $unparsedParams")
            // TODO: need to manually parse and construct the object
        }
        return setDeviceStateParams
    }

    private fun parseOffsetNumericDeviceStateByValueParams(
        params: GenericDocument?
    ): OffsetNumericDeviceStateByValueParams? {
        val offsetNumericDeviceStateByValueParams: OffsetNumericDeviceStateByValueParams? = null

        val unparsedParams = params?.getPropertyDocument("offsetNumericDeviceStateByValueParams")
        if (unparsedParams == null) {
            throw IllegalArgumentException(
                "Missing offsetNumericDeviceStateByValueParams in the request: $params"
            )
        } else {
            Log.i(TAG, "Found offsetNumericDeviceStateByValueParams: $unparsedParams")
            // TODO: need to manually parse and construct the object
        }
        return offsetNumericDeviceStateByValueParams
    }

    private fun parseAdjustNumericDeviceStateByPercentageParams(
        params: GenericDocument?
    ): AdjustNumericDeviceStateByPercentageParams? {
        val adjustNumericDeviceStateByPercentageParams:
            AdjustNumericDeviceStateByPercentageParams? =
            null

        val unparsedParams =
            params?.getPropertyDocument("adjustNumericDeviceStateByPercentageParams")
        if (unparsedParams == null) {
            throw IllegalArgumentException(
                "Missing adjustNumericDeviceStateByPercentageParams in the request: $params"
            )
        } else {
            Log.i(TAG, "Found adjustNumericDeviceStateByPercentageParams: $unparsedParams")
            // TODO: need to manually parse and construct the object
        }
        return adjustNumericDeviceStateByPercentageParams
    }

    private fun parseToggleDeviceStateParams(params: GenericDocument?): ToggleDeviceStateParams? {
        val toggleDeviceStateParams: ToggleDeviceStateParams? = null

        val unparsedParams = params?.getPropertyDocument("toggleDeviceStateParams")
        if (unparsedParams == null) {
            throw IllegalArgumentException(
                "Missing toggleDeviceStateParams in the request: $params"
            )
        } else {
            Log.i(TAG, "Found toggleDeviceStateParams: $unparsedParams")
            // TODO: need to manually parse and construct the object
        }
        return toggleDeviceStateParams
    }

    companion object {
        private const val TAG = "CatalystStateSetterExecutor"
    }
}
