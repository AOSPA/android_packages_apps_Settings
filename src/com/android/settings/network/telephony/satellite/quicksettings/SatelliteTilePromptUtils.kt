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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.core.app.NotificationCompat
import com.android.settings.R

/** Utility class for handling the satellite tile prompt notification and preferences. */
open class SatelliteTilePromptUtils {
    companion object {
        private const val TAG = "SatelliteTilePromptUtils"
        private const val NOTIFICATION_CHANNEL_ID = "satellite_tile_prompt_channel"
        private const val SHARED_PREFS_NAME = "SatelliteTilePromptPrefs"
        private const val PROMPT_SHOWN_KEY = "prompt_shown"
        const val ACTION_DISMISS =
            "com.android.settings.network.telephony.satellite.quicksettings.ACTION_DISMISS"
    }

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** Returns true if the prompt to add the satellite tile has already been shown to the user. */
    open fun hasAddTilePromptBeenShown(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(PROMPT_SHOWN_KEY, false)
    }

    /** Sets the flag indicating that the prompt to add the satellite tile has been shown. */
    open fun setAddTilePromptShown(context: Context, shown: Boolean) {
        getSharedPreferences(context).edit().putBoolean(PROMPT_SHOWN_KEY, shown).apply()
        Log.d(TAG, "setAddTilePromptShown: $shown")
    }

    /**
     * Shows a notification indicating that satellite is available, which triggers the
     * AddSatelliteTileActivity.
     */
    open fun showSatelliteTileAvailableNotification(context: Context) {
        val notificationManager =
            context.getSystemService(NotificationManager::class.java) ?: return

        val channel =
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.satellite_tile_prompt_channel_title),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        notificationManager.createNotificationChannel(channel)

        val intent =
            Intent(context, AddSatelliteTileActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(
                    AddSatelliteTileActivity.EXTRA_NOTIFICATION_ID,
                    R.id.satellite_prompt_notification_id,
                )
            }
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val dismissIntent =
            Intent(context, SatellitePromptDismissalReceiver::class.java).apply {
                action = ACTION_DISMISS
            }
        val dismissPendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_satellite_tile)
                .setContentTitle(
                    context.getString(R.string.satellite_tile_prompt_notification_title)
                )
                .setContentText(
                    context.getString(R.string.satellite_tile_prompt_notification_summary)
                )
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(
                            context.getString(R.string.satellite_tile_prompt_notification_summary)
                        )
                )
                .addAction(0, context.getString(R.string.add_tile_action), pendingIntent)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setDeleteIntent(dismissPendingIntent)
                .setOnlyAlertOnce(true)
                .build()

        notificationManager.notify(R.id.satellite_prompt_notification_id, notification)
    }

    /** Receiver to handle the dismissal of the Satellite Quick Settings prompt notification. */
    class SatellitePromptDismissalReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_DISMISS == intent.action) {
                Log.d(TAG, "Notification dismissed by user. Marking prompt as shown.")
                SatelliteTilePromptUtils().setAddTilePromptShown(context, true)
            }
        }
    }
}
