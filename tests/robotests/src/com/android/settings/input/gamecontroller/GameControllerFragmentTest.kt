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
import android.hardware.input.InputDeviceIdentifier
import android.hardware.input.InputManager
import android.os.Bundle
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.testutils.InstantTaskExecutorRule
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat
import com.android.settings.testutils.shadow.ShadowInputManager
import com.android.settingslib.widget.ButtonPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/** Tests for [GameControllerFragment]. */
@RunWith(AndroidJUnit4::class)
@EnableFlags(com.android.hardware.input.Flags.FLAG_CONTROLLER_REMAPPING)
@SuppressLint("MissingPermission")
@Config(shadows = [ShadowInputManager::class, ShadowAlertDialogCompat::class])
class GameControllerFragmentTest {

    @get:Rule val setFlagsRule = SetFlagsRule()

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val inputManager: InputManager = context.getSystemService(InputManager::class.java)
    private val shadowInputManager: ShadowInputManager =
        shadowOf(inputManager) as ShadowInputManager

    private fun launchFragment(identifier: InputDeviceIdentifier) =
        launchFragmentInContainer<GameControllerFragment>(
            fragmentArgs =
                Bundle().apply {
                    putParcelable(GameControllerUtils.EXTRA_INPUT_DEVICE_IDENTIFIER, identifier)
                },
            themeResId = R.style.Theme_Settings,
        )

    @Test
    fun onLaunch_titleIsSet() {
        val device = createFakeController(1, "Test Controller")
        shadowInputManager.addInputDevice(device)
        val scenario = launchFragment(device.identifier)

        scenario.onFragment { fragment ->
            assertThat(fragment.requireActivity().title).isEqualTo("Test Controller")
        }
    }

    @Test
    fun onDeviceDisconnected_activityFinishes() {
        val device = createFakeController(1, "Test Controller")
        shadowInputManager.addInputDevice(device)
        val scenario = launchFragment(device.identifier)

        scenario.onFragment { fragment ->
            assertThat(fragment.requireActivity().isFinishing).isFalse()
            shadowInputManager.removeInputDevice(device.id)
            assertThat(fragment.requireActivity().isFinishing).isTrue()
        }
    }

    @Test
    fun onFragmentResult_applyRemappingIsCalledForButton() {
        val device = createFakeController(1, "Remapping Controller")
        shadowInputManager.addInputDevice(device)
        val scenario = launchFragment(device.identifier)

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

        assertThat(inputManager.getControllerButtonRemappings(device.identifier))
            .containsExactly(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B)
        assertThat(inputManager.getControllerAxisRemappings(device.identifier)).isEmpty()
    }

    @Test
    fun onFragmentResult_applyRemappingIsCalledForAxis() {
        val device = createFakeController(1, "Axis Remapping Controller")
        shadowInputManager.addInputDevice(device)
        val scenario = launchFragment(device.identifier)
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

        assertThat(inputManager.getControllerButtonRemappings(device.identifier)).isEmpty()
        assertThat(inputManager.getControllerAxisRemappings(device.identifier))
            .containsExactly(
                MotionEvent.AXIS_X,
                MotionEvent.AXIS_Z,
                MotionEvent.AXIS_Y,
                MotionEvent.AXIS_RZ,
            )
    }

    @Test
    fun onFragmentResult_BToL2_enablesAxis_then_BToX_removesAxis() {
        val device = createFakeController(1, "Remapping Controller")
        shadowInputManager.addInputDevice(device)
        val scenario = launchFragment(device.identifier)

        scenario.onFragment { fragment ->
            val resultBToL2 =
                Bundle().apply {
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_FROM_PREFERENCE_KEY,
                        "controller_button_b",
                    )
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_TO_PREFERENCE_KEY,
                        "controller_button_l2",
                    )
                }
            fragment.parentFragmentManager.setFragmentResult(
                GameControllerRemappingDialogFragment.REQUEST_REMAPPING,
                resultBToL2,
            )
        }

        assertThat(inputManager.getControllerButtonRemappings(device.identifier))
            .containsExactly(KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_L2)
        assertThat(shadowInputManager.getControllerButtonToAxisRemappings(device.identifier))
            .containsExactly(KeyEvent.KEYCODE_BUTTON_B, MotionEvent.AXIS_LTRIGGER)
        assertThat(inputManager.getControllerAxisRemappings(device.identifier)).isEmpty()

        scenario.onFragment { fragment ->
            val resultBToX =
                Bundle().apply {
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_FROM_PREFERENCE_KEY,
                        "controller_button_b",
                    )
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_TO_PREFERENCE_KEY,
                        "controller_button_x",
                    )
                }
            fragment.parentFragmentManager.setFragmentResult(
                GameControllerRemappingDialogFragment.REQUEST_REMAPPING,
                resultBToX,
            )
        }

        assertThat(inputManager.getControllerButtonRemappings(device.identifier))
            .containsExactly(KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_X)
        assertThat(shadowInputManager.getControllerButtonToAxisRemappings(device.identifier))
            .isEmpty()
        assertThat(inputManager.getControllerAxisRemappings(device.identifier)).isEmpty()
    }

    @Test
    fun onFragmentResult_AToY_remapsButton_then_AToR2_remapsButtonToAxis() {
        val device = createFakeController(1, "Remapping Controller")
        shadowInputManager.addInputDevice(device)
        val scenario = launchFragment(device.identifier)

        scenario.onFragment { fragment ->
            val result =
                Bundle().apply {
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_FROM_PREFERENCE_KEY,
                        "controller_button_a",
                    )
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_TO_PREFERENCE_KEY,
                        "controller_button_y",
                    )
                }
            fragment.parentFragmentManager.setFragmentResult(
                GameControllerRemappingDialogFragment.REQUEST_REMAPPING,
                result,
            )
        }

        assertThat(inputManager.getControllerButtonRemappings(device.identifier))
            .containsExactly(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_Y)
        assertThat(shadowInputManager.getControllerButtonToAxisRemappings(device.identifier))
            .isEmpty()
        assertThat(inputManager.getControllerAxisRemappings(device.identifier)).isEmpty()

        scenario.onFragment { fragment ->
            val result =
                Bundle().apply {
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_FROM_PREFERENCE_KEY,
                        "controller_button_a",
                    )
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_TO_PREFERENCE_KEY,
                        "controller_button_r2",
                    )
                }
            fragment.parentFragmentManager.setFragmentResult(
                GameControllerRemappingDialogFragment.REQUEST_REMAPPING,
                result,
            )
        }

        assertThat(inputManager.getControllerButtonRemappings(device.identifier))
            .containsExactly(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_R2)
        assertThat(shadowInputManager.getControllerButtonToAxisRemappings(device.identifier))
            .containsExactly(KeyEvent.KEYCODE_BUTTON_A, MotionEvent.AXIS_RTRIGGER)
        assertThat(inputManager.getControllerAxisRemappings(device.identifier)).isEmpty()
    }

    @Test
    fun onFragmentResult_XToR2_remapsButtonToAxis_then_XToX_removesRemappings() {
        val device = createFakeController(1, "Remapping Controller")
        shadowInputManager.addInputDevice(device)
        val scenario = launchFragment(device.identifier)

        scenario.onFragment { fragment ->
            val result =
                Bundle().apply {
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_FROM_PREFERENCE_KEY,
                        "controller_button_x",
                    )
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_TO_PREFERENCE_KEY,
                        "controller_button_r2",
                    )
                }
            fragment.parentFragmentManager.setFragmentResult(
                GameControllerRemappingDialogFragment.REQUEST_REMAPPING,
                result,
            )
        }

        assertThat(inputManager.getControllerButtonRemappings(device.identifier))
            .containsExactly(KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_R2)
        assertThat(shadowInputManager.getControllerButtonToAxisRemappings(device.identifier))
            .containsExactly(KeyEvent.KEYCODE_BUTTON_X, MotionEvent.AXIS_RTRIGGER)
        assertThat(inputManager.getControllerAxisRemappings(device.identifier)).isEmpty()

        scenario.onFragment { fragment ->
            val result =
                Bundle().apply {
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_FROM_PREFERENCE_KEY,
                        "controller_button_x",
                    )
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_TO_PREFERENCE_KEY,
                        "controller_button_x",
                    )
                }
            fragment.parentFragmentManager.setFragmentResult(
                GameControllerRemappingDialogFragment.REQUEST_REMAPPING,
                result,
            )
        }

        assertThat(inputManager.getControllerButtonRemappings(device.identifier))
            .isEmpty()
        assertThat(shadowInputManager.getControllerButtonToAxisRemappings(device.identifier))
            .isEmpty()
        assertThat(inputManager.getControllerAxisRemappings(device.identifier)).isEmpty()
    }

    @Test
    fun onFragmentResult_BToY_remapsButton_then_BToB_clearsRemappings() {
        val device = createFakeController(1, "Remapping Controller")
        shadowInputManager.addInputDevice(device)
        val scenario = launchFragment(device.identifier)

        scenario.onFragment { fragment ->
            val result =
                Bundle().apply {
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_FROM_PREFERENCE_KEY,
                        "controller_button_b",
                    )
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_TO_PREFERENCE_KEY,
                        "controller_button_y",
                    )
                }
            fragment.parentFragmentManager.setFragmentResult(
                GameControllerRemappingDialogFragment.REQUEST_REMAPPING,
                result,
            )
        }

        assertThat(inputManager.getControllerButtonRemappings(device.identifier))
            .containsExactly(KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_Y)
        assertThat(shadowInputManager.getControllerButtonToAxisRemappings(device.identifier))
            .isEmpty()
        assertThat(inputManager.getControllerAxisRemappings(device.identifier)).isEmpty()

        scenario.onFragment { fragment ->
            val result =
                Bundle().apply {
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_FROM_PREFERENCE_KEY,
                        "controller_button_b",
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

        assertThat(inputManager.getControllerButtonRemappings(device.identifier)).isEmpty()
        assertThat(shadowInputManager.getControllerButtonToAxisRemappings(device.identifier)).isEmpty()
        assertThat(inputManager.getControllerAxisRemappings(device.identifier)).isEmpty()
    }

    @Test
    fun onFragmentResult_L2ToR2_remapsAxis_then_L2ToA_disablesAxis() {
        val device = createFakeController(1, "Remapping Controller")
        shadowInputManager.addInputDevice(device)
        val scenario = launchFragment(device.identifier)

        scenario.onFragment { fragment ->
            val result =
                Bundle().apply {
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_FROM_PREFERENCE_KEY,
                        "controller_button_l2",
                    )
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_TO_PREFERENCE_KEY,
                        "controller_button_r2",
                    )
                }
            fragment.parentFragmentManager.setFragmentResult(
                GameControllerRemappingDialogFragment.REQUEST_REMAPPING,
                result,
            )
        }

        assertThat(inputManager.getControllerButtonRemappings(device.identifier))
            .containsExactly(KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2)
        assertThat(shadowInputManager.getControllerButtonToAxisRemappings(device.identifier))
            .isEmpty()
        assertThat(inputManager.getControllerAxisRemappings(device.identifier))
            .containsExactly(MotionEvent.AXIS_LTRIGGER, MotionEvent.AXIS_RTRIGGER)

        scenario.onFragment { fragment ->
            val result =
                Bundle().apply {
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_FROM_PREFERENCE_KEY,
                        "controller_button_l2",
                    )
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_TO_PREFERENCE_KEY,
                        "controller_button_a",
                    )
                }
            fragment.parentFragmentManager.setFragmentResult(
                GameControllerRemappingDialogFragment.REQUEST_REMAPPING,
                result,
            )
        }

        assertThat(inputManager.getControllerButtonRemappings(device.identifier))
            .containsExactly(KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_A)
        assertThat(shadowInputManager.getControllerButtonToAxisRemappings(device.identifier))
            .isEmpty()
        assertThat(inputManager.getControllerAxisRemappings(device.identifier))
            .containsExactly(MotionEvent.AXIS_LTRIGGER, MotionEvent.AXIS_DISABLED)
    }

    @Test
    fun onFragmentResult_L2ToA_disablesAxis_then_L2ToR2_remapsAxis() {
        val device = createFakeController(1, "Remapping Controller")
        shadowInputManager.addInputDevice(device)
        val scenario = launchFragment(device.identifier)

        scenario.onFragment { fragment ->
            val result =
                Bundle().apply {
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_FROM_PREFERENCE_KEY,
                        "controller_button_l2",
                    )
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_TO_PREFERENCE_KEY,
                        "controller_button_a",
                    )
                }
            fragment.parentFragmentManager.setFragmentResult(
                GameControllerRemappingDialogFragment.REQUEST_REMAPPING,
                result,
            )
        }

        assertThat(inputManager.getControllerButtonRemappings(device.identifier))
            .containsExactly(KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_A)
        assertThat(shadowInputManager.getControllerButtonToAxisRemappings(device.identifier))
            .isEmpty()
        assertThat(inputManager.getControllerAxisRemappings(device.identifier))
            .containsExactly(MotionEvent.AXIS_LTRIGGER, MotionEvent.AXIS_DISABLED)

        scenario.onFragment { fragment ->
            val result =
                Bundle().apply {
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_FROM_PREFERENCE_KEY,
                        "controller_button_l2",
                    )
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_TO_PREFERENCE_KEY,
                        "controller_button_r2",
                    )
                }
            fragment.parentFragmentManager.setFragmentResult(
                GameControllerRemappingDialogFragment.REQUEST_REMAPPING,
                result,
            )
        }

        assertThat(inputManager.getControllerButtonRemappings(device.identifier))
            .containsExactly(KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2)
        assertThat(shadowInputManager.getControllerButtonToAxisRemappings(device.identifier))
            .isEmpty()
        assertThat(inputManager.getControllerAxisRemappings(device.identifier))
            .containsExactly(MotionEvent.AXIS_LTRIGGER, MotionEvent.AXIS_RTRIGGER)
    }

    @Test
    fun onFragmentResult_R2ToL2_remapsAxis_then_R2ToR2_clearsRemappings() {
        val device = createFakeController(1, "Remapping Controller")
        shadowInputManager.addInputDevice(device)
        val scenario = launchFragment(device.identifier)

        scenario.onFragment { fragment ->
            val resultR2ToL2 =
                Bundle().apply {
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_FROM_PREFERENCE_KEY,
                        "controller_button_r2",
                    )
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_TO_PREFERENCE_KEY,
                        "controller_button_l2",
                    )
                }
            fragment.parentFragmentManager.setFragmentResult(
                GameControllerRemappingDialogFragment.REQUEST_REMAPPING,
                resultR2ToL2,
            )
        }

        assertThat(inputManager.getControllerButtonRemappings(device.identifier))
            .containsExactly(KeyEvent.KEYCODE_BUTTON_R2, KeyEvent.KEYCODE_BUTTON_L2)
        assertThat(shadowInputManager.getControllerButtonToAxisRemappings(device.identifier))
            .isEmpty()
        assertThat(inputManager.getControllerAxisRemappings(device.identifier))
            .containsExactly(MotionEvent.AXIS_RTRIGGER, MotionEvent.AXIS_LTRIGGER)

        scenario.onFragment { fragment ->
            val resultR2ToA =
                Bundle().apply {
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_FROM_PREFERENCE_KEY,
                        "controller_button_r2",
                    )
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_TO_PREFERENCE_KEY,
                        "controller_button_r2",
                    )
                }
            fragment.parentFragmentManager.setFragmentResult(
                GameControllerRemappingDialogFragment.REQUEST_REMAPPING,
                resultR2ToA,
            )
        }

        assertThat(inputManager.getControllerButtonRemappings(device.identifier))
            .isEmpty()
        assertThat(shadowInputManager.getControllerButtonToAxisRemappings(device.identifier))
            .isEmpty()
        assertThat(inputManager.getControllerAxisRemappings(device.identifier)).isEmpty()
    }

    @Test
    fun onFragmentResult_L2ToA_disablesAxis_then_L2ToL2_clearsRemappings() {
        val device = createFakeController(1, "Remapping Controller")
        shadowInputManager.addInputDevice(device)
        val scenario = launchFragment(device.identifier)

        scenario.onFragment { fragment ->
            val result =
                Bundle().apply {
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_FROM_PREFERENCE_KEY,
                        "controller_button_l2",
                    )
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_TO_PREFERENCE_KEY,
                        "controller_button_a",
                    )
                }
            fragment.parentFragmentManager.setFragmentResult(
                GameControllerRemappingDialogFragment.REQUEST_REMAPPING,
                result,
            )
        }

        assertThat(inputManager.getControllerButtonRemappings(device.identifier))
            .containsExactly(KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_A)
        assertThat(shadowInputManager.getControllerButtonToAxisRemappings(device.identifier))
            .isEmpty()
        assertThat(inputManager.getControllerAxisRemappings(device.identifier))
            .containsExactly(MotionEvent.AXIS_LTRIGGER, MotionEvent.AXIS_DISABLED)

        scenario.onFragment { fragment ->
            val result =
                Bundle().apply {
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_FROM_PREFERENCE_KEY,
                        "controller_button_l2",
                    )
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_TO_PREFERENCE_KEY,
                        "controller_button_l2",
                    )
                }
            fragment.parentFragmentManager.setFragmentResult(
                GameControllerRemappingDialogFragment.REQUEST_REMAPPING,
                result,
            )
        }

        assertThat(inputManager.getControllerButtonRemappings(device.identifier)).isEmpty()
        assertThat(shadowInputManager.getControllerButtonToAxisRemappings(device.identifier)).isEmpty()
        assertThat(inputManager.getControllerAxisRemappings(device.identifier)).isEmpty()
    }

    @Test
    fun onFragmentResult_AToL2_motionRangeNotSupported_remapsButtonOnly() {
        val device = createFakeControllerWithoutMotionRanges()
        shadowInputManager.addInputDevice(device)
        val scenario = launchFragment(device.identifier)

        scenario.onFragment { fragment ->
            val result =
                Bundle().apply {
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_FROM_PREFERENCE_KEY,
                        "controller_button_a",
                    )
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_TO_PREFERENCE_KEY,
                        "controller_button_l2",
                    )
                }
            fragment.parentFragmentManager.setFragmentResult(
                GameControllerRemappingDialogFragment.REQUEST_REMAPPING,
                result,
            )
        }

        assertThat(inputManager.getControllerButtonRemappings(device.identifier))
            .containsExactly(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_L2)
        assertThat(shadowInputManager.getControllerButtonToAxisRemappings(device.identifier))
            .isEmpty()
        assertThat(inputManager.getControllerAxisRemappings(device.identifier))
            .isEmpty()
    }

    @Test
    fun onFragmentResult_l2ToR2_motionRangesNotSupported_remapsButtonOnly() {
        val device = createFakeControllerWithoutMotionRanges()
        shadowInputManager.addInputDevice(device)
        val scenario = launchFragment(device.identifier)

        scenario.onFragment { fragment ->
            val resultL2ToR2 =
                Bundle().apply {
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_FROM_PREFERENCE_KEY,
                        "controller_button_l2",
                    )
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_TO_PREFERENCE_KEY,
                        "controller_button_r2",
                    )
                }
            fragment.parentFragmentManager.setFragmentResult(
                GameControllerRemappingDialogFragment.REQUEST_REMAPPING,
                resultL2ToR2,
            )
        }

        assertThat(inputManager.getControllerButtonRemappings(device.identifier))
            .containsExactly(KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2)
        assertThat(shadowInputManager.getControllerButtonToAxisRemappings(device.identifier))
            .isEmpty()
        assertThat(inputManager.getControllerAxisRemappings(device.identifier))
            .isEmpty()
    }

    @Test
    fun onFragmentResult_R2ToL2_nonJoystickMotionRange_remapsButtonOnly() {
        val device = InputDevice.Builder()
            .setSources(InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK)
            .setId(1)
            .setName("Remapping Controller")
            .setDescriptor("device 1")
            .addMotionRange(MotionEvent.AXIS_LTRIGGER, InputDevice.SOURCE_MOUSE, 0f, 1f, 0f, 0f, 0f)
            .addMotionRange(MotionEvent.AXIS_RTRIGGER, InputDevice.SOURCE_MOUSE, 0f, 1f, 0f, 0f, 0f)
            .build()
        shadowInputManager.addInputDevice(device)
        val scenario = launchFragment(device.identifier)

        scenario.onFragment { fragment ->
            val result =
                Bundle().apply {
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_FROM_PREFERENCE_KEY,
                        "controller_button_r2",
                    )
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_TO_PREFERENCE_KEY,
                        "controller_button_l2",
                    )
                }
            fragment.parentFragmentManager.setFragmentResult(
                GameControllerRemappingDialogFragment.REQUEST_REMAPPING,
                result,
            )
        }

        assertThat(inputManager.getControllerButtonRemappings(device.identifier))
            .containsExactly(KeyEvent.KEYCODE_BUTTON_R2, KeyEvent.KEYCODE_BUTTON_L2)
        assertThat(shadowInputManager.getControllerButtonToAxisRemappings(device.identifier))
            .isEmpty()
        assertThat(inputManager.getControllerAxisRemappings(device.identifier))
            .isEmpty()
    }

    @Test
    fun onRequestRemappingDialog_showsRemappingDialogFragment() {
        val device = createFakeController(1, "Test Controller")
        shadowInputManager.addInputDevice(device)
        val scenario = launchFragment(device.identifier)

        scenario.onFragment { fragment ->
            fragment.findPreference<Preference>("controller_button_a")!!.performClick()
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

    @Test
    fun resetToDefault_showsDialog_positiveButton_clearsMappings() {
        val device = createFakeController(1, "Test Controller")
        shadowInputManager.addInputDevice(device)
        inputManager.remapControllerButton(
            device.identifier,
            KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BUTTON_X,
        )
        inputManager.remapControllerButton(
            device.identifier,
            KeyEvent.KEYCODE_BUTTON_L1,
            KeyEvent.KEYCODE_BUTTON_R2,
        )
        inputManager.remapControllerButtonToAxis(
            device.identifier,
            KeyEvent.KEYCODE_BUTTON_A,
            MotionEvent.AXIS_LTRIGGER,
        )
        inputManager.remapControllerAxis(device.identifier, MotionEvent.AXIS_Y, MotionEvent.AXIS_RZ)
        val scenario = launchFragment(device.identifier)

        scenario.onFragment { fragment ->
            // Scroll to the preference to ensure RecyclerView binds it and creates the View
            fragment.scrollToPreference(GameControllerFragment.RESET_BUTTON_PREFERENCE_KEY)
            ShadowLooper.idleMainLooper()
            val preference =
                fragment.findPreference<ButtonPreference>(
                    GameControllerFragment.RESET_BUTTON_PREFERENCE_KEY
                )!!
            val button = preference.button
            assertThat(button).isNotNull()
            button!!.performClick()

            ShadowLooper.idleMainLooper()
            val dialog = ShadowAlertDialogCompat.getLatestAlertDialog()
            assertThat(dialog).isNotNull()
            val shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog)
            assertThat(shadowDialog.title)
                .isEqualTo(context.getString(R.string.game_controller_remapping_reset_dialog_title))
            assertThat(shadowDialog.message)
                .isEqualTo(
                    context.getString(R.string.game_controller_remapping_reset_dialog_message)
                )

            dialog!!.getButton(AlertDialog.BUTTON_POSITIVE).performClick()

            ShadowLooper.idleMainLooper()
            assertThat(inputManager.getControllerButtonRemappings(device.identifier)).isEmpty()
            assertThat(shadowInputManager.getControllerButtonToAxisRemappings(device.identifier))
                .isEmpty()
            assertThat(inputManager.getControllerAxisRemappings(device.identifier)).isEmpty()
            // Check that the mappings have been reset in the UI: the button (title) and the
            // corresponding action (summary) is the same for each preference.
            for ((key, nameResId) in GameControllerUtils.preferenceKeyToNameMap) {
                val preference = fragment.findPreference<Preference>(key)!!
                assertThat(preference.title).isEqualTo(context.getString(nameResId))
                assertThat(preference.summary).isEqualTo(context.getString(nameResId))
            }
        }
    }

    @Test
    fun resetToDefault_showsDialog_negativeButton_doesNotClearMappings() {
        val device = createFakeController(1, "Test Controller")
        shadowInputManager.addInputDevice(device)
        inputManager.remapControllerButton(
            device.identifier,
            KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BUTTON_X,
        )
        inputManager.remapControllerButton(
            device.identifier,
            KeyEvent.KEYCODE_BUTTON_L1,
            KeyEvent.KEYCODE_BUTTON_R2,
        )
        inputManager.remapControllerButtonToAxis(
            device.identifier,
            KeyEvent.KEYCODE_BUTTON_A,
            MotionEvent.AXIS_LTRIGGER,
        )
        inputManager.remapControllerButtonToAxis(
            device.identifier,
            KeyEvent.KEYCODE_BUTTON_Y,
            MotionEvent.AXIS_RTRIGGER,
        )
        inputManager.remapControllerAxis(device.identifier, MotionEvent.AXIS_Y, MotionEvent.AXIS_RZ)
        val scenario = launchFragment(device.identifier)

        scenario.onFragment { fragment ->
            // Scroll to the preference to ensure RecyclerView binds it and creates the View
            fragment.scrollToPreference(GameControllerFragment.RESET_BUTTON_PREFERENCE_KEY)
            ShadowLooper.idleMainLooper()
            val preference =
                fragment.findPreference<ButtonPreference>(
                    GameControllerFragment.RESET_BUTTON_PREFERENCE_KEY
                )!!
            val button = preference.button
            assertThat(button).isNotNull()
            button!!.performClick()

            ShadowLooper.idleMainLooper()
            val dialog = ShadowAlertDialogCompat.getLatestAlertDialog()
            assertThat(dialog).isNotNull()
            val shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog)
            assertThat(shadowDialog.title)
                .isEqualTo(context.getString(R.string.game_controller_remapping_reset_dialog_title))
            assertThat(shadowDialog.message)
                .isEqualTo(
                    context.getString(R.string.game_controller_remapping_reset_dialog_message)
                )

            dialog!!.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()

            ShadowLooper.idleMainLooper()
            assertThat(inputManager.getControllerButtonRemappings(device.identifier))
                .containsExactly(
                    KeyEvent.KEYCODE_BUTTON_B,
                    KeyEvent.KEYCODE_BUTTON_X,
                    KeyEvent.KEYCODE_BUTTON_L1,
                    KeyEvent.KEYCODE_BUTTON_R2,
                )
            assertThat(shadowInputManager.getControllerButtonToAxisRemappings(device.identifier))
                .containsExactly(
                    KeyEvent.KEYCODE_BUTTON_A,
                    MotionEvent.AXIS_LTRIGGER,
                    KeyEvent.KEYCODE_BUTTON_Y,
                    MotionEvent.AXIS_RTRIGGER,
                )
            assertThat(inputManager.getControllerAxisRemappings(device.identifier))
                .containsExactly(MotionEvent.AXIS_Y, MotionEvent.AXIS_RZ)
            for ((key, nameResId) in GameControllerUtils.preferenceKeyToNameMap) {
                val preference = fragment.findPreference<Preference>(key)!!
                assertThat(preference.title).isEqualTo(context.getString(nameResId))
            }
            // Check that the mappings in the UI remained unchanged.
            val buttonBPreference = fragment.findPreference<Preference>("controller_button_b")!!
            assertThat(buttonBPreference.summary)
                .isEqualTo(context.getString(R.string.game_controller_button_x))
            val buttonL1Preference = fragment.findPreference<Preference>("controller_button_l1")!!
            assertThat(buttonL1Preference.summary)
                .isEqualTo(context.getString(R.string.game_controller_button_r2))
            val axisYPreference = fragment.findPreference<Preference>("controller_stick_right")!!
            assertThat(axisYPreference.summary)
                .isEqualTo(context.getString(R.string.game_controller_stick_right))
        }
    }

    @Test
    fun onLaunch_contentDescriptionIsSet_andUpdatesAfterRemapping() {
        val device = createFakeController(1, "Test Controller")
        shadowInputManager.addInputDevice(device)
        val scenario = launchFragment(device.identifier)

        scenario.onFragment { fragment ->
            val preference =
                fragment.findPreference<GameControllerRemappingPreference>("controller_button_a")!!
            // Check that content description is set correctly on launch
            assertThat(preference.summaryView!!.contentDescription)
                .isEqualTo(
                    context.getString(
                        R.string.game_controller_remapping_preference_content_description,
                        context.getString(R.string.game_controller_button_a),
                    )
                )

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
            ShadowLooper.idleMainLooper()

            assertThat(preference.summaryView!!.contentDescription)
                .isEqualTo(
                    context.getString(
                        R.string.game_controller_remapping_preference_content_description,
                        context.getString(R.string.game_controller_button_b),
                    )
                )
        }
    }

    private fun createFakeController(deviceId: Int, name: String): InputDevice {
        return InputDevice.Builder()
            .setSources(InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK)
            .setId(deviceId)
            .setName(name)
            .setDescriptor("device $deviceId")
            .addMotionRange(MotionEvent.AXIS_LTRIGGER, InputDevice.SOURCE_JOYSTICK, 0f, 1f, 0f, 0f, 0f)
            .addMotionRange(MotionEvent.AXIS_RTRIGGER, InputDevice.SOURCE_JOYSTICK, 0f, 1f, 0f, 0f, 0f)
            .build()
    }

    private fun createFakeControllerWithoutMotionRanges(): InputDevice {
        return InputDevice.Builder()
            .setSources(InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK)
            .setId(1)
            .setName("Remapping Controller")
            .setDescriptor("device 1")
            .build()
    }
}
