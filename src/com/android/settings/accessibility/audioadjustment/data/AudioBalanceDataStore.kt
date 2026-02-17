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

package com.android.settings.accessibility.audioadjustment.data

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyValueStoreDelegate
import com.android.settingslib.datastore.SettingsSystemStore
import kotlin.math.roundToInt

/**
 * Data store for audio balance settings.
 *
 * This class handles reading and writing the audio balance value to
 * `Settings.System.MASTER_BALANCE`.
 */
class AudioBalanceDataStore(context: Context) : KeyValueStoreDelegate {
    override val keyValueStoreDelegate: KeyValueStore by lazy {
        SettingsSystemStore.get(context).apply { setDefaultValue(SETTING_KEY, 0f) }
    }

    override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>): T? {
        if (valueType == Int::class.javaObjectType) {
            @Suppress("UNCHECKED_CAST")
            return BALANCE_CENTER_VALUE as T?
        }
        return null
    }

    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        if (valueType != Int::class.javaObjectType) {
            Log.w(LOG_TAG, "Unsupported valueType for $key: $valueType")
            return null
        }
        val balance =
            keyValueStoreDelegate.getFloat(SETTING_KEY)
                ?: keyValueStoreDelegate.getDefaultValue(SETTING_KEY, Float::class.javaObjectType)
                ?: 0f
        @Suppress("UNCHECKED_CAST")
        return ((balance * 100f) + BALANCE_CENTER_VALUE).roundToInt() as? T
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        if (value !is Int) {
            Log.w(LOG_TAG, "Unsupported $valueType for $key: $value")
            return
        }
        val newValue = (value - BALANCE_CENTER_VALUE) * 0.01f
        keyValueStoreDelegate.setFloat(SETTING_KEY, newValue)
    }

    companion object {
        const val SETTING_KEY = Settings.System.MASTER_BALANCE
        const val BALANCE_CENTER_VALUE = 100
        const val BALANCE_MIN_VALUE = 0
        const val BALANCE_MAX_VALUE = 200
        private const val LOG_TAG = "AudioBalanceDataStore"
    }
}
