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

package com.android.settings.bluetooth

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.input.InputDeviceIdentifier
import android.hardware.input.InputManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import android.view.InputDevice
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import androidx.test.core.app.ApplicationProvider
import com.android.settingslib.bluetooth.CachedBluetoothDevice
import com.android.settingslib.core.lifecycle.Lifecycle
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@SuppressLint("MissingPermission")
@RunWith(RobolectricTestRunner::class)
class BluetoothDetailsGameControllerPreferenceControllerTest {

    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @get:Rule val setFlagsRule = SetFlagsRule()

    @Mock private lateinit var cachedDevice: CachedBluetoothDevice
    @Mock private lateinit var lifecycle: Lifecycle
    @Mock private lateinit var inputManager: InputManager
    @Mock private lateinit var preferenceScreen: PreferenceScreen
    @Mock private lateinit var preferenceGroup: PreferenceGroup

    private lateinit var context: Context

    @Before
    fun setUp() {
        val appContext: Context = ApplicationProvider.getApplicationContext()
        context = spy(appContext.applicationContext)
        whenever(context.getSystemService(InputManager::class.java)).doReturn(inputManager)
        whenever(cachedDevice.address).thenReturn(BT_ADDRESS)
        doNothing().whenever(context).startActivity(any())
    }

    private fun setupController(
        inputDevice: InputDevice
    ): BluetoothDetailsGameControllerPreferenceController {
        whenever(inputManager.inputDeviceIds).thenReturn(intArrayOf(DEVICE_ID))
        whenever(inputManager.getInputDeviceBluetoothAddress(DEVICE_ID)).thenReturn(BT_ADDRESS)
        whenever(inputManager.getInputDevice(DEVICE_ID)).thenReturn(inputDevice)
        whenever(
                preferenceScreen.findPreference<PreferenceGroup>(
                    eq(
                        BluetoothDetailsGameControllerPreferenceController
                            .KEY_GAME_CONTROLLER_SETTINGS
                    )
                )
            )
            .thenReturn(preferenceGroup)
        whenever(preferenceGroup.context).thenReturn(context)
        whenever(
                preferenceGroup.findPreference<Preference>(
                    eq(
                        BluetoothDetailsGameControllerPreferenceController
                            .KEY_GAME_CONTROLLER_SETTINGS_PREF
                    )
                )
            )
            .thenReturn(null)
        val controller =
            BluetoothDetailsGameControllerPreferenceController(context, cachedDevice, lifecycle)
        return controller
    }

    @Test
    @EnableFlags(com.android.hardware.input.Flags.FLAG_CONTROLLER_REMAPPING)
    fun isAvailable_isGamepad_returnsTrue() {
        val controller = setupController(createInputDevice(InputDevice.SOURCE_GAMEPAD))

        assert(controller.isAvailable)
    }

    @Test
    @EnableFlags(com.android.hardware.input.Flags.FLAG_CONTROLLER_REMAPPING)
    fun isAvailable_isJoystick_returnsTrue() {
        val controller = setupController(createInputDevice(InputDevice.SOURCE_JOYSTICK))

        assert(controller.isAvailable)
    }

    @Test
    @EnableFlags(com.android.hardware.input.Flags.FLAG_CONTROLLER_REMAPPING)
    fun isAvailable_notGamepad_returnsFalse() {
        val controller = setupController(createInputDevice(InputDevice.SOURCE_MOUSE))

        assert(!controller.isAvailable)
    }

    @Test
    @DisableFlags(com.android.hardware.input.Flags.FLAG_CONTROLLER_REMAPPING)
    fun isAvailable_featureFlagOff_returnsFalse() {
        val controller = setupController(createInputDevice(InputDevice.SOURCE_GAMEPAD))

        assert(!controller.isAvailable)
    }

    @Test
    @EnableFlags(com.android.hardware.input.Flags.FLAG_CONTROLLER_REMAPPING)
    fun displayPreference_isAvailable_addsPreference() {
        val controller = setupController(createInputDevice(InputDevice.SOURCE_GAMEPAD))
        controller.displayPreference(preferenceScreen)

        verify(preferenceGroup).addPreference(any())
    }

    @Test
    @EnableFlags(com.android.hardware.input.Flags.FLAG_CONTROLLER_REMAPPING)
    fun displayPreference_isNotAvailable_removesAllPreferences() {
        val controller = setupController(createInputDevice(InputDevice.SOURCE_MOUSE))
        controller.displayPreference(preferenceScreen)

        verify(preferenceGroup).removeAll()
    }

    @Test
    @EnableFlags(com.android.hardware.input.Flags.FLAG_CONTROLLER_REMAPPING)
    fun refresh_isAvailableAndPreferenceExists_doesNothing() {
        val controller = setupController(createInputDevice(InputDevice.SOURCE_GAMEPAD))
        val existingPref = Preference(context)
        whenever(
                preferenceGroup.findPreference<Preference>(
                    eq(
                        BluetoothDetailsGameControllerPreferenceController
                            .KEY_GAME_CONTROLLER_SETTINGS_PREF
                    )
                )
            )
            .thenReturn(existingPref)
        controller.displayPreference(preferenceScreen)

        verify(preferenceGroup, never()).addPreference(any())
    }

    @Test
    @EnableFlags(com.android.hardware.input.Flags.FLAG_CONTROLLER_REMAPPING)
    fun onPreferenceClick_startsCorrectIntent() {
        val inputDevice = createInputDevice(InputDevice.SOURCE_GAMEPAD)
        val controller = setupController(inputDevice)
        controller.displayPreference(preferenceScreen)

        val preferenceCaptor = ArgumentCaptor.forClass(Preference::class.java)
        verify(preferenceGroup).addPreference(preferenceCaptor.capture())

        controller.onPreferenceClick(preferenceCaptor.value)
        val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
        verify(context).startActivity(intentCaptor.capture())

        val capturedIntent = intentCaptor.value
        assert(capturedIntent.action == Settings.ACTION_GAME_CONTROLLER_SETTINGS)
        assert(
            capturedIntent.getParcelableExtra(
                Settings.EXTRA_INPUT_DEVICE_IDENTIFIER,
                InputDeviceIdentifier::class.java,
            ) == inputDevice.identifier
        )
    }

    @Test
    @EnableFlags(com.android.hardware.input.Flags.FLAG_CONTROLLER_REMAPPING)
    fun onResume_registersListener() {
        val controller = setupController(createInputDevice(InputDevice.SOURCE_GAMEPAD))
        controller.onResume()

        verify(inputManager).registerInputDeviceListener(any(), any())
    }

    @Test
    fun onPause_unregistersListener() {
        val controller = setupController(createInputDevice(InputDevice.SOURCE_GAMEPAD))
        controller.onPause()

        verify(inputManager).unregisterInputDeviceListener(any())
    }

    @Test
    @EnableFlags(com.android.hardware.input.Flags.FLAG_CONTROLLER_REMAPPING)
    fun onInputDeviceRemoved_matchingDevice_refreshesAndRemovesPreference() {
        val controller = setupController(createInputDevice(InputDevice.SOURCE_GAMEPAD))
        controller.displayPreference(preferenceScreen)
        verify(preferenceGroup).addPreference(any())

        controller.onInputDeviceRemoved(DEVICE_ID)

        assert(!controller.isAvailable)
        verify(preferenceGroup).removeAll()
    }

    @Test
    @EnableFlags(com.android.hardware.input.Flags.FLAG_CONTROLLER_REMAPPING)
    fun onInputDeviceRemoved_nonMatchingDevice_doesNothing() {
        val controller = setupController(createInputDevice(InputDevice.SOURCE_GAMEPAD))
        controller.displayPreference(preferenceScreen)
        verify(preferenceGroup).addPreference(any())

        controller.onInputDeviceRemoved(DEVICE_ID + 1)

        assert(controller.isAvailable)
        verify(preferenceGroup, never()).removeAll()
    }

    private fun createInputDevice(source: Int): InputDevice {
        return InputDevice.Builder()
            .setSources(source)
            .setId(DEVICE_ID)
            .setName("Fake device")
            .setDescriptor("Fake descriptor")
            .build()
    }

    companion object {
        const val DEVICE_ID = 1

        const val BT_ADDRESS = "Fake address"
    }
}
