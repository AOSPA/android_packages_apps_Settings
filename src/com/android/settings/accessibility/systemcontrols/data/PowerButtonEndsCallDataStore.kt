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
package com.android.settings.accessibility.systemcontrols.data

import android.content.Context
import android.provider.Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR
import android.provider.Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_DEFAULT
import android.provider.Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP
import android.provider.Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF
import android.util.Log
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyValueStoreDelegate
import com.android.settingslib.datastore.SettingsSecureStore

/**
 * Data store for
 * [com.android.settings.accessibility.systemcontrols.ui.PowerButtonEndsCallPreference]
 */
class PowerButtonEndsCallDataStore(private val context: Context) : KeyValueStoreDelegate {
    override val keyValueStoreDelegate: KeyValueStore
        get() =
            SettingsSecureStore.get(context).apply {
                setDefaultValue(KEY, INCALL_POWER_BUTTON_BEHAVIOR_DEFAULT)
            }

    override fun contains(key: String): Boolean = keyValueStoreDelegate.contains(KEY)

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        return (keyValueStoreDelegate.getInt(KEY) == INCALL_POWER_BUTTON_BEHAVIOR_HANGUP) as? T?
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        if (value !is Boolean?) {
            Log.w(LOG_TAG, "Unsupported $valueType for $key: $value")
            return
        }

        if (value is Boolean) {
            keyValueStoreDelegate.setInt(
                KEY,
                when (value) {
                    true -> INCALL_POWER_BUTTON_BEHAVIOR_HANGUP
                    else -> INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF
                },
            )
        }
    }

    companion object {
        const val KEY = INCALL_POWER_BUTTON_BEHAVIOR
        private const val LOG_TAG = "PowerButtonEndsCallDataStore"
    }
}
