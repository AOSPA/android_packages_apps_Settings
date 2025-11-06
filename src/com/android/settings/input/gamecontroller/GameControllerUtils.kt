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
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.hardware.input.InputDeviceIdentifier
import android.hardware.input.InputManager
import android.util.Log
import android.view.InputDevice
import java.text.Collator

/** Utility class for game controller related settings pages */
@SuppressLint("MissingPermission")
object GameControllerUtils {
    const val TAG = "GameControllerSettings"
    const val EXTRA_INPUT_DEVICE_IDENTIFIER = "input_device_identifier"

    /**
     * Provides a list of connected game controllers, wrapped in a ControllerDevice class, sorted by
     * name and then by descriptor.
     */
    fun getGameControllers(context: Context): List<ControllerDevice> {
        if (!com.android.hardware.input.Flags.controllerRemapping()) {
            return emptyList()
        }
        val inputManager = context.getSystemService(InputManager::class.java) ?: return emptyList()
        val bluetoothManager =
            context.getSystemService(BluetoothManager::class.java) ?: return emptyList()
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
        val foundControllers = mutableListOf<ControllerDevice>()

        for (deviceId in inputManager.inputDeviceIds) {
            val device = inputManager.getInputDevice(deviceId)
            if (device == null || !device.isPhysicalDevice) {
                continue
            }
            val sources = device.sources
            val isGameController =
                ((sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) or
                    ((sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)
            if (isGameController) {
                // Try to find the corresponding BluetoothDevice
                val bluetoothDevice = findBluetoothDevice(device, bluetoothAdapter)
                // Add the new wrapper class to the list
                foundControllers.add(ControllerDevice(device, bluetoothDevice))
            }
        }
        foundControllers.sortedWith(
            compareBy(Collator.getInstance()) { it: ControllerDevice -> it.name }
        )
        return foundControllers
    }

    /** Finds the BluetoothDevice corresponding to an InputDevice. */
    @SuppressLint("MissingPermission")
    private fun findBluetoothDevice(
        inputDevice: InputDevice,
        bluetoothAdapter: BluetoothAdapter?,
    ): BluetoothDevice? {
        if (bluetoothAdapter == null) {
            Log.w(TAG, "Cannot find BluetoothDevice, adapter is null.")
            return null
        }
        val macAddress = inputDevice.bluetoothAddress ?: return null

        return try {
            bluetoothAdapter.getRemoteDevice(macAddress)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid Bluetooth address: $macAddress", e)
            null
        }
    }

    /**
     * A data class to hold both the InputDevice and its associated BluetoothDevice. The
     * bluetoothDevice is nullable, as a controller might be connected via USB.
     */
    data class ControllerDevice(
        val name: String,
        val inputDeviceId: Int,
        val inputDeviceIdentifier: InputDeviceIdentifier,
        val bluetoothAddress: String?,
        val sources: Int
    ) {
        constructor(
            inputDevice: InputDevice,
            bluetoothDevice: BluetoothDevice?,
        ) : this(
            name = bluetoothDevice?.name ?: inputDevice.name,
            inputDeviceId = inputDevice.id,
            inputDeviceIdentifier = inputDevice.identifier,
            bluetoothAddress = inputDevice.bluetoothAddress,
            sources = inputDevice.sources
        )
    }
}
