/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.input.gamecontroller

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.hardware.input.InputDeviceIdentifier
import android.hardware.input.InputManager
import android.view.InputDevice
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.core.BasePreferenceController
import com.android.settings.input.gamecontroller.GameControllerUtils.preferenceKeyToAxesMap
import com.android.settings.input.gamecontroller.GameControllerUtils.preferenceKeyToButtonMap
import com.android.settings.input.gamecontroller.GameControllerUtils.preferenceKeyToNameMap
import com.android.settings.testutils.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Tests for [GameControllerRemappingPreferenceController]. */
@RunWith(AndroidJUnit4::class)
@SuppressLint("MissingPermission")
class GameControllerRemappingPreferenceControllerTest {

    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock private lateinit var inputManager: InputManager
    @Mock private lateinit var screen: PreferenceScreen

    private lateinit var context: Context
    private lateinit var application: Application
    private lateinit var viewModel: GameControllerViewModel
    private lateinit var controller: GameControllerRemappingPreferenceController
    private lateinit var identifier: InputDeviceIdentifier
    private lateinit var preferenceMocks: Map<String, Preference>

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        application = spy(context.applicationContext as Application)
        whenever(application.getSystemService(InputManager::class.java)).doReturn(inputManager)
        // Create a map of mock preferences for all possible keys.
        preferenceMocks =
            preferenceKeyToNameMap.keys.associateWith { key ->
                mock(Preference::class.java).also { whenever(it.key).doReturn(key) }
            }

        // When findPreference is called, return the corresponding mock from our map.
        whenever(screen.findPreference<Preference>(any())).thenAnswer { invocation ->
            val key = invocation.arguments[0] as String
            preferenceMocks[key]
        }
    }

    private fun setupController(
        device: InputDevice,
        buttonRemapping: Map<Int, Int> = mapOf(),
        axisRemapping: Map<Int, Int> = mapOf(),
    ) {
        identifier = device.identifier
        whenever(inputManager.getInputDeviceByDescriptor(identifier.descriptor)).doReturn(device)
        whenever(inputManager.getControllerButtonRemappings(identifier)).doReturn(buttonRemapping)
        whenever(inputManager.getControllerAxisRemappings(identifier)).doReturn(axisRemapping)

        viewModel = GameControllerViewModel(application, identifier)
        controller = GameControllerRemappingPreferenceController(context, viewModel)
        controller.displayPreference(screen)
    }

    @Test
    fun getAvailabilityStatus_isAvailable() {
        setupController(device = createGameController(deviceId = 1, name = "Test Controller"))
        assertThat(controller.availabilityStatus).isEqualTo(BasePreferenceController.AVAILABLE)
    }

    @Test
    fun displayPreference_deviceIsGamepadAndJoystick_allPreferencesAreVisible() {
        setupController(
            device =
                createGameController(
                    deviceId = 1,
                    name = "Full Controller",
                    source = InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK,
                )
        )

        preferenceKeyToButtonMap.keys.forEach { verify(preferenceMocks[it]!!).isVisible = true }
        preferenceKeyToAxesMap.keys.forEach { verify(preferenceMocks[it]!!).isVisible = true }
    }

    @Test
    fun displayPreference_deviceIsOnlyGamepad_onlyButtonPreferencesAreVisible() {
        setupController(
            device =
                createGameController(
                    deviceId = 1,
                    name = "Gamepad Only Controller",
                    source = InputDevice.SOURCE_GAMEPAD,
                )
        )

        preferenceKeyToButtonMap.keys.forEach { verify(preferenceMocks[it]!!).isVisible = true }
        preferenceKeyToAxesMap.keys.forEach { verify(preferenceMocks[it]!!).isVisible = false }
    }

    @Test
    fun displayPreference_deviceIsOnlyJoystick_onlyAxisPreferencesAreVisible() {
        setupController(
            device =
                createGameController(
                    deviceId = 1,
                    name = "Joystick Only Controller",
                    source = InputDevice.SOURCE_JOYSTICK,
                )
        )

        preferenceKeyToButtonMap.keys.forEach { verify(preferenceMocks[it]!!).isVisible = false }
        preferenceKeyToAxesMap.keys.forEach { verify(preferenceMocks[it]!!).isVisible = true }
    }

    @Test
    fun updateState_withButtonRemapping_setsCorrectSummary() {
        val buttonKeys = preferenceKeyToButtonMap.keys.toList()
        buttonKeys.forEachIndexed { i, fromKey ->
            val toKey = buttonKeys[(i + 1) % buttonKeys.size]
            val fromButton = preferenceKeyToButtonMap[fromKey]!!
            val toButton = preferenceKeyToButtonMap[toKey]!!

            // Initialize with the remapping for this iteration
            setupController(
                device = createGameController(deviceId = 1, name = "Test Controller"),
                buttonRemapping = mapOf(fromButton to toButton),
            )

            controller.updateState(screen)

            // Verify the summary of the "from" preference is now the name of the "to" preference
            verify(preferenceMocks[fromKey]!!).setSummary(preferenceKeyToNameMap[toKey]!!)
        }
    }

    @Test
    fun updateState_withAxisRemapping_setsCorrectSummary() {
        val axisKeys = preferenceKeyToAxesMap.keys.toList()
        axisKeys.forEachIndexed { i, fromKey ->
            val toKey = axisKeys[(i + 1) % axisKeys.size]
            val fromAxes = preferenceKeyToAxesMap[fromKey]!!
            val toAxes = preferenceKeyToAxesMap[toKey]!!
            val remapping = fromAxes.zip(toAxes).toMap()

            // Initialize with the remapping for this iteration
            setupController(
                device = createGameController(deviceId = 1, name = "Test Controller"),
                axisRemapping = remapping,
            )

            controller.updateState(screen)

            // Verify the summary of the "from" preference is now the name of the "to" preference
            verify(preferenceMocks[fromKey]!!).setSummary(preferenceKeyToNameMap[toKey]!!)
        }
    }

    @Test
    fun updateState_withDefaultMapping_setsCorrectSummary() {
        // Simulate default mappings (empty maps)
        setupController(device = createGameController(deviceId = 1, name = "Test Controller"))

        controller.updateState(screen)

        // Verify that all preferences have their default summary
        preferenceMocks.forEach { (key, pref) ->
            verify(pref).setSummary(preferenceKeyToNameMap[key]!!)
        }
    }

    @Test
    fun handlePreferenceTreeClick_requestsRemappingDialog() =
        runTest(UnconfinedTestDispatcher()) {
            // Simulate default mappings
            setupController(device = createGameController(deviceId = 1, name = "Test Controller"))

            val preferenceKeys = preferenceKeyToNameMap.keys.toList()
            preferenceKeys.forEachIndexed { i, fromKey ->
                // Launch a separate coroutine to collect the flow emission.
                val job = launch {
                    val request = viewModel.showRemappingDialog.first()
                    assertThat(request.fromPreferenceKey).isEqualTo(fromKey)
                    assertThat(request.toPreferenceKey).isEqualTo(fromKey)
                }

                // Trigger the action that should emit to the flow.
                val handled = controller.handlePreferenceTreeClick(preferenceMocks[fromKey]!!)
                assertThat(handled).isTrue()

                // Wait for the collector coroutine to complete.
                job.join()
            }
        }

    private fun createGameController(
        deviceId: Int,
        name: String,
        source: Int = InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK,
    ): InputDevice {
        return InputDevice.Builder()
            .setSources(source)
            .setId(deviceId)
            .setName(name)
            .setDescriptor("device $deviceId")
            .build()
    }
}
