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
package com.android.settings.display.ambient

import android.content.Context
import android.provider.Settings.Secure.DOZE_ALWAYS_ON_WALLPAPER_ENABLED
import com.android.internal.R as InternalR
import com.android.settings.R
import com.android.settings.display.AmbientDisplayAlwaysOnPreference
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyValueStoreDelegate
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.SwitchPreference
import com.android.systemui.shared.Flags.ambientAod

class AmbientWallpaperPreference :
    SwitchPreference(
        KEY,
        R.string.doze_always_on_wallpaper_title,
        R.string.doze_always_on_wallpaper_description,
    ),
    PreferenceAvailabilityProvider {

    override fun isAvailable(context: Context): Boolean =
        ambientAod() && context.resources.getBoolean(InternalR.bool.config_dozeSupportsAodWallpaper)

    override fun storage(context: Context): KeyValueStore =
        Storage(SettingsSecureStore.get(context))

    fun isChecked(context: Context) = isAvailable(context) && storage(context).getBoolean(KEY)!!

    @Suppress("UNCHECKED_CAST")
    private class Storage(private val settingsStore: KeyValueStore) :
        KeyValueStoreDelegate, KeyedObserver<String> {

        override val keyValueStoreDelegate
            get() = settingsStore

        override fun contains(key: String) = settingsStore.contains(key)

        override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>) = true as T

        override fun <T : Any> getValue(key: String, valueType: Class<T>) =
            settingsStore.getValue(key, valueType) ?: getDefaultValue(key, valueType)

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) =
            settingsStore.setValue(key, valueType, value)

        override fun onKeyChanged(key: String, reason: Int) {
            notifyChange(AmbientDisplayAlwaysOnPreference.KEY, reason)
            notifyChange(AmbientDisplayMainSwitchPreference.KEY, reason)
        }
    }

    companion object {
        val KEY = DOZE_ALWAYS_ON_WALLPAPER_ENABLED
    }
}
