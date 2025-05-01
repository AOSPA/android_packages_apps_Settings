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
package com.android.settings.accessibility

import android.content.Context
import android.os.VibrationAttributes.Usage
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.SwitchPreference
import com.android.settingslib.preference.SwitchPreferenceBinding

/**
 * SwitchPreference for vibration intensity.
 *
 * This implementation uses VibrationIntensitySettingsStore to save the device default vibration
 * intensity value when the switch is turned on, also playing a haptic preview.
 *
 * This preference observes the state of the VibrationMainSwitchPreference in this fragment,
 * disabling and unchecking this switch when the main switch is unchecked. This "unchecked" state
 * should not be persisted, as the original user settings value must be preserved and restored once
 * the main switch is turned back on. This behavior reflects the actual system behavior that
 * restricts all vibrations when the main switch is off.
 */
// LINT.IfChange
open class VibrationIntensitySwitchPreference(
    key: String,
    @Usage val vibrationUsage: Int,
    @StringRes title: Int = 0,
    @StringRes summary: Int = 0,
) : SwitchPreference(key, title, summary),
    OnPreferenceChangeListener,
    SwitchPreferenceBinding {

    private var storage: VibrationIntensitySettingsStore? = null

    override fun storage(context: Context): KeyValueStore {
        if (storage == null) {
            storage = VibrationIntensitySettingsStore(context, vibrationUsage)
        }
        return storage!!
    }

    override fun dependencies(context: Context) = arrayOf(VibrationMainSwitchPreference.KEY)

    @CallSuper
    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.onPreferenceChangeListener = this
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        if (newValue as Boolean) {
            preference.context.playVibrationSettingsPreview(vibrationUsage)
        }
        return true
    }

    @CallSuper
    override fun isEnabled(context: Context) = storage?.isPreferenceEnabled() ?: true
}
// LINT.ThenChange(VibrationTogglePreferenceController.java)
