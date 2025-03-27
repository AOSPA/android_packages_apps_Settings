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

package com.android.settings.sound

import android.content.Context
import android.provider.Settings.Secure.MEDIA_CONTROLS_LOCK_SCREEN

import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObservableDelegate
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.datastore.SettingsStore
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.SwitchPreference
import com.android.settings.R

// LINT.IfChange
class MediaControlsLockscreenSwitchPreference : SwitchPreference(
    KEY,
    R.string.media_controls_lockscreen_title,
    R.string.media_controls_lockscreen_description,
) {

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    override fun getReadPermissions(context: Context) = SettingsSecureStore.getReadPermissions()

    override fun getWritePermissions(context: Context) = SettingsSecureStore.getWritePermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun storage(context: Context): KeyValueStore =
        MediaControlsLockscreenStore(SettingsSecureStore.get(context))

    @Suppress("UNCHECKED_CAST")
    private class MediaControlsLockscreenStore(private val settingsStore: SettingsStore) :
        KeyedObservableDelegate<String>(settingsStore), KeyValueStore {
        override fun contains(key: String) = settingsStore.contains(key)

        override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>) = true as T

        override fun <T : Any> getValue(key: String, valueType: Class<T>) =
            settingsStore.getValue(key, valueType) ?: getDefaultValue(key, valueType)

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) =
            settingsStore.setValue(key, valueType, value)
    }

    companion object {
        const val KEY = MEDIA_CONTROLS_LOCK_SCREEN
    }
}
// LINT.ThenChange(MediaControlsLockScreenPreferenceController.java)