/*
 * Copyright (C) 2024 The Android Open Source Project
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
import android.content.Intent
import android.content.res.Resources
import android.media.AudioManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.platform.test.annotations.EnableFlags
import android.provider.Settings
import androidx.core.content.getSystemService
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.testutils.shadow.ShadowAudioManager
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.preference.CatalystScreenTestCase
import com.android.settingslib.widget.MainSwitchPreference
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowAudioManager::class])
class VibrationScreenTest : CatalystScreenTestCase() {
    private lateinit var vibratorMock: Vibrator

    private val resourcesSpy: Resources =
        spy((ApplicationProvider.getApplicationContext() as Context).resources)

    private val context: Context =
        object : ContextWrapper(appContext) {
            override fun getSystemService(name: String): Any? =
                when {
                    name == VIBRATOR_SERVICE -> vibratorMock
                    else -> super.getSystemService(name)
                }

            override fun getResources(): Resources = resourcesSpy
        }

    override val preferenceScreenCreator = VibrationScreen()

    override val flagName: String
        get() = Flags.FLAG_CATALYST_VIBRATION_INTENSITY_SCREEN

    @Before
    fun setUp() {
        setRingerMode(AudioManager.RINGER_MODE_NORMAL)
    }

    @Test
    fun key() {
        assertThat(preferenceScreenCreator.key).isEqualTo(VibrationScreen.KEY)
    }

    @Test
    fun isAvailable_noVibrator_unavailable() {
        vibratorMock = mock { on { hasVibrator() } doReturn false }
        resourcesSpy.stub {
            on { getInteger(R.integer.config_vibration_supported_intensity_levels) } doReturn 1
        }
        assertThat(preferenceScreenCreator.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_hasVibratorAndMultipleIntensityLevels_unavailable() {
        vibratorMock = mock { on { hasVibrator() } doReturn true }
        resourcesSpy.stub {
            on { getInteger(R.integer.config_vibration_supported_intensity_levels) } doReturn 3
        }
        assertThat(preferenceScreenCreator.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_hasVibratorAndSingleIntensityLevel_available() {
        vibratorMock = mock { on { hasVibrator() } doReturn true }
        resourcesSpy.stub {
            on { getInteger(R.integer.config_vibration_supported_intensity_levels) } doReturn 1
        }
        assertThat(preferenceScreenCreator.isAvailable(context)).isTrue()
    }

    @EnableFlags(Flags.FLAG_CATALYST_VIBRATION_INTENSITY_SCREEN_25Q4)
    @Test
    fun mainSwitchClick_withIntensitiesSet_disablesAndUnchecksAllIntensitiesAndPreservesStorage() {
        setRingerMode(AudioManager.RINGER_MODE_NORMAL)
        val intensityKeys = findVibrationIntensitySwitchPreferences()
        assertThat(intensityKeys).isNotEmpty()

        // Setup initial vibration intensities.
        val originalIntensity = Vibrator.VIBRATION_INTENSITY_HIGH
        intensityKeys.forEach { key -> setStoredIntensity(key, originalIntensity) }

        testOnFragment { fragment ->
            val allSwitches = intensityKeys.stream()
                .map { key -> fragment.findPreference<SwitchPreferenceCompat>(key)!! }
                .toList()
            val mainSwitch: MainSwitchPreference =
                fragment.findPreference(VibrationMainSwitchPreference.KEY)!!

            // Check all intensity switches are enabled and checked.
            assertThat(mainSwitch.isChecked).isTrue()
            allSwitches.forEach { switch -> assertSwitchCheckedAndEnabled(switch) }

            // Turn main switch off.
            mainSwitch.performClick()
            ShadowLooper.idleMainLooper();

            // Check all intensities are disabled and unchecked, and stored value is preserved.
            assertThat(mainSwitch.isChecked).isFalse()
            allSwitches.forEach { switch ->
                assertSwitchUncheckedAndDisabled(switch, originalIntensity)
            }

            // Turn main switch back on.
            mainSwitch.performClick()
            ShadowLooper.idleMainLooper();

            // Check all intensity switches restored.
            assertThat(mainSwitch.isChecked).isTrue()
            allSwitches.forEach { switch -> assertSwitchCheckedAndEnabled(switch) }
        }
    }

    @EnableFlags(Flags.FLAG_CATALYST_VIBRATION_INTENSITY_SCREEN_25Q4)
    @Test
    fun ringerModeChange_disablesOnlyRingAndNotificationOnSilentMode() {
        setRingerMode(AudioManager.RINGER_MODE_NORMAL)
        val intensityKeys = findVibrationIntensitySwitchPreferences()
        assertThat(intensityKeys).isNotEmpty()

        // Setup initial vibration intensities.
        val originalIntensity = Vibrator.VIBRATION_INTENSITY_MEDIUM
        intensityKeys.forEach { key -> setStoredIntensity(key, originalIntensity) }

        testOnFragment { fragment ->
            val allSwitches = intensityKeys.stream()
                .map { key -> fragment.findPreference<SwitchPreferenceCompat>(key)!! }
                .toList()
            val otherSwitches = allSwitches.stream()
                .filter { switch ->
                    switch.key != RingVibrationIntensitySwitchPreference.KEY
                            && switch.key != NotificationVibrationIntensitySwitchPreference.KEY
                }
                .toList()
            val ringSwitch: SwitchPreferenceCompat =
                fragment.findPreference(RingVibrationIntensitySwitchPreference.KEY)!!
            val notificationSwitch: SwitchPreferenceCompat =
                fragment.findPreference(NotificationVibrationIntensitySwitchPreference.KEY)!!

            // Check all intensity switches are enabled and checked.
            allSwitches.forEach { switch -> assertSwitchCheckedAndEnabled(switch) }

            // Turn ringer mode silent.
            setRingerMode(AudioManager.RINGER_MODE_SILENT)
            context.sendBroadcast(Intent(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION))
            ShadowLooper.idleMainLooper();

            // Check only ring and notification are disabled and unchecked.
            assertSwitchUncheckedAndDisabled(ringSwitch, originalIntensity)
            assertSwitchUncheckedAndDisabled(notificationSwitch, originalIntensity)
            otherSwitches.forEach { switch -> assertSwitchCheckedAndEnabled(switch) }

            // Turn ringer mode vibrate-only.
            setRingerMode(AudioManager.RINGER_MODE_VIBRATE)
            context.sendBroadcast(Intent(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION))
            ShadowLooper.idleMainLooper();

            // Check all intensity switches restored.
            allSwitches.forEach { switch -> assertSwitchCheckedAndEnabled(switch) }
        }
    }

    @EnableFlags(Flags.FLAG_CATALYST_VIBRATION_INTENSITY_SCREEN_25Q4)
    @Test
    fun mainVibrationChange_disablesKeyboardSwitchAndPreservesStorage() {
        setStoredBoolean(VibrationMainSwitchPreference.KEY, true)
        setStoredBoolean(KeyboardVibrationSwitchPreference.KEY, true)

        testOnFragment { fragment ->
            val mainSwitch: MainSwitchPreference =
                fragment.findPreference(VibrationMainSwitchPreference.KEY)!!
            val keyboardSwitch: SwitchPreferenceCompat =
                fragment.findPreference(KeyboardVibrationSwitchPreference.KEY)!!

            // Check keyboard switch enabled and checked.
            assertThat(mainSwitch.isChecked).isTrue()
            assertSwitchCheckedAndEnabled(keyboardSwitch)

            // Turn main vibration switch off.
            mainSwitch.performClick()
            ShadowLooper.idleMainLooper();

            // Check keyboard switch disabled and unchecked.
            assertThat(mainSwitch.isChecked).isFalse()
            assertSwitchUncheckedAndDisabled(keyboardSwitch, expectedStoredValue = true)

            // Turn main vibration switch back on.
            mainSwitch.performClick()
            ShadowLooper.idleMainLooper();

            // Check keyboard switch restored.
            assertThat(mainSwitch.isChecked).isTrue()
            assertSwitchCheckedAndEnabled(keyboardSwitch)
        }
    }

    private fun assertSwitchUncheckedAndDisabled(
        switch: SwitchPreferenceCompat,
        expectedIntensity: Int,
    ) {
        assertWithSwitch(switch).that(switch.isEnabled).isFalse()
        assertWithSwitch(switch).that(switch.isChecked).isFalse()
        assertWithSwitch(switch).that(getStoredIntensity(switch.key)).isEqualTo(expectedIntensity)
    }

    private fun assertSwitchUncheckedAndDisabled(
        switch: SwitchPreferenceCompat,
        expectedStoredValue: Boolean,
    ) {
        assertWithSwitch(switch).that(switch.isEnabled).isFalse()
        assertWithSwitch(switch).that(switch.isChecked).isFalse()
        assertWithSwitch(switch).that(getStoredBoolean(switch.key)).isEqualTo(expectedStoredValue)
    }

    private fun assertSwitchCheckedAndEnabled(switch: SwitchPreferenceCompat) {
        assertWithSwitch(switch).that(switch.isEnabled).isTrue()
        assertWithSwitch(switch).that(switch.isChecked).isTrue()
    }

    private fun assertWithSwitch(switch: SwitchPreferenceCompat) =
        assertWithMessage("On switch with key %s", switch.key)

    private fun setStoredBoolean(settingsKey: String, value: Boolean?) =
        SettingsSystemStore.get(context).setBoolean(settingsKey, value)

    private fun getStoredBoolean(settingsKey: String) =
        SettingsSystemStore.get(context).getBoolean(settingsKey)

    private fun setStoredIntensity(settingsKey: String, value: Int?) =
        SettingsSystemStore.get(context).setInt(settingsKey, value)

    private fun getStoredIntensity(settingsKey: String) =
        SettingsSystemStore.get(context).getInt(settingsKey)

    private fun testOnFragment(action: (PreferenceFragmentCompat) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        val clazz = preferenceScreenCreator.fragmentClass() as Class<PreferenceFragmentCompat>
        launchFragment(clazz) { fragment -> action.invoke(fragment) }
    }

    private fun findVibrationIntensitySwitchPreferences(): List<String> {
        val switches = ArrayList<String>()
        preferenceScreenCreator.getPreferenceHierarchy(context).forEachRecursively { child ->
            if (child.metadata is VibrationIntensitySwitchPreference) {
                switches.add(child.metadata.key)
            }
        }
        return switches
    }

    private fun setRingerMode(ringerMode: Int) {
        val audioManager = context.getSystemService<AudioManager>()
        audioManager?.ringerModeInternal = ringerMode
        assertThat(audioManager?.ringerModeInternal).isEqualTo(ringerMode)
    }
}
// LINT.ThenChange(VibrationPreferenceControllerTest.java)
