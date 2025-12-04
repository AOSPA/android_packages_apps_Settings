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
import android.util.Log
import com.android.settings.flags.Flags
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A [BroadcastReceiver] that listens for boot completed events and updates the satellite tile's
 * enabled state.
 */
open class SatelliteTileStateReceiver(
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        val isFlagEnabled = Flags.enableSatelliteTile()
        Log.i(TAG, "onReceive: ${intent.action}, enableSatelliteTileFlag: $isFlagEnabled")

        if (isFlagEnabled) {
            val pendingResult = goAsync()
            scheduleSatelliteTileUpdateAfterBoot(context, pendingResult)
        }
    }

    internal fun scheduleSatelliteTileUpdateAfterBoot(
        context: Context,
        pendingResult: PendingResult,
    ) {
        CoroutineScope(defaultDispatcher).launch {
            try {
                // Wait for a period to allow Telephony services to initialize.
                delay(NTN_SUPPORT_CHECK_DELAY_MS)

                // Perform the check once after the delay.
                val isNtnSupported = isNtnSupported(context)
                updateTileEnabledState(context, isNtnSupported)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Returns true if NTN is supported.
     *
     * @param context The context of the application.
     */
    private suspend fun isNtnSupported(context: Context): Boolean {
        // TODO(b/434793872): Replace this with the actual check for NTN support.
        // We should check whether LTE NTN or NBIoT NTN is supported.
        val isNtnSupported = true
        return isNtnSupported
    }

    /**
     * Update the tile state. This enables or disables the service, making the tile appear or
     * disappear.
     *
     * @param context The context of the application.
     * @param isNtnSupported Whether NTN is supported. If true, the tile will be enabled. Otherwise,
     *   the tile will be disabled.
     */
    private fun updateTileEnabledState(context: Context, isNtnSupported: Boolean) {
        val newState =
            if (isNtnSupported) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
        Log.i(TAG, "SatelliteTileService enabled state: $isNtnSupported")

        // This enables or disables the service, making the tile appear or disappear.
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, SatelliteTileService::class.java),
            newState,
            PackageManager.DONT_KILL_APP,
        )
    }

    companion object {
        // TODO(b/434793872): Update this with a profound value once NTN support check is
        // implemented. Or listen for a related, proximal Broadcast from Telephony signfying NTN API
        // is ready.
        private const val NTN_SUPPORT_CHECK_DELAY_MS = 30000L
        private const val TAG = "SatelliteTileStateReceiver"
    }
}
