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

package com.android.settings.accessibility.audioadjustment.ui

import android.content.Context
import android.provider.Settings
import com.android.settings.R
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.SwitchPreference

/** Preference for controlling mono audio. */
class MonoAudioPreference :
    SwitchPreference(
        key = KEY,
        purpose = R.string.a11y_mono_audio_setting_purpose,
        title = R.string.accessibility_toggle_primary_mono_title,
        summary = R.string.accessibility_toggle_primary_mono_summary,
    ) {

    override fun storage(context: Context): KeyValueStore = SettingsSystemStore.get(context).apply { setDefaultValue(KEY, false) }

    override val sensitivityLevel: Int
        get() = SensitivityLevel.NO_SENSITIVITY

    override fun getWritePermit(
        context: Context,
        value: Boolean?,
        callingPid: Int,
        callingUid: Int,
    ) = ReadWritePermit.ALLOW

    override val supportsWrite = true

    companion object {
        const val KEY = Settings.System.MASTER_MONO
    }
}
