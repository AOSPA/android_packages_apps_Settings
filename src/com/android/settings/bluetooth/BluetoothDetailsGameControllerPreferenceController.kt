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

package com.android.settings.bluetooth

import android.content.Context
import android.content.Intent
import android.hardware.input.InputManager
import android.provider.Settings
import android.view.InputDevice
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settingslib.bluetooth.BluetoothUtils
import com.android.settingslib.bluetooth.CachedBluetoothDevice
import com.android.settingslib.core.AbstractPreferenceController
import com.android.settingslib.core.lifecycle.Lifecycle
import com.android.settingslib.core.lifecycle.LifecycleObserver
import com.android.settingslib.core.lifecycle.events.OnPause
import com.android.settingslib.core.lifecycle.events.OnResume

class BluetoothDetailsGameControllerPreferenceController(
    context: Context,
    private val cachedDevice: CachedBluetoothDevice,
    val lifecycle: Lifecycle,
) :
    AbstractPreferenceController(context),
    Preference.OnPreferenceClickListener,
    InputManager.InputDeviceListener,
    LifecycleObserver,
    OnResume,
    OnPause {

    init {
        lifecycle.addObserver(this)
    }

    val inputManager = mContext.getSystemService(InputManager::class.java)
    var preferencesContainer: PreferenceGroup? = null
    var inputDevice: InputDevice? = BluetoothUtils.getInputDevice(mContext, cachedDevice.address)

    override fun isAvailable(): Boolean {
        if (!com.android.hardware.input.Flags.controllerRemapping()) {
            return false
        }
        return isGameController(inputDevice)
    }

    override fun displayPreference(screen: PreferenceScreen) {
        preferencesContainer = screen.findPreference(preferenceKey)
        super.displayPreference(screen)
        refresh()
    }

    override fun getPreferenceKey(): String = KEY_GAME_CONTROLLER_SETTINGS

    override fun onPreferenceClick(preference: Preference): Boolean {
        if (preference.key == KEY_GAME_CONTROLLER_SETTINGS_PREF) {
            val intent =
                Intent(Settings.ACTION_GAME_CONTROLLER_SETTINGS).apply {
                    `package` = mContext.packageName
                    inputDevice?.let {
                        putExtra(Settings.EXTRA_INPUT_DEVICE_IDENTIFIER, it.identifier)
                    }
                }
            mContext.startActivity(intent)
            return true
        }
        return false
    }

    override fun onResume() {
        inputDevice = BluetoothUtils.getInputDevice(mContext, cachedDevice.address)
        inputManager?.registerInputDeviceListener(this, null)
        refresh()
    }

    override fun onPause() {
        inputManager?.unregisterInputDeviceListener(this)
    }

    private fun refresh() {
        val container = preferencesContainer ?: return
        val currSettingsPref: Preference? =
            container.findPreference(KEY_GAME_CONTROLLER_SETTINGS_PREF)
        if (isAvailable) {
            if (currSettingsPref == null) {
                val pref = Preference(mContext)
                pref.setKey(KEY_GAME_CONTROLLER_SETTINGS_PREF)
                pref.setTitle(R.string.bluetooth_device_game_controller_settings_preference_title)
                pref.onPreferenceClickListener = this
                pref.isEnabled = true
                preferencesContainer!!.addPreference(pref)
            }
        } else {
            container.removeAll()
        }
    }

    fun isGameController(inputDevice: InputDevice?): Boolean {
        if (inputDevice == null) {
            return false
        }
        val sources = inputDevice.sources
        return ((sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) or
            ((sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)
    }

    override fun onInputDeviceAdded(deviceId: Int) {}

    override fun onInputDeviceRemoved(deviceId: Int) {
        if (inputDevice?.id == deviceId) {
            inputDevice = null
        }
        refresh()
    }

    override fun onInputDeviceChanged(deviceId: Int) {}

    companion object {
        const val KEY_GAME_CONTROLLER_SETTINGS = "game_controller_settings"
        const val KEY_GAME_CONTROLLER_SETTINGS_PREF = "game_controller_settings_pref"
    }
}
