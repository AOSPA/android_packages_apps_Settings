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

package com.android.settings.accessibility.buttonshortcutsetting.data

import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyValueStoreDelegate
import com.android.settingslib.datastore.SettingsSecureStore
import kotlin.math.roundToInt

/**
 * Manages the transparency setting for the accessibility floating menu.
 *
 * This class converts an integer-based transparency progress value into the floating-point opacity
 * value that is persisted in [Settings.Secure].
 */
class FloatingMenuTransparencyDataStore(private val context: Context) : KeyValueStoreDelegate {
    override val keyValueStoreDelegate: KeyValueStore
        get() = SettingsSecureStore.get(context)

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>): T? {
        return DEFAULT_TRANSPARENCY_PROGRESS as? T
    }

    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        val opacityValue = keyValueStoreDelegate.getFloat(SETTING_KEY) ?: DEFAULT_OPACITY
        val transparency = MAX_TRANSPARENCY - opacityValue
        val transparencyProgress = (transparency * PRECISION).roundToInt()

        @Suppress("UNCHECKED_CAST")
        return transparencyProgress as? T
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {

        if (value !is Int) {
            val actualType = value?.javaClass?.simpleName ?: "null"
            Log.w(LOG_TAG, "Unsupported type '$actualType' for key '$key'. Expected Int.")
            return
        }

        if (value !in MIN_TRANSPARENCY_PROGRESS..MAX_TRANSPARENCY_PROGRESS) {
            Log.w(
                LOG_TAG,
                "Value $value for key '$key' is outside the expected range " +
                    "[$MIN_TRANSPARENCY_PROGRESS, $MAX_TRANSPARENCY_PROGRESS]",
            )
            return
        }

        val opacity = MAX_TRANSPARENCY - (value.toFloat() / PRECISION)
        keyValueStoreDelegate.setFloat(SETTING_KEY, opacity)
    }

    companion object {
        const val SETTING_KEY = Settings.Secure.ACCESSIBILITY_FLOATING_MENU_OPACITY
        const val PRECISION = 100f
        const val MAX_TRANSPARENCY = 1
        @FloatRange(from = 0.1, to = 1.0)
        const val DEFAULT_OPACITY = 0.45f // Corresponds to 55% transparency
        const val DEFAULT_TRANSPARENCY_PROGRESS: Int =
            ((MAX_TRANSPARENCY - DEFAULT_OPACITY) * PRECISION).toInt()
        private const val LOG_TAG = "FloatingMenuTransparencyDataStore"
        @IntRange(from = 0, to = 90) const val MIN_TRANSPARENCY_PROGRESS = 0
        @IntRange(from = 0, to = 90) const val MAX_TRANSPARENCY_PROGRESS = 90
    }
}
