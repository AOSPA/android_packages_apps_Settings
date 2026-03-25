/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.connecteddevice

import android.bluetooth.BluetoothManager
import android.util.Log
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Disallowed
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean

// LINT.IfChange
@ProvidePreferenceScreen(BluetoothDashboardScreenApi.KEY)
class BluetoothDashboardScreenApi :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.CONNECTED_DEVICES,
        fragment = BluetoothDashboardFragment::class,
        purpose = R.string.bluetooth_switchbar_screen_purpose,
        alreadyPartiallyMigrated = BluetoothDashboardScreen::class,
    ) {

    init {
        flag { Flags.catalystMigration26q2() }

        preference(
            key = BLUETOOTH_AUTO_ON_TOGGLE_KEY,
            purpose = R.string.bluetooth_auto_on_toggle_purpose,
            type = AnyBoolean,
        ) {
            sensitivityLevel(SensitivityLevel.REQUIRES_CONFIRMATION)

            preconditions(R.string.bluetooth_auto_on_preconditions) {
                val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
                val bluetoothAdapter =
                    bluetoothManager?.adapter
                        ?: return@preconditions Disallowed(R.string.bluetooth_not_supported_error)

                try {
                    if (bluetoothAdapter.isAutoOnSupported) {
                        Allowed
                    } else {
                        Disallowed(R.string.bluetooth_auto_on_not_supported_error)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking if Auto-On is supported", e)
                    Disallowed(R.string.bluetooth_auto_on_hardware_check_failed_error)
                } catch (e: NoSuchMethodError) {
                    Log.e(TAG, "isAutoOnSupported method not found", e)
                    Disallowed(R.string.bluetooth_auto_on_api_missing_error)
                }
            }

            get {
                execute {
                    val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
                    val bluetoothAdapter = bluetoothManager?.adapter ?: return@execute false

                    bluetoothAdapter.isAutoOnEnabled
                }
            }

            set {
                execute { value ->
                    val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
                    val bluetoothAdapter = bluetoothManager?.adapter ?: return@execute

                    bluetoothAdapter.isAutoOnEnabled = value
                }
            }
        }
    }

    companion object {
        private const val TAG = "BluetoothDashboardApi"
        const val KEY = "api_bluetooth_switchbar_screen"
        const val BLUETOOTH_AUTO_ON_TOGGLE_KEY = "bluetooth_auto_on_settings_toggle"
    }
}
// LINT.ThenChange(BluetoothDashboardScreen.kt, BluetoothDashboardFragment.java)
