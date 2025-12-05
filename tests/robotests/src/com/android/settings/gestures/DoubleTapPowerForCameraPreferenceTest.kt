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

package com.android.settings.gestures

import android.content.Context
import android.provider.Settings.Secure.DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.internal.R as IR
import com.android.settings.R
import com.android.settings.gestures.DoubleTapPowerSettingsUtils.DOUBLE_TAP_POWER_LAUNCH_CAMERA_MODE
import com.android.settings.gestures.DoubleTapPowerSettingsUtils.DOUBLE_TAP_POWER_MULTI_TARGET_MODE
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
class DoubleTapPowerForCameraPreferenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference = DoubleTapPowerForCameraPreference(DoubleTapPowerStorage(context))
    private val storage = DoubleTapPowerMainSwitchPreference.createDataStore(context)

    @Test
    fun keyAndTitle_areCorrect() {
        assertThat(preference.key).isEqualTo(DoubleTapPowerForCameraPreference.KEY)
        assertThat(preference.title).isEqualTo(R.string.double_tap_power_camera_action_title)
    }

    @Test
    @Config(shadows = [SettingsShadowResources::class])
    fun isAvailable_multiTargetAvailable_returnsTrue() {
        SettingsShadowResources.overrideResource(
            IR.integer.config_doubleTapPowerGestureMode,
            DOUBLE_TAP_POWER_MULTI_TARGET_MODE,
        )

        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    @Config(shadows = [SettingsShadowResources::class])
    fun isAvailable_multiTargetNotAvailable_returnsFalse() {
        SettingsShadowResources.overrideResource(
            IR.integer.config_doubleTapPowerGestureMode,
            DOUBLE_TAP_POWER_LAUNCH_CAMERA_MODE,
        )

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun isEnabled_gestureEnabled_returnsTrue() {
        storage.setBoolean(DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED, true)

        assertThat(preference.isEnabled(context)).isTrue()
    }

    @Test
    fun isEnabled_gestureDisabled_returnsFalse() {
        storage.setBoolean(DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED, false)

        assertThat(preference.isEnabled(context)).isFalse()
    }
}
// LINT.ThenChange(DoubleTapPowerForCameraPreferenceControllerTest.java)
