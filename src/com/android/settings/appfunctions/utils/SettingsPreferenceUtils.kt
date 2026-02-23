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
import android.service.settings.preferences.SetValueResult
import android.service.settings.preferences.SettingsPreferenceValue
import android.util.Log
import com.android.settings.appfunctions.PreferenceServiceClient
import com.android.settingslib.graph.PreferenceGetterFlags
import com.android.settingslib.graph.PreferenceGetterRequest
import com.android.settingslib.graph.PreferenceSetterRequest
import com.android.settingslib.graph.preferenceValueProto
import com.android.settingslib.graph.proto.PreferenceProto
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.android.settingslib.metadata.KeyParameters
import com.android.settingslib.metadata.PreferenceCoordinate
import com.android.settingslib.metadata.PreferenceScreenRegistry

private const val TAG = "SettingsPreferenceUtils"
private const val SETTINGS_PACKAGE_NAME = "com.android.settings"

/** Helper method to get a preference value using the SettingsPreferenceServiceClient. */
suspend fun getPreference(
    context: Context,
    screenKey: String,
    key: String,
    keyParameters: KeyParameters? = null,
): SettingsPreferenceValue? {
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
        val response = client.use { it.getPreferences(catalystRequest) }

        // Find the preference proto by matching the coordinate
        val preferenceProto = response.preferences[coord]

        Log.d(TAG, "Found preferenceProto: $preferenceProto")
        val result = preferenceProto?.toSettingsPreferenceValue()
        Log.d(TAG, "Result value: $result")
        result
    } catch (e: Exception) {
        Log.e(TAG, "Error getting preference value", e)
        null
    }
}

/** Helper method to set a preference value using the SettingsPreferenceServiceClient. */
suspend fun setPreference(
    context: Context,
    screenKey: String,
    key: String,
    value: SettingsPreferenceValue,
    keyParameters: KeyParameters? = null,
): Int {
    Log.d(TAG, "setPreference started for $screenKey/$key")
    val valueProto =
        when (value.type) {
            SettingsPreferenceValue.TYPE_BOOLEAN ->
                preferenceValueProto { booleanValue = value.booleanValue }
            SettingsPreferenceValue.TYPE_INT -> preferenceValueProto { intValue = value.intValue }
            SettingsPreferenceValue.TYPE_STRING ->
                preferenceValueProto { stringValue = value.stringValue }
            else -> return SetValueResult.RESULT_INVALID_REQUEST
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
        SetValueResult.RESULT_INTERNAL_ERROR
    }
}

fun PreferenceProto.toSettingsPreferenceValue(): SettingsPreferenceValue? {
    if (hasValue()) {
        val protoValue = value
        return when {
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
            else -> null
        }
    }
    return null
}

/** Helper method to convert a SettingsPreferenceValue to its string representation. */
fun settingsPreferenceValueToString(value: SettingsPreferenceValue?): String {
    if (value == null) return ""
    return try {
        when (value.type) {
            SettingsPreferenceValue.TYPE_BOOLEAN -> value.booleanValue.toString()
            SettingsPreferenceValue.TYPE_INT -> value.intValue.toString()
            SettingsPreferenceValue.TYPE_STRING -> value.stringValue ?: ""
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
