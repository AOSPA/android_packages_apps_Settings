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

package com.android.settings.appfunctions.executors

import android.app.appsearch.GenericDocument
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.appfunctions.DeviceStateItemProviderExecutorResult
import com.android.settings.appfunctions.utils.determineParamName
import com.android.settings.appfunctions.utils.getPreference
import com.android.settings.appfunctions.utils.settingsPreferenceValueToString
import com.android.settingslib.metadata.KeyParameters
import com.android.settingslib.utils.applications.AppUtils
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItem
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItemResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A [DeviceStateExecutor] that provides a single device state item from Catalyst. */
class CatalystStateGetterExecutor(private val context: Context) : DeviceStateExecutor {
    override suspend fun execute(
        appFunctionType: DeviceStateAppFunctionType,
        params: GenericDocument?,
    ): DeviceStateItemProviderExecutorResult =
        withContext(Dispatchers.IO) {
            check(appFunctionType == DeviceStateAppFunctionType.GET_DEVICE_STATE) {
                "Requesting getSetDeviceStateItemParams for a $appFunctionType params."
            }
            if (params == null) {
                throw IllegalArgumentException("Provided params are null.")
            }

            if (
                !(AppUtils.isDebuggable() &&
                    Settings.Global.getInt(
                        context.contentResolver,
                        "com.android.settings.APP_FUNCTION_ITEM_GETTER_AVAILABLE",
                        0,
                    ) == 1)
            ) {
                return@withContext DeviceStateItemProviderExecutorResult(result = null)
            }

            val unparsedParams =
                requireNotNull(params.getPropertyDocument(GET_DEVICE_STATE_ITEM_PARAMS)) {
                    "Missing getDeviceStateItemParams in the request: $params"
                }

            val fullKey = unparsedParams.getPropertyString(PREFERENCE_KEY)
            requireNotNull(fullKey) { "Missing key in parameters $params" }

            val keyParts = fullKey.split("/", limit = 2)
            val screenKey = keyParts[0]
            val key = keyParts.getOrElse(1) { fullKey }

            val itemizationKeys =
                unparsedParams.getPropertyStringArray(ITEMIZATION_KEYS)?.toList() ?: emptyList()

            val keyParameters =
                if (itemizationKeys.isNotEmpty()) {
                    val paramName = determineParamName(screenKey)
                    if (paramName != null) {
                        Log.d(TAG, "Determined paramName: $paramName for screen: $screenKey")
                        KeyParameters(mapOf(paramName to itemizationKeys.joinToString(",")))
                    } else {
                        null
                    }
                } else {
                    null
                }

            val value = getPreference(context, screenKey, key, keyParameters)
            requireNotNull(value) { "Key [$fullKey] not found" }
            val errorPrefix = if (value.hasError) "Error: " else ""
            val jsonValueString = settingsPreferenceValueToString(value.settingsPreferenceValue)
            val item =
                DeviceStateItem(
                    key = fullKey,
                    purpose = fullKey,
                    jsonValue = "$errorPrefix$jsonValueString",
                )
            val itemResponse = DeviceStateItemResponse(deviceStateItem = item)
            DeviceStateItemProviderExecutorResult(result = itemResponse)
        }

    companion object {
        private const val TAG = "DeviceStateItemProviderExecutor"

        private const val GET_DEVICE_STATE_ITEM_PARAMS = "getDeviceStateItemParams"

        private const val PREFERENCE_KEY = "key"
        private const val ITEMIZATION_KEYS = "itemizationKeys"
    }
}
