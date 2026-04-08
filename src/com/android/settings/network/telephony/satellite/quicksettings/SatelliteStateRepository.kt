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
import android.database.ContentObserver
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telephony.ServiceState
import android.telephony.SubscriptionManager
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
    private val isLteNtnSupportedChecker: (Context, Int) -> Boolean = { ctx, subId ->
        SatelliteUtils.isLteBasedNtnSupported(ctx, subId)
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
                    Log.i(TAG, "onCarrierRoamingNtnEligibleStateChanged: $eligible")
                    state = state.copy(isEligible = eligible)
                    trySend(state)
                }

                override fun onCarrierRoamingNtnModeChanged(active: Boolean) {
                    Log.i(TAG, "onCarrierRoamingNtnModeChanged: $active")
                    state = state.copy(isActive = active)
                    trySend(state)
                }
            }

        // no initial state needs to be emited.
        // registerTelephonyCallback always callsback immediately with current state as per
        // documentation of the API

        Log.i(TAG, "registerTelephonyCallback for carrierRoamingNtnStateFlow")
        telephonyManager?.registerTelephonyCallback(callbackExecutor, callback)
        awaitClose {
            Log.i(TAG, "unregisterTelephonyCallback for carrierRoamingNtnStateFlow")
            runCatching { telephonyManager?.unregisterTelephonyCallback(callback) }
        }
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

        Log.i(TAG, "registerTelephonyCallback for isCellularAvailableFlow")
        telephonyManager?.registerTelephonyCallback(callbackExecutor, callback)
        awaitClose {
            Log.i(TAG, "unregisterTelephonyCallback for isCellularAvailableFlow")
            runCatching { telephonyManager?.unregisterTelephonyCallback(callback) }
        }
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

        Log.i(TAG, "registerForModemStateChanged for isOemSatelliteActiveFlow")
        satelliteManager?.registerForModemStateChanged(callbackExecutor, callback)
        awaitClose { runCatching { satelliteManager?.unregisterForModemStateChanged(callback) } }
    }

    /**
     * A flow that emits whether satellite is allowed by the OEM.
     *
     * This represents the check for OEM NB-IoT availability.
     */
    /** A flow that emits the list of disallowed reasons for satellite service. */
    open val satelliteDisallowedReasons: StateFlow<IntArray> =
        callbackFlow {
                val callback = SatelliteDisallowedReasonsCallback { reasons ->
                    logd { "onSatelliteDisallowedReasonsChanged: $reasons" }
                    trySend(reasons)
                }

                try {
                    Log.i(TAG, "registerForSatelliteDisallowedReasonsChanged for satelliteDisallowedReasons flow")
                    satelliteManager?.registerForSatelliteDisallowedReasonsChanged(
                        callbackExecutor,
                        callback,
                    )
                } catch (e: IllegalStateException) {
                    Log.w(TAG, "Failed to register for satellite disallowed reasons: ${e.message}")
                }

                awaitClose {
                    try {
                        satelliteManager?.unregisterForSatelliteDisallowedReasonsChanged(callback)
                    } catch (e: IllegalStateException) {
                        Log.w(TAG, "Failed to unregister for satellite disallowed reasons", e)
                    }
                }
            }
            .stateIn(scope, SharingStarted.WhileSubscribed(), intArrayOf())

    private fun isAirplaneModeEnabled(): Boolean {
        return Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON,
            0,
        ) != 0
    }

    private val isAirplaneModeEnabledFlow = callbackFlow {
        val uri = Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON)
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    trySend(isAirplaneModeEnabled())
                }
            }

        context.contentResolver.registerContentObserver(uri, false, observer)
        trySend(isAirplaneModeEnabled())

        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
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

    /** Indicates if a terrestrial network (Cellular or Wi-Fi) is connected. */
    open val isTerrestrialConnected: StateFlow<Boolean> =
        combine(isCellularAvailableFlow, isInternetConnectedFlow) { cell, wifi -> cell || wifi }
            .stateIn(scope, SharingStarted.WhileSubscribed(), false)

    open val activeSubIdFlow: StateFlow<Int> =
        callbackFlow {
                val sm = context.getSystemService(SubscriptionManager::class.java)
                val listener =
                    object : SubscriptionManager.OnSubscriptionsChangedListener() {
                        override fun onSubscriptionsChanged() {
                            trySend(SubscriptionManager.getActiveDataSubscriptionId())
                        }
                    }
                sm?.addOnSubscriptionsChangedListener(context.mainExecutor, listener)
                trySend(SubscriptionManager.getActiveDataSubscriptionId())
                awaitClose { sm?.removeOnSubscriptionsChangedListener(listener) }
            }
            .stateIn(
                scope,
                SharingStarted.WhileSubscribed(),
                SubscriptionManager.getActiveDataSubscriptionId(),
            )

    /**
     * A flow that groups the current state of satellite modem signals.
     *
     * This includes the carrier roaming NTN state, OEM satellite activity, and disallowed reasons.
     */
    private val satelliteModemSignalsFlow =
        combine(carrierRoamingNtnStateFlow, isOemSatelliteActiveFlow, satelliteDisallowedReasons) {
            carrierState,
            isOemActive,
            disallowedReasons ->
            Triple(carrierState, isOemActive, disallowedReasons)
        }

    /**
     * The current status of satellite connectivity.
     *
     * The flow is shared and keeps the producer active only while there are subscribers.
     */
    open val satelliteStatus: StateFlow<SatelliteStatus> =
        combine(
                satelliteModemSignalsFlow,
                isTerrestrialConnected,
                isAirplaneModeEnabledFlow,
                activeSubIdFlow,
            ) { (carrierState, isOemActive, disallowedReasons), isTerrestrial, isAirplaneMode, subId
                ->
                val isCarrierSupported = SatelliteUtils.isCarrierRoamingNtnSupported(subId)
                val isSatelliteAvailable =
                    checkSatelliteAvailability(
                        isCarrierEligible = carrierState.isEligible,
                        isOemAllowed = disallowedReasons.isEmpty(),
                        isTerrestrialConnected = isTerrestrial,
                        isCarrierSupported = isCarrierSupported,
                        subId = subId,
                    )

                Log.i(
                    TAG,
                    "satelliteStatus check: subId=$subId, isAirplaneMode=$isAirplaneMode, " +
                        "isTerrestrial=$isTerrestrial, isCarrierSupported=$isCarrierSupported, " +
                        "carrierEligible=${carrierState.isEligible}, carrierActive=${carrierState.isActive}, " +
                        "oemActive=$isOemActive, oemDisallowedReasons=${disallowedReasons.contentToString()}, " +
                        "isSatelliteAvailable=$isSatelliteAvailable",
                )

                // Rule: Immediate return if Airplane Mode is on
                if (isAirplaneMode) return@combine SatelliteStatus.NOT_AVAILABLE

                val status =
                    when {
                        carrierState.isActive || isOemActive -> SatelliteStatus.ACTIVE
                        isSatelliteAvailable -> SatelliteStatus.AVAILABLE
                        else -> SatelliteStatus.NOT_AVAILABLE
                    }
                Log.i(TAG, "satelliteStatus final status: $status")
                status
            }
            .stateIn(scope, SharingStarted.WhileSubscribed(), SatelliteStatus.NOT_AVAILABLE)

    /**
     * Returns the satellite data support mode for the carrier.
     *
     * Includes restricted, constrained, and unrestricted modes.
     *
     * @param subId The subscription ID.
     * @return The [SatelliteManager.SatelliteDataSupportMode].
     */
    open fun getSatelliteDataSupportMode(subId: Int): Int {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return SatelliteManager.SATELLITE_DATA_SUPPORT_UNKNOWN
        }
        return try {
            satelliteManager?.getSatelliteDataSupportMode(subId)
                ?: SatelliteManager.SATELLITE_DATA_SUPPORT_UNKNOWN
        } catch (e: Exception) {
            Log.e(TAG, "Error getting satellite data support mode", e)
            SatelliteManager.SATELLITE_DATA_SUPPORT_UNKNOWN
        }
    }

    /**
     * Returns the attach restriction reasons for the carrier.
     *
     * @param subId The subscription ID.
     * @return A set of restriction reasons.
     */
    open fun getAttachRestrictionReasons(subId: Int): Set<Int> {
        return try {
            satelliteManager?.getAttachRestrictionReasonsForCarrier(subId) ?: emptySet()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting attach restriction reasons", e)
            emptySet()
        }
    }

    /**
     * Returns true if satellite connectivity is available.
     *
     * LTE-based NTN must not supported on the device to be eligible for AVAILABLE state.
     */
    private fun checkSatelliteAvailability(
        isCarrierEligible: Boolean,
        isOemAllowed: Boolean,
        isTerrestrialConnected: Boolean,
        isCarrierSupported: Boolean,
        subId: Int,
    ): Boolean {
        if (isTerrestrialConnected) return false

        // Rule: LTE-NTN devices never show "Available".
        // This state is strictly for NB-IoT (Carrier Roaming/Pixel Skylo).
        if (isLteNtnSupportedChecker(context, subId)) return false

        return if (isCarrierSupported) {
            isCarrierEligible
        } else {
            isOemAllowed
        }
    }

    /* Returns true if cellular is available and not using a non-terrestrial network. */
    private fun checkInitialCellularAvailability(): Boolean {
        return try {
            val state = telephonyManager?.serviceState
            Log.i(TAG, "checkInitialCellularAvailability: serviceState is ${if (state == null) "null" else "not null"}")
            Log.i(TAG, "checkInitialCellularAvailability: serviceState=$state")
            if (state == null) return false
            state.state == ServiceState.STATE_IN_SERVICE && !state.isUsingNonTerrestrialNetwork
        } catch (e: Exception) {
            Log.e(TAG, "Error checking initial cellular state", e)
            false
        }
    }

    /* Returns true if internet is available. */
    private fun checkInitialInternetAvailability(): Boolean {
        return try {
            val activeNetwork = connectivityManager?.activeNetwork
            Log.i(TAG, "checkInitialInternetAvailability: activeNetwork is ${if (activeNetwork == null) "null" else "not null"}")
            if (activeNetwork == null) return false
            val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
            Log.i(TAG, "checkInitialInternetAvailability: networkCapabilities=$caps")
            if (caps == null) return false
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
                state == SatelliteManager.SATELLITE_MODEM_STATE_ENABLING_SATELLITE ||
                state == SatelliteManager.SATELLITE_MODEM_STATE_NOT_CONNECTED
        }
    }

    private inline fun logd(message: () -> String) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, message())
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
