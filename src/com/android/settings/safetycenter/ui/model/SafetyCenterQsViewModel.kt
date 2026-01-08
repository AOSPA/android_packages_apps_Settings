/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.settings.safetycenter.ui.model

import android.Manifest.permission_group.CAMERA
import android.Manifest.permission_group.LOCATION
import android.Manifest.permission_group.MICROPHONE
import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.SensorPrivacyManager
import android.hardware.SensorPrivacyManager.Sensors
import android.hardware.SensorPrivacyManager.TOGGLE_TYPE_SOFTWARE
import android.location.LocationManager
import android.os.Process
import android.os.UserManager
import android.provider.DeviceConfig
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.settingslib.RestrictedLockUtils
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin

/**
 * Handles toggling sensors like camera, microphone and location access from the Safety Center quick
 * settings screen.
 */
// The Settings app has access to all needed sensor permissions.
@SuppressLint("MissingPermission")
class SafetyCenterQsViewModel(private val app: Application) : AndroidViewModel(app) {
    private val sensorPrivacyManager: SensorPrivacyManager =
        app.getSystemService(SensorPrivacyManager::class.java)!!
    private val locationManager: LocationManager =
        app.getSystemService(LocationManager::class.java)!!
    private val userManager: UserManager = app.getSystemService(UserManager::class.java)!!
    val sensorPrivacyLiveData: LiveData<Map<String, SensorState>> by this::_sensorPrivacyLiveData
    private val _sensorPrivacyLiveData = SensorStateLiveData()

    /** Toggles the enabled state of the provided sensor (camera / microphone / location). */
    fun toggleSensor(groupName: String) {
        when (groupName) {
            MICROPHONE -> {
                val isEnabled =
                    sensorPrivacyManager.isSensorPrivacyEnabled(
                        TOGGLE_TYPE_SOFTWARE,
                        Sensors.MICROPHONE,
                    )
                sensorPrivacyManager.setSensorPrivacy(Sensors.MICROPHONE, !isEnabled)
            }
            CAMERA -> {
                val isEnabled =
                    sensorPrivacyManager.isSensorPrivacyEnabled(
                        TOGGLE_TYPE_SOFTWARE,
                        Sensors.CAMERA,
                    )
                sensorPrivacyManager.setSensorPrivacy(Sensors.CAMERA, !isEnabled)
            }
            LOCATION -> {
                val isEnabled = locationManager.isLocationEnabledForUser(Process.myUserHandle())
                locationManager.setLocationEnabledForUser(!isEnabled, Process.myUserHandle())
            }
        }
    }

    /** Describes the current state of a sensor (e.g. camera / mic / location sensor). */
    data class SensorState(val visible: Boolean, val enabled: Boolean, val admin: EnforcedAdmin?)

    inner class SensorStateLiveData :
        MutableLiveData<Map<String, SensorState>>(),
        SensorPrivacyManager.OnSensorPrivacyChangedListener {

        fun updateState() {
            val locationEnabled = locationManager.isLocationEnabledForUser(Process.myUserHandle())
            val locationEnforcedAdmin =
                getEnforcedAdmin(UserManager.DISALLOW_SHARE_LOCATION)
                    ?: getEnforcedAdmin(UserManager.DISALLOW_CONFIG_LOCATION)

            val newValue =
                mapOf(
                    CAMERA to
                        getSensorState(
                            Sensors.CAMERA,
                            UserManager.DISALLOW_CAMERA_TOGGLE,
                            CONFIG_CAMERA_TOGGLE_ENABLED,
                        ),
                    MICROPHONE to
                        getSensorState(
                            Sensors.MICROPHONE,
                            UserManager.DISALLOW_MICROPHONE_TOGGLE,
                            CONFIG_MIC_TOGGLE_ENABLED,
                        ),
                    LOCATION to SensorState(true, locationEnabled, locationEnforcedAdmin),
                )

            if (value != newValue) {
                value = newValue
            }
        }

        @Deprecated("Use onSensorPrivacyChanged(SensorPrivacyChangedParams)")
        override fun onSensorPrivacyChanged(sensor: Int, enabled: Boolean) {
            updateState()
        }

        override fun onActive() {
            super.onActive()
            sensorPrivacyManager.addSensorPrivacyListener(Sensors.CAMERA, this)
            sensorPrivacyManager.addSensorPrivacyListener(Sensors.MICROPHONE, this)

            val locationStateChangeFilter = IntentFilter(LocationManager.MODE_CHANGED_ACTION)
            app.applicationContext.registerReceiver(locationModeReceiver, locationStateChangeFilter)

            updateState()
        }

        override fun onInactive() {
            super.onInactive()
            sensorPrivacyManager.removeSensorPrivacyListener(Sensors.CAMERA, this)
            sensorPrivacyManager.removeSensorPrivacyListener(Sensors.MICROPHONE, this)

            app.applicationContext.unregisterReceiver(locationModeReceiver)
        }

        private val locationModeReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == LocationManager.MODE_CHANGED_ACTION) {
                        updateState()
                    }
                }
            }
    }

    private fun getSensorState(
        sensor: Int,
        restriction: String,
        enableConfig: String,
    ): SensorState {
        val sensorConfigEnabled =
            DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_PRIVACY, enableConfig, true)
        return SensorState(
            sensorConfigEnabled && sensorPrivacyManager.supportsSensorToggle(sensor),
            !sensorPrivacyManager.isSensorPrivacyEnabled(TOGGLE_TYPE_SOFTWARE, sensor),
            getEnforcedAdmin(restriction),
        )
    }

    private fun getEnforcedAdmin(restriction: String) =
        if (
            userManager.getUserRestrictionSources(restriction, Process.myUserHandle()).isNotEmpty()
        ) {
            RestrictedLockUtils.getProfileOrDeviceOwner(app, Process.myUserHandle())
        } else {
            null
        }

    companion object {
        // Device config property name for the microphone sensor.
        private const val CONFIG_MIC_TOGGLE_ENABLED = "mic_toggle_enabled"
        // Device config property name for the camera sensor.
        private const val CONFIG_CAMERA_TOGGLE_ENABLED = "camera_toggle_enabled"
    }
}

/**
 * Factory for a SafetyCenterQsViewModel
 *
 * @param app The current application
 */
class SafetyCenterQsViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SafetyCenterQsViewModel(app) as T
    }
}
