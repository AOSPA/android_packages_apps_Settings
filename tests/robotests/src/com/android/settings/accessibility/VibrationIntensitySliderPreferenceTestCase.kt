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
import android.content.ContextWrapper
import android.content.res.Resources
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.SliderPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

/** Test case for vibration slider preferences. */
// LINT.IfChange
abstract class VibrationIntensitySliderPreferenceTestCase {
    protected abstract val preference: VibrationIntensitySliderPreference

    protected val resourcesSpy: Resources =
        spy(ApplicationProvider.getApplicationContext<Context>().resources)

    protected val vibratorSpy: Vibrator =
        spy(ApplicationProvider.getApplicationContext<Context>().getSystemService<Vibrator>()!!)

    protected val context: Context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getResources(): Resources = resourcesSpy
            override fun getSystemService(name: String): Any? =
                when {
                    name == getSystemServiceName(Vibrator::class.java) -> vibratorSpy
                    else -> super.getSystemService(name)
                }
        }

    @Test
    fun minMaxIncrement_usesConfigIntensityLevels() {
        setSupportedLevels(2)
        val widget = createWidget()

        assertThat(widget.min).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
        assertThat(widget.max).isEqualTo(2)
        assertThat(widget.sliderIncrement).isEqualTo(1)
    }

    @Test
    fun maxValue_badConfigTooHigh_returnsVibratorMaxIntensity() {
        setSupportedLevels(5)
        val widget = createWidget()

        assertThat(widget.max).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
    }

    @Test
    fun state_valueUnsetAndDefaultSupported_enabledAndDefaultIntensity() {
        setSupportedLevels(3)
        setDefaultIntensity(Vibrator.VIBRATION_INTENSITY_LOW)
        setValue(null)
        val widget = createWidget()

        assertThat(widget.isEnabled).isTrue()
        assertThat(widget.value).isEqualTo(Vibrator.VIBRATION_INTENSITY_LOW)
    }

    @Test
    fun state_valueUnsetDefaultTooHigh_enabledAndMaxIntensity() {
        setSupportedLevels(1)
        setDefaultIntensity(Vibrator.VIBRATION_INTENSITY_HIGH)
        setValue(null)
        val widget = createWidget()

        assertThat(widget.isEnabled).isTrue()
        assertThat(widget.value).isEqualTo(1)
    }

    @Test
    fun state_valueSetToSupported_enabledAndStoredValue() {
        setSupportedLevels(3)
        setDefaultIntensity(Vibrator.VIBRATION_INTENSITY_MEDIUM)
        setValue(Vibrator.VIBRATION_INTENSITY_LOW)
        val widget = createWidget()

        assertThat(widget.isEnabled).isTrue()
        assertThat(widget.value).isEqualTo(Vibrator.VIBRATION_INTENSITY_LOW)
    }

    @Test
    fun state_valueOff_enabledAndValueOff() {
        setSupportedLevels(3)
        setDefaultIntensity(Vibrator.VIBRATION_INTENSITY_MEDIUM)
        setValue(Vibrator.VIBRATION_INTENSITY_OFF)
        val widget = createWidget()

        assertThat(widget.isEnabled).isTrue()
        assertThat(widget.value).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
    }

    @Test
    fun state_valueTooHigh_enabledAndMaxIntensity() {
        setSupportedLevels(2)
        setDefaultIntensity(Vibrator.VIBRATION_INTENSITY_LOW)
        setValue(Vibrator.VIBRATION_INTENSITY_HIGH)
        val widget = createWidget()

        assertThat(widget.isEnabled).isTrue()
        assertThat(widget.value).isEqualTo(2)
    }

    @Test
    fun setValue_supportedValues_storesIntensityValues() {
        setSupportedLevels(3)
        setDefaultIntensity(Vibrator.VIBRATION_INTENSITY_MEDIUM)
        setValue(null)
        val widget = createWidget()

        assertThat(getRawStoredValue()).isNull()
        assertThat(widget.value).isEqualTo(Vibrator.VIBRATION_INTENSITY_MEDIUM)

        widget.value = Vibrator.VIBRATION_INTENSITY_OFF

        assertThat(getRawStoredValue()).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)

        widget.value = Vibrator.VIBRATION_INTENSITY_HIGH

        assertThat(getRawStoredValue()).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
    }

    @Test
    fun setValue_oneLevelSupported_storesDefaultIntensity() {
        setSupportedLevels(1)
        setDefaultIntensity(Vibrator.VIBRATION_INTENSITY_MEDIUM)
        setValue(null)
        val widget = createWidget()

        assertThat(getRawStoredValue()).isNull()
        assertThat(widget.value).isEqualTo(1)

        widget.value = Vibrator.VIBRATION_INTENSITY_OFF

        assertThat(getRawStoredValue()).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)

        widget.value = 1

        assertThat(getRawStoredValue()).isEqualTo(Vibrator.VIBRATION_INTENSITY_MEDIUM)
    }

    @Test
    fun setValue_twoLevelsSupported_storesMinAndMaxIntensities() {
        setSupportedLevels(2)
        setDefaultIntensity(Vibrator.VIBRATION_INTENSITY_LOW)
        setValue(null)
        val widget = createWidget()

        assertThat(getRawStoredValue()).isNull()
        assertThat(widget.value).isEqualTo(Vibrator.VIBRATION_INTENSITY_LOW)

        widget.value = Vibrator.VIBRATION_INTENSITY_OFF

        assertThat(getRawStoredValue()).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)

        widget.value = 1

        assertThat(getRawStoredValue()).isEqualTo(Vibrator.VIBRATION_INTENSITY_LOW)

        widget.value = 2

        assertThat(getRawStoredValue()).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
    }

    protected fun setSupportedLevels(levels: Int) {
        resourcesSpy.stub {
            on { getInteger(R.integer.config_vibration_supported_intensity_levels) } doReturn levels
        }
    }

    protected fun setDefaultIntensity(intensity: Int) {
        vibratorSpy.stub {
            on { getDefaultVibrationIntensity(preference.vibrationUsage) } doReturn intensity
        }
    }

    private fun getRawStoredValue() =
        SettingsSystemStore.get(context).getInt(preference.key)

    private fun setValue(value: Int?) =
        preference.storage(context).setInt(preference.key, value)

    private fun createWidget(): SliderPreference =
        preference.createAndBindWidget(context)
}
// LINT.ThenChange(VibrationTogglePreferenceControllerTest.java)
