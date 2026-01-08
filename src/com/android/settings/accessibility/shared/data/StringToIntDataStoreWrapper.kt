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

package com.android.settings.accessibility.shared.data

import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyValueStoreDelegate

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

/**
 * A [KeyValueStore] wrapper that stores string values as integers in the underlying data store. It
 * handles the conversion between [String] and [Int] types.
 *
 * This is useful for settings that are represented as strings in the UI (e.g., in a ListPreference)
 * but are stored as integers in the underlying [KeyValueStore].
 */
class StringToIntDataStoreWrapper(override val keyValueStoreDelegate: KeyValueStore) :
    KeyValueStoreDelegate {

    override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>): T? {
        return when (valueType) {
            String::class.javaObjectType ->
                super.getDefaultValue(key, Int::class.javaObjectType)?.toString() as? T
            else -> super.getDefaultValue(key, valueType)
        }
    }

    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        return when (valueType) {
            String::class.javaObjectType ->
                super.getValue(key, Int::class.javaObjectType)?.toString() as? T
            else -> super.getValue(key, valueType)
        }
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        when (valueType) {
            String::class.javaObjectType -> {
                val intValue = (value as? String)?.toIntOrNull()
                super.setValue(key, Int::class.javaObjectType, intValue)
            }
            else -> super.setValue(key, valueType, value)
        }
    }
}
