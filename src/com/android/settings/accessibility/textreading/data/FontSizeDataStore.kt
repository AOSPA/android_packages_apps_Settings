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

package com.android.settings.accessibility.textreading.data

import android.content.Context
import android.provider.Settings
import com.android.settings.accessibility.AccessibilityStatsLogUtils
import com.android.settings.accessibility.TextReadingPreferenceFragment.EntryPoint
import com.android.settings.core.instrumentation.SettingsStatsLog
import com.android.settingslib.R as SettingsLibR
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyValueStoreDelegate
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.datastore.SettingsStore
import com.android.settingslib.datastore.SettingsSystemStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** DataStore for the font size setting. */
class FontSizeDataStore(
    private val context: Context,
    private val settingsSecure: SettingsStore = SettingsSecureStore.get(context),
    private val settingsSystem: SettingsStore = SettingsSystemStore.get(context),
    @EntryPoint private val entryPoint: Int = EntryPoint.UNKNOWN_ENTRY,
) : KeyValueStoreDelegate {
    init {
        settingsSystem.run {
            setDefaultValue(DEFAULT_FONT_SIZE_KEY, FONT_SCALE_DEF_VALUE)
            setDefaultValue(FONT_SCALE_KEY, getFloat(DEFAULT_FONT_SIZE_KEY) ?: FONT_SCALE_DEF_VALUE)
        }
    }

    override val keyValueStoreDelegate: KeyValueStore
        get() = settingsSystem

    private val _fontSizeData = MutableStateFlow(loadFontSize())
    val fontSizeData = _fontSizeData.asStateFlow()

    override fun contains(key: String): Boolean = true

    // The value is the index of the font size in the array
    override fun getInt(key: String): Int? {
        return _fontSizeData.value.currentIndex
    }

    // The value is the index of the font size in the array
    override fun setInt(key: String, value: Int?) {
        if (value == null) {
            return super.setInt(key, value)
        }
        if (settingsSecure.getBoolean(HAS_BEEN_CHANGED_KEY) != true) {
            // HAS_BEEN_CHANGED settings is used to automatically add font size tile to quick
            // settings panel
            settingsSecure.setBoolean(HAS_BEEN_CHANGED_KEY, true)
        }
        val data = _fontSizeData.value
        if (data.currentIndex != value) {
            SettingsStatsLog.write(
                SettingsStatsLog.ACCESSIBILITY_TEXT_READING_OPTIONS_CHANGED,
                SettingsStatsLog
                    .ACCESSIBILITY_TEXT_READING_OPTIONS_CHANGED__NAME__TEXT_READING_FONT_SIZE,
                value,
                AccessibilityStatsLogUtils.convertToEntryPoint(entryPoint),
            )
            keyValueStoreDelegate.setFloat(FONT_SCALE_KEY, data.values[value])
            _fontSizeData.value = data.copy(currentIndex = value)
        }
    }

    fun resetToDefault() {
        val data = _fontSizeData.value
        setInt(
            key = "reset",
            value = getClosestFontIndex(value = data.defaultValue, fontSizes = data.values),
        )
    }

    private fun loadFontSize(): FontSize {
        val fontSizes: FloatArray =
            context.resources
                .getStringArray(SettingsLibR.array.entryvalues_font_size)
                .mapNotNull { it.toFloatOrNull() }
                .toFloatArray()
        val defaultValue =
            keyValueStoreDelegate.getFloat(DEFAULT_FONT_SIZE_KEY) ?: FONT_SCALE_DEF_VALUE
        val currentValue = settingsSystem.getFloat(FONT_SCALE_KEY) ?: defaultValue
        val currentIndex = getClosestFontIndex(currentValue, fontSizes)

        return FontSize(currentIndex, fontSizes, defaultValue)
    }

    private fun getClosestFontIndex(value: Float, fontSizes: FloatArray): Int {
        var lastValue = fontSizes[0]
        for (i in 1 until fontSizes.size) {
            val currentValue = fontSizes[i]
            if (value < (lastValue + (currentValue - lastValue) * .5f)) {
                return i - 1
            }
            lastValue = currentValue
        }
        return fontSizes.size - 1
    }

    companion object {
        internal const val FONT_SCALE_DEF_VALUE = 1.0f
        private const val HAS_BEEN_CHANGED_KEY =
            Settings.Secure.ACCESSIBILITY_FONT_SCALING_HAS_BEEN_CHANGED
        private const val FONT_SCALE_KEY = Settings.System.FONT_SCALE
        private const val DEFAULT_FONT_SIZE_KEY = Settings.System.DEFAULT_DEVICE_FONT_SCALE
    }
}
