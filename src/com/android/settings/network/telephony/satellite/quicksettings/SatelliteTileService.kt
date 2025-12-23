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

import android.app.PendingIntent
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.telephony.ServiceState
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.telephony.satellite.SatelliteDisallowedReasonsCallback
import android.telephony.satellite.SatelliteManager
import android.telephony.satellite.SatelliteModemStateCallback
import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import com.android.settings.R

/**
 * A [TileService] that provides a Quick Settings tile for satellite connectivity.
 *
 * This service monitors both Carrier Roaming NTN (Non-Terrestrial Network) and OEM-based satellite
 * services (e.g., Pixel Satellite SOS) to update the tile state.
 *
 * **State Priority:**
 * 1. **Active (On):** Satellite Modem is connected (Skylo) OR Carrier NTN is active.
 * 2. **Available:** Device is eligible/allowed AND has **NO Terrestrial Connectivity** (No Cellular
 *    Service, No Internet).
 * 3. **Not Available:** Default state.
 */
open class SatelliteTileService : TileService() {

    private lateinit var telephonyManager: TelephonyManager
    private var satelliteManager: SatelliteManager? = null
    private lateinit var connectivityManager: ConnectivityManager

    @VisibleForTesting internal var satelliteTilePromptUtils = SatelliteTilePromptUtils()

    // Carrier Roaming NTN states
    private var isCarrierRoamingNtnEligible = false
    private var isCarrierRoamingNtnModeActive = false

    // OEM Satellite states
    private var isOemSatelliteAllowed = false
    private var isOemSatelliteConnected = false

    // Cellular service state. Used to determine if OEM satellite should be shown as "Available".
    // Defaulting to true prevents the tile from flashing "Available" transiently during
    // initialization.
    private var isCellularAvailable = true

    private var isInternetConnected = false

    private val carrierRoamingNtnCallback = SatelliteCarrierRoamingNtnCallback()
    private val serviceStateCallback = SatelliteServiceStateCallback()
    private val satelliteModemStateCallback = SatelliteModemStateCallbackImpl()
    private val satelliteDisallowedReasonsCallback = SatelliteDisallowedReasonsCallbackImpl()

    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                val wasConnected = isInternetConnected
                // We have internet if the default network is VALIDATED
                isInternetConnected =
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                if (wasConnected != isInternetConnected) {
                    Log.d(TAG, "onCapabilitiesChanged: isInternetConnected=$isInternetConnected")
                    updateTile()
                }
            }

            override fun onLost(network: Network) {
                if (isInternetConnected) {
                    isInternetConnected = false
                    Log.d(TAG, "onLost: isInternetConnected=false")
                    updateTile()
                }
            }
        }

    /** Callback for monitoring Carrier Roaming NTN (NB-IoT) states. */
    private inner class SatelliteCarrierRoamingNtnCallback :
        TelephonyCallback(), TelephonyCallback.CarrierRoamingNtnListener {

        /** Triggered when the device becomes eligible for carrier roaming NTN (NB-IoT). */
        override fun onCarrierRoamingNtnEligibleStateChanged(eligible: Boolean) {
            if (isCarrierRoamingNtnEligible != eligible) {
                Log.d(TAG, "onCarrierRoamingNtnEligibleStateChanged: $eligible")
                isCarrierRoamingNtnEligible = eligible
                updateTile()
            }
        }

        /** Triggered when the carrier roaming NTN mode becomes active (LTE or NB-IoT). */
        override fun onCarrierRoamingNtnModeChanged(active: Boolean) {
            if (isCarrierRoamingNtnModeActive != active) {
                Log.d(TAG, "onCarrierRoamingNtnModeChanged: $active")
                isCarrierRoamingNtnModeActive = active
                updateTile()
            }
        }
    }

    /**
     * Callback for monitoring Service State.
     *
     * We monitor [ServiceState] because OEM satellite (Skylo) is generally designed as a fallback
     * when terrestrial networks are unavailable.
     */
    private inner class SatelliteServiceStateCallback :
        TelephonyCallback(), TelephonyCallback.ServiceStateListener {

        /** Monitor service state to determine if regular cellular service is available. */
        override fun onServiceStateChanged(serviceState: ServiceState) {
            val isNtn = serviceState.isUsingNonTerrestrialNetwork
            // We consider cellular is available only if we are In Service AND NOT using satellite
            // already.
            isCellularAvailable = (serviceState.state == ServiceState.STATE_IN_SERVICE) && !isNtn
            Log.d(
                TAG,
                "onServiceStateChanged: state=${serviceState.state}, isNtn=$isNtn, isCellularAvailable=$isCellularAvailable",
            )
            updateTile()
        }
    }

    /** Callback for monitoring the satellite modem state for OEM services (Skylo). */
    private inner class SatelliteModemStateCallbackImpl : SatelliteModemStateCallback {
        override fun onSatelliteModemStateChanged(state: Int) {
            // We consider the satellite "Connected" (Tile Active) if the modem is
            // actually connected, transferring data, or actively listening.
            val isConnected =
                state == SatelliteManager.SATELLITE_MODEM_STATE_CONNECTED ||
                    state == SatelliteManager.SATELLITE_MODEM_STATE_DATAGRAM_TRANSFERRING ||
                    state == SatelliteManager.SATELLITE_MODEM_STATE_LISTENING
            if (isOemSatelliteConnected != isConnected) {
                Log.d(TAG, "onSatelliteModemStateChanged: state=$state, isConnected=$isConnected")
                isOemSatelliteConnected = isConnected
                updateTile()
            }
        }
    }

    /**
     * Callback for monitoring reasons why satellite might be disallowed.
     *
     * If the list of disallowed reasons is empty, OEM satellite (Skylo) is considered "Allowed".
     */
    private inner class SatelliteDisallowedReasonsCallbackImpl :
        SatelliteDisallowedReasonsCallback {
        override fun onSatelliteDisallowedReasonsChanged(disallowedReasons: IntArray) {
            val isAllowed = disallowedReasons.isEmpty()
            if (isOemSatelliteAllowed != isAllowed) {
                Log.d(
                    TAG,
                    "onSatelliteDisallowedReasonsChanged: allowed=$isAllowed, disallowedReasons=[${disallowedReasons.joinToString(", ")}]",
                )
                isOemSatelliteAllowed = isAllowed
                updateTile()
            }
        }
    }

    @MainThread
    override fun onCreate() {
        super.onCreate()
        try {
            telephonyManager = getSystemService(TelephonyManager::class.java)
            satelliteManager = getSystemService(SatelliteManager::class.java)
            connectivityManager = getSystemService(ConnectivityManager::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing managers", e)
        }
        if (satelliteManager == null) {
            Log.e(TAG, "SatelliteManager is null, OEM satellite features will be unavailable.")
        }

        refreshCellularState()
        // Initialize isInternetConnected synchronously
        val activeNetwork = connectivityManager.activeNetwork
        isInternetConnected =
            if (activeNetwork != null) {
                connectivityManager
                    .getNetworkCapabilities(activeNetwork)
                    ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
            } else {
                false
            }

        // Register callbacks. All updates MUST happen on the main thread to ensure
        // thread-safe updates to the Tile.
        try {
            telephonyManager.registerTelephonyCallback(mainExecutor, carrierRoamingNtnCallback)
            telephonyManager.registerTelephonyCallback(mainExecutor, serviceStateCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering telephony callbacks", e)
        }

        try {
            satelliteManager?.registerForModemStateChanged(
                mainExecutor,
                satelliteModemStateCallback,
            )
            satelliteManager?.registerForSatelliteDisallowedReasonsChanged(
                mainExecutor,
                satelliteDisallowedReasonsCallback,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error registering satellite callbacks", e)
        }

        try {
            connectivityManager.registerDefaultNetworkCallback(
                networkCallback,
                Handler(Looper.getMainLooper()),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error registering connectivity callback", e)
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        refreshCellularState()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        // Mark as shown so we don't prompt user to add the tile.
        satelliteTilePromptUtils.setAddTilePromptShown(this, true)
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        satelliteTilePromptUtils.setAddTilePromptShown(this, false)
    }

    override fun onClick() {
        super.onClick()
        unlockAndRun {
            // Launch the Satellite Landing Page
            val intent = Intent(this, SatelliteLandingPageActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pendingIntent =
                PendingIntent.getActivity(
                    this,
                    REQUEST_CODE_SATELLITE_LANDING_PAGE,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE,
                )
            startActivityAndCollapse(pendingIntent)
        }
    }

    @MainThread
    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    @VisibleForTesting
    internal fun cleanup() {
        telephonyManager.unregisterTelephonyCallback(carrierRoamingNtnCallback)
        telephonyManager.unregisterTelephonyCallback(serviceStateCallback)
        try {
            satelliteManager?.unregisterForModemStateChanged(satelliteModemStateCallback)
            satelliteManager?.unregisterForSatelliteDisallowedReasonsChanged(
                satelliteDisallowedReasonsCallback
            )
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering satellite callbacks", e)
        }
    }

    /**
     * Updates the Quick Settings tile state based on current satellite and cellular signals.
     *
     * Logic Priority:
     * 1. **ACTIVE:** If either Carrier NTN is active OR OEM Satellite is connected.
     * 2. **AVAILABLE:** If Carrier NTN is eligible OR (OEM Satellite is allowed AND Cellular is
     *    OOS).
     * 3. **UNAVAILABLE:** Default state.
     */
    @MainThread
    @VisibleForTesting
    fun updateTile() {
        val qsTile = qsTile ?: return

        val newState: Int
        val newSubtitle: String

        // 1. Check for Active State first (Priority)
        val isSatelliteOn = isCarrierRoamingNtnModeActive || isOemSatelliteConnected

        // 2. Check for "Available" State (Inactive)
        // Eligible/Allowed AND No Terrestrial Connection (No Voice, No Internet)
        val isTerrestrialAvailable = isCellularAvailable || isInternetConnected
        val isSatelliteAvailable =
            (isCarrierRoamingNtnEligible || isOemSatelliteAllowed) && !isTerrestrialAvailable

        when {
            isSatelliteOn -> {
                newState = Tile.STATE_ACTIVE
                newSubtitle = getString(R.string.satellite_tile_subtitle_on)
            }
            isSatelliteAvailable -> {
                newState = Tile.STATE_INACTIVE
                newSubtitle = getString(R.string.satellite_tile_subtitle_available)
            }
            else -> {
                newState = Tile.STATE_INACTIVE
                newSubtitle = getString(R.string.satellite_tile_subtitle_not_available)
            }
        }

        if (qsTile.state == newState && qsTile.subtitle == newSubtitle) {
            return
        }

        qsTile.state = newState
        qsTile.subtitle = newSubtitle
        qsTile.updateTile()

        Log.i(
            TAG,
            "updateTile: State=${qsTile.state}, Subtitle=${qsTile.subtitle}, " +
                "NTN[Active=$isCarrierRoamingNtnModeActive, Eligible=$isCarrierRoamingNtnEligible], " +
                "OEM[Connected=$isOemSatelliteConnected, Allowed=$isOemSatelliteAllowed], " +
                "CellularAvailable=$isCellularAvailable, InternetConnected=$isInternetConnected",
        )
    }

    private fun refreshCellularState() {
        try {
            val currentServiceState = telephonyManager.serviceState
            if (currentServiceState != null) {
                val isNtn = currentServiceState.isUsingNonTerrestrialNetwork
                // Cellular is available if IN_SERVICE and NOT using Satellite
                isCellularAvailable =
                    (currentServiceState.state == ServiceState.STATE_IN_SERVICE) && !isNtn
            } else {
                // If service state is null, we assume no cellular service is available (e.g. No SIM
                // or Modem not ready).
                isCellularAvailable = false
            }
            Log.d(
                TAG,
                "refreshCellularState: state=${currentServiceState?.state}, " +
                    "isNtn=${currentServiceState?.isUsingNonTerrestrialNetwork}, " +
                    "isCellularAvailable=$isCellularAvailable",
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh ServiceState", e)
        }
        updateTile()
    }

    companion object {
        private const val TAG = "SatelliteTileService"
        private const val REQUEST_CODE_SATELLITE_LANDING_PAGE = 1987
    }
}
