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
package com.android.settings.accessibility

import android.content.Context
import android.os.VibrationAttributes.Usage
import android.os.Vibrator
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyValueStoreDelegate
import com.android.settingslib.datastore.SettingsSystemStore

/** SettingsStore for vibration intensity preferences with custom default value. */
class VibrationIntensitySettingsStore(
    context: Context,
    @Usage vibrationUsage: Int,
    override val keyValueStoreDelegate: KeyValueStore = SettingsSystemStore.get(context),
    private val defaultIntensity: Int = context.getDefaultVibrationIntensity(vibrationUsage),
) : KeyValueStoreDelegate {

    override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>) =
        intensityToValue(valueType, defaultIntensity)

    override fun <T : Any> getValue(key: String, valueType: Class<T>) =
        intensityToValue(valueType, keyValueStoreDelegate.getInt(key) ?: defaultIntensity)

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) =
        keyValueStoreDelegate.setInt(key, value?.let { valueToIntensity(valueType, it) })

    @Suppress("UNCHECKED_CAST")
    private fun <T: Any> intensityToValue(valueType: Class<T>, intensity: Int): T? =
        when (valueType) {
            Boolean::class.javaObjectType -> intensity != Vibrator.VIBRATION_INTENSITY_OFF
            Int::class.javaObjectType -> intensity
            else -> null
        } as T?

    private fun <T: Any> valueToIntensity(valueType: Class<T>, value: T): Int? =
        when (valueType) {
            Boolean::class.javaObjectType ->
                if (value as Boolean) defaultIntensity else Vibrator.VIBRATION_INTENSITY_OFF
            Int::class.javaObjectType -> value as Int
            else -> null
        }
}

/** Returns the device default vibration intensity for given usage. */
private fun Context.getDefaultVibrationIntensity(@Usage vibrationUsage: Int): Int =
    getSystemService(Vibrator::class.java).getDefaultVibrationIntensity(vibrationUsage)
