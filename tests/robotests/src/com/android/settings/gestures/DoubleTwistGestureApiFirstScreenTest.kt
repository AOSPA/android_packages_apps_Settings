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

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.HardwareUnsupportedException
import com.android.settings.testutils2.MissingPermissionException
import com.android.settingslib.datastore.SettingsSecureStore
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSensor
import org.robolectric.shadows.ShadowSensorManager
import org.robolectric.util.ReflectionHelpers

@Config(shadows = [SettingsShadowResources::class])
@RunWith(AndroidJUnit4::class)
class DoubleTwistGestureApiFirstScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val tester = ApiTester(DoubleTwistGestureApiFirstScreen())
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val shadowApplication = shadowOf(context as Application)

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val shadowSensorManager: ShadowSensorManager = shadowOf(sensorManager)

    @Before
    fun setUp() {
        shadowApplication.grantPermissions(WRITE_SECURE_SETTINGS)
        SettingsShadowResources.overrideResource(R.string.gesture_double_twist_sensor_type, "test")
        SettingsShadowResources.overrideResource(
            R.string.gesture_double_twist_sensor_vendor,
            "test",
        )
        ensureHasDoubleTwistSensor()
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
    fun getMainSwitchPreference_defaultOn_returnTrue() {
        SettingsSecureStore.get(context)
            .setInt(
                Settings.Secure.CAMERA_DOUBLE_TWIST_TO_FLIP_ENABLED,
                DoubleTwistGestureApiFirstScreen.ON,
            )

        assertThat(tester.get<Boolean>(DoubleTwistGestureApiFirstScreen.MAIN_SWITCH_KEY)).isTrue()
    }

    @Test
    fun getMainSwitchPreference_defaultOff_returnFalse() {
        SettingsSecureStore.get(context)
            .setInt(
                Settings.Secure.CAMERA_DOUBLE_TWIST_TO_FLIP_ENABLED,
                DoubleTwistGestureApiFirstScreen.OFF,
            )

        assertThat(tester.get<Boolean>(DoubleTwistGestureApiFirstScreen.MAIN_SWITCH_KEY)).isFalse()
    }

    @Test
    fun setMainSwitchPreference_noDoubleTwistSensor_throwsException() {
        ensureDoesNotHaveDoubleTwistSensor()

        assertFailsWith<HardwareUnsupportedException> {
            tester.set(DoubleTwistGestureApiFirstScreen.MAIN_SWITCH_KEY, true)
        }
    }

    @Test
    fun setMainSwitchPreference_asTrue_returnOn() {
        tester.set(DoubleTwistGestureApiFirstScreen.MAIN_SWITCH_KEY, true)

        assertThat(
                SettingsSecureStore.get(context)
                    .getInt(Settings.Secure.CAMERA_DOUBLE_TWIST_TO_FLIP_ENABLED)
            )
            .isEqualTo(DoubleTwistGestureApiFirstScreen.ON)
    }

    @Test
    fun setMainSwitchPreference_asFalse_returnOff() {
        tester.set(DoubleTwistGestureApiFirstScreen.MAIN_SWITCH_KEY, false)

        assertThat(
                SettingsSecureStore.get(context)
                    .getInt(Settings.Secure.CAMERA_DOUBLE_TWIST_TO_FLIP_ENABLED)
            )
            .isEqualTo(DoubleTwistGestureApiFirstScreen.OFF)
    }

    @Test
    fun setMainSwitchPreference_noPermission_throwsException() {
        shadowApplication.denyPermissions(WRITE_SECURE_SETTINGS)

        assertFailsWith<MissingPermissionException> {
            tester.set(DoubleTwistGestureApiFirstScreen.MAIN_SWITCH_KEY, true)
        }
    }

    private fun ensureHasDoubleTwistSensor() {
        val customSensor: Sensor = ShadowSensor.newInstance(Sensor.TYPE_ALL)
        ReflectionHelpers.setField(customSensor, "mStringType", "test")
        ReflectionHelpers.setField(customSensor, "mVendor", "test")
        shadowSensorManager.addSensor(customSensor)
    }

    private fun ensureDoesNotHaveDoubleTwistSensor() {
        ShadowSensorManager.reset()
    }
}
