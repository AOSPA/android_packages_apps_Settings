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

import android.annotation.SuppressLint
import android.app.Application
import android.hardware.input.InputDeviceIdentifier
import android.hardware.input.InputManager
import android.hardware.input.KeyGlyphMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.settings.input.gamecontroller.GameControllerUtils.preferenceKeyToAxesMap
import com.android.settings.input.gamecontroller.GameControllerUtils.preferenceKeyToButtonMap
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import android.view.KeyEvent
import android.view.MotionEvent

/**
 * View model for per device game controller settings pages, managing data for a particular
 * controller device.
 */
@SuppressLint("MissingPermission")
class GameControllerViewModel(
    application: Application,
    private val identifier: InputDeviceIdentifier,
) : AndroidViewModel(application), InputManager.InputDeviceListener {

    private val inputManager = application.getSystemService(InputManager::class.java)!!
    val controllerDevice: GameControllerUtils.ControllerDevice =
        GameControllerUtils.getControllerDeviceFromIdentifier(application, identifier)
            ?: throw IllegalStateException("ControllerDevice could not be created for identifier.")

    val glyphMap: KeyGlyphMap? = inputManager.getKeyGlyphMap(controllerDevice.inputDeviceId)

    // Holds the current state of all button remappings.
    private val _buttonRemapping = MutableLiveData<Map<Int, Int>>()
    val buttonRemapping: LiveData<Map<Int, Int>> = _buttonRemapping

    // Holds the current state of all axis remappings.
    private val _axisRemapping = MutableLiveData<Map<Int, Int>>()
    val axisRemapping: LiveData<Map<Int, Int>> = _axisRemapping

    // Emits requests to listeners to show the remapping dialog
    private val _showRemappingDialog =
        MutableSharedFlow<RemappingDialogRequest>(replay = 0, extraBufferCapacity = 1)
    val showRemappingDialog = _showRemappingDialog.asSharedFlow()

    private val _onDeviceDisconnected = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val onDeviceDisconnected = _onDeviceDisconnected.asSharedFlow()

    init {
        loadInitialRemapping()
        inputManager.registerInputDeviceListener(this, null)
    }

    fun loadInitialRemapping() {
        _buttonRemapping.value = inputManager.getControllerButtonRemappings(identifier)
        _axisRemapping.value = inputManager.getControllerAxisRemappings(identifier)
    }

    override fun onInputDeviceRemoved(deviceId: Int) {
        if (controllerDevice.inputDeviceId == deviceId) {
            _onDeviceDisconnected.tryEmit(Unit)
        }
    }

    override fun onInputDeviceAdded(deviceId: Int) {}

    override fun onInputDeviceChanged(deviceId: Int) {}

    override fun onCleared() {
        super.onCleared()
        inputManager.unregisterInputDeviceListener(this)
    }

    fun requestRemappingDialog(fromPreferenceKey: String, toPreferenceKey: String) {
        _showRemappingDialog.tryEmit(RemappingDialogRequest(fromPreferenceKey, toPreferenceKey))
    }

    fun applyRemapping(fromPreferenceKey: String, toPreferenceKey: String) {
        if (preferenceKeyToButtonMap.containsKey(fromPreferenceKey)) {
            val fromButton = preferenceKeyToButtonMap[fromPreferenceKey]!!
            val toButton = preferenceKeyToButtonMap[toPreferenceKey]!!
            applyKeyRemapping(fromButton, toButton)
        } else if (preferenceKeyToAxesMap.containsKey(fromPreferenceKey)) {
            val fromAxes = preferenceKeyToAxesMap[fromPreferenceKey]!!
            val toAxes = preferenceKeyToAxesMap[toPreferenceKey]!!
            // Remap each axis in the stick individually.
            fromAxes.forEachIndexed { index, fromAxis ->
                applyAxisRemapping(fromAxis, toAxes[index])
            }
        }
    }

    private fun getAxisForButton(buttonKey: Int): Int? {
        return when (buttonKey) {
            KeyEvent.KEYCODE_BUTTON_L2 -> MotionEvent.AXIS_LTRIGGER
            KeyEvent.KEYCODE_BUTTON_R2 -> MotionEvent.AXIS_RTRIGGER
            else -> null
        }.takeIf { controllerDevice.axes.contains(it) }
    }

    private fun applyKeyRemapping(fromButtonKey: Int, toButtonKey: Int) {
        val fromAxis = getAxisForButton(fromButtonKey)
        val toAxis = getAxisForButton(toButtonKey)

        // Remove any existing axis remappings for the "from" button.
        inputManager.removeControllerButtonToAxisRemapping(identifier, fromButtonKey);

        if (fromAxis != null && toAxis != null) {
            // Both buttons produce motion events, remap the axis directly.
            // E.g. if L2 is mapped to R2, then L2 should produce RTRIGGER axis events.
            inputManager.remapControllerAxis(identifier, fromAxis, toAxis)
        } else if (fromAxis != null) {
            // The "from" button produces motion events, disable those events.
            inputManager.remapControllerAxis(identifier, fromAxis, MotionEvent.AXIS_DISABLED)
        } else if (toAxis != null) {
            // Make sure the "from" button produces motion events which the "to" button does.
            inputManager.remapControllerButtonToAxis(identifier, fromButtonKey, toAxis)
        } else {
            // Neither of the buttons produce motion events, no special handling required.
        }

        inputManager.remapControllerButton(identifier, fromButtonKey, toButtonKey)
        _buttonRemapping.postValue(inputManager.getControllerButtonRemappings(identifier))
    }

    private fun applyAxisRemapping(fromAxis: Int, toAxis: Int) {
        inputManager.remapControllerAxis(identifier, fromAxis, toAxis)
        _axisRemapping.postValue(inputManager.getControllerAxisRemappings(identifier))
    }

    fun resetRemapping() {
        inputManager.clearAllControllerButtonRemappings(identifier)
        _buttonRemapping.postValue(inputManager.getControllerButtonRemappings(identifier))

        inputManager.clearAllControllerButtonToAxisRemappings(identifier)

        inputManager.clearAllControllerAxisRemappings(identifier)
        _axisRemapping.postValue(inputManager.getControllerAxisRemappings(identifier))
    }

    data class RemappingDialogRequest(val fromPreferenceKey: String, val toPreferenceKey: String)
}
