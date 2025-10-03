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

package com.android.settings.accessibility.shortcuts.data

import android.content.Context
import android.os.UserHandle
import android.view.accessibility.AccessibilityManager
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType
import com.android.internal.accessibility.util.ShortcutUtils
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.DataChangeReason
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore

/**
 * Observes and controls the enabled state for a set of accessibility shortcut targets.
 *
 * This class monitors a specific secure setting and determines if all of its configured `targets`
 * are enabled for the given `shortcutType`. It allows setting the state for these targets and
 * notifies observers when the underlying system setting changes.
 *
 * @param context The application context.
 * @param shortcutType The type of shortcut to observe, defined by [UserShortcutType].
 * @param targets The set of shortcut target component names this instance represents.
 * @param settingsStore The underlying key-value store for observing secure settings changes.
 */
class ShortcutOptionDataStore(
    private val context: Context,
    @param:UserShortcutType private val shortcutType: Int,
    private val targets: Set<String>,
    private val settingsStore: KeyValueStore = SettingsSecureStore.get(context),
) : AbstractKeyedDataObservable<String>(), KeyedObserver<String>, KeyValueStore {

    private val settingKey = ShortcutUtils.convertToKey(shortcutType)

    override fun onFirstObserverAdded() {
        settingsStore.addObserver(settingKey, this, HandlerExecutor.main)
    }

    override fun onLastObserverRemoved() {
        settingsStore.removeObserver(settingKey, this)
    }

    override fun onKeyChanged(key: String, reason: Int) {
        notifyChange(DataChangeReason.UPDATE)
    }

    override fun contains(key: String): Boolean = settingsStore.contains(settingKey)

    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        if (valueType != Boolean::class.javaObjectType) return null

        val shortcutOptionEnabledTargets =
            context
                .getSystemService(AccessibilityManager::class.java)
                ?.getAccessibilityShortcutTargets(shortcutType) ?: emptyList()

        return (!shortcutOptionEnabledTargets.isEmpty() &&
            shortcutOptionEnabledTargets.containsAll(targets))
            as T?
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        if (value !is Boolean) {
            throw IllegalStateException("ShortcutOptionDataStore only supports boolean values")
        }

        context
            .getSystemService(AccessibilityManager::class.java)
            ?.enableShortcutsForTargets(value, shortcutType, targets, UserHandle.myUserId())
    }
}
