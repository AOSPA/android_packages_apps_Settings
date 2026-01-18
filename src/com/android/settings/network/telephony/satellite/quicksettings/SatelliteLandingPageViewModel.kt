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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for the satellite landing page.
 *
 * This ViewModel manages the state for the Satellite Landing Page, including:
 * - The list of satellite-enabled applications.
 * - The device and carrier support for LTE-based Non-Terrestrial Networks (NTN).
 * - The enabling/disabling of apps based on satellite connectivity status.
 */
class SatelliteLandingPageViewModel(
    private val context: Context,
    private val appsRepository: SatelliteAppsRepository,
    private val packageManager: PackageManager,
    satelliteStateRepository: SatelliteStateRepository,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _satelliteAppItems = MutableStateFlow<List<SatelliteAppItem>>(emptyList())
    /** The list of satellite-related applications to display on the landing page. */
    val satelliteAppItems: StateFlow<List<SatelliteAppItem>> = _satelliteAppItems.asStateFlow()

    private val _isLteBasedNtnSupported = MutableStateFlow<Boolean?>(null)
    /**
     * Indicates if the device supports LTE-based NTN. Null indicates the support status is
     * currently being determined.
     */
    val isLteBasedNtnSupported: StateFlow<Boolean?> = _isLteBasedNtnSupported.asStateFlow()

    private val _isCarrierRoamingNtnSupported = MutableStateFlow(false)
    /** Indicates if the current carrier supports roaming NTN. */
    val isCarrierRoamingNtnSupported: StateFlow<Boolean> =
        _isCarrierRoamingNtnSupported.asStateFlow()

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

    private val _bannerState = MutableStateFlow(SatelliteBannerState())
    /** The state of the satellite warning banners. */
    val bannerState: StateFlow<SatelliteBannerState> = _bannerState

    /**
     * Refreshes the satellite landing page data.
     *
     * This method fetches the latest support status for LTE-based NTN and Carrier Roaming NTN, and
     * reloads the list of satellite applications.
     *
     * @param subId The subscription ID to check carrier support for.
     */
    fun refresh(subId: Int) {
        viewModelScope.launch {
            // Fetch support status on a background thread to avoid blocking the UI.
            val isLteSupported =
                withContext(backgroundDispatcher) {
                    SatelliteUtils.isLteBasedNtnSupportedByDevice(context)
                }
            _isLteBasedNtnSupported.value = isLteSupported

            val isCarrierSupported =
                withContext(backgroundDispatcher) {
                    SatelliteUtils.isCarrierRoamingNtnSupported(context, subId)
                }
            _isCarrierRoamingNtnSupported.value = isCarrierSupported

            loadSatelliteAppItems(isLteSupported, isCarrierSupported)

            updateBannerState()
        }
    }

    /**
     * Updates the state flow for banners.
     *
     * TODO(b/465479769): Implement actual checks for [SatelliteBannerState].
     */
    fun updateBannerState() {
        _bannerState.value = SatelliteBannerState()
    }

    /**
     * Returns the [Intent] for the Satellite Settings page.
     *
     * Delegates the intent creation to [SatelliteAppsRepository], passing the current
     * [isCarrierRoamingNtnSupported] state.
     *
     * @return The resolved [Intent] to launch the settings page, or null if unavailable.
     */
    fun getSettingsIntent(): Intent? {
        return appsRepository.getSettingsIntent(isCarrierRoamingNtnSupported.value)
    }

    private fun loadSatelliteAppItems(
        isLteBasedNtnSupported: Boolean,
        isCarrierRoamingNtnSupported: Boolean,
    ) {
        val items = mutableListOf<SatelliteAppItem>()

        // Emergency SOS app
        createSatelliteAppItem(
                packageName = SatelliteAppsRepository.PACKAGE_NAME_SAFETY_HUB,
                intent = appsRepository.getEmergencySosIntent(),
                appLabel = context.getString(R.string.satellite_emergency_sos),
            )
            ?.let { items.add(it) }

        // Configurable apps based on NTN support
        val appPackages =
            if (isLteBasedNtnSupported) {
                appsRepository.getAppsPackagesForLteLandingPage()
            } else {
                appsRepository.getAppsPackagesForNbNtnLandingPage()
            }

        appPackages.forEach { packageName ->
            createSatelliteAppItem(
                    packageName = packageName,
                    intent = packageManager.getLaunchIntentForPackage(packageName),
                )
                ?.let { items.add(it) }
        }

        // Settings app
        createSatelliteAppItem(
                packageName = SatelliteAppsRepository.PACKAGE_NAME_SETTINGS,
                intent = appsRepository.getSettingsIntent(isCarrierRoamingNtnSupported),
            )
            ?.let { items.add(it) }

        _satelliteAppItems.value = items
    }

    private fun createSatelliteAppItem(
        packageName: String,
        intent: Intent?,
        appLabel: String? = null,
    ): SatelliteAppItem? {
        if (intent == null) return null
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
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SatelliteLandingPageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SatelliteLandingPageViewModel(
                context,
                appsRepository,
                packageManager,
                SatelliteStateRepository.getInstance(context),
                backgroundDispatcher,
            )
                as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * UI state for satellite warning banners.
 *
 * Default values indicate banner is off.
 */
data class SatelliteBannerState(
    val isNetworkConnected: Boolean = false,
    val isSatelliteAvailableInRegion: Boolean = true,
    val isEntitled: Boolean = true,
    val isDefaultMessagingApp: Boolean = true,
    val isSatelliteEnabledByCarrier: Boolean = false,
    val isSatelliteAllowed: Boolean = true,
)
