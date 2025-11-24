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

package com.android.settings.accessibility.screenmagnification.ui

import android.content.Context
import android.provider.Settings
import com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.DataChangeReason
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.datastore.SettingsStore

class MagnificationModeStorage(val context: Context) :
    KeyValueStore, AbstractKeyedDataObservable<String>(), KeyedObserver<String> {
    val settingsStore: SettingsStore = SettingsSecureStore.get(context)

    override fun contains(key: String) =
        key == FullScreenModeSelectorPreference.KEY ||
            key == WindowModeSelectorPreference.KEY ||
            key == AllModeSelectorPreference.KEY

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        val mode = settingsStore.getInt(SETTINGS_KEY)
        return when (key) {
            FullScreenModeSelectorPreference.KEY -> (mode == MagnificationMode.FULLSCREEN) as T?
            WindowModeSelectorPreference.KEY -> (mode == MagnificationMode.WINDOW) as T?
            AllModeSelectorPreference.KEY -> (mode == MagnificationMode.ALL) as T?
            else -> false as T?
        }
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        if (value !is Boolean) return
        when (key) {
            FullScreenModeSelectorPreference.KEY ->
                settingsStore.setInt(SETTINGS_KEY, MagnificationMode.FULLSCREEN)
            WindowModeSelectorPreference.KEY ->
                settingsStore.setInt(SETTINGS_KEY, MagnificationMode.WINDOW)
            AllModeSelectorPreference.KEY ->
                settingsStore.setInt(SETTINGS_KEY, MagnificationMode.ALL)
        }
    }

    override fun onFirstObserverAdded() {
        settingsStore.addObserver(SETTINGS_KEY, this, HandlerExecutor.main)
    }

    override fun onLastObserverRemoved() {
        settingsStore.removeObserver(SETTINGS_KEY, this)
    }

    override fun onKeyChanged(key: String, reason: Int) {
        notifyChange(DataChangeReason.UPDATE)
    }

    companion object {
        const val SETTINGS_KEY = Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CAPABILITY

        fun getReadPermissions() = SettingsSecureStore.getReadPermissions()

        fun getWritePermissions() = SettingsSecureStore.getWritePermissions()
    }
}
