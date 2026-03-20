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

import android.app.Activity
import android.app.NotificationManager
import android.app.StatusBarManager
import android.app.settings.SettingsEnums
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Bundle
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.graphics.drawable.toBitmap
import com.android.settings.R
import com.android.settings.overlay.FeatureFactory

/**
 * An activity that is launched from a notification to prompt the user to add the satellite quick
 * settings tile.
 *
 * This activity is transparent and its only purpose is to provide a foreground context to call
 * [StatusBarManager.requestAddTileService]. We are unable to call that API from
 * [SatelliteEligibilityListenerService], therefore we must use a notification to launch this
 * activity which triggers the request to add the tile.
 */
class AddSatelliteTileActivity : Activity() {

    @VisibleForTesting internal var satelliteTilePromptUtils = SatelliteTilePromptUtils()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Dismiss the notification that launched this activity.
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        if (notificationId != 0) {
            getSystemService(NotificationManager::class.java)?.cancel(notificationId)
        }

        val statusBarManager = getSystemService(StatusBarManager::class.java)
        val componentName = ComponentName(this, SatelliteTileService::class.java)

        Log.i(TAG, "Requesting to add SatelliteTileService")

        val drawable = getDrawable(R.drawable.ic_satellite_tile)
        if (drawable == null) {
            Log.e(TAG, "Icon drawable not found")
            finish()
            return
        }
        // We are using Icon.createWithBitmap instead of Icon.createWithResource because the latter
        // may not work for requestAddTileService due to a permission check.
        val icon = Icon.createWithBitmap(drawable.toBitmap())
        statusBarManager.requestAddTileService(
            componentName,
            getString(R.string.satellite_tile_label),
            icon,
            mainExecutor,
            { result ->
                Log.i(TAG, "requestAddTileService result: $result")

                // Log success only when the tile is actually added
                if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED) {
                    FeatureFactory.featureFactory.metricsFeatureProvider.action(
                        this,
                        SettingsEnums.ACTION_SATELLITE_NOTIFICATION_ADD_TILE,
                    )
                }

                when (result) {
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED,
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED,
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED,
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_DIALOG_DISMISSED,
                    StatusBarManager.TILE_ADD_REQUEST_ERROR_NOT_CURRENT_USER -> {
                        // 1. If the tile was added, the user was prompted and accepted, so we
                        // should mark as shown so we don't prompt again.
                        // 2. If the tile was not added, the user was prompted and declined, so we
                        // should mark as shown so we don't potentially spam the user.
                        // 3. If the tile is already added, then we'll assume the prompt has already
                        // been shown.
                        // 4. If the dialog was dismissed, the user was prompted and dismissed it,
                        // so we should mark as shown so we don't potentially spam the user.
                        // 5. If the user is not the current user, then we should mark as shown to
                        // avoid potentially spamming the user (the user likely does not have
                        // permissions to add the tile).
                        satelliteTilePromptUtils.markAllPromptsShown(this)
                    }
                    else -> {
                        // An error occurred, so we don't mark the prompt as shown so that we can
                        // try again later.
                        Log.w(TAG, "requestAddTileService returned an error: $result")
                    }
                }
                finish()
            },
        )
    }

    companion object {
        private const val TAG = "AddSatelliteTileActivity"

        /**
         * Intent extra key for the ID of the notification that launched this activity. This is used
         * to cancel the notification when the user interacts with it.
         */
        internal const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}
