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
import android.os.VibrationAttributes
import android.os.Vibrator
import android.provider.Settings
import androidx.core.content.getSystemService
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.testutils.shadow.ShadowAudioManager
import com.android.settingslib.datastore.SettingsSystemStore
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowAudioManager::class])
class VibrationIntensitySettingsStoreTest {
    private companion object {
        const val KEY: String = Settings.System.HAPTIC_FEEDBACK_INTENSITY
        const val VIBRATION_USAGE: Int = VibrationAttributes.USAGE_TOUCH
        const val DEFAULT_INTENSITY: Int = Vibrator.VIBRATION_INTENSITY_MEDIUM
        const val SUPPORTED_INTENSITIES: Int = Vibrator.VIBRATION_INTENSITY_HIGH
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val settingsStore = SettingsSystemStore.get(context)
    private val store = VibrationIntensitySettingsStore(
        context = context,
        vibrationUsage = VIBRATION_USAGE,
        hasRingerModeDependency = false,
        key = KEY,
        keyValueStoreDelegate = settingsStore,
        defaultIntensity = DEFAULT_INTENSITY,
        supportedIntensityLevels = SUPPORTED_INTENSITIES,
    )

    @Test
    fun isPreferenceEnabled_returnsVibrateOnSettingOrTrue() {
        settingsStore.setBoolean(Settings.System.VIBRATE_ON, null)
        assertThat(store.isPreferenceEnabled()).isTrue()

        settingsStore.setBoolean(Settings.System.VIBRATE_ON, true)
        assertThat(store.isPreferenceEnabled()).isTrue()

        settingsStore.setBoolean(Settings.System.VIBRATE_ON, false)
        assertThat(store.isPreferenceEnabled()).isFalse()
    }

    @Test
    fun isPreferenceEnabled_noRingerModeDependency_ignoresRingerMode() {
        val testStore = VibrationIntensitySettingsStore(
            context = context,
            vibrationUsage = VIBRATION_USAGE,
            hasRingerModeDependency = false,
            key = KEY,
            keyValueStoreDelegate = settingsStore,
            defaultIntensity = DEFAULT_INTENSITY,
            supportedIntensityLevels = SUPPORTED_INTENSITIES,
        )

        setRingerMode(AudioManager.RINGER_MODE_SILENT)
        testStore.setInt(KEY, Vibrator.VIBRATION_INTENSITY_HIGH)

        assertThat(testStore.isPreferenceEnabled()).isTrue()
        assertThat(settingsStore.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
        assertThat(testStore.getBoolean(KEY)).isTrue()
        assertThat(testStore.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
    }

    @Test
    fun isPreferenceEnabled_withRingerModeDependency_returnsDisabledWhenRingerModeSilent() {
        val testStore = VibrationIntensitySettingsStore(
            context = context,
            vibrationUsage = VIBRATION_USAGE,
            hasRingerModeDependency = true,
            key = KEY,
            keyValueStoreDelegate = settingsStore,
            defaultIntensity = DEFAULT_INTENSITY,
            supportedIntensityLevels = SUPPORTED_INTENSITIES,
        )

        setRingerMode(AudioManager.RINGER_MODE_VIBRATE)
        testStore.setInt(KEY, Vibrator.VIBRATION_INTENSITY_HIGH)

        assertThat(testStore.isPreferenceEnabled()).isTrue()
        assertThat(settingsStore.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
        assertThat(testStore.getBoolean(KEY)).isTrue()
        assertThat(testStore.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)

        setRingerMode(AudioManager.RINGER_MODE_SILENT)

        assertThat(testStore.isPreferenceEnabled()).isFalse()
        assertThat(settingsStore.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
        assertThat(testStore.getBoolean(KEY)).isFalse()
        assertThat(testStore.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
    }

    @Test
    fun isDisabledByRingerMode_returnsWhenRingerModeSilentAndMainSwitchOn() {
        val testStore = VibrationIntensitySettingsStore(
            context = context,
            vibrationUsage = VIBRATION_USAGE,
            hasRingerModeDependency = true,
            key = KEY,
            keyValueStoreDelegate = settingsStore,
            defaultIntensity = DEFAULT_INTENSITY,
            supportedIntensityLevels = SUPPORTED_INTENSITIES,
        )

        settingsStore.setBoolean(Settings.System.VIBRATE_ON, false)
        setRingerMode(AudioManager.RINGER_MODE_SILENT)
        assertThat(testStore.isDisabledByRingerMode()).isFalse()

        settingsStore.setBoolean(Settings.System.VIBRATE_ON, false)
        setRingerMode(AudioManager.RINGER_MODE_VIBRATE)
        assertThat(testStore.isDisabledByRingerMode()).isFalse()

        settingsStore.setBoolean(Settings.System.VIBRATE_ON, true)
        setRingerMode(AudioManager.RINGER_MODE_VIBRATE)
        assertThat(testStore.isDisabledByRingerMode()).isFalse()

        settingsStore.setBoolean(Settings.System.VIBRATE_ON, true)
        setRingerMode(AudioManager.RINGER_MODE_SILENT)
        assertThat(testStore.isDisabledByRingerMode()).isTrue()
    }

    @Test
    fun getValue_preferenceDisabledByMainSwitch_returnsIntensityOffAndPreservesValue() {
        settingsStore.setBoolean(Settings.System.VIBRATE_ON, false)
        setIntValue(Vibrator.VIBRATION_INTENSITY_HIGH)

        assertThat(settingsStore.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
        assertThat(store.getBoolean(KEY)).isFalse()
        assertThat(store.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
    }

    @Test
    fun getValue_preferenceDisabledByRingerMode_returnsIntensityOffAndPreservesValue() {
        settingsStore.setBoolean(Settings.System.VIBRATE_ON, true)
        setRingerMode(AudioManager.RINGER_MODE_SILENT)
        val testStore = VibrationIntensitySettingsStore(
            context = context,
            vibrationUsage = VIBRATION_USAGE,
            hasRingerModeDependency = true,
            key = KEY,
            keyValueStoreDelegate = settingsStore,
            defaultIntensity = DEFAULT_INTENSITY,
            supportedIntensityLevels = SUPPORTED_INTENSITIES,
        )
        setIntValue(Vibrator.VIBRATION_INTENSITY_HIGH)

        assertThat(settingsStore.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
        assertThat(testStore.getBoolean(KEY)).isFalse()
        assertThat(testStore.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
    }

    @Test
    fun getValue_valueNull_returnDefaultIntensity() {
        setIntValue(null)

        assertThat(settingsStore.getInt(KEY)).isNull()
        assertThat(store.getBoolean(KEY)).isTrue()
        assertThat(store.getInt(KEY)).isEqualTo(DEFAULT_INTENSITY)
    }

    @Test
    fun getValue_valueTrue_returnDefaultIntensity() {
        setBooleanValue(true)

        assertThat(settingsStore.getInt(KEY)).isEqualTo(DEFAULT_INTENSITY)
        assertThat(store.getBoolean(KEY)).isTrue()
        assertThat(store.getInt(KEY)).isEqualTo(DEFAULT_INTENSITY)
    }

    @Test
    fun getValue_valueFalse_returnIntensityOff() {
        setBooleanValue(false)

        assertThat(settingsStore.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
        assertThat(store.getBoolean(KEY)).isFalse()
        assertThat(store.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
    }

    @Test
    fun getValue_valueIntensityIntSupported_returnValueSet() {
        setIntValue(Vibrator.VIBRATION_INTENSITY_HIGH)

        assertThat(settingsStore.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
        assertThat(store.getBoolean(KEY)).isTrue()
        assertThat(store.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
    }

    @Test
    fun getValue_valueIntensityIntUnsupported_returnMaxSupported() {
        setIntValue(Vibrator.VIBRATION_INTENSITY_HIGH + 1)

        assertThat(settingsStore.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
        assertThat(store.getBoolean(KEY)).isTrue()
        assertThat(store.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
    }

    @Test
    fun getValue_valueIntensityOff_returnIntensityOff() {
        setIntValue(Vibrator.VIBRATION_INTENSITY_OFF)

        assertThat(settingsStore.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
        assertThat(store.getBoolean(KEY)).isFalse()
        assertThat(store.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
    }

    @Test
    fun getValue_preferenceDisabled_returnOffAndPreservesValue() {
        settingsStore.setBoolean(Settings.System.VIBRATE_ON, false)
        setBooleanValue(true)

        assertThat(settingsStore.getInt(KEY)).isEqualTo(DEFAULT_INTENSITY)
        assertThat(store.getBoolean(KEY)).isFalse()
        assertThat(store.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
    }

    @Test
    fun setValue_updatesCorrectly() {
        setBooleanValue(null)

        assertThat(settingsStore.getInt(KEY)).isNull()
        assertThat(store.getBoolean(KEY)).isTrue()
        assertThat(store.getInt(KEY)).isEqualTo(DEFAULT_INTENSITY)

        setBooleanValue(false)

        assertThat(settingsStore.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
        assertThat(store.getBoolean(KEY)).isFalse()
        assertThat(store.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)

        setIntValue(Vibrator.VIBRATION_INTENSITY_HIGH)

        assertThat(settingsStore.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
        assertThat(store.getBoolean(KEY)).isTrue()
        assertThat(store.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)

        setBooleanValue(true)

        assertThat(settingsStore.getInt(KEY)).isEqualTo(DEFAULT_INTENSITY)
        assertThat(store.getBoolean(KEY)).isTrue()
        assertThat(store.getInt(KEY)).isEqualTo(DEFAULT_INTENSITY)
    }

    @Test
    fun setUnsupportedIntValue_updatesWithinSupportedLevels() {
        setIntValue(Vibrator.VIBRATION_INTENSITY_HIGH + 1)

        assertThat(settingsStore.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
        assertThat(store.getBoolean(KEY)).isTrue()
        assertThat(store.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
    }

    @Test
    fun supportsOneLevel_usesDefaultIntensity() {
        val testStore = VibrationIntensitySettingsStore(
            context = context,
            vibrationUsage = VIBRATION_USAGE,
            hasRingerModeDependency = false,
            key = KEY,
            keyValueStoreDelegate = settingsStore,
            defaultIntensity = Vibrator.VIBRATION_INTENSITY_MEDIUM,
            supportedIntensityLevels = 1,
        )

        testStore.setInt(KEY, null)

        assertThat(settingsStore.getInt(KEY)).isNull()
        assertThat(testStore.getBoolean(KEY)).isTrue()
        assertThat(testStore.getInt(KEY)).isEqualTo(1)

        testStore.setInt(KEY, 1)

        assertThat(settingsStore.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_MEDIUM)
        assertThat(testStore.getBoolean(KEY)).isTrue()
        assertThat(testStore.getInt(KEY)).isEqualTo(1)
    }

    @Test
    fun supportsTwoLevels_usesLowAndHighIntensities() {
        val testStore = VibrationIntensitySettingsStore(
            context = context,
            vibrationUsage = VibrationAttributes.USAGE_TOUCH,
            hasRingerModeDependency = false,
            key = KEY,
            keyValueStoreDelegate = settingsStore,
            defaultIntensity = Vibrator.VIBRATION_INTENSITY_MEDIUM,
            supportedIntensityLevels = 2,
        )

        testStore.setInt(KEY, null)

        assertThat(settingsStore.getInt(KEY)).isNull()
        assertThat(testStore.getBoolean(KEY)).isTrue()
        assertThat(testStore.getInt(KEY)).isEqualTo(2)

        testStore.setInt(KEY, 1)

        assertThat(settingsStore.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_LOW)
        assertThat(testStore.getBoolean(KEY)).isTrue()
        assertThat(testStore.getInt(KEY)).isEqualTo(1)

        testStore.setInt(KEY, 2)

        assertThat(settingsStore.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
        assertThat(testStore.getBoolean(KEY)).isTrue()
        assertThat(testStore.getInt(KEY)).isEqualTo(2)
    }

    private fun setRingerMode(ringerMode: Int) {
        val audioManager = context.getSystemService<AudioManager>()
        audioManager?.ringerModeInternal = ringerMode
        assertThat(audioManager?.ringerModeInternal).isEqualTo(ringerMode)
    }

    private fun setIntValue(value: Int?) =
        store.setInt(KEY, value)

    private fun setBooleanValue(value: Boolean?) =
        store.setBoolean(KEY, value)
}
