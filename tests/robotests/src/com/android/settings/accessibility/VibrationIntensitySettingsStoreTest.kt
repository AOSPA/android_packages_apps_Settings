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
import com.android.settings.accessibility.AccessibilityUtil.State
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
        const val TOUCH_USAGE: Int = VibrationAttributes.USAGE_TOUCH
        const val RINGTONE_USAGE: Int = VibrationAttributes.USAGE_RINGTONE
        const val DEFAULT_INTENSITY: Int = Vibrator.VIBRATION_INTENSITY_MEDIUM
        const val SUPPORTED_INTENSITIES: Int = Vibrator.VIBRATION_INTENSITY_HIGH
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val settingsStore = SettingsSystemStore.get(context)
    private val store = VibrationIntensitySettingsStore(
        context = context,
        vibrationUsage = TOUCH_USAGE,
        keyValueStoreDelegate = settingsStore,
        defaultIntensity = DEFAULT_INTENSITY,
        supportedIntensityLevels = SUPPORTED_INTENSITIES,
    )
    private val storeWithRingerMode = VibrationIntensitySettingsStore(
        context = context,
        vibrationUsage = RINGTONE_USAGE,
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
        setRingerMode(AudioManager.RINGER_MODE_SILENT)
        store.setInt(KEY, Vibrator.VIBRATION_INTENSITY_HIGH)

        assertThat(store.isPreferenceEnabled()).isTrue()
        assertThat(settingsStore.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
        assertThat(store.getBoolean(KEY)).isTrue()
        assertThat(store.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
    }

    @Test
    fun isPreferenceEnabled_withRingerModeDependency_returnsDisabledWhenRingerModeSilent() {
        setRingerMode(AudioManager.RINGER_MODE_VIBRATE)
        storeWithRingerMode.setInt(KEY, Vibrator.VIBRATION_INTENSITY_HIGH)

        assertThat(storeWithRingerMode.isPreferenceEnabled()).isTrue()
        assertThat(settingsStore.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
        assertThat(storeWithRingerMode.getBoolean(KEY)).isTrue()
        assertThat(storeWithRingerMode.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)

        setRingerMode(AudioManager.RINGER_MODE_SILENT)

        assertThat(storeWithRingerMode.isPreferenceEnabled()).isFalse()
        assertThat(settingsStore.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
        assertThat(storeWithRingerMode.getBoolean(KEY)).isFalse()
        assertThat(storeWithRingerMode.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
    }

    @Test
    fun isDisabledByRingerMode_returnsWhenRingerModeSilentAndMainSwitchOn() {
        settingsStore.setBoolean(Settings.System.VIBRATE_ON, false)
        setRingerMode(AudioManager.RINGER_MODE_SILENT)
        assertThat(storeWithRingerMode.isDisabledByRingerMode()).isFalse()

        settingsStore.setBoolean(Settings.System.VIBRATE_ON, false)
        setRingerMode(AudioManager.RINGER_MODE_VIBRATE)
        assertThat(storeWithRingerMode.isDisabledByRingerMode()).isFalse()

        settingsStore.setBoolean(Settings.System.VIBRATE_ON, true)
        setRingerMode(AudioManager.RINGER_MODE_VIBRATE)
        assertThat(storeWithRingerMode.isDisabledByRingerMode()).isFalse()

        settingsStore.setBoolean(Settings.System.VIBRATE_ON, true)
        setRingerMode(AudioManager.RINGER_MODE_SILENT)
        assertThat(storeWithRingerMode.isDisabledByRingerMode()).isTrue()
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
        setIntValue(Vibrator.VIBRATION_INTENSITY_HIGH)

        assertThat(settingsStore.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
        assertThat(storeWithRingerMode.getBoolean(KEY)).isFalse()
        assertThat(storeWithRingerMode.getInt(KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
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
    fun getValue_hapticFeedbackEnabled_returnsIntensityValue() {
        store.setInt(Settings.System.HAPTIC_FEEDBACK_INTENSITY, Vibrator.VIBRATION_INTENSITY_LOW)
        setHapticFeedbackEnabled(true)

        assertThat(store.getBoolean(Settings.System.HAPTIC_FEEDBACK_INTENSITY)).isTrue()
        assertThat(store.getInt(Settings.System.HAPTIC_FEEDBACK_INTENSITY))
            .isEqualTo(Vibrator.VIBRATION_INTENSITY_LOW)

        store.setBoolean(Settings.System.HAPTIC_FEEDBACK_INTENSITY, false)
        setHapticFeedbackEnabled(true)

        assertThat(store.getBoolean(Settings.System.HAPTIC_FEEDBACK_INTENSITY)).isFalse()
        assertThat(store.getInt(Settings.System.HAPTIC_FEEDBACK_INTENSITY))
            .isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
    }

    @Test
    fun getValue_hapticFeedbackDisabledIntensityOn_returnsIntensityOff() {
        store.setInt(Settings.System.HAPTIC_FEEDBACK_INTENSITY, Vibrator.VIBRATION_INTENSITY_HIGH)
        setHapticFeedbackEnabled(false)

        assertThat(store.getBoolean(Settings.System.HAPTIC_FEEDBACK_INTENSITY)).isFalse()
        assertThat(store.getInt(Settings.System.HAPTIC_FEEDBACK_INTENSITY))
            .isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
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
            vibrationUsage = TOUCH_USAGE,
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
            vibrationUsage = TOUCH_USAGE,
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

    @Test
    fun setValue_ringIntensity_updatesVibrateWhenRinging() {
        setVibrateWhenRinging(null)

        store.setInt(Settings.System.RING_VIBRATION_INTENSITY, null)
        assertThat(getStoredVibrateWhenRinging()).isNull()

        store.setBoolean(Settings.System.RING_VIBRATION_INTENSITY, false)
        assertThat(getStoredVibrateWhenRinging()).isFalse()

        store.setInt(Settings.System.RING_VIBRATION_INTENSITY, Vibrator.VIBRATION_INTENSITY_LOW)
        assertThat(getStoredVibrateWhenRinging()).isTrue()
    }

    @Test
    fun setValue_hapticFeedbackIntensity_updateHapticFeedbackEnabledAndHardwareFeedbackIntensity() {
        setHapticFeedbackEnabled(null)
        setHardwareFeedbackIntensity(null)

        store.setInt(Settings.System.HAPTIC_FEEDBACK_INTENSITY, null)
        assertThat(getStoredHapticFeedbackEnabled()).isNull()
        assertThat(getStoredHardwareFeedbackIntensity()).isNull()

        store.setBoolean(Settings.System.HAPTIC_FEEDBACK_INTENSITY, false)
        assertThat(getStoredHapticFeedbackEnabled()).isFalse()
        assertThat(getStoredHardwareFeedbackIntensity()).isEqualTo(DEFAULT_INTENSITY)

        store.setInt(Settings.System.HAPTIC_FEEDBACK_INTENSITY, Vibrator.VIBRATION_INTENSITY_LOW)
        assertThat(getStoredHapticFeedbackEnabled()).isTrue()
        assertThat(getStoredHardwareFeedbackIntensity()).isEqualTo(Vibrator.VIBRATION_INTENSITY_LOW)
    }

    @Suppress("DEPRECATION")
    private fun getStoredVibrateWhenRinging() =
        SettingsSystemStore.get(context).getBoolean(Settings.System.VIBRATE_WHEN_RINGING)

    @Suppress("DEPRECATION")
    private fun setVibrateWhenRinging(value: Boolean?) =
        SettingsSystemStore.get(context).setBoolean(Settings.System.VIBRATE_WHEN_RINGING, value)

    @Suppress("DEPRECATION")
    private fun getStoredHapticFeedbackEnabled() =
        SettingsSystemStore.get(context).getInt(Settings.System.HAPTIC_FEEDBACK_ENABLED)?.let {
            it == State.ON
        }

    @Suppress("DEPRECATION")
    private fun setHapticFeedbackEnabled(value: Boolean?) =
        SettingsSystemStore.get(context).setInt(
            Settings.System.HAPTIC_FEEDBACK_ENABLED,
            value?.let { if (it) State.ON else State.OFF },
        )

    private fun getStoredHardwareFeedbackIntensity() =
        SettingsSystemStore.get(context).getInt(Settings.System.HARDWARE_HAPTIC_FEEDBACK_INTENSITY)

    private fun setHardwareFeedbackIntensity(value: Int?) =
        SettingsSystemStore.get(context).setInt(
            Settings.System.HARDWARE_HAPTIC_FEEDBACK_INTENSITY,
            value,
        )

    private fun setIntValue(value: Int?) =
        store.setInt(KEY, value)

    private fun setBooleanValue(value: Boolean?) =
        store.setBoolean(KEY, value)

    private fun setRingerMode(ringerMode: Int) {
        val audioManager = context.getSystemService<AudioManager>()
        audioManager?.ringerModeInternal = ringerMode
        assertThat(audioManager?.ringerModeInternal).isEqualTo(ringerMode)
    }
}
