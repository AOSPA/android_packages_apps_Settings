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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.preference.PreferenceCategory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.testutils.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import java.lang.reflect.Field
import kotlin.reflect.KClass
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever

/** Tests for [GameControllerListFragment]. */
@RunWith(AndroidJUnit4::class)
@EnableFlags(com.android.hardware.input.Flags.FLAG_CONTROLLER_REMAPPING)
class GameControllerListFragmentTest {

    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @get:Rule val setFlagsRule = SetFlagsRule()

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock private lateinit var inputManager: InputManager

    private lateinit var application: Application

    @Before
    fun setUp() {
        application = spy(ApplicationProvider.getApplicationContext<Application>())
        whenever(application.getSystemService(InputManager::class.java)).thenReturn(inputManager)
    }

    private fun launchFragmentWithViewModel(
        viewModel: GameControllerListViewModel
    ): FragmentScenario<GameControllerListFragment> {
        val viewModelFactory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: KClass<T>,
                    extras: CreationExtras,
                ): T {
                    return viewModel as T
                }
            }

        val fragmentFactory =
            object : FragmentFactory() {
                override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                    val fragment = super.instantiate(classLoader, className)
                    if (fragment is GameControllerListFragment) {
                        val factoryField: Field =
                            Fragment::class.java.getDeclaredField("mDefaultFactory")
                        factoryField.isAccessible = true
                        factoryField.set(fragment, viewModelFactory)
                    }
                    return fragment
                }
            }

        return launchFragmentInContainer(
            factory = fragmentFactory,
            themeResId = R.style.Theme_Settings,
        )
    }

    @Test
    fun withControllers_preferencesAreAdded() {
        val device1 = createFakeController(1, "Controller A")
        val device2 = createFakeController(2, "Controller B")
        whenever(inputManager.inputDeviceIds).thenReturn(intArrayOf(1, 2))
        whenever(inputManager.getInputDevice(1)).thenReturn(device1)
        whenever(inputManager.getInputDevice(2)).thenReturn(device2)
        val scenario = launchFragmentWithViewModel(GameControllerListViewModel(application))

        scenario.onFragment { fragment ->
            val preferenceScreen = fragment.preferenceScreen
            assertThat(preferenceScreen.preferenceCount).isEqualTo(1)

            val category = preferenceScreen.getPreference(0) as PreferenceCategory
            assertThat(category.preferenceCount).isEqualTo(2)
            assertThat(category.getPreference(0).title).isEqualTo("Controller A")
            assertThat(category.getPreference(1).title).isEqualTo("Controller B")
        }
    }

    @Test
    fun withEmptyControllers_activityFinishes() {
        whenever(inputManager.inputDeviceIds).thenReturn(intArrayOf())
        val scenario = launchFragmentWithViewModel(GameControllerListViewModel(application))

        scenario.onFragment { fragment ->
            assertThat(fragment.requireActivity().isFinishing).isTrue()
        }
    }

    @Test
    fun onControllerAdded_updatesPreferenceList() {
        val device = createFakeController(1, "Controller A")
        whenever(inputManager.inputDeviceIds).thenReturn(intArrayOf(1))
        whenever(inputManager.getInputDevice(1)).thenReturn(device)
        val viewModel = GameControllerListViewModel(application)
        val scenario = launchFragmentWithViewModel(viewModel)

        val newDevice = createFakeController(2, "New Controller")
        whenever(inputManager.inputDeviceIds).thenReturn(intArrayOf(1, 2))
        whenever(inputManager.getInputDevice(2)).thenReturn(newDevice)
        viewModel.onInputDeviceAdded(2)

        scenario.onFragment { fragment ->
            val preferenceScreen = fragment.preferenceScreen
            val category = preferenceScreen.getPreference(0) as PreferenceCategory
            assertThat(category.preferenceCount).isEqualTo(2)
            assertThat(category.getPreference(0).title).isEqualTo("Controller A")
            assertThat(category.getPreference(1).title).isEqualTo("New Controller")
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
