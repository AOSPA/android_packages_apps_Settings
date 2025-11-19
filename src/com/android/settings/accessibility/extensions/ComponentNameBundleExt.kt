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
@file:JvmName("ComponentNameBundleUtils")

package com.android.settings.accessibility.extensions

import android.content.ComponentName
import android.os.Bundle
import com.android.settings.flags.Flags

/**
 * Retrieves a [ComponentName] from a [Bundle] by a given [key]. The method of retrieval depends on
 * the `Flags.catalystUseStringBundle()` flag. If the flag is enabled, it attempts to retrieve a
 * flattened `String` and unflatten it. Otherwise, it retrieves the value as a `Parcelable`.
 *
 * This function is useful for migrations where a feature flag controls the data type of a
 * parameter. It checks the `Flags.catalystUseStringBundle()` flag to determine whether to retrieve
 * the value as a `String` (and unflatten it) or to retrieve it directly as a `Parcelable`.
 *
 * @param key The key to look up in the Bundle.
 * @return The [ComponentName] associated with the key, or `null` if the key is not found or the
 *   flattened string is malformed.
 */
fun Bundle.getComponentName(key: String): ComponentName? {
    return if (Flags.catalystUseStringBundle()) {
        val componentNameString = getString(key) ?: return null
        ComponentName.unflattenFromString(componentNameString)
    } else {
        getParcelable(key, ComponentName::class.java)
    }
}

/**
 * Puts a [ComponentName] into a [Bundle] with a given [key].
 *
 * This function is the counterpart to [getComponentName] and is useful for migrations where a
 * feature flag controls the data type of a parameter. It checks the
 * `Flags.catalystUseStringBundle()` flag to determine whether to store the value as a flattened
 * `String` or as a `Parcelable`.
 *
 * @param key The key with which to associate the value.
 * @param componentName The [ComponentName] to store.
 */
fun Bundle.putComponentName(key: String, componentName: ComponentName) {
    if (Flags.catalystUseStringBundle()) {
        putString(key, componentName.flattenToString())
    } else {
        putParcelable(key, componentName)
    }
}
