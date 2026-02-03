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

import android.media.AudioDeviceAttributes
import android.media.AudioDeviceInfo
import android.media.Spatializer
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.HardwareUnsupportedException
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@RunWith(AndroidJUnit4::class)
class SpatialAudioApiScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val tester = ApiTester(SpatialAudioApiScreen())
    private val speaker = AudioDeviceAttributes(
        AudioDeviceAttributes.ROLE_OUTPUT, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, ""
    )

    private val wiredHeadphones = AudioDeviceAttributes(
        AudioDeviceAttributes.ROLE_OUTPUT, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, ""
    )

    @Before
    fun setUp() {
        ShadowSpatializer.reset()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagEnabled_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    @Config(shadows = [ShadowSpatializer::class])
    fun get_spatialAudio_whenCompatible_returnsTrue() {
        ShadowSpatializer.compatibleDevices.add(speaker)

        assertThat(tester.get<Boolean>("spatial_audio")).isTrue()
    }

    @Test
    @Config(shadows = [ShadowSpatializer::class])
    fun get_spatialAudio_whenNotCompatible_returnsFalse() {
        ShadowSpatializer.compatibleDevices.clear()

        assertThat(tester.get<Boolean>("spatial_audio")).isFalse()
    }

    @Test
    @Config(shadows = [ShadowSpatializer::class])
    fun set_spatialAudio_toTrue_addsDevice() {
        tester.set("spatial_audio", true)

        assertThat(ShadowSpatializer.compatibleDevices).contains(speaker)
    }

    @Test
    @Config(shadows = [ShadowSpatializer::class])
    fun set_spatialAudio_toFalse_removesDevice() {
        ShadowSpatializer.compatibleDevices.add(speaker)

        tester.set("spatial_audio", false)

        assertThat(ShadowSpatializer.compatibleDevices).doesNotContain(speaker)
    }

    @Test(expected = HardwareUnsupportedException::class)
    @Config(shadows = [ShadowSpatializer::class])
    fun get_screenPreconditionFails_throwsException() {
        // Simulate hardware not supporting spatial audio at all
        ShadowSpatializer.level = Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_NONE

        tester.get<Boolean>("spatial_audio")
    }

    @Test(expected = HardwareUnsupportedException::class)
    @Config(shadows = [ShadowSpatializer::class])
    fun get_preferencePreconditionFails_throwsException() {
        // Simulate speaker specifically not supporting spatial audio
        ShadowSpatializer.availableForDevice = false

        tester.get<Boolean>("spatial_audio")
    }

    @Test
    @Config(shadows = [ShadowSpatializer::class])
    fun get_wiredHeadphones_whenCompatible_returnsTrue() {
        ShadowSpatializer.compatibleDevices.add(wiredHeadphones)

        assertThat(tester.get<Boolean>("spatial_audio_wired_headphones")).isTrue()
    }

    @Test
    @Config(shadows = [ShadowSpatializer::class])
    fun get_wiredHeadphones_whenNotCompatible_returnsFalse() {
        ShadowSpatializer.compatibleDevices.clear()

        assertThat(tester.get<Boolean>("spatial_audio_wired_headphones")).isFalse()
    }

    @Test
    @Config(shadows = [ShadowSpatializer::class])
    fun set_wiredHeadphones_toTrue_addsDevice() {
        tester.set("spatial_audio_wired_headphones", true)

        assertThat(ShadowSpatializer.compatibleDevices).contains(wiredHeadphones)
    }

    @Test
    @Config(shadows = [ShadowSpatializer::class])
    fun set_wiredHeadphones_toFalse_removesDevice() {
        ShadowSpatializer.compatibleDevices.add(wiredHeadphones)

        tester.set("spatial_audio_wired_headphones", false)

        assertThat(ShadowSpatializer.compatibleDevices).doesNotContain(wiredHeadphones)
    }

    @Test(expected = HardwareUnsupportedException::class)
    @Config(shadows = [ShadowSpatializer::class])
    fun get_wiredHeadphonesPreconditionFails_throwsException() {
        // Simulate wired headphones specifically not supporting spatial audio
        ShadowSpatializer.availableForDevice = false

        tester.get<Boolean>("spatial_audio_wired_headphones")
    }

    @Test
    @Config(shadows = [ShadowSpatializer::class])
    fun getLaunchIntent_hasIntent() {
        assertThat(tester.getLaunchIntent()).isNotNull()
    }
}

@Implements(Spatializer::class)
class ShadowSpatializer {
    companion object {
        var level = Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_MULTICHANNEL
        val compatibleDevices = mutableListOf<AudioDeviceAttributes>()
        var availableForDevice = true

        fun reset() {
            level = Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_MULTICHANNEL
            compatibleDevices.clear()
            availableForDevice = true
        }
    }

    @Implementation
    fun getImmersiveAudioLevel(): Int = level

    @Implementation
    fun isAvailableForDevice(attributes: AudioDeviceAttributes): Boolean = availableForDevice

    @Implementation
    fun getCompatibleAudioDevices(): List<AudioDeviceAttributes> = compatibleDevices

    @Implementation
    fun addCompatibleAudioDevice(attributes: AudioDeviceAttributes) {
        compatibleDevices.add(attributes)
    }

    @Implementation
    fun removeCompatibleAudioDevice(attributes: AudioDeviceAttributes) {
        compatibleDevices.remove(attributes)
    }
}

