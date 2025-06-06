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

import android.app.settings.SettingsEnums
import android.content.Context
import android.os.VibrationAttributes
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import androidx.preference.Preference.OnPreferenceChangeListener
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settings.R
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyValueStoreDelegate
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.SwitchPreference
import com.android.settingslib.preference.SwitchPreferenceBinding

/** Accessibility settings for keyboard vibration, using a switch toggle. */
// LINT.IfChange
class KeyboardVibrationSwitchPreference :
    SwitchPreference(
        key = KEY,
        title = R.string.accessibility_keyboard_vibration_title,
    ),
    PreferenceActionMetricsProvider,
    PreferenceAvailabilityProvider,
    OnPreferenceChangeListener,
    SwitchPreferenceBinding {

    private var storage: KeyboardVibrationSwitchStore? = null

    override val preferenceActionMetrics: Int
        get() = SettingsEnums.ACTION_KEYBOARD_VIBRATION_CHANGED

    override val keywords: Int
        get() = R.string.keywords_keyboard_vibration

    override fun isAvailable(context: Context) =
        context.resources.getBoolean(
            com.android.internal.R.bool.config_keyboardVibrationSettingsSupported)

    override fun isEnabled(context: Context) = storage?.isPreferenceEnabled() ?: true

    override fun storage(context: Context) : KeyValueStore {
        if (storage == null) {
            storage = KeyboardVibrationSwitchStore(SettingsSystemStore.get(context))
        }
        return storage!!
    }

    override fun dependencies(context: Context) = arrayOf(VibrationMainSwitchPreference.KEY)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.onPreferenceChangeListener = this
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        val isChecked = newValue as Boolean
        // must make new value effective before preview
        (preference as TwoStatePreference).setChecked(isChecked)
        if (isChecked) {
            // Vibrate when toggle is enabled for consistency with all the other toggle/slides
            // in the same screen. Use IME feedback intensity for this preview.
            preference.context.playVibrationSettingsPreview(VibrationAttributes.USAGE_IME_FEEDBACK)
        }
        return false
    }

    @Suppress("UNCHECKED_CAST")
    private class KeyboardVibrationSwitchStore(private val settingsStore: KeyValueStore)
        : KeyValueStoreDelegate {

        override val keyValueStoreDelegate: KeyValueStore
            get() = settingsStore

        override fun contains(key: String) = settingsStore.contains(key)

        override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>) =
            DEFAULT_VALUE as T

        override fun <T : Any> getValue(key: String, valueType: Class<T>) =
            if (isPreferenceEnabled()) {
                (settingsStore.getBoolean(key) ?: DEFAULT_VALUE) as T
            } else {
                // Preference must show off when disabled, but value stored must be preserved.
                false as T?
            }

        fun isPreferenceEnabled(): Boolean {
            return settingsStore.getBoolean(VibrationMainSwitchPreference.KEY) != false
        }
    }

    companion object {
        const val KEY = Settings.System.KEYBOARD_VIBRATION_ENABLED
        const val DEFAULT_VALUE = true
    }
}
// LINT.ThenChange(KeyboardVibrationTogglePreferenceController.java)