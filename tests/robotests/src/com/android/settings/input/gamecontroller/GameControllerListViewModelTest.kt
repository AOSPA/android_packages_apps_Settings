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

import android.app.Application
import android.hardware.input.InputManager
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.view.InputDevice
import androidx.lifecycle.Observer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowLooper

/** Tests for [GameControllerListViewModel]. */
@RunWith(RobolectricTestRunner::class)
@EnableFlags(com.android.hardware.input.Flags.FLAG_CONTROLLER_REMAPPING)
class GameControllerListViewModelTest {

    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @get:Rule val setFlagsRule = SetFlagsRule()

    @Mock private lateinit var observer: Observer<List<GameControllerUtils.ControllerDevice>>

    @Mock private lateinit var inputManager: InputManager

    private lateinit var application: Application
    private lateinit var viewModel: GameControllerListViewModel

    @Before
    fun setUp() {
        application = Mockito.spy(RuntimeEnvironment.application)
        whenever(application.getSystemService(InputManager::class.java)).thenReturn(inputManager)
        whenever(inputManager.inputDeviceIds).thenReturn(intArrayOf())
    }

    @Test
    fun viewModelNotifiesObservers_onControllerAdded() {
        viewModel = GameControllerListViewModel(application)
        viewModel.controllers.observeForever(observer)

        val deviceId = 1
        val device = createFakeController(deviceId)
        whenever(inputManager.inputDeviceIds).thenReturn(intArrayOf(deviceId))
        whenever(inputManager.getInputDevice(deviceId)).thenReturn(device)
        viewModel.onInputDeviceAdded(deviceId)
        ShadowLooper.idleMainLooper()

        verify(observer).onChanged(listOf(GameControllerUtils.ControllerDevice(device, null)))
    }

    @Test
    fun viewModelNotifiesObservers_onControllerRemoved() {
        val deviceId = 1
        val device = createFakeController(deviceId)
        whenever(inputManager.inputDeviceIds).thenReturn(intArrayOf(deviceId))
        whenever(inputManager.getInputDevice(deviceId)).thenReturn(device)
        viewModel = GameControllerListViewModel(application)
        viewModel.controllers.observeForever(observer)

        whenever(inputManager.inputDeviceIds).thenReturn(intArrayOf())
        viewModel.onInputDeviceRemoved(deviceId)
        ShadowLooper.idleMainLooper()

        verify(observer).onChanged(emptyList())
    }

    private fun createFakeController(deviceId: Int): InputDevice {
        return InputDevice.Builder()
            .setSources(InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK)
            .setId(deviceId)
            .setDescriptor("device $deviceId")
            .setIsVirtualDevice(false)
            .build()
    }
}
