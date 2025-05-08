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
import com.android.settings.R
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyValueStoreDelegate
import com.android.settingslib.datastore.SettingsSystemStore
import kotlin.math.min

/** SettingsStore for vibration intensity preferences with custom default value. */
class VibrationIntensitySettingsStore(
    context: Context,
    @Usage vibrationUsage: Int,
    override val keyValueStoreDelegate: KeyValueStore = SettingsSystemStore.get(context),
    private val defaultIntensity: Int = context.getDefaultVibrationIntensity(vibrationUsage),
    private val supportedIntensityLevels: Int = context.getSupportedVibrationIntensityLevels(),
) : KeyValueStoreDelegate {

    /** Returns true if the settings key should be enabled, false otherwise. */
    fun isPreferenceEnabled() =
        keyValueStoreDelegate.getBoolean(VibrationMainSwitchPreference.KEY) != false // default true

    override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>) =
        intensityToValue(valueType, defaultIntensity)

    override fun <T : Any> getValue(key: String, valueType: Class<T>) =
        if (isPreferenceEnabled()) {
            intensityToValue(valueType, keyValueStoreDelegate.getInt(key) ?: defaultIntensity)
        } else {
            // Preference must show intensity off when disabled, but value stored must be preserved.
            intensityToValue(valueType, Vibrator.VIBRATION_INTENSITY_OFF)
        }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) =
        keyValueStoreDelegate.setInt(key, value?.let { valueToIntensity(valueType, it) })

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> intensityToValue(valueType: Class<T>, intensity: Int): T? =
        when (valueType) {
            Boolean::class.javaObjectType -> intensityToBooleanValue(intensity)
            Int::class.javaObjectType -> intensityToIntValue(intensity)
            else -> null
        }
            as T?

    private fun intensityToBooleanValue(intensity: Int): Boolean? =
        intensity != Vibrator.VIBRATION_INTENSITY_OFF

    private fun intensityToIntValue(intensity: Int): Int? = min(intensity, supportedIntensityLevels)

    private fun <T : Any> valueToIntensity(valueType: Class<T>, value: T): Int? =
        when (valueType) {
            Boolean::class.javaObjectType -> booleanValueToIntensity(value as Boolean)
            Int::class.javaObjectType -> intValueToIntensity(value as Int)
            else -> null
        }

    private fun booleanValueToIntensity(value: Boolean): Int? =
        if (value) defaultIntensity else Vibrator.VIBRATION_INTENSITY_OFF

    private fun intValueToIntensity(value: Int): Int? =
        if (value == Vibrator.VIBRATION_INTENSITY_OFF) {
            Vibrator.VIBRATION_INTENSITY_OFF
        } else if (supportedIntensityLevels == 1) {
            // If there is only one intensity available besides OFF, then use the device default
            // intensity to ensure no scaling will ever happen in the platform.
            defaultIntensity
        } else if (value < supportedIntensityLevels) {
            // If value is within supported intensity levels then return the raw value as intensity.
            value
        } else {
            // If the settings granularity is lower than the platform's then map the max position to
            // the highest vibration intensity, skipping intermediate values in the scale.
            Vibrator.VIBRATION_INTENSITY_HIGH
        }
}

/** Returns the device default vibration intensity for given usage. */
private fun Context.getDefaultVibrationIntensity(@Usage vibrationUsage: Int): Int =
    getSystemService(Vibrator::class.java).getDefaultVibrationIntensity(vibrationUsage)

/** Returns the number of vibration intensity levels supported by this device. */
fun Context.getSupportedVibrationIntensityLevels(): Int =
    resources.getInteger(R.integer.config_vibration_supported_intensity_levels)
