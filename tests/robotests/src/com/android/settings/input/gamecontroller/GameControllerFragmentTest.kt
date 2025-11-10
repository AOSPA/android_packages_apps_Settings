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
import android.hardware.input.InputManager
import android.os.Bundle
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.testutils.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import kotlin.reflect.KClass
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.shadows.ShadowLooper

/** Tests for [GameControllerFragment]. */
@RunWith(AndroidJUnit4::class)
@EnableFlags(com.android.hardware.input.Flags.FLAG_CONTROLLER_REMAPPING)
@SuppressLint("MissingPermission")
class GameControllerFragmentTest {

    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @get:Rule val setFlagsRule = SetFlagsRule()

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock private lateinit var inputManager: InputManager

    private lateinit var application: Application

    @Before
    fun setUp() {
        application = spy(ApplicationProvider.getApplicationContext<Application>())
        whenever(application.getSystemService(InputManager::class.java)).thenReturn(inputManager)
        whenever(inputManager.getControllerButtonRemappings(any())).thenReturn(mapOf())
        whenever(inputManager.getControllerAxisRemappings(any())).thenReturn(mapOf())
    }

    private fun launchFragment(viewModel: GameControllerViewModel) =
        launchFragmentInContainer<GameControllerFragment>(
            fragmentArgs =
                Bundle().apply {
                    putParcelable(
                        GameControllerUtils.EXTRA_INPUT_DEVICE_IDENTIFIER,
                        viewModel.controllerDevice.inputDeviceIdentifier,
                    )
                },
            themeResId = R.style.Theme_Settings,
            factory =
                object : FragmentFactory() {
                    override fun instantiate(
                        classLoader: ClassLoader,
                        className: String,
                    ): Fragment {
                        if (className == GameControllerFragment::class.java.name) {
                            val testViewModelFactory =
                                object : ViewModelProvider.Factory {
                                    @Suppress("UNCHECKED_CAST")
                                    override fun <T : ViewModel> create(
                                        modelClass: KClass<T>,
                                        extras: CreationExtras,
                                    ): T {
                                        return viewModel as T
                                    }
                                }
                            // Use the test-only constructor to inject the factory
                            return GameControllerFragment(testViewModelFactory)
                        }
                        return super.instantiate(classLoader, className)
                    }
                },
        )

    @Test
    fun onLaunch_titleIsSet() {
        val device = createFakeController(1, "Test Controller")
        whenever(inputManager.getInputDevice(1)).thenReturn(device)
        whenever(inputManager.getInputDeviceByDescriptor(device.descriptor)).thenReturn(device)
        val scenario = launchFragment(GameControllerViewModel(application, device.identifier))

        scenario.onFragment { fragment ->
            assertThat(fragment.requireActivity().title).isEqualTo("Test Controller")
        }
    }

    @Test
    fun onDeviceDisconnected_activityFinishes() {
        val device = createFakeController(1, "Test Controller")
        whenever(inputManager.getInputDevice(1)).thenReturn(device)
        whenever(inputManager.getInputDeviceByDescriptor(device.descriptor)).thenReturn(device)
        val viewModel = GameControllerViewModel(application, device.identifier)
        val scenario = launchFragment(viewModel)

        scenario.onFragment { fragment ->
            assertThat(fragment.requireActivity().isFinishing).isFalse()
            viewModel.onInputDeviceRemoved(device.id)
        }

        scenario.onFragment { fragment ->
            assertThat(fragment.requireActivity().isFinishing).isTrue()
        }
    }

    @Test
    fun onFragmentResult_applyRemappingIsCalledForButton() {
        val device = createFakeController(1, "Remapping Controller")
        whenever(inputManager.getInputDevice(1)).thenReturn(device)
        whenever(inputManager.getInputDeviceByDescriptor(device.descriptor)).thenReturn(device)
        val viewModel = GameControllerViewModel(application, device.identifier)
        val scenario = launchFragment(viewModel)

        scenario.onFragment { fragment ->
            val result =
                Bundle().apply {
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_FROM_PREFERENCE_KEY,
                        "controller_button_a",
                    )
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_TO_PREFERENCE_KEY,
                        "controller_button_b",
                    )
                }
            fragment.parentFragmentManager.setFragmentResult(
                GameControllerRemappingDialogFragment.REQUEST_REMAPPING,
                result,
            )
        }

        // Verify that the remapping was applied via the InputManager mock
        verify(inputManager)
            .remapControllerButton(
                eq(device.identifier),
                eq(KeyEvent.KEYCODE_BUTTON_A),
                eq(KeyEvent.KEYCODE_BUTTON_B),
            )
    }

    @Test
    fun onFragmentResult_applyRemappingIsCalledForAxis() {
        val device = createFakeController(1, "Axis Remapping Controller")
        whenever(inputManager.getInputDevice(1)).thenReturn(device)
        whenever(inputManager.getInputDeviceByDescriptor(device.descriptor)).thenReturn(device)
        val viewModel = GameControllerViewModel(application, device.identifier)
        val scenario = launchFragment(viewModel)
        scenario.onFragment { fragment ->
            val result =
                Bundle().apply {
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_FROM_PREFERENCE_KEY,
                        "controller_stick_left",
                    )
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_TO_PREFERENCE_KEY,
                        "controller_stick_right",
                    )
                }
            fragment.parentFragmentManager.setFragmentResult(
                GameControllerRemappingDialogFragment.REQUEST_REMAPPING,
                result,
            )
        }

        verify(inputManager)
            .remapControllerAxis(
                eq(device.identifier),
                eq(MotionEvent.AXIS_X),
                eq(MotionEvent.AXIS_Z),
            )
        verify(inputManager)
            .remapControllerAxis(
                eq(device.identifier),
                eq(MotionEvent.AXIS_Y),
                eq(MotionEvent.AXIS_RZ),
            )
    }

    @Test
    fun onRequestRemappingDialog_showsRemappingDialogFragment() {
        val device = createFakeController(1, "Test Controller")
        whenever(inputManager.getInputDevice(1)).thenReturn(device)
        whenever(inputManager.getInputDeviceByDescriptor(device.descriptor)).thenReturn(device)

        val viewModel = GameControllerViewModel(application, device.identifier)
        val scenario = launchFragment(viewModel)

        scenario.onFragment { fragment ->
            viewModel.requestRemappingDialog("controller_button_a", "controller_button_b")

            ShadowLooper.idleMainLooper()

            val dialogFragment =
                fragment.parentFragmentManager.findFragmentByTag(
                    "GameControllerRemappingDialogFragment"
                )
            assertThat(dialogFragment).isNotNull()
            assertThat(dialogFragment)
                .isInstanceOf(GameControllerRemappingDialogFragment::class.java)
        }
    }

    private fun createFakeController(deviceId: Int, name: String): InputDevice {
        return InputDevice.Builder()
            .setSources(InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK)
            .setId(deviceId)
            .setName(name)
            .setDescriptor("device $deviceId")
            .build()
    }
}
