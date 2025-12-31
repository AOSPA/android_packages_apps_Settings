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
import android.media.AudioManager
import android.media.Spatializer
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported

// LINT.IfChange
@ProvidePreferenceScreen(SpatialAudioApiScreen.KEY)
class SpatialAudioApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.SOUND,
        fragment = SpatialAudioSettings::class,
        purpose = R.string.spatial_audio_screen_purpose,
    ) {

    init {
        flag { Flags.catalystMigration26q2() }

        preconditions(R.string.spatial_audio_screen_preconditions) {
            if (context.isSpatializerAvailable()) {
                Allowed
            } else {
                HardwareUnsupported(R.string.spatial_audio_screen_hardware_unsupported)
            }
        }
    }

    companion object {
        const val KEY = "spatial_audio_screen"

        fun Context.isSpatializerAvailable(): Boolean {
            val audioManager: AudioManager =
                getSystemService(AudioManager::class.java) ?: return false
            return audioManager.spatializer.immersiveAudioLevel !=
                Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_NONE
        }
    }
}
// LINT.ThenChange(SpatialAudioSettings.java, SpatialAudioParentPreferenceController.java)
