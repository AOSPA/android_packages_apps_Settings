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

package com.android.settings.notification

import android.content.Context
import android.media.AudioDeviceAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.Spatializer
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_NOTIFICATIONS

// LINT.IfChange
@ProvidePreferenceScreen(SpatialAudioApiScreen.KEY)
class SpatialAudioApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.SOUND,
        fragment = SpatialAudioSettings::class,
        purpose = R.string.spatial_audio_screen_purpose,
    ) {
    /**
     * Cache of the Spatializer to reduce the number of calls to the system service.
     */
    @Volatile
    private var cachedSpatializer: Spatializer? = null

    init {
        flag { Flags.catalystMigration26q2() }

        tags(APP_FUNCTION_NOTIFICATIONS)

        preconditions(R.string.spatial_audio_screen_preconditions) {
            if (context.isSpatializerAvailable()) {
                Allowed
            } else {
                HardwareUnsupported(R.string.spatial_audio_screen_hardware_unsupported)
            }
        }

        preference(
            key = "spatial_audio",
            purpose = R.string.spatial_audio_phone_speakers_purpose,
            type = AnyBoolean,
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)

            val speaker by lazy {
                AudioDeviceAttributes(
                AudioDeviceAttributes.ROLE_OUTPUT, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, ""
                )
            }

            preconditions(R.string.spatial_audio_phone_speaker_preconditions) {
                if (context.spatializer?.isAvailableForDevice(speaker) == true) {
                    Allowed
                } else {
                    HardwareUnsupported(R.string.spatial_audio_screen_hardware_unsupported)
                }
            }

            get {
                execute {
                    context.spatializer?.compatibleAudioDevices?.contains(speaker) ?: false
                }
            }

            set {
                execute { value ->
                    context.spatializer?.let {
                        if (value) {
                            it.addCompatibleAudioDevice(speaker)
                        } else {
                            it.removeCompatibleAudioDevice(speaker)
                        }
                    }
                }
            }
        }

        preference(
            key = "spatial_audio_wired_headphones",
            purpose = R.string.spatial_audio_wired_headphones_purpose,
            type = AnyBoolean,
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)

            val wiredHeadphones by lazy {
                AudioDeviceAttributes(
                    AudioDeviceAttributes.ROLE_OUTPUT, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, ""
                )
            }

            preconditions(R.string.spatial_audio_wired_headphones_preconditions) {
                if (context.spatializer?.isAvailableForDevice(wiredHeadphones) == true) {
                    Allowed
                } else {
                    HardwareUnsupported(R.string.spatial_audio_screen_hardware_unsupported)
                }
            }

            get {
                execute {
                    context.spatializer?.compatibleAudioDevices?.contains(wiredHeadphones) ?: false
                }
            }

            set {
                execute { value ->
                    context.spatializer?.let {
                        if (value) {
                            it.addCompatibleAudioDevice(wiredHeadphones)
                        } else {
                            it.removeCompatibleAudioDevice(wiredHeadphones)
                        }
                    }
                }
            }
        }
    }


    /**
     * Private helper to fetch and cache the Spatializer from the Application Context.
     */
    private val Context.spatializer: Spatializer?
        get() {
            if (cachedSpatializer == null) {
                cachedSpatializer = applicationContext
                    .getSystemService(AudioManager::class.java)
                    ?.spatializer
            }
            return cachedSpatializer
        }

    private fun Context.isSpatializerAvailable(): Boolean {
        return spatializer?.immersiveAudioLevel != Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_NONE
    }

    companion object {
        const val KEY = "spatial_audio_screen"
    }
}
// LINT.ThenChange(
//      SpatialAudioSettings.java,
//      SpatialAudioParentPreferenceController.java,
//      SpatialAudioWiredHeadphonesController.java
// )
