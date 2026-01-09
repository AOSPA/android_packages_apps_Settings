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
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.telephony.ServiceState
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.telephony.satellite.SatelliteDisallowedReasonsCallback
import android.telephony.satellite.SatelliteManager
import android.telephony.satellite.SatelliteModemStateCallback
import android.util.Log
import androidx.annotation.VisibleForTesting
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Repository that monitors and exposes the satellite connectivity status.
 *
 * This repository acts as the single source of truth for satellite states, aggregating signals from
 * [TelephonyManager], [SatelliteManager], and [ConnectivityManager].
 */
open class SatelliteStateRepository
@VisibleForTesting
constructor(
    private val context: Context,
    private val telephonyManager: TelephonyManager?,
    private val satelliteManager: SatelliteManager?,
    private val connectivityManager: ConnectivityManager?,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val isLteNtnSupportedChecker: (Context) -> Boolean = {
        SatelliteUtils.isLteBasedNtnSupportedByDevice(it)
    },
) {

    // Shared executor for callbacks to avoid thread churn on every collection.
    // Since this repository is a singleton, this executor lives for the app's lifecycle.
    private val callbackExecutor = Executors.newSingleThreadExecutor()

    private data class CarrierRoamingNtnState(
        val isEligible: Boolean = false,
        val isActive: Boolean = false,
    )

    private val carrierRoamingNtnStateFlow = callbackFlow {
        val callback =
            object : TelephonyCallback(), TelephonyCallback.CarrierRoamingNtnListener {
                // Local state to track partial updates
                var state = CarrierRoamingNtnState()

                override fun onCarrierRoamingNtnEligibleStateChanged(eligible: Boolean) {
                    if (state.isEligible != eligible) {
                        Log.i(TAG, "onCarrierRoamingNtnEligibleStateChanged: $eligible")
                        state = state.copy(isEligible = eligible)
                        trySend(state)
                    }
                }

                override fun onCarrierRoamingNtnModeChanged(active: Boolean) {
                    if (state.isActive != active) {
                        Log.i(TAG, "onCarrierRoamingNtnModeChanged: $active")
                        state = state.copy(isActive = active)
                        trySend(state)
                    }
                }
            }

        // Emit initial default state to unblock 'combine' immediately, in case the callback is
        // delayed or async.
        trySend(CarrierRoamingNtnState())

        telephonyManager?.registerTelephonyCallback(callbackExecutor, callback)
        awaitClose { runCatching { telephonyManager?.unregisterTelephonyCallback(callback) } }
    }

    private val isCellularAvailableFlow = callbackFlow {
        var isAvailable = checkInitialCellularAvailability()
        val callback =
            object : TelephonyCallback(), TelephonyCallback.ServiceStateListener {
                override fun onServiceStateChanged(serviceState: ServiceState) {
                    val isNtn = serviceState.isUsingNonTerrestrialNetwork
                    val isInService = serviceState.state == ServiceState.STATE_IN_SERVICE
                    val newAvailable = isInService && !isNtn
                    if (isAvailable != newAvailable) {
                        Log.i(TAG, "onServiceStateChanged: isCellularAvailable=$newAvailable")
                        isAvailable = newAvailable
                        trySend(newAvailable)
                    }
                }
            }

        // Emit initial state to unblock 'combine' immediately, in case the callback is
        // delayed or async.
        trySend(isAvailable)

        telephonyManager?.registerTelephonyCallback(callbackExecutor, callback)
        awaitClose { runCatching { telephonyManager?.unregisterTelephonyCallback(callback) } }
    }

    private val isOemSatelliteActiveFlow = callbackFlow {
        var isActive = false
        val callback = SatelliteModemStateCallback { state ->
            val newActive = isSatelliteModemStateActive(state)
            if (isActive != newActive) {
                Log.i(TAG, "onSatelliteModemStateChanged: isOemSatelliteActive=$newActive")
                isActive = newActive
                trySend(newActive)
            }
        }

        // Emit initial state
        trySend(isActive)

        satelliteManager?.registerForModemStateChanged(callbackExecutor, callback)
        awaitClose { runCatching { satelliteManager?.unregisterForModemStateChanged(callback) } }
    }

    private val isOemSatelliteAllowedFlow = callbackFlow {
        var isAllowed = false
        val callback = SatelliteDisallowedReasonsCallback { reasons ->
            val newAllowed = reasons.isEmpty()
            if (isAllowed != newAllowed) {
                Log.i(TAG, "onSatelliteDisallowedReasonsChanged: isOemSatelliteAllowed=$newAllowed")
                isAllowed = newAllowed
                trySend(newAllowed)
            }
        }

        // Emit initial state
        trySend(isAllowed)

        satelliteManager?.registerForSatelliteDisallowedReasonsChanged(callbackExecutor, callback)
        awaitClose {
            runCatching {
                satelliteManager?.unregisterForSatelliteDisallowedReasonsChanged(callback)
            }
        }
    }

    private val isInternetConnectedFlow = callbackFlow {
        var isConnected = checkInitialInternetAvailability()
        val callback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    val newConnected =
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    if (isConnected != newConnected) {
                        Log.i(TAG, "onCapabilitiesChanged: isInternetConnected=$newConnected")
                        isConnected = newConnected
                        trySend(newConnected)
                    }
                }

                override fun onLost(network: Network) {
                    if (isConnected) {
                        Log.i(TAG, "onLost: isInternetConnected=false")
                        isConnected = false
                        trySend(false)
                    }
                }
            }

        // Emit initial state
        trySend(isConnected)

        connectivityManager?.registerDefaultNetworkCallback(
            callback,
            Handler(Looper.getMainLooper()),
        )
        awaitClose { runCatching { connectivityManager?.unregisterNetworkCallback(callback) } }
    }

    /**
     * The current status of satellite connectivity.
     *
     * The flow is shared and keeps the producer active only while there are subscribers.
     */
    open val satelliteStatus: StateFlow<SatelliteStatus> =
        combine(
                carrierRoamingNtnStateFlow,
                isCellularAvailableFlow,
                isOemSatelliteActiveFlow,
                isOemSatelliteAllowedFlow,
                isInternetConnectedFlow,
            ) { carrierState, isCellular, isOemActive, isOemAllowed, isInternet ->
                val isSatelliteActive = carrierState.isActive || isOemActive
                val isTerrestrialAvailable = isCellular || isInternet
                val isSatelliteAvailable =
                    checkSatelliteAvailability(
                        isCarrierEligible = carrierState.isEligible,
                        isOemAllowed = isOemAllowed,
                        isTerrestrialAvailable = isTerrestrialAvailable,
                    )

                when {
                    isSatelliteActive -> SatelliteStatus.ACTIVE
                    isSatelliteAvailable -> SatelliteStatus.AVAILABLE
                    else -> SatelliteStatus.NOT_AVAILABLE
                }
            }
            .stateIn(scope, SharingStarted.WhileSubscribed(), SatelliteStatus.NOT_AVAILABLE)

    /**
     * Returns true if satellite connectivity is available.
     *
     * LTE-based NTN must not supported on the device to be eligible for AVAILABLE state.
     */
    private fun checkSatelliteAvailability(
        isCarrierEligible: Boolean,
        isOemAllowed: Boolean,
        isTerrestrialAvailable: Boolean,
    ): Boolean {
        return (isCarrierEligible || isOemAllowed) &&
            !isTerrestrialAvailable &&
            !isLteNtnSupportedChecker(context)
    }

    /* Returns true if cellular is available and not using a non-terrestrial network. */
    private fun checkInitialCellularAvailability(): Boolean {
        return try {
            val state = telephonyManager?.serviceState ?: return false
            state.state == ServiceState.STATE_IN_SERVICE && !state.isUsingNonTerrestrialNetwork
        } catch (e: Exception) {
            Log.e(TAG, "Error checking initial cellular state", e)
            false
        }
    }

    /* Returns true if internet is available. */
    private fun checkInitialInternetAvailability(): Boolean {
        return try {
            val activeNetwork = connectivityManager?.activeNetwork ?: return false
            val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking initial internet state", e)
            false
        }
    }

    companion object {
        private const val TAG = "SatelliteStateRepo"

        @Volatile private var instance: SatelliteStateRepository? = null

        fun getInstance(context: Context): SatelliteStateRepository {
            return instance
                ?: synchronized(this) {
                    instance
                        ?: SatelliteStateRepository(
                                context.applicationContext,
                                context.getSystemService(TelephonyManager::class.java),
                                context.getSystemService(SatelliteManager::class.java),
                                context.getSystemService(ConnectivityManager::class.java),
                            )
                            .also { instance = it }
                }
        }

        @VisibleForTesting
        fun setInstance(repository: SatelliteStateRepository?) {
            instance = repository
        }

        /**
         * Returns true if the satellite modem is in a state that indicates active usage or an
         * ongoing session.
         */
        private fun isSatelliteModemStateActive(
            @SatelliteManager.SatelliteModemState state: Int
        ): Boolean {
            return state == SatelliteManager.SATELLITE_MODEM_STATE_CONNECTED ||
                state == SatelliteManager.SATELLITE_MODEM_STATE_DATAGRAM_TRANSFERRING ||
                state == SatelliteManager.SATELLITE_MODEM_STATE_DATAGRAM_RETRYING ||
                state == SatelliteManager.SATELLITE_MODEM_STATE_LISTENING ||
                state == SatelliteManager.SATELLITE_MODEM_STATE_ENABLING_SATELLITE
        }
    }
}

/** Represents the high-level status of satellite connectivity. */
enum class SatelliteStatus {
    /** Satellite is connected and active. */
    ACTIVE,
    /**
     * Satellite is available (allowed/eligible) but not currently active. Terrestrial is
     * unavailable.
     */
    AVAILABLE,
    /** Satellite is not available or terrestrial network is available. */
    NOT_AVAILABLE,
}
