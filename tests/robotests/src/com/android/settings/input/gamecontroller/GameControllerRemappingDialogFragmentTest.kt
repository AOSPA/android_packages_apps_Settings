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
import android.content.Context
import android.hardware.input.InputManager
import android.os.Bundle
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.view.InputDevice
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragment
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.input.gamecontroller.GameControllerUtils.preferenceKeyToAxesMap
import com.android.settings.input.gamecontroller.GameControllerUtils.preferenceKeyToButtonMap
import com.android.settings.input.gamecontroller.GameControllerUtils.preferenceKeyToNameMap
import com.android.settings.testutils.shadow.ShadowInputManager
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/** Tests for [GameControllerRemappingDialogFragment]. */
@RunWith(AndroidJUnit4::class)
@EnableFlags(com.android.hardware.input.Flags.FLAG_CONTROLLER_REMAPPING)
@Config(shadows = [ShadowInputManager::class])
class GameControllerRemappingDialogFragmentTest {

    @get:Rule val setFlagsRule = SetFlagsRule()

    private lateinit var inputManager: InputManager
    private lateinit var shadowInputManager: ShadowInputManager
    private lateinit var context: Context
    private lateinit var viewModel: GameControllerViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        inputManager = context.getSystemService(InputManager::class.java)
        shadowInputManager = shadowOf(inputManager) as ShadowInputManager

        val device = createGameController(deviceId = 1, name = "Test Controller")
        shadowInputManager.addInputDevice(device)

        viewModel =
            GameControllerViewModel(context.applicationContext as Application, device.identifier)
    }

    @Test
    fun onLaunch_forButtonRemapping_showsCorrectNumberOfItems() {
        val fromKey = "controller_button_a"
        val toKey = "controller_button_b"
        val scenario = launchFragment<Fragment>(themeResId = R.style.Theme_Settings)

        scenario.onFragment { fragment ->
            val dialog = GameControllerRemappingDialogFragment(viewModel)
            dialog.arguments =
                Bundle().apply {
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_FROM_PREFERENCE_KEY,
                        fromKey,
                    )
                    putString(GameControllerRemappingDialogFragment.ARGS_TO_PREFERENCE_KEY, toKey)
                }

            dialog.show(fragment.childFragmentManager, "TestDialog")
            ShadowLooper.idleMainLooper() // Ensure the dialog is shown

            val recyclerView = dialog.dialog!!.findViewById<RecyclerView>(R.id.recycler_view)
            val adapter = recyclerView.adapter

            // Verify only button preferences are shown by checking the item count.
            assertThat(adapter!!.itemCount).isEqualTo(preferenceKeyToButtonMap.size)
        }
    }

    @Test
    fun onLaunch_forAxisRemapping_showsCorrectNumberOfItems() {
        val fromKey = "controller_stick_left"
        val toKey = "controller_stick_right"
        val scenario = launchFragment<Fragment>(themeResId = R.style.Theme_Settings)

        scenario.onFragment { fragment ->
            val dialog = GameControllerRemappingDialogFragment(viewModel)
            dialog.arguments =
                Bundle().apply {
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_FROM_PREFERENCE_KEY,
                        fromKey,
                    )
                    putString(GameControllerRemappingDialogFragment.ARGS_TO_PREFERENCE_KEY, toKey)
                }

            dialog.show(fragment.childFragmentManager, "TestDialog")
            ShadowLooper.idleMainLooper() // Ensure the dialog is shown

            val recyclerView = dialog.dialog!!.findViewById<RecyclerView>(R.id.recycler_view)
            val adapter = recyclerView.adapter

            // Verify only axis preferences are shown by checking the item count.
            assertThat(adapter!!.itemCount).isEqualTo(preferenceKeyToAxesMap.size)
        }
    }

    @Test
    fun onItemClick_setsFragmentResultAndDismisses() {
        val fromKey = "controller_button_a"
        val toKey = "controller_button_b"
        val newSelectionKey = "controller_button_x"
        var receivedResult: Bundle? = null
        val scenario = launchFragment<Fragment>(themeResId = R.style.Theme_Settings)

        scenario.onFragment { fragment ->
            val dialog = GameControllerRemappingDialogFragment(viewModel)
            dialog.arguments =
                Bundle().apply {
                    putString(
                        GameControllerRemappingDialogFragment.ARGS_FROM_PREFERENCE_KEY,
                        fromKey,
                    )
                    putString(GameControllerRemappingDialogFragment.ARGS_TO_PREFERENCE_KEY, toKey)
                }

            fragment.childFragmentManager.setFragmentResultListener(
                GameControllerRemappingDialogFragment.REQUEST_REMAPPING,
                fragment,
            ) { _, bundle ->
                receivedResult = bundle
            }

            dialog.show(fragment.childFragmentManager, "TestDialog")
            ShadowLooper.idleMainLooper()

            val recyclerView = dialog.dialog!!.findViewById<RecyclerView>(R.id.recycler_view)
            val adapter = recyclerView.adapter
            val expectedText =
                fragment.requireContext().getString(preferenceKeyToNameMap[newSelectionKey]!!)

            // Find the ViewHolder by its text content and click it.
            for (i in 0 until adapter!!.itemCount) {
                val holder = recyclerView.findViewHolderForAdapterPosition(i)
                if (holder != null) {
                    val titleView = holder.itemView.findViewById<TextView>(R.id.preference_text)
                    if (titleView.text.toString() == expectedText) {
                        holder.itemView.performClick()
                        break
                    }
                }
            }
        }
        ShadowLooper.idleMainLooper()

        assertThat(receivedResult).isNotNull()
        assertThat(
                receivedResult!!.getString(
                    GameControllerRemappingDialogFragment.ARGS_FROM_PREFERENCE_KEY
                )
            )
            .isEqualTo(fromKey)
        assertThat(
                receivedResult.getString(
                    GameControllerRemappingDialogFragment.ARGS_TO_PREFERENCE_KEY
                )
            )
            .isEqualTo(newSelectionKey)

        // Verify the dialog was dismissed
        scenario.onFragment { fragment ->
            val dialog =
                fragment.childFragmentManager.findFragmentByTag("TestDialog")
                    as GameControllerRemappingDialogFragment?
            assertThat(dialog?.dialog?.isShowing ?: false).isFalse()
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
