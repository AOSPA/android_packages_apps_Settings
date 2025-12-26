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

import android.app.Application
import android.hardware.input.InputManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/** ViewModel for settings pages and preferences managing all connected game controllers. */
class GameControllerListViewModel(application: Application) :
    AndroidViewModel(application), InputManager.InputDeviceListener {

    private val inputManager = application.getSystemService(InputManager::class.java)!!

    // Holds the current list of connected controllers.
    private val _controllers = MutableLiveData<List<GameControllerUtils.ControllerDevice>>()
    val controllers: LiveData<List<GameControllerUtils.ControllerDevice>> = _controllers

    init {
        inputManager.registerInputDeviceListener(this, null)
        updateControllerList()
    }

    override fun onCleared() {
        super.onCleared()
        inputManager.unregisterInputDeviceListener(this)
    }

    override fun onInputDeviceAdded(deviceId: Int) {
        updateControllerList()
    }

    override fun onInputDeviceRemoved(deviceId: Int) {
        updateControllerList()
    }

    override fun onInputDeviceChanged(deviceId: Int) {
        // Due to the way devices are merged, "button" or "joystick" device might be added
        // afterwards in an onInputDeviceChanged callback
        updateControllerList()
    }

    private fun updateControllerList() {
        val connectedControllers = GameControllerUtils.getGameControllers(getApplication())
        _controllers.postValue(connectedControllers)
    }
}
