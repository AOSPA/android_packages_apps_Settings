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

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.OutcomeReceiver
import android.telephony.CarrierConfigManager
import android.telephony.TelephonyManager
import android.telephony.satellite.SatelliteManager
import android.util.Log
import com.android.internal.annotations.VisibleForTesting
import com.android.settings.R
import com.android.settings.flags.Flags
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private const val TAG = "SatelliteTileStateReceiver"

/**
 * A [BroadcastReceiver] that listens for boot completed events and subscription changes to update
 * the [SatelliteTileService] enabled state.
 *
 * This receiver is responsible for monitoring the boot completion event. It then checks the current
 * satellite supported state and updates the [SatelliteTileService] enabled state accordingly.
 *
 * The enabled state of the service is updated based on the following logic:
 * 1. If the device supports any NTN, the service is enabled.
 * 2. If NTN is not supported, the service is disabled.
 *
 * Even if the current carrier or geo location doesn't support satellite connectivity, the tile
 * service will still be enabled if the device supports NTN. The Landing Page will be responsible
 * for educating the user on why certain NTN features are not available. We will only prompt the
 * user to add the satellite tile to quick settings when eligible for NTN.
 */
open class SatelliteTileStateReceiver(
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : BroadcastReceiver() {

    /**
     * Handles incoming broadcasts to update the satellite tile's enabled state.
     *
     * This method responds to boot completion, SIM state changes, and carrier config changes by
     * checking for NTN support and enabling or disabling the [SatelliteTileService] accordingly.
     */
    override fun onReceive(context: Context, intent: Intent) {
        if (!isSatelliteTileFeatureEnabled(context)) {
            return
        }

        val action = intent.action
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            TelephonyManager.ACTION_SIM_CARD_STATE_CHANGED,
            CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED -> {
                // Legitimate actions, continue processing.
                Log.d(TAG, "onReceive: $action")
            }
            else -> return // Exit for any other actions.
        }

        val pendingResult = goAsync()
        CoroutineScope(defaultDispatcher).launch {
            try {
                val isAnyNtnSupported = isAnyNtnSupportedFlow(context).first()
                updateTileServiceEnabledState(context, isAnyNtnSupported)
            } finally {
                pendingResult.finish()
            }
        }

        // To combat false negatives for [SatelliteManager.requestIsSupported] on boot, we need to
        // register for satellite supported state changes.
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            SatelliteSupportedStateChangeHandler.register(context, defaultDispatcher)
        }
    }

    /**
     * Returns a [Flow] that emits true if NB-IoT-based NTN is supported, false otherwise.
     *
     * This flow uses a callback flow to asynchronously receive the supported state from
     * [SatelliteManager.requestIsSupported].
     */
    private fun isNbIotBasedNtnSupportedFlow(context: Context): Flow<Boolean> {
        val satelliteManager: SatelliteManager? =
            context.getSystemService(SatelliteManager::class.java)
        if (satelliteManager == null) {
            Log.w(TAG, "SatelliteManager is null, returning false")
            return flowOf(false)
        }
        Log.i(TAG, "isNbIotBasedNtnSupportedFlow started")
        return callbackFlow {
            val callback =
                object : OutcomeReceiver<Boolean, SatelliteManager.SatelliteException> {
                    override fun onResult(isSupported: Boolean) {
                        Log.i(TAG, "isNbIotBasedNtnSupportedFlow onResult: $isSupported")
                        trySend(isSupported)
                        close()
                    }

                    override fun onError(error: SatelliteManager.SatelliteException) {
                        Log.e(TAG, "isNbIotBasedNtnSupportedFlow failed: $error")
                        trySend(false)
                        close()
                    }
                }
            satelliteManager.requestIsSupported(defaultDispatcher.asExecutor(), callback)
            awaitClose {}
        }
    }

    /**
     * Returns a [Flow] that emits `true` if any form of NTN (LTE-based or NB-IoT-based) is
     * supported, and `false` otherwise.
     *
     * This flow short-circuits and returns `true` immediately if LTE-based NTN is supported.
     */
    private fun isAnyNtnSupportedFlow(context: Context): Flow<Boolean> {
        if (SatelliteUtils.isLteBasedNtnSupportedByDevice(context)) {
            return flowOf(true)
        }
        return isNbIotBasedNtnSupportedFlow(context)
    }

    companion object {
        /**
         * Verifies that the satellite tile feature is enabled for the device.
         *
         * This function checks for the following conditions in order:
         * 1. The master aconfig flag `FLAG_ENABLE_SATELLITE_TILE` is enabled.
         * 2. The device supports satellite telephony via `FEATURE_TELEPHONY_SATELLITE`.
         * 3. The feature is enabled by the OEM via `config_show_satellite_tile`.
         *
         * @param context The application context.
         * @return `true` if all conditions are met, `false` otherwise.
         */
        fun isSatelliteTileFeatureEnabled(context: Context): Boolean {
            // Master aconfig flag check for the entire feature
            if (!Flags.enableSatelliteTile()) {
                Log.d(TAG, "enable_satellite_tile aconfig flag is false.")
                return false
            }

            // Hardware/Software Capability Check for NTN
            if (
                !context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_SATELLITE)
            ) {
                Log.d(TAG, "FEATURE_TELEPHONY_SATELLITE not supported.")
                return false
            }

            // OEM opt-in for the feature
            if (!context.resources.getBoolean(R.bool.config_show_satellite_tile)) {
                Log.d(TAG, "config_show_satellite_tile is false, feature disabled.")
                return false
            }

            Log.d(TAG, "Satellite tile feature enabled for this device.")
            return true
        }

        /**
         * Update the tile service state. This enables or disables the service, making the tile
         * appear or disappear.
         */
        fun updateTileServiceEnabledState(context: Context, isAnyNtnSupported: Boolean) {
            val componentName = ComponentName(context, SatelliteTileService::class.java)
            val packageManager = context.packageManager
            val newState =
                if (isAnyNtnSupported) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }

            if (packageManager.getComponentEnabledSetting(componentName) == newState) {
                Log.d(TAG, "Not updating SatelliteTileService state, already $newState")
                return
            }

            Log.i(TAG, "Setting SatelliteTileService enabled state to: $isAnyNtnSupported")

            // This enables or disables the service, making the tile appear or disappear.
            packageManager.setComponentEnabledSetting(
                componentName,
                newState,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}

/**
 * Handles the registration for satellite supported state changes.
 *
 * This object is used to register a callback with the [SatelliteManager] to listen for changes in
 * satellite support status. It is designed as a singleton to ensure that the callback is registered
 * only once and to prevent leaking the [BroadcastReceiver] context, as the callback may be held by
 * the system service for an extended period.
 */
@VisibleForTesting
internal object SatelliteSupportedStateChangeHandler {
    private var isRegistered = false
    private val lock = Any()

    @VisibleForTesting
    internal fun reset() {
        synchronized(lock) { isRegistered = false }
    }

    fun register(context: Context, dispatcher: CoroutineDispatcher) {
        synchronized(lock) {
            if (isRegistered) {
                Log.d(TAG, "Already registered for satellite state changes.")
                return
            }

            // Use application context to avoid leaking the receiver context
            val appContext = context.applicationContext
            val satelliteManager = appContext.getSystemService(SatelliteManager::class.java)
            if (satelliteManager == null) {
                Log.e(TAG, "SatelliteManager is not available for registration.")
                return
            }

            try {
                satelliteManager.registerForSupportedStateChanged(dispatcher.asExecutor()) {
                    isNbIotBasedNtnSupported ->
                    Log.i(
                        TAG,
                        "onSatelliteSupportedStateChanged: isSupported=$isNbIotBasedNtnSupported",
                    )
                    val isLteBasedNtnSupported =
                        SatelliteUtils.isLteBasedNtnSupportedByDevice(appContext)
                    SatelliteTileStateReceiver.updateTileServiceEnabledState(
                        appContext,
                        isLteBasedNtnSupported || isNbIotBasedNtnSupported,
                    )
                }
                isRegistered = true
                Log.i(TAG, "Successfully registered for satellite state changes.")
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Failed to register for satellite state changes", e)
            }
        }
    }
}
