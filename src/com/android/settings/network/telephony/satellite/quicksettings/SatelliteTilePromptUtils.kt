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
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.core.app.NotificationCompat
import com.android.settings.R
import com.android.settings.overlay.FeatureFactory
import java.util.concurrent.TimeUnit

/** Utility class for handling the satellite tile prompt notification and preferences. */
open class SatelliteTilePromptUtils {
    companion object {
        private const val TAG = "SatelliteTilePromptUtils"
        private const val NOTIFICATION_CHANNEL_ID = "satellite_tile_prompt_channel"
        private const val SHARED_PREFS_NAME = "SatelliteTilePromptPrefs"
        private const val PREF_PROMPT_COUNT = "satellite_tile_prompt_count"
        private const val PREF_LAST_PROMPT_TIMESTAMP = "satellite_tile_prompt_last_timestamp"
        private const val MAX_PROMPTS = 4 // Initial + 3 retries

        // The intervals *after* which a prompt should be shown again, indexed by the number of
        // prompts already shown. The first prompt (index 0) has no delay.
        private val RETRY_INTERVALS_MS =
            longArrayOf(
                0L, // Show immediately for the 1st time (count = 0)
                TimeUnit.HOURS.toMillis(24), // Wait 24h for the 2nd time (count = 1)
                TimeUnit.DAYS.toMillis(7), // Wait 7d for the 3rd time (count = 2)
                TimeUnit.DAYS.toMillis(28), // Wait 28d for the 4th time (count = 3)
            )
    }

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getPromptCount(context: Context): Int {
        return getSharedPreferences(context).getInt(PREF_PROMPT_COUNT, 0)
    }

    /**
     * Checks if the satellite tile prompt should be permanently suppressed, either because the user
     * has already added the tile or all retry attempts have been exhausted.
     *
     * @return `true` if no more prompts should be shown, `false` otherwise.
     */
    open fun areAllPromptsShown(context: Context): Boolean {
        return getPromptCount(context) >= MAX_PROMPTS
    }

    /**
     * Permanently marks the prompt as "shown" by setting the prompt count to the maximum value.
     * This is used when the user affirmatively acts on the prompt (e.g., adds the tile), preventing
     * any future notifications.
     */
    open fun markAllPromptsShown(context: Context) {
        getSharedPreferences(context).edit().putInt(PREF_PROMPT_COUNT, MAX_PROMPTS).apply()
        Log.i(TAG, "All prompts marked as shown; no future prompts will be displayed.")
    }

    /**
     * Determines if a satellite tile prompt should be shown based on the retry logic.
     *
     * This checks the number of times the prompt has been shown and the time elapsed since the last
     * prompt to decide if it's time for the next one.
     *
     * @return `true` if the notification should be displayed, `false` otherwise.
     */
    open fun shouldShowSatelliteTilePrompt(context: Context): Boolean {
        val count = getPromptCount(context)
        if (areAllPromptsShown(context)) {
            Log.i(TAG, "shouldShowSatelliteTilePrompt: false, all prompts shown.")
            return false
        }

        // The first prompt is always shown.
        if (count == 0) {
            Log.i(TAG, "shouldShowSatelliteTilePrompt: true, first prompt.")
            return true
        }

        val lastTimestamp = getSharedPreferences(context).getLong(PREF_LAST_PROMPT_TIMESTAMP, 0L)
        val timeSinceLastPrompt = System.currentTimeMillis() - lastTimestamp
        val requiredInterval = RETRY_INTERVALS_MS.getOrElse(count) { Long.MAX_VALUE }

        val shouldShow = timeSinceLastPrompt >= requiredInterval
        Log.i(
            TAG,
            "shouldShowSatelliteTilePrompt: $shouldShow. " +
                "Count=$count, " +
                "TimeSinceLastPrompt=${timeSinceLastPrompt}ms, " +
                "RequiredInterval=${requiredInterval}ms",
        )
        return shouldShow
    }

    /** Increments the prompt count and records the current timestamp. */
    open fun recordPromptShown(context: Context) {
        val sharedPreferences = getSharedPreferences(context)
        val currentCount = sharedPreferences.getInt(PREF_PROMPT_COUNT, 0)
        val newCount = currentCount + 1
        sharedPreferences
            .edit()
            .putInt(PREF_PROMPT_COUNT, newCount)
            .putLong(PREF_LAST_PROMPT_TIMESTAMP, System.currentTimeMillis())
            .apply()
        logd { "recordPromptShown: newCount=$newCount" }
    }

    private inline fun logd(message: () -> String) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, message())
        }
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

        val notification =
            NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_satellite_tile)
                .setContentTitle(
                    context.getString(R.string.satellite_tile_prompt_notification_title)
                )
                .setContentText(
                    context.getString(R.string.satellite_tile_prompt_notification_summary)
                )
                // Don't prompt user to add phone tile on paired watch, etc.
                .setLocalOnly(true)
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
                .setOnlyAlertOnce(true)
                .build()

        FeatureFactory.featureFactory.metricsFeatureProvider.action(
            context,
            SettingsEnums.ACTION_SATELLITE_NOTIFICATION_SHOWN,
        )
        notificationManager.notify(R.id.satellite_prompt_notification_id, notification)
    }
}
