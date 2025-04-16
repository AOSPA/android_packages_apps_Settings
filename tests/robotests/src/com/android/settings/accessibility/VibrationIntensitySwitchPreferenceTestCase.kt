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
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

/** Test case for vibration switch preferences. */
// LINT.IfChange
abstract class VibrationIntensitySwitchPreferenceTestCase {
    protected abstract val preference: VibrationIntensitySwitchPreference

    protected val vibratorSpy: Vibrator =
        spy(ApplicationProvider.getApplicationContext<Context>().getSystemService<Vibrator>()!!)

    protected val context: Context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getSystemService(name: String): Any? =
                when {
                    name == getSystemServiceName(Vibrator::class.java) -> vibratorSpy
                    else -> super.getSystemService(name)
                }
        }

    @Test
    fun state_valueUnset_enabledAndChecked() {
        setValue(null)
        val widget = createWidget()

        assertThat(widget.isEnabled).isTrue()
        assertThat(widget.isChecked).isTrue()
    }

    @Test
    fun state_valueTrue_enabledAndChecked() {
        setValue(true)
        val widget = createWidget()

        assertThat(widget.isEnabled).isTrue()
        assertThat(widget.isChecked).isTrue()
    }

    @Test
    fun state_valueFalse_enabledAndUnchecked() {
        setValue(false)
        val widget = createWidget()

        assertThat(widget.isEnabled).isTrue()
        assertThat(widget.isChecked).isFalse()
    }

    @Test
    fun click_withDifferentStates_updatesStateCorrectly() {
        setValue(null)
        val widget = createWidget()

        assertThat(widget.isChecked).isTrue()

        widget.performClick()

        assertThat(widget.isChecked).isFalse()

        widget.performClick()

        assertThat(widget.isChecked).isTrue()
    }

    @Test
    fun click_withDefaultIntensity_storesIntensityValues() {
        val defaultIntensity = Vibrator.VIBRATION_INTENSITY_HIGH
        vibratorSpy.stub {
            on { getDefaultVibrationIntensity(preference.vibrationUsage) } doReturn defaultIntensity
        }
        setValue(null)
        val widget = createWidget()

        assertThat(getRawStoredValue()).isNull()
        assertThat(widget.isChecked).isTrue()

        widget.performClick()

        assertThat(getRawStoredValue()).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
        assertThat(widget.isChecked).isFalse()

        widget.performClick()

        assertThat(getRawStoredValue()).isEqualTo(defaultIntensity)
        assertThat(widget.isChecked).isTrue()
    }

    @Test
    fun click_withVibrator_playsHapticPreviewWhenChecked() {
        setValue(null)
        val widget = createWidget()

        assertThat(widget.isChecked).isTrue()

        widget.performClick()

        assertThat(widget.isChecked).isFalse()
        verify(vibratorSpy, never()).vibrate(any<VibrationEffect>(), any<VibrationAttributes>())

        widget.performClick()

        assertThat(widget.isChecked).isTrue()
        verify(vibratorSpy).vibrate(any<VibrationEffect>(), any<VibrationAttributes>())
    }

    private fun getRawStoredValue() =
        SettingsSystemStore.get(context).getInt(preference.key)

    private fun setValue(value: Boolean?) =
        preference.storage(context).setBoolean(preference.key, value)

    private fun createWidget(): SwitchPreferenceCompat =
        preference.createAndBindWidget(context)
}
// LINT.ThenChange(VibrationTogglePreferenceControllerTest.java)
