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

package com.android.settings.accessibility.shared.utils

import android.content.Context
import androidx.annotation.ArrayRes

/**
 * A helper class for managing lazy-initialized maps that associate preference values with their
 * corresponding summary strings.
 *
 * This is particularly useful for list-like preferences where the summary needs to be updated based
 * on the currently selected value from a set of resource arrays.
 *
 * @param K The type of the key used to look up summaries.
 */
class SummaryMap<K>(
    @param:ArrayRes private val valuesRes: Int,
    @param:ArrayRes private val entriesRes: Int,
    private val useIntValues: Boolean = true,
    private val keySelector: (Context, Any) -> K,
) {
    private var map: Map<K, String>? = null

    fun getSummary(context: Context, currentValue: K?): CharSequence? {
        if (currentValue == null) return null
        val currentMap =
            map
                ?: run {
                    val values =
                        if (useIntValues) {
                            context.resources.getIntArray(valuesRes).toTypedArray()
                        } else {
                            context.resources.getStringArray(valuesRes)
                        }
                    val entries = context.resources.getStringArray(entriesRes)
                    values.indices
                        .associate { i -> keySelector(context, values[i]) to entries[i] }
                        .also { map = it }
                }
        return currentMap[currentValue]
    }
}
