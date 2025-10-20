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
@file:JvmName("SubIdBundleUtils")

package com.android.settings.utils

import android.os.Bundle
import com.android.settings.flags.Flags

/**
 * Retrieves an integer value from a [Bundle] by a given [key], gracefully handling cases where the
 * underlying value is stored as either a `String` or an `Int`.
 *
 * This function is useful for migrations where a feature flag controls the data type of a
 * parameter. It checks the `Flags.catalystUseStringBundle()` flag to determine whether to retrieve
 * the value as a `String` and convert it to an `Int`, or to retrieve it directly as an `Int`.
 *
 * @param key The key to look up in the Bundle.
 * @param defaultValue The value to return if the key is not found or if the `String` value cannot
 *   be parsed as an integer.
 * @return The integer value associated with the key, or [defaultValue] if the key is not found or a
 *   parsing error occurs.
 */
fun Bundle.getSubId(key: String, defaultValue: Int): Int {
    return if (Flags.catalystUseStringBundle()) {
        getString(key)?.toIntOrNull() ?: defaultValue
    } else {
        getInt(key, defaultValue)
    }
}

/**
 * Puts an integer subscription ID (`subId`) into a [Bundle] with a given [key].
 *
 * This function is the counterpart to [getSubId] and is useful for migrations where a feature flag
 * controls the data type of a parameter. It checks the `Flags.catalystUseStringBundle()` flag to
 * determine whether to store the value as a `String` or an `Int`.
 *
 * @param key The key with which to associate the value.
 * @param subId The integer value to store.
 */
fun Bundle.putSubId(key: String, subId: Int) {
    if (Flags.catalystUseStringBundle()) {
        putString(key, subId.toString())
    } else {
        putInt(key, subId)
    }
}
