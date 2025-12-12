/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.input.gamecontroller

import android.content.Context
import android.hardware.input.InputManager
import android.os.Looper
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.view.InputDevice
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.system.SystemDashboardFragment
import com.android.settings.testutils.shadow.ShadowInputManager
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.androidx.fragment.FragmentController

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowInputManager::class])
class GameControllerSettingsTest {

    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @get:Rule val setFlagsRule = SetFlagsRule()

    private lateinit var shadowInputManager: ShadowInputManager

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowInputManager =
            shadowOf(context.getSystemService(InputManager::class.java)) as ShadowInputManager
    }

    @Test
    @EnableFlags(com.android.hardware.input.Flags.FLAG_CONTROLLER_REMAPPING)
    fun noDevicesExist_preferenceIsNotVisible() {
        val fragment =
            FragmentController.of(SystemDashboardFragment()).create().start().resume().get()
        val preference = fragment.findPreference<Preference>(PREFERENCE_KEY)!!

        assertThat(preference.isVisible).isFalse()
    }

    @Test
    @EnableFlags(com.android.hardware.input.Flags.FLAG_CONTROLLER_REMAPPING)
    fun gamepadDeviceExists_preferenceIsVisible() {
        shadowInputManager.addInputDevice(
            createInputDevice(1, "Gamepad", InputDevice.SOURCE_GAMEPAD)
        )
        val fragment =
            FragmentController.of(SystemDashboardFragment()).create().start().resume().get()
        val preference = fragment.findPreference<Preference>(PREFERENCE_KEY)!!

        assertThat(preference.isVisible).isTrue()
        assertThat(preference.title)
            .isEqualTo(
                context.getString(com.android.settings.R.string.game_controller_settings_title)
            )
        assertThat(preference.fragment).isEqualTo(GameControllerListFragment::class.qualifiedName)
        assertThat(preference.intent).isNull()
    }

    @Test
    @EnableFlags(com.android.hardware.input.Flags.FLAG_CONTROLLER_REMAPPING)
    fun joystickDeviceExists_preferenceIsVisible() {
        shadowInputManager.addInputDevice(
            createInputDevice(1, "Joystick", InputDevice.SOURCE_JOYSTICK)
        )
        val fragment =
            FragmentController.of(SystemDashboardFragment()).create().start().resume().get()
        val preference = fragment.findPreference<Preference>(PREFERENCE_KEY)!!

        assertThat(preference.isVisible).isTrue()
        assertThat(preference.title)
            .isEqualTo(
                context.getString(com.android.settings.R.string.game_controller_settings_title)
            )
        assertThat(preference.fragment).isEqualTo(GameControllerListFragment::class.qualifiedName)
        assertThat(preference.intent).isNull()
    }

    @Test
    @DisableFlags(com.android.hardware.input.Flags.FLAG_CONTROLLER_REMAPPING)
    fun joystickDeviceExists_flagDisabled_preferenceIsNotVisible() {
        shadowInputManager.addInputDevice(
            createInputDevice(1, "Joystick", InputDevice.SOURCE_JOYSTICK)
        )
        val fragment =
            FragmentController.of(SystemDashboardFragment()).create().start().resume().get()
        val preference = fragment.findPreference<Preference>(PREFERENCE_KEY)!!

        assertThat(preference.isVisible).isFalse()
    }

    @Test
    @EnableFlags(com.android.hardware.input.Flags.FLAG_CONTROLLER_REMAPPING)
    fun noGameDevices_preferenceIsNotVisible_addGamepad_preferenceBecomesVisible() {
        shadowInputManager.addInputDevice(createInputDevice(1, "Mouse", InputDevice.SOURCE_MOUSE))
        shadowInputManager.addInputDevice(
            createInputDevice(2, "Keyboard", InputDevice.SOURCE_KEYBOARD)
        )
        val fragment =
            FragmentController.of(SystemDashboardFragment()).create().start().resume().get()
        val preference = fragment.findPreference<Preference>(PREFERENCE_KEY)!!

        assertThat(preference.isVisible).isFalse()

        shadowInputManager.addInputDevice(
            createInputDevice(3, "Gamepad", InputDevice.SOURCE_GAMEPAD)
        )

        shadowOf(Looper.getMainLooper()).idle()
        assertThat(preference.isVisible).isTrue()
    }

    @Test
    @EnableFlags(com.android.hardware.input.Flags.FLAG_CONTROLLER_REMAPPING)
    fun gamepadDeviceRemoved_preferenceBecomesNotVisible() {
        shadowInputManager.addInputDevice(
            createInputDevice(1, "Gamepad", InputDevice.SOURCE_GAMEPAD)
        )
        val fragment =
            FragmentController.of(SystemDashboardFragment()).create().start().resume().get()
        val preference = fragment.findPreference<Preference>(PREFERENCE_KEY)!!
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(preference.isVisible).isTrue()

        shadowInputManager.removeInputDevice(1)

        shadowOf(Looper.getMainLooper()).idle()
        assertThat(preference.isVisible).isFalse()
    }

    @Test
    @EnableFlags(com.android.hardware.input.Flags.FLAG_CONTROLLER_REMAPPING)
    fun fragmentStops_addGamepadDevice_preferenceIsNotVisible() {
        val fragmentController =
            FragmentController.of(SystemDashboardFragment()).create().start().resume()
        val preference = fragmentController.get().findPreference<Preference>(PREFERENCE_KEY)!!
        assertThat(preference.isVisible).isFalse()
        fragmentController.pause().stop().destroy()
        shadowOf(Looper.getMainLooper()).idle()

        shadowInputManager.addInputDevice(
            createInputDevice(1, "Gamepad", InputDevice.SOURCE_GAMEPAD)
        )

        shadowOf(Looper.getMainLooper()).idle()
        assertThat(preference.isVisible).isFalse()
    }

    private fun createInputDevice(deviceId: Int, name: String, sources: Int): InputDevice {
        return InputDevice.Builder()
            .setId(deviceId)
            .setName(name)
            .setDescriptor("device $deviceId")
            .setSources(sources)
            .build()
    }

    companion object {
        val PREFERENCE_KEY = "game_controller_settings"
    }
}
