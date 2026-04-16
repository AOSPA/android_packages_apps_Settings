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
import android.app.settings.SettingsEnums
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.telephony.SubscriptionManager
import android.telephony.satellite.SatelliteManager
import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import com.android.settings.R
import com.android.settings.overlay.FeatureFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * A [TileService] that provides a Quick Settings tile for satellite connectivity.
 *
 * This service delegates state monitoring to [SatelliteStateRepository] and strictly handles UI
 * updates.
 */
open class SatelliteTileService : TileService() {

    @VisibleForTesting internal var satelliteTilePromptUtils = SatelliteTilePromptUtils()

    // Using Main scope for UI updates.
    private val scope = CoroutineScope(Dispatchers.Main)
    private var updateJob: Job? = null

    override fun onStartListening() {
        Log.i(TAG, "onStartListening")
        super.onStartListening()
        val repo = SatelliteStateRepository.getInstance(this)
        // Ensure any previous job is cancelled before starting a new one.
        updateJob?.cancel()
        updateJob =
            scope.launch {
                repo.satelliteStatus.collect { status ->
                    logd { "onStartListening: status=$status" }
                    Log.i(TAG, "onStartListening: status=$status")
                    updateTile(status)
                }
            }
    }

    override fun onStopListening() {
        Log.i(TAG, "onStopListening")
        super.onStopListening()
        updateJob?.cancel()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        FeatureFactory.featureFactory.metricsFeatureProvider.action(
            this,
            SettingsEnums.QS_SATELLITE_TILE_ADDED,
        )
        satelliteTilePromptUtils.markAllPromptsShown(this)
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        FeatureFactory.featureFactory.metricsFeatureProvider.action(
            this,
            SettingsEnums.QS_SATELLITE_TILE_REMOVED,
        )
    }

    override fun onClick() {
        super.onClick()
        unlockAndRun {
            val intent = SatelliteUtils.resolveSatelliteSettingsIntent(this)
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
        super.onDestroy()
        scope.cancel()
    }

    @MainThread
    @VisibleForTesting
    fun updateTile(status: SatelliteStatus) {
        val qsTile = qsTile ?: return

        val (newState, newSubtitleRes) =
            when (status) {
                SatelliteStatus.ACTIVE -> Tile.STATE_ACTIVE to R.string.satellite_tile_subtitle_on
                SatelliteStatus.AVAILABLE ->
                    Tile.STATE_INACTIVE to R.string.satellite_tile_subtitle_available
                SatelliteStatus.NOT_AVAILABLE ->
                    Tile.STATE_INACTIVE to R.string.satellite_tile_subtitle_not_available
            }

        val newSubtitle = getString(newSubtitleRes)
        if (qsTile.state != newState || qsTile.subtitle != newSubtitle) {
            qsTile.state = newState
            qsTile.subtitle = newSubtitle
            qsTile.updateTile()
            Log.i(TAG, "Updated tile: state=$newState, subtitle=$newSubtitle")
        }
    }

    companion object {
        private const val TAG = "SatelliteTileService"
        private const val REQUEST_CODE_SATELLITE_LANDING_PAGE = 1987
    }

    private inline fun logd(message: () -> String) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, message())
        }
    }
}
