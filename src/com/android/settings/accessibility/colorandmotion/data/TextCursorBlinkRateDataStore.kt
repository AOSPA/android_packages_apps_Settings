/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.accessibility.colorandmotion.data

import android.content.Context
import android.content.res.Resources
import android.provider.Settings
import com.android.settingslib.datastore.KeyValueStoreDelegate
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.datastore.SettingsStore

@Suppress("UNCHECKED_CAST")
class TextCursorBlinkRateDataStore(
    private val context: Context,
    private val settingsStore: SettingsStore = SettingsSecureStore.get(context),
) : KeyValueStoreDelegate {
    private val resources: Resources = context.getResources()

    val defaultDurationMs =
        resources.getInteger(
            com.android.internal.R.integer.def_accessibility_text_cursor_blink_interval_ms
        )

    private val noBlinkDurationMs =
        resources.getInteger(
            com.android.internal.R.integer.no_blink_accessibility_text_cursor_blink_interval_ms
        )

    // The blink intervals are displayed from no-blink to slow to fast on the slider, so the
    // intervals should be reversed from how they are stored (smallest to largest).
    // The resulting array should look like this:
    // [0, 1000, 833, 714, 625, 556, 500, 455, 417, 385, 357, 333]
    val durationValues =
        resources
            .getIntArray(com.android.internal.R.array.accessibility_text_cursor_blink_intervals)
            .plus(noBlinkDurationMs)
            .reversed()

    private val noBlinkLabel =
        resources.getString(
            com.android.internal.R.string.no_blink_accessibility_text_cursor_blink_label
        )

    // The blink intervals are displayed from no-blink to slow to fast on the slider, so the
    // corresponding labels should be reversed from how they are stored (smallest to largest).
    // The resulting array should look like this:
    // [Don't blink, 50%, 60%, 70%, 80%, 90%, 100% (default), 110%, 120%, 130%, 140%, 150%]
    val durationLabels =
        resources
            .getStringArray(com.android.internal.R.array.accessibility_text_cursor_blink_labels)
            .plus(noBlinkLabel)
            .reversed()

    override val keyValueStoreDelegate
        get() = settingsStore

    override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>) =
        convertDurationToIndex(defaultDurationMs) as T

    override fun <T : Any> getValue(key: String, valueType: Class<T>): T {
        val durationValue = settingsStore.getInt(key) ?: defaultDurationMs
        return convertDurationToIndex(durationValue) as T
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        if (value is Int) {
            settingsStore.setInt(key, convertIndexToDuration(value))
        }
    }

    fun resetToDefault() {
        settingsStore.setInt(KEY, defaultDurationMs)
    }

    fun convertDurationToIndex(duration: Int): Int {
        val index = durationValues.indexOf(duration)
        return if (index != -1) {
            index
        } else {
            durationValues.indexOf(defaultDurationMs)
        }
    }

    fun convertIndexToDuration(index: Int): Int {
        return if (index in durationValues.indices) {
            durationValues[index]
        } else {
            defaultDurationMs
        }
    }

    companion object {
        const val KEY = Settings.Secure.ACCESSIBILITY_TEXT_CURSOR_BLINK_INTERVAL_MS
    }
}
