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
import com.android.settingslib.metadata.CatalystFlagProviderFactory

/**
 * Retrieves a subscription ID from a [Bundle] by a given [key].
 *
 * This function retrieves the value associated with the key and checks its type.
 * If the value is a [String], it attempts to parse it as an integer.
 * If the value is an [Int], it is returned directly.
 * This provides compatibility for cases where the subscription ID might be stored in either format.
 *
 * @param key The key to look up in the Bundle.
 * @param defaultValue The value to return if the key is not found or if the `String` value cannot
 *   be parsed as an integer.
 * @return The integer value associated with the key, or [defaultValue] if the key is not found or a
 *   parsing error occurs.
 */
fun Bundle.getSubId(key: String, defaultValue: Int): Int {
    return when (@Suppress("DEPRECATION") val value = get(key)) {
        is String -> value.toIntOrNull() ?: defaultValue
        is Int -> value
        else -> defaultValue
    }
}

/**
 * Puts an integer subscription ID (`subId`) into a [Bundle] with a given [key].
 *
 * This function is the counterpart to [getSubId] and is useful for migrations where a feature flag
 * controls the data type of a parameter. It checks [Flags.catalystUseStringBundle] and
 * [CatalystFlagProviderFactory.catalystUseKeyParameters] to determine whether to store the value as
 * a `String` or an `Int`.
 *
 * @param key The key with which to associate the value.
 * @param subId The integer value to store.
 */
fun Bundle.putSubId(key: String, subId: Int) {
    if (Flags.catalystUseStringBundle() || CatalystFlagProviderFactory.catalystUseKeyParameters()) {
        putString(key, subId.toString())
    } else {
        putInt(key, subId)
    }
}
