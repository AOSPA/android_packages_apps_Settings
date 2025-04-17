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
import android.os.VibrationAttributes
import android.os.Vibrator
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.datastore.SettingsSystemStore
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VibrationIntensitySettingsStoreTest {
    private companion object {
        const val KEY: String = Settings.System.HAPTIC_FEEDBACK_INTENSITY
        const val DEFAULT_INTENSITY: Int = Vibrator.VIBRATION_INTENSITY_MEDIUM
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val settingsStore = SettingsSystemStore.get(context)
    private val store = VibrationIntensitySettingsStore(
        context = context,
        vibrationUsage = VibrationAttributes.USAGE_RINGTONE,
        keyValueStoreDelegate = settingsStore,
        defaultIntensity = DEFAULT_INTENSITY,
    )

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
    fun getValue_valueIntensityInt_returnValueSet() {
        setIntValue(Vibrator.VIBRATION_INTENSITY_HIGH)

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

    private fun setIntValue(value: Int?) =
        store.setInt(KEY, value)

    private fun setBooleanValue(value: Boolean?) =
        store.setBoolean(KEY, value)
}
