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
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.android.settings.R

/**
 * A [TileService] that provides a quick settings tile for satellite connectivity.
 *
 * This service is responsible for monitoring the satellite connectivity status and updating the
 * tile's appearance and behavior accordingly.
 */
open class SatelliteTileService : TileService() {

    private lateinit var telephonyManager: TelephonyManager
    @VisibleForTesting internal var satelliteTilePromptUtils = SatelliteTilePromptUtils()

    @VisibleForTesting internal var isCarrierRoamingNtnEligible = false
    @VisibleForTesting internal var isCarrierRoamingNtnModeActive = false

    private val satelliteTelephonyCallback = SatelliteTelephonyCallback()

    private inner class SatelliteTelephonyCallback :
        TelephonyCallback(), TelephonyCallback.CarrierRoamingNtnListener {

        /**
         * Called when the device becomes eligible for carrier roaming NTN.
         *
         * Triggered only for NB-IoT NTN. Updates tile to "Available" state.
         */
        override fun onCarrierRoamingNtnEligibleStateChanged(eligible: Boolean) {
            Log.d(TAG, "onCarrierRoamingNtnEligibleStateChanged: $eligible")
            isCarrierRoamingNtnEligible = eligible
            updateTile()
        }

        /**
         * Called when the carrier roaming NTN mode is active, not necessarily when connected to
         * satellite.
         *
         * Triggered when either LTE or NB-IoT NTN becomes active. Updates tile to "On" state.
         */
        override fun onCarrierRoamingNtnModeChanged(active: Boolean) {
            Log.d(TAG, "onCarrierRoamingNtnModeChanged: $active")
            isCarrierRoamingNtnModeActive = active
            updateTile()
        }
    }

    override fun onCreate() {
        super.onCreate()
        telephonyManager = getSystemService(TelephonyManager::class.java)
        telephonyManager.registerTelephonyCallback(mainExecutor, satelliteTelephonyCallback)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
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
            // Create PendingIntent to launch SatelliteLandingPageActivity.
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

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    @VisibleForTesting
    internal fun cleanup() {
        telephonyManager.unregisterTelephonyCallback(satelliteTelephonyCallback)
    }

    @VisibleForTesting
    fun updateTile() {
        val qsTile = qsTile ?: return

        when {
            isCarrierRoamingNtnModeActive -> {
                qsTile.state = Tile.STATE_ACTIVE
                qsTile.subtitle = getString(R.string.satellite_tile_subtitle_on)
            }
            isCarrierRoamingNtnEligible -> {
                qsTile.state = Tile.STATE_INACTIVE
                qsTile.subtitle = getString(R.string.satellite_tile_subtitle_available)
            }
            else -> {
                qsTile.state = Tile.STATE_INACTIVE
                qsTile.subtitle = getString(R.string.satellite_tile_subtitle_not_available)
            }
        }
        qsTile.updateTile()

        Log.i(
            TAG,
            "updateTile: New State: ${qsTile.subtitle}, isCarrierRoamingNtnModeActive: $isCarrierRoamingNtnModeActive, isCarrierRoamingNtnEligible: $isCarrierRoamingNtnEligible",
        )
    }

    companion object {
        private const val TAG = "SatelliteTileService"
        private const val REQUEST_CODE_SATELLITE_LANDING_PAGE = 1987
    }
}
