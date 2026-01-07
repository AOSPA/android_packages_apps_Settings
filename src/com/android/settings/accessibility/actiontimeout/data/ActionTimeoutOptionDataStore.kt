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

package com.android.settings.accessibility.actiontimeout.data

import android.content.Context
import android.provider.Settings
import androidx.annotation.StringRes
import com.android.settings.R
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore

/**
 * Data store for action timeout options.
 *
 * This class manages the storage and retrieval of action timeout settings, providing a key-value
 * interface and observing changes in the underlying secure settings.
 */
class ActionTimeoutOptionDataStore(context: Context) :
    AbstractKeyedDataObservable<String>(), KeyValueStore {
    private val timeoutOptionsByKey by lazy {
        DISCRETE_TIMEOUT_OPTIONS.values.associateBy { it.preferenceKey }
    }
    private val settingsSecureStore = SettingsSecureStore.get(context)
    private var observer: KeyedObserver<String>? = null

    override fun contains(key: String): Boolean {
        return settingsSecureStore.contains(INTERACTIVE_UI_TIMEOUT_SETTING_KEY)
    }

    override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>): T? {
        return getValue(key, valueType)
    }

    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        val optionValue = settingsSecureStore.getInt(INTERACTIVE_UI_TIMEOUT_SETTING_KEY)

        @Suppress("UNCHECKED_CAST")
        return when (valueType) {
            Boolean::class.javaObjectType ->
                DISCRETE_TIMEOUT_OPTIONS[optionValue]?.preferenceKey == key
            Int::class.javaObjectType -> optionValue ?: 0
            else -> null
        }
            as? T?
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        val newValue: Int? =
            when (valueType) {
                Boolean::class.javaObjectType -> {
                    // Only update if the toggle is enabled (true).
                    // Returns null (no-op) if value is false, null, or not a Boolean.
                    if (value as? Boolean == true) {
                        timeoutOptionsByKey[key]?.optionValue
                    } else {
                        null
                    }
                }
                Int::class.javaObjectType -> {
                    (value as? Int) ?: 0
                }
                else -> null
            }

        newValue?.let { setTimeoutValue(it) }
    }

    private fun setTimeoutValue(timeoutValue: Int) {
        settingsSecureStore.run {
            setInt(INTERACTIVE_UI_TIMEOUT_SETTING_KEY, timeoutValue)
            setInt(NON_INTERACTIVE_UI_TIMEOUT_SETTING_KEY, timeoutValue)
        }
    }

    override fun onFirstObserverAdded() {
        if (observer == null) {
            observer = KeyedObserver { key, reason ->
                if (key == INTERACTIVE_UI_TIMEOUT_SETTING_KEY) notifyChange(reason)
            }
            observer?.also {
                settingsSecureStore.addObserver(
                    INTERACTIVE_UI_TIMEOUT_SETTING_KEY,
                    it,
                    HandlerExecutor.main,
                )
            }
        }
    }

    override fun onLastObserverRemoved() {
        observer?.also {
            settingsSecureStore.removeObserver(INTERACTIVE_UI_TIMEOUT_SETTING_KEY, it)
        }
    }

    companion object {
        const val NON_INTERACTIVE_UI_TIMEOUT_SETTING_KEY =
            Settings.Secure.ACCESSIBILITY_NON_INTERACTIVE_UI_TIMEOUT_MS
        const val INTERACTIVE_UI_TIMEOUT_SETTING_KEY =
            Settings.Secure.ACCESSIBILITY_INTERACTIVE_UI_TIMEOUT_MS

        val DISCRETE_TIMEOUT_OPTIONS =
            mapOf<Int, ActionTimeoutOption>(
                Pair(
                    0,
                    ActionTimeoutOption(
                        optionValue = 0,
                        preferenceKey = "accessibility_control_timeout_default",
                        optionDescriptionRes = R.string.accessibility_timeout_default,
                    ),
                ),
                Pair(
                    10_000,
                    ActionTimeoutOption(
                        optionValue = 10_000,
                        preferenceKey = "accessibility_control_timeout_10secs",
                        optionDescriptionRes = R.string.accessibility_timeout_10secs,
                    ),
                ),
                Pair(
                    30_000,
                    ActionTimeoutOption(
                        optionValue = 30_000,
                        preferenceKey = "accessibility_control_timeout_30secs",
                        optionDescriptionRes = R.string.accessibility_timeout_30secs,
                    ),
                ),
                Pair(
                    60_000,
                    ActionTimeoutOption(
                        optionValue = 60_000,
                        preferenceKey = "accessibility_control_timeout_1min",
                        optionDescriptionRes = R.string.accessibility_timeout_1min,
                    ),
                ),
                Pair(
                    120_000,
                    ActionTimeoutOption(
                        optionValue = 120_000,
                        preferenceKey = "accessibility_control_timeout_2mins",
                        optionDescriptionRes = R.string.accessibility_timeout_2mins,
                    ),
                ),
            )
    }
}

data class ActionTimeoutOption(
    val optionValue: Int,
    val preferenceKey: String,
    @param:StringRes val optionDescriptionRes: Int,
)
