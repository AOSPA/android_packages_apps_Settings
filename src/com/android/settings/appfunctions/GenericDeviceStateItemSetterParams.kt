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
import android.util.Log
import com.google.android.appfunctions.schema.common.v1.devicestate.AdjustNumericDeviceStateItemByPercentageParams
import com.google.android.appfunctions.schema.common.v1.devicestate.OffsetNumericDeviceStateItemByValueParams
import com.google.android.appfunctions.schema.common.v1.devicestate.SetDeviceStateItemParams
import com.google.android.appfunctions.schema.common.v1.devicestate.ToggleDeviceStateItemParams
import java.lang.IllegalArgumentException

/**
 * A wrapper class for the different types of device state setter params. It accepts a
 * GenericDocument and parses it according to the provided appFunctionType.
 */
class GenericDeviceStateItemSetterParams(
    private val appFunctionType: DeviceStateAppFunctionType,
    unparsedParams: GenericDocument,
) {

    private val params: Any = parseParams(appFunctionType, unparsedParams)

    private fun parseParams(
        appFunctionType: DeviceStateAppFunctionType,
        params: GenericDocument,
    ): Any {
        return when (appFunctionType) {
            DeviceStateAppFunctionType.SET_DEVICE_STATE -> parseSetDeviceStateItemParams(params)
            DeviceStateAppFunctionType.OFFSET_DEVICE_STATE_BY_VALUE ->
                parseOffsetNumericDeviceStateItemByValueParams(params)
            DeviceStateAppFunctionType.ADJUST_DEVICE_STATE_BY_PERCENTAGE ->
                parseAdjustNumericDeviceStateItemByPercentageParams(params)
            DeviceStateAppFunctionType.TOGGLE_DEVICE_STATE ->
                parseToggleDeviceStateItemParams(params)
            else -> {
                throw IllegalArgumentException("Unrecognised appFunctionType: $appFunctionType")
            }
        }
    }

    /**
     * Returns the parsed SetDeviceStateItemParams if the provided appFunctionType was
     * SET_DEVICE_STATE, otherwise throws an Exception.
     */
    fun getSetDeviceStateItemParams(): SetDeviceStateItemParams {
        check(appFunctionType == DeviceStateAppFunctionType.SET_DEVICE_STATE) {
            "Requesting getSetDeviceStateItemParams for a $appFunctionType params."
        }
        return params as SetDeviceStateItemParams
    }

    /**
     * Returns the parsed OffsetNumericDeviceStateItemByValueParams if the provided appFunctionType
     * was OFFSET_DEVICE_STATE_BY_VALUE, otherwise throws an Exception.
     */
    fun getOffsetNumericDeviceStateItemByValueParams(): OffsetNumericDeviceStateItemByValueParams {
        check(appFunctionType == DeviceStateAppFunctionType.OFFSET_DEVICE_STATE_BY_VALUE) {
            "Requesting getOffsetNumericDeviceStateItemByValueParams for a $appFunctionType params."
        }
        return params as OffsetNumericDeviceStateItemByValueParams
    }

    /**
     * Returns the parsed AdjustNumericDeviceStateItemByPercentageParams if the provided
     * appFunctionType was ADJUST_DEVICE_STATE_BY_PERCENTAGE, otherwise throws an Exception.
     */
    fun getAdjustNumericDeviceStateItemByPercentageParams():
        AdjustNumericDeviceStateItemByPercentageParams {
        check(appFunctionType == DeviceStateAppFunctionType.ADJUST_DEVICE_STATE_BY_PERCENTAGE) {
            "Requesting getAdjustNumericDeviceStateItemByPercentageParams for a $appFunctionType params."
        }
        return params as AdjustNumericDeviceStateItemByPercentageParams
    }

    /**
     * Returns the parsed ToggleDeviceStateItemParams if the provided appFunctionType was
     * TOGGLE_DEVICE_STATE, otherwise throws an Exception.
     */
    fun getToggleDeviceStateItemParams(): ToggleDeviceStateItemParams {
        check(appFunctionType == DeviceStateAppFunctionType.TOGGLE_DEVICE_STATE) {
            "Requesting getToggleDeviceStateItemParams for a $appFunctionType params."
        }
        return params as ToggleDeviceStateItemParams
    }

    /** Returns the non-parametrised preference key. */
    fun getKey(): String {
        return when (appFunctionType) {
            DeviceStateAppFunctionType.SET_DEVICE_STATE -> getSetDeviceStateItemParams().key
            DeviceStateAppFunctionType.OFFSET_DEVICE_STATE_BY_VALUE ->
                getOffsetNumericDeviceStateItemByValueParams().key
            DeviceStateAppFunctionType.ADJUST_DEVICE_STATE_BY_PERCENTAGE ->
                getAdjustNumericDeviceStateItemByPercentageParams().key
            DeviceStateAppFunctionType.TOGGLE_DEVICE_STATE -> getToggleDeviceStateItemParams().key
            else -> {
                throw IllegalStateException("Invalid app function type $appFunctionType")
            }
        }
    }

    private fun parseSetDeviceStateItemParams(params: GenericDocument): SetDeviceStateItemParams {
        val unparsedParams = requireNotNull(
            params.getPropertyDocument(SET_DEVICE_STATE_ITEM_PARAMS)) {
            "Missing setDeviceStateItemParams in the request: $params"
            }
        Log.i(TAG, "Found setDeviceStateItemParams: $unparsedParams")
        val key = requireNotNull(
            unparsedParams.getPropertyString(PREFERENCE_KEY)) {
            "Missing key in the params: $unparsedParams"
        }
        val itemizationKeys = unparsedParams.getPropertyStringArray(ITEMIZATION_KEYS)?.toList()
            ?: emptyList()
        val value = requireNotNull(unparsedParams.getPropertyString(VALUE)) {
            "Missing value in the params: $unparsedParams"
        }
        val requestInitiatedWhileUnlocked = unparsedParams.getPropertyBoolean(REQUEST_INITIATED_WHILE_UNLOCKED)
        return SetDeviceStateItemParams(key = key, value = value, itemizationKeys = itemizationKeys, requestInitiatedWhileUnlocked = requestInitiatedWhileUnlocked)

    }

    private fun parseOffsetNumericDeviceStateItemByValueParams(
        params: GenericDocument
    ): OffsetNumericDeviceStateItemByValueParams {
        val unparsedParams = requireNotNull(
            params.getPropertyDocument(OFFSET_NUMERIC_DEVICE_STATE_ITEM_BY_VALUE_PARAMS)) {
            "Missing offsetNumericDeviceStateItemByValueParams in the request: $params"
        }
        Log.i(TAG, "Found offsetNumericDeviceStateItemByValueParams: $unparsedParams")
        val key = requireNotNull(
            unparsedParams.getPropertyString(PREFERENCE_KEY)) {
            "Missing key in the params: $unparsedParams"
        }
        val itemizationKeys = unparsedParams.getPropertyStringArray(ITEMIZATION_KEYS)?.toList()
            ?: emptyList()
        val valueAdjustment = requireNotNull(unparsedParams.getPropertyDouble(
            VALUE_ADJUSTMENT)) {
            "Missing valueAdjustment in the params: $unparsedParams"
        }
        val requestInitiatedWhileUnlocked = unparsedParams.getPropertyBoolean(REQUEST_INITIATED_WHILE_UNLOCKED)
        return OffsetNumericDeviceStateItemByValueParams(key = key, valueAdjustment = valueAdjustment, itemizationKeys = itemizationKeys, requestInitiatedWhileUnlocked = requestInitiatedWhileUnlocked)
    }

    private fun parseAdjustNumericDeviceStateItemByPercentageParams(
        params: GenericDocument
    ): AdjustNumericDeviceStateItemByPercentageParams {
        val unparsedParams = requireNotNull(
            params.getPropertyDocument(ADJUST_NUMERIC_DEVICE_STATE_ITEM_BY_PERCENTAGE_PARAMS)) {
            "Missing adjustNumericDeviceStateItemByPercentageParams in the request: $params"
        }
        Log.i(TAG, "Found adjustNumericDeviceStateItemByPercentageParams: $unparsedParams")
        val key = requireNotNull(
            unparsedParams.getPropertyString(PREFERENCE_KEY)) {
            "Missing key in the params: $unparsedParams"
        }
        val itemizationKeys = unparsedParams.getPropertyStringArray(ITEMIZATION_KEYS)?.toList()
            ?: emptyList()
        val percentageAdjustment = requireNotNull(unparsedParams.getPropertyLong(
            PERCENTAGE_ADJUSTMENT).toInt()) {
            "Missing valueAdjustment in the params: $unparsedParams"
        }
        val requestInitiatedWhileUnlocked = unparsedParams.getPropertyBoolean(REQUEST_INITIATED_WHILE_UNLOCKED)
        return AdjustNumericDeviceStateItemByPercentageParams(key = key, percentageAdjustment = percentageAdjustment, itemizationKeys = itemizationKeys, requestInitiatedWhileUnlocked = requestInitiatedWhileUnlocked)
    }

    private fun parseToggleDeviceStateItemParams(
        params: GenericDocument?
    ): ToggleDeviceStateItemParams {
        val toggleDeviceStateParams = ToggleDeviceStateItemParams(key = "key")

        val unparsedParams = params?.getPropertyDocument("toggleDeviceStateItemParams")
        if (unparsedParams == null) {
            throw IllegalArgumentException(
                "Missing toggleDeviceStateItemParams in the request: $params"
            )
        } else {
            Log.i(TAG, "Found toggleDeviceStateItemParams: $unparsedParams")
            // TODO: need to manually parse and construct the object
        }
        return toggleDeviceStateParams
    }

    companion object {
        private const val TAG = "GenericDeviceStateItemSetterParams"

        private const val SET_DEVICE_STATE_ITEM_PARAMS = "setDeviceStateItemParams"
        private const val OFFSET_NUMERIC_DEVICE_STATE_ITEM_BY_VALUE_PARAMS = "offsetNumericDeviceStateItemByValueParams"
        private const val ADJUST_NUMERIC_DEVICE_STATE_ITEM_BY_PERCENTAGE_PARAMS = "adjustNumericDeviceStateItemByPercentageParams"

        private const val PREFERENCE_KEY = "key"
        private const val ITEMIZATION_KEYS = "itemizationKeys"
        private const val VALUE = "value"
        private const val VALUE_ADJUSTMENT = "valueAdjustment"
        private const val PERCENTAGE_ADJUSTMENT = "percentageAdjustment"
        private const val REQUEST_INITIATED_WHILE_UNLOCKED = "requestInitiatedWhileUnlocked"
    }
}
