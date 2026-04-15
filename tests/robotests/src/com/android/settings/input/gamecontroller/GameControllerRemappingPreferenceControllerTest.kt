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
import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.hardware.input.InputDeviceIdentifier
import android.hardware.input.InputManager
import android.hardware.input.KeyGlyphMap
import android.util.SparseArray
import android.util.SparseIntArray
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
import com.android.settings.testutils.shadow.ShadowInputManager
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
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Tests for [GameControllerRemappingPreferenceController]. */
@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowInputManager::class])
@SuppressLint("MissingPermission")
class GameControllerRemappingPreferenceControllerTest {

    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock private lateinit var screen: PreferenceScreen

    private lateinit var context: Context
    private lateinit var application: Application
    private lateinit var shadowInputManager: ShadowInputManager
    private lateinit var viewModel: GameControllerViewModel
    private lateinit var controller: GameControllerRemappingPreferenceController
    private lateinit var identifier: InputDeviceIdentifier
    private lateinit var preferences: Map<String, Preference>

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        application = context.applicationContext as Application

        val inputManager = context.getSystemService(InputManager::class.java)
        shadowInputManager = shadowOf(inputManager) as ShadowInputManager

        preferences =
            preferenceKeyToNameMap.keys.associateWith { key ->
                GameControllerRemappingPreference(context, null).apply { this.key = key }
            }
        whenever(screen.findPreference<Preference>(any())).thenAnswer { invocation ->
            val key = invocation.arguments[0] as String
            preferences[key]
        }
    }

    private fun setupController(
        device: InputDevice,
        buttonRemapping: Map<Int, Int> = mapOf(),
        axisRemapping: Map<Int, Int> = mapOf(),
    ) {
        identifier = device.identifier
        shadowInputManager.addInputDevice(device)

        shadowInputManager.clearAllControllerButtonRemappings(identifier)
        shadowInputManager.clearAllControllerAxisRemappings(identifier)

        val inputManager = context.getSystemService(InputManager::class.java)
        buttonRemapping.forEach { (from, to) ->
            inputManager.remapControllerButton(identifier, from, to)
        }
        axisRemapping.forEach { (from, to) ->
            inputManager.remapControllerAxis(identifier, from, to)
        }

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

        preferenceKeyToButtonMap.keys.forEach { assertThat(preferences[it]!!.isVisible).isTrue() }
        preferenceKeyToAxesMap.keys.forEach { assertThat(preferences[it]!!.isVisible).isTrue() }
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

        preferenceKeyToButtonMap.keys.forEach { assertThat(preferences[it]!!.isVisible).isTrue() }
        preferenceKeyToAxesMap.keys.forEach { assertThat(preferences[it]!!.isVisible).isFalse() }
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

        preferenceKeyToButtonMap.keys.forEach { assertThat(preferences[it]!!.isVisible).isFalse() }
        preferenceKeyToAxesMap.keys.forEach { assertThat(preferences[it]!!.isVisible).isTrue() }
    }

    @Test
    fun displayPreference_withGlyphMap_setsIconAndTitle() {
        val key = preferenceKeyToButtonMap.keys.first()
        val keycode = preferenceKeyToButtonMap[key]!!
        val glyphMap = createGlyphMapForKey(keycode, android.R.drawable.ic_delete, "Custom Button")

        val device = createGameController(deviceId = 1, name = "Test Controller")
        shadowInputManager.setKeyGlyphMap(device.id, glyphMap)

        setupController(device = device)

        val pref = preferences[key]!!
        assertThat(pref.icon).isNotNull()
        assertThat(shadowOf(pref.icon).createdFromResId).isEqualTo(android.R.drawable.ic_delete)
        assertThat(pref.title).isEqualTo("Custom Button")
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
            assertThat(preferences[fromKey]!!.summary)
                .isEqualTo(context.getString(preferenceKeyToNameMap[toKey]!!))
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
            assertThat(preferences[fromKey]!!.summary)
                .isEqualTo(context.getString(preferenceKeyToNameMap[toKey]!!))
        }
    }

    @Test
    fun updateState_withDefaultMapping_setsCorrectSummary() {
        // Simulate default mappings (empty maps)
        setupController(device = createGameController(deviceId = 1, name = "Test Controller"))

        controller.updateState(screen)

        // Verify that all preferences have their default summary
        preferences.forEach { (key, pref) ->
            assertThat(pref.summary).isEqualTo(context.getString(preferenceKeyToNameMap[key]!!))
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
                val handled = controller.handlePreferenceTreeClick(preferences[fromKey]!!)
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

    private fun createGlyphMapForKey(keycode: Int, glyph: Int, displayName: String): KeyGlyphMap {
        val componentName =
            ComponentName(context.packageName, "com.android.settings.test.DummyReceiver")

        val activityInfo = ActivityInfo()
        activityInfo.packageName = componentName.packageName
        activityInfo.name = componentName.className
        activityInfo.applicationInfo = context.applicationInfo

        val pm = shadowOf(context.packageManager)

        val packageInfo = PackageInfo()
        packageInfo.packageName = context.packageName
        packageInfo.applicationInfo = context.applicationInfo
        packageInfo.receivers = arrayOf(activityInfo)
        pm.installPackage(packageInfo)

        pm.addOrUpdateReceiver(activityInfo)

        val keyGlyphs = SparseIntArray()
        keyGlyphs.put(keycode, glyph)

        val keyNames = SparseArray<String>()
        keyNames.put(keycode, displayName)

        return KeyGlyphMap(
            componentName,
            keyGlyphs,
            keyNames,
            SparseIntArray(),
            intArrayOf(),
            emptyMap(),
        )
    }
}
