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
import android.media.AudioManager
import android.os.VibrationAttributes.USAGE_RINGTONE
import android.provider.DeviceConfig
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import com.android.settings.R
import com.android.settings.Utils
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyValueStoreDelegate
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.SwitchPreference
import com.android.settingslib.preference.SwitchPreferenceBinding

/**
 * SwitchPreference for vibration ramping ringer.
 *
 * This preference observes the state of the ring vibration intensity preference (slider or switch)
 * in this fragment, disabling and unchecking this switch when the ring intensity switch is off.
 * This "unchecked" state should not be persisted, as the original user settings value must be
 * preserved and restored once the ring intensity is turned back on. This behavior reflects the
 * actual system behavior that will not ramp ring vibrations when they're off.
 */
// LINT.IfChange
class RampingRingerVibrationSwitchPreference(
    context: Context,
    private val deviceConfig: TelephonyConfigProvider = object: TelephonyConfigProvider {},
) : SwitchPreference(
    key = KEY,
    title = R.string.vibrate_when_ringing_option_ramping_ringer,
),
    PreferenceAvailabilityProvider,
    OnPreferenceChangeListener,
    SwitchPreferenceBinding {

    private val storage by lazy { RampingRingerVibrationSwitchStore(context) }

    override val keywords: Int
        get() = R.string.keywords_ramping_ringer_vibration

    override fun isAvailable(context: Context) =
        deviceConfig.isVoiceCapable(context) && !deviceConfig.isTelephonyRampingRingerEnabled()

    override fun storage(context: Context): KeyValueStore = storage

    override fun dependencies(context: Context) = arrayOf(RING_KEY)

    override fun isEnabled(context: Context) = storage.isPreferenceEnabled()

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.onPreferenceChangeListener = this
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        val enabled = newValue as Boolean
        preference.context.getSystemService(AudioManager::class.java)?.let {
            it.isRampingRingerEnabled = enabled
        }
        if (enabled) {
            // Vibrate when toggle is enabled for consistency with all the other toggle/slides
            // in the same screen. Use ring vibration intensity for this preview.
            preference.context.playVibrationSettingsPreview(USAGE_RINGTONE)
        }
        return true
    }

    companion object {
        const val KEY = Settings.System.APPLY_RAMPING_RINGER
        const val RING_KEY = Settings.System.RING_VIBRATION_INTENSITY
    }

    /** Telephony config wrapper for testing. */
    interface TelephonyConfigProvider {
        fun isTelephonyRampingRingerEnabled() =
            DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_TELEPHONY,
                "ramping_ringer_enabled",
                false
            )

        fun isVoiceCapable(context: Context) = Utils.isVoiceCapable(context)
    }
}

/** Provides SettingsStore for vibration ramping ringer switch with custom enabled state. */
private class RampingRingerVibrationSwitchStore(
    context: Context,
    override val keyValueStoreDelegate: KeyValueStore = SettingsSystemStore.get(context),
) : KeyValueStoreDelegate {
    private val ringIntensityStore = VibrationIntensitySettingsStore(context, USAGE_RINGTONE)

    fun isPreferenceEnabled() =
        ringIntensityStore.getBoolean(RampingRingerVibrationSwitchPreference.RING_KEY) == true

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        if (!isPreferenceEnabled()) {
            // Preference must show state off when disabled, but value stored must be preserved.
            return false as T?
        }
        return super.getValue(key, valueType)
    }
}
// LINT.ThenChange(VibrationRampingRingerTogglePreferenceController.java)
