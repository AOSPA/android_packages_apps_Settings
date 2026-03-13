/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.appfunctions.utils

import android.content.Context
import android.service.settings.preferences.SettingsPreferenceValue
import android.util.Log
import com.android.settings.appfunctions.PreferenceServiceClient
import com.android.settingslib.graph.PreferenceGetterFlags
import com.android.settingslib.graph.PreferenceGetterRequest
import com.android.settingslib.graph.PreferenceGetterResponse
import com.android.settingslib.graph.PreferenceSetterRequest
import com.android.settingslib.graph.PreferenceSetterResponse
import com.android.settingslib.graph.PreferenceSetterResult
import com.android.settingslib.graph.preferenceValueProto
import com.android.settingslib.graph.proto.PreferenceProto
import com.android.settingslib.graph.toPreferenceSetterResponse
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.android.settingslib.metadata.KeyParameters
import com.android.settingslib.metadata.PreferenceCoordinate
import com.android.settingslib.metadata.PreferenceScreenRegistry
import com.android.settingslib.metadata.SensitivityLevel
import kotlin.Boolean

private const val TAG = "SettingsPreferenceUtils"
private const val SETTINGS_PACKAGE_NAME = "com.android.settings"

const val DO_NOT_EXPOSE = "DO_NOT_EXPOSE"
const val NO_SENSITIVITY = "NO_SENSITIVITY"
const val MUST_PROVIDE_UNDO = "MUST_PROVIDE_UNDO"
const val REQUIRES_CONFIRMATION = "REQUIRES_CONFIRMATION"
const val DEEP_LINK_ONLY = "DEEP_LINK_ONLY"

data class PreferenceDetails(
    val settingsPreferenceValue: SettingsPreferenceValue?,
    val sensitivityLevel: String,
    val isAvailable: Boolean,
    val isEnabled: Boolean,
    val isWriteable: Boolean,
    val hasError: Boolean
)

data class SettingsPreferenceValueResult(
    val value: SettingsPreferenceValue?,
    val hasError: Boolean
)

/** Helper method to get a preference value using the SettingsPreferenceServiceClient. */
suspend fun getPreference(
    context: Context,
    screenKey: String,
    key: String,
    keyParameters: KeyParameters? = null,
): PreferenceDetails? {
    Log.d(TAG, "getPreference started for $screenKey/$key")
    val coord =
        if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
            PreferenceCoordinate(screenKey, keyParameters, key)
        } else {
            PreferenceCoordinate(screenKey, keyParameters?.toBundle(), key)
        }
    val catalystRequest = PreferenceGetterRequest(arrayOf(coord), PreferenceGetterFlags.ALL)

    return try {
        val client = PreferenceServiceClient(context)
        val response: PreferenceGetterResponse = client.use { it.getPreferences(catalystRequest) }

        // Find the preference proto by matching the coordinate
        val preferenceProto: PreferenceProto? = response.preferences[coord]

        Log.d(TAG, "Found preferenceProto: $preferenceProto")
        if (preferenceProto == null) {
            return null
        }

        val result = preferenceProto.toSettingsPreferenceValue()
        Log.d(TAG, "Result value: $result")

        val sensitivityLevelString = getSensitivityLevelString(preferenceProto)

        PreferenceDetails(
            settingsPreferenceValue = result.value,
            sensitivityLevel = sensitivityLevelString,
            isAvailable = preferenceProto.available,
            isEnabled = preferenceProto.enabled,
            isWriteable = preferenceProto.writable,
            hasError = result.hasError
        )
    } catch (e: Exception) {
        Log.e(TAG, "Error getting preference value", e)
        null
    }
}

private fun getSensitivityLevelString(preferenceProto: PreferenceProto): String {
    val sensitivityLevelString =
        when (preferenceProto.sensitivityLevel) {
            SensitivityLevel.DO_NOT_EXPOSE -> DO_NOT_EXPOSE
            SensitivityLevel.NO_SENSITIVITY -> NO_SENSITIVITY
            SensitivityLevel.MUST_PROVIDE_UNDO -> MUST_PROVIDE_UNDO
            SensitivityLevel.REQUIRES_CONFIRMATION -> REQUIRES_CONFIRMATION
            SensitivityLevel.DEEP_LINK_ONLY -> DEEP_LINK_ONLY
            else -> "UNKNOWN"
        }
    return sensitivityLevelString
}

/** Helper method to set a preference value using the SettingsPreferenceServiceClient. */
suspend fun setPreference(
    context: Context,
    screenKey: String,
    key: String,
    value: SettingsPreferenceValue,
    keyParameters: KeyParameters? = null,
): PreferenceSetterResponse {
    Log.d(TAG, "setPreference started for $screenKey/$key")
    val valueProto =
        when (value.type) {
            SettingsPreferenceValue.TYPE_BOOLEAN ->
                preferenceValueProto { booleanValue = value.booleanValue }
            SettingsPreferenceValue.TYPE_INT -> preferenceValueProto { intValue = value.intValue }
            SettingsPreferenceValue.TYPE_STRING ->
                preferenceValueProto { stringValue = value.stringValue }
            else -> return PreferenceSetterResult.INVALID_REQUEST.toPreferenceSetterResponse()
        }

    val catalystRequest =
        if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
            PreferenceSetterRequest(
                screenKey = screenKey,
                keyParameters = keyParameters,
                key = key,
                value = valueProto,
            )
        } else {
            PreferenceSetterRequest(
                screenKey = screenKey,
                args = keyParameters?.toBundle(),
                key = key,
                value = valueProto,
            )
        }

    return try {
        val client = PreferenceServiceClient(context)
        client.use { it.setPreferenceValue(catalystRequest) }
    } catch (e: Exception) {
        Log.e(TAG, "Error setting preference value", e)
        PreferenceSetterResult.INTERNAL_ERROR.toPreferenceSetterResponse(failureReason = e.message)
    }
}

private fun PreferenceProto.toSettingsPreferenceValue(): SettingsPreferenceValueResult {
    var hasError = false

    val settingsPreferenceValue = if (hasValue()) {
        val protoValue = value
        when {
            protoValue.hasBooleanValue() -> {
                SettingsPreferenceValue.Builder(SettingsPreferenceValue.TYPE_BOOLEAN)
                    .setBooleanValue(protoValue.booleanValue)
                    .build()
            }

            protoValue.hasIntValue() -> {
                SettingsPreferenceValue.Builder(SettingsPreferenceValue.TYPE_INT)
                    .setIntValue(protoValue.intValue)
                    .build()
            }

            protoValue.hasStringValue() -> {
                SettingsPreferenceValue.Builder(SettingsPreferenceValue.TYPE_STRING)
                    .setStringValue(protoValue.stringValue)
                    .build()
            }

            protoValue.hasLongValue() -> {
                SettingsPreferenceValue.Builder(SettingsPreferenceValue.TYPE_LONG)
                    .setLongValue(protoValue.longValue)
                    .build()
            }

            protoValue.hasFloatValue() -> {
                SettingsPreferenceValue.Builder(SettingsPreferenceValue.TYPE_DOUBLE)
                    .setDoubleValue(protoValue.floatValue.toDouble())
                    .build()
            }

            protoValue.hasError() -> {
                hasError = true
                SettingsPreferenceValue.Builder(SettingsPreferenceValue.TYPE_STRING)
                    .setStringValue("${protoValue.error.error}")
                    .build()
            }

            else -> {
                hasError = true
                null
            }
        }
    } else {
        hasError = true
        null
    }

    return SettingsPreferenceValueResult(
        value = settingsPreferenceValue,
        hasError = hasError
    )
}

/** Helper method to convert a SettingsPreferenceValue to its string representation. */
fun settingsPreferenceValueToString(value: SettingsPreferenceValue?): String {
    if (value == null) return ""
    return try {
        when (value.type) {
            SettingsPreferenceValue.TYPE_BOOLEAN -> value.booleanValue.toString()
            SettingsPreferenceValue.TYPE_INT -> value.intValue.toString()
            SettingsPreferenceValue.TYPE_STRING -> value.stringValue ?: ""
            SettingsPreferenceValue.TYPE_LONG -> value.longValue.toString()
            SettingsPreferenceValue.TYPE_DOUBLE -> value.doubleValue.toString()
            else -> ""
        }
    } catch (e: Exception) {
        Log.w(TAG, "Failed to get value from SettingsPreferenceValue for type ${value.type}", e)
        ""
    }
}

/**
 * Helper method to convert a string value to a SettingsPreferenceValue based on the provided type.
 */
fun toSettingsPreferenceValue(value: String, type: Int?): SettingsPreferenceValue? {
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

fun determineParamName(screenKey: String): String? {
    val schema = PreferenceScreenRegistry.getScreenParametersSchema(screenKey)
    if (schema != null) {
        try {
            val field = schema.javaClass.getDeclaredField("schema")
            // TODO b/483316989: get the schema without altering the `isAccessible` field.
            field.isAccessible = true
            val map = field.get(schema) as? Map<*, *>
            if (!map.isNullOrEmpty()) {
                // TODO b/483316989: handle all parameters.
                return map.keys.first() as? String
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to determine param name from schema via reflection", e)
        }
    }
    return null
}
