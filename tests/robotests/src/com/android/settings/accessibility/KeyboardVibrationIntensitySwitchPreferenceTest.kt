/*
 * Copyright (C) 2026 The Android Open Source Project
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
import android.os.vibrator.Flags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

// LINT.IfChange
class KeyboardVibrationIntensitySwitchPreferenceTest :
    VibrationIntensitySwitchPreferenceTestCase() {
    @get:Rule val setFlagsRule = SetFlagsRule()

    override val hasRingerModeDependency = false
    override val preference =
        KeyboardVibrationIntensitySwitchPreference(context, "key", 0, "main_key")

    private val resourcesSpy: Resources =
        spy(ApplicationProvider.getApplicationContext<Context>().resources)

    private val contextWrapper: Context =
        object : ContextWrapper(context) {
            override fun getResources(): Resources = resourcesSpy
        }

    @Test
    fun isAvailable_keyboardVibrationNotSupported_unavailable() {
        resourcesSpy.stub {
            on {
                getBoolean(com.android.internal.R.bool.config_keyboardVibrationSettingsSupported)
            } doReturn false
            on {
                getBoolean(
                    com.android.internal.R.bool.config_keyboardVibrationSettingsIntensitySupported
                )
            } doReturn true
        }
        setFlagsRule.enableFlags(Flags.FLAG_KEYBOARD_INTENSITY_SLIDER_ENABLED)
        assertThat(preference.isAvailable(contextWrapper)).isFalse()
    }

    @Test
    fun isAvailable_keyboardVibrationIntensityNotSupported_unavailable() {
        resourcesSpy.stub {
            on {
                getBoolean(com.android.internal.R.bool.config_keyboardVibrationSettingsSupported)
            } doReturn true
            on {
                getBoolean(
                    com.android.internal.R.bool.config_keyboardVibrationSettingsIntensitySupported
                )
            } doReturn false
        }
        setFlagsRule.enableFlags(Flags.FLAG_KEYBOARD_INTENSITY_SLIDER_ENABLED)
        assertThat(preference.isAvailable(contextWrapper)).isFalse()
    }

    @Test
    fun isAvailable_flagDisabled_unavailable() {
        resourcesSpy.stub {
            on {
                getBoolean(com.android.internal.R.bool.config_keyboardVibrationSettingsSupported)
            } doReturn true
            on {
                getBoolean(
                    com.android.internal.R.bool.config_keyboardVibrationSettingsIntensitySupported
                )
            } doReturn true
        }
        setFlagsRule.disableFlags(Flags.FLAG_KEYBOARD_INTENSITY_SLIDER_ENABLED)
        assertThat(preference.isAvailable(contextWrapper)).isFalse()
    }

    @Test
    fun isAvailable_allSupported_available() {
        resourcesSpy.stub {
            on {
                getBoolean(com.android.internal.R.bool.config_keyboardVibrationSettingsSupported)
            } doReturn true
            on {
                getBoolean(
                    com.android.internal.R.bool.config_keyboardVibrationSettingsIntensitySupported
                )
            } doReturn true
        }
        setFlagsRule.enableFlags(Flags.FLAG_KEYBOARD_INTENSITY_SLIDER_ENABLED)
        assertThat(preference.isAvailable(contextWrapper)).isTrue()
    }
}
// LINT.ThenChange(KeyboardVibrationIntensitySwitchPreferenceControllerTest.java)
