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
import com.google.android.appfunctions.schema.common.v1.devicestate.AdjustNumericDeviceStateItemByPercentageParams
import com.google.android.appfunctions.schema.common.v1.devicestate.OffsetNumericDeviceStateItemByValueParams
import com.google.android.appfunctions.schema.common.v1.devicestate.SetDeviceStateItemParams
import com.google.android.appfunctions.schema.common.v1.devicestate.SetDeviceStateItemResponse
import com.google.android.appfunctions.schema.common.v1.devicestate.ToggleDeviceStateItemParams

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
                SetDeviceStateItemResponse(isSuccessful = true, currentValue = "currentValue")
            return DeviceStateSetterExecutorResult(result = dummyResult)
        } catch (e: Exception) {
            Log.e(TAG, "error executing $appFunctionType", e)
            return DeviceStateSetterExecutorResult(result = null)
        }
    }

    private fun executeSetDeviceStateRequest(
        appFunctionType: DeviceStateAppFunctionType,
        params: GenericDocument?,
    ): SetDeviceStateItemResponse? {
        Log.i(TAG, "Executing a setDeviceStateRequest with appFunctionType: $appFunctionType")
        return when (appFunctionType) {
            DeviceStateAppFunctionType.SET_DEVICE_STATE -> setDeviceState(params)
            DeviceStateAppFunctionType.OFFSET_DEVICE_STATE_BY_VALUE ->
                offsetNumericDeviceStateByValue(params)
            DeviceStateAppFunctionType.ADJUST_DEVICE_STATE_BY_PERCENTAGE ->
                adjustNumericDeviceStateByPercentage(params)
            DeviceStateAppFunctionType.TOGGLE_DEVICE_STATE -> toggleDeviceStateItemParams(params)
            else -> {
                Log.i(TAG, "Unrecognised appFunctionType: $appFunctionType")
                return null
            }
        }
    }

    private fun setDeviceState(unparsedParams: GenericDocument?): SetDeviceStateItemResponse? {
        val params = parseSetDeviceStateItemParams(unparsedParams)
        // TODO: call into appropriate setter APIs

        return null
    }

    private fun offsetNumericDeviceStateByValue(
        unparsedParams: GenericDocument?
    ): SetDeviceStateItemResponse? {
        val params = parseOffsetNumericDeviceStateItemByValueParams(unparsedParams)
        // TODO: call into appropriate setter APIs

        return null
    }

    private fun adjustNumericDeviceStateByPercentage(
        unparsedParams: GenericDocument?
    ): SetDeviceStateItemResponse? {
        val params = parseAdjustNumericDeviceStateItemByPercentageParams(unparsedParams)
        // TODO: call into appropriate setter APIs

        return null
    }

    private fun toggleDeviceStateItemParams(
        unparsedParams: GenericDocument?
    ): SetDeviceStateItemResponse? {
        val params = parseToggleDeviceStateItemParams(unparsedParams)
        // TODO: call into appropriate setter APIs

        return null
    }

    private fun parseSetDeviceStateItemParams(params: GenericDocument?): SetDeviceStateItemParams? {
        val setDeviceStateItemParams: SetDeviceStateItemParams? = null

        val unparsedParams = params?.getPropertyDocument("setDeviceStateItemParams")
        if (unparsedParams == null) {
            throw IllegalArgumentException(
                "Missing setDeviceStateItemParams in the request: $params"
            )
        } else {
            Log.i(TAG, "Found setDeviceStateItemParams: $unparsedParams")
            // TODO: need to manually parse and construct the object
        }
        return setDeviceStateItemParams
    }

    private fun parseOffsetNumericDeviceStateItemByValueParams(
        params: GenericDocument?
    ): OffsetNumericDeviceStateItemByValueParams? {
        val offsetNumericDeviceStateItemByValueParams: OffsetNumericDeviceStateItemByValueParams? =
            null

        val unparsedParams =
            params?.getPropertyDocument("offsetNumericDeviceStateItemByValueParams")
        if (unparsedParams == null) {
            throw IllegalArgumentException(
                "Missing offsetNumericDeviceStateItemByValueParams in the request: $params"
            )
        } else {
            Log.i(TAG, "Found offsetNumericDeviceStateItemByValueParams: $unparsedParams")
            // TODO: need to manually parse and construct the object
        }
        return offsetNumericDeviceStateItemByValueParams
    }

    private fun parseAdjustNumericDeviceStateItemByPercentageParams(
        params: GenericDocument?
    ): AdjustNumericDeviceStateItemByPercentageParams? {
        val adjustNumericDeviceStateItemByPercentageParams:
            AdjustNumericDeviceStateItemByPercentageParams? =
            null

        val unparsedParams =
            params?.getPropertyDocument("adjustNumericDeviceStateItemByPercentageParams")
        if (unparsedParams == null) {
            throw IllegalArgumentException(
                "Missing adjustNumericDeviceStateItemByPercentageParams in the request: $params"
            )
        } else {
            Log.i(TAG, "Found adjustNumericDeviceStateItemByPercentageParams: $unparsedParams")
            // TODO: need to manually parse and construct the object
        }
        return adjustNumericDeviceStateItemByPercentageParams
    }

    private fun parseToggleDeviceStateItemParams(
        params: GenericDocument?
    ): ToggleDeviceStateItemParams? {
        val toggleDeviceStateItemParams: ToggleDeviceStateItemParams? = null

        val unparsedParams = params?.getPropertyDocument("toggleDeviceStateItemParams")
        if (unparsedParams == null) {
            throw IllegalArgumentException(
                "Missing toggleDeviceStateItemParams in the request: $params"
            )
        } else {
            Log.i(TAG, "Found toggleDeviceStateItemParams: $unparsedParams")
            // TODO: need to manually parse and construct the object
        }
        return toggleDeviceStateItemParams
    }

    companion object {
        private const val TAG = "CatalystStateSetterExecutor"
    }
}
