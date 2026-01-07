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

package com.android.settings.network.telephony.satellite.quicksettings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.settings.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the satellite landing page, responsible for preparing and managing the data to be
 * displayed.
 */
class SatelliteLandingPageViewModel(
    private val context: Context,
    private val appsRepository: SatelliteAppsRepository,
    private val packageManager: PackageManager,
    satelliteStateRepository: SatelliteStateRepository,
) : ViewModel() {

    private val _satelliteAppItems = MutableStateFlow<List<SatelliteAppItem>>(emptyList())
    val satelliteAppItems: StateFlow<List<SatelliteAppItem>> = _satelliteAppItems

    /**
     * Whether the satellite apps should be enabled (clickable and fully opaque).
     *
     * Apps are enabled if satellite connectivity is either [SatelliteStatus.ACTIVE] or
     * [SatelliteStatus.AVAILABLE].
     */
    val areAppsEnabled: StateFlow<Boolean> =
        satelliteStateRepository.satelliteStatus
            .map { it != SatelliteStatus.NOT_AVAILABLE }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    fun loadSatelliteAppItems() {
        val items = mutableListOf<SatelliteAppItem>()

        // Helper to create and add an item, reducing boilerplate.
        fun addItem(packageName: String, intent: Intent?, appLabel: String? = null) {
            createSatelliteAppItem(packageName, intent, appLabel)?.let { items.add(it) }
        }

        // Emergency SOS app
        addItem(
            packageName = SatelliteAppsRepository.PACKAGE_NAME_SAFETY_HUB,
            intent = appsRepository.getEmergencySosIntent(),
            appLabel = context.getString(R.string.satellite_emergency_sos),
        )

        // Configurable apps based on NTN support
        val appPackages =
            if (SatelliteUtils.isLteBasedNtnSupportedByDevice(context)) {
                appsRepository.getAppsPackagesForLteLandingPage()
            } else {
                appsRepository.getAppsPackagesForNbNtnLandingPage()
            }
        appPackages.forEach { packageName ->
            addItem(packageName, packageManager.getLaunchIntentForPackage(packageName))
        }

        // Settings app
        addItem(
            packageName = SatelliteAppsRepository.PACKAGE_NAME_SETTINGS,
            intent = appsRepository.getSettingsIntent(),
        )

        _satelliteAppItems.value = items
    }

    private fun createSatelliteAppItem(
        packageName: String,
        intent: Intent?,
        appLabel: String? = null,
    ): SatelliteAppItem? {
        intent ?: return null
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            SatelliteAppItem(appInfo, intent, appLabel)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "App not found: $packageName")
            null
        }
    }

    companion object {
        private const val TAG = "SatelliteLandingPageVM"
    }
}

/**
 * Factory for creating a [SatelliteLandingPageViewModel] with a constructor that takes a
 * [SatelliteAppsRepository].
 */
class SatelliteLandingPageViewModelFactory(
    private val context: Context,
    private val appsRepository: SatelliteAppsRepository,
    private val packageManager: PackageManager,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SatelliteLandingPageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SatelliteLandingPageViewModel(
                context,
                appsRepository,
                packageManager,
                SatelliteStateRepository.getInstance(context),
            )
                as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
