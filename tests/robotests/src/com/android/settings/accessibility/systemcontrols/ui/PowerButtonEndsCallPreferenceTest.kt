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
package com.android.settings.accessibility.systemcontrols.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.provider.Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR
import android.provider.Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP
import android.provider.Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF
import android.telephony.TelephonyManager
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.testutils.shadow.ShadowKeyCharacterMap
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.annotation.Config

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowKeyCharacterMap::class])
class PowerButtonEndsCallPreferenceTest {
    private val telephonyManager = mock<TelephonyManager>()

    private val mockResources = mock<Resources>()

    private val context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getSystemService(name: String): Any? =
                when (name) {
                    Context.TELEPHONY_SERVICE -> telephonyManager
                    else -> super.getSystemService(name)
                }

            override fun getResources(): Resources = mockResources
        }

    private val powerButtonEndsCallPreference = PowerButtonEndsCallPreference()

    @Test
    fun isAvailable_hasPowerKeyAndVoiceCapable_shouldReturnTrue() {
        ShadowKeyCharacterMap.setDevicehasKey(true)
        mockResources.stub {
            on { getBoolean(com.android.settings.R.bool.config_show_sim_info) } doReturn true
        }
        telephonyManager.stub { on { isDeviceVoiceCapable } doReturn true }

        assertThat(powerButtonEndsCallPreference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_noPowerKey_shouldReturnFalse() {
        ShadowKeyCharacterMap.setDevicehasKey(false)
        mockResources.stub {
            on { getBoolean(com.android.settings.R.bool.config_show_sim_info) } doReturn true
        }
        telephonyManager.stub { on { isDeviceVoiceCapable } doReturn true }

        assertThat(powerButtonEndsCallPreference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_noVoiceCapable_shouldReturnFalse() {
        ShadowKeyCharacterMap.setDevicehasKey(true)
        mockResources.stub {
            on { getBoolean(com.android.settings.R.bool.config_show_sim_info) } doReturn false
        }
        telephonyManager.stub { on { isDeviceVoiceCapable } doReturn true }

        assertThat(powerButtonEndsCallPreference.isAvailable(context)).isFalse()
    }

    @Test
    fun performClick_shouldPreferenceChangetoUnchecked() {
        enablePowerButtonEndsCall(true)

        val preference = getSwitchPreference().apply { performClick() }

        assertThat(preference.isChecked).isFalse()
    }

    @Test
    fun powerButtonEndsCallEnabled_shouldCheckedPreference() {
        enablePowerButtonEndsCall(true)

        assertThat(getSwitchPreference().isChecked).isTrue()
    }

    @Test
    fun powerButtonEndsCallDisabled_shouldUncheckedPreference() {
        enablePowerButtonEndsCall(false)

        assertThat(getSwitchPreference().isChecked).isFalse()
    }

    private fun getSwitchPreference(): SwitchPreferenceCompat =
        powerButtonEndsCallPreference.createAndBindWidget(context)

    private fun enablePowerButtonEndsCall(enabled: Boolean) =
        SettingsSecureStore.get(context)
            .setInt(
                INCALL_POWER_BUTTON_BEHAVIOR,
                when (enabled) {
                    true -> INCALL_POWER_BUTTON_BEHAVIOR_HANGUP
                    else -> INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF
                },
            )
}
// LINT.ThenChange(../../PowerButtonEndsCallPreferenceControllerTest.java)
