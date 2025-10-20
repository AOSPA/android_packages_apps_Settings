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

package com.android.settings.safetycenter.ui

import android.Manifest
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import com.android.settings.R

/**
 * Utility object for sending PendingIntents from Safety Center UI components in Settings. It
 * handles logic for determining the appropriate task ID and ActivityOptions for launching the
 * intent.
 */
object PendingIntentSender {

    private const val TAG = "PendingIntentSender"

    /**
     * Sends the given PendingIntent.
     *
     * It uses [getTaskIdToSend] to determine whether to use the provided [launchTaskId] or to
     * launch the intent in a new task. ActivityOptions are created to handle task ID and background
     * start permissions.
     *
     * @param context The context to use for accessing resources.
     * @param pendingIntent The PendingIntent to send.
     * @param safetySourceId The ID of the safety source associated with the intent.
     * @param launchTaskId The task ID to potentially launch the intent in, if applicable.
     * @return true if the intent was sent successfully, false otherwise.
     */
    @RequiresPermission(Manifest.permission.START_TASKS_FROM_RECENTS)
    fun sendIntent(
        context: Context,
        pendingIntent: PendingIntent?,
        safetySourceId: String,
        launchTaskId: Int? = null,
    ): Boolean {
        Log.d(TAG, "sendIntent called for source: $safetySourceId, launchTaskId: $launchTaskId")

        if (pendingIntent == null) {
            Log.e(TAG, "PendingIntent is null for $safetySourceId, cannot send.")
            return false
        }

        try {
            val taskIdToSend = getTaskIdToSend(context, safetySourceId, launchTaskId)
            val activityOptions = createActivityOptions(pendingIntent, taskIdToSend)
            send(pendingIntent, activityOptions)
            return true
        } catch (ex: PendingIntent.CanceledException) {
            Log.e(TAG, "Failed to execute pending intent for $safetySourceId", ex)
            return false
        }
    }

    private fun getTaskIdToSend(
        context: Context,
        safetySourceId: String,
        launchTaskId: Int?,
    ): Int? {
        val sameTaskSourceIds =
            context.resources.getString(R.string.config_same_task_safety_source_ids).split(',')
        val useSameTask = sameTaskSourceIds.contains(safetySourceId)
        return if (useSameTask) {
            Log.d(TAG, "Using provided launchTaskId: $launchTaskId")
            launchTaskId
        } else {
            Log.d(TAG, "Not in same task safety source config, using null taskId")
            null
        }
    }

    /** Sends a PendingIntent with the given ActivityOptions. */
    private fun send(pendingIntent: PendingIntent, activityOptions: ActivityOptions?) {
        if (activityOptions == null) {
            pendingIntent.send()
            return
        }
        pendingIntent.send(
            /* context= */ null,
            /* code= */ 0,
            /* intent= */ null,
            /* onFinished= */ null,
            /* handler= */ null,
            /* requiredPermission= */ null,
            activityOptions.toBundle(),
        )
    }

    @RequiresPermission(Manifest.permission.START_TASKS_FROM_RECENTS)
    private fun createActivityOptions(
        pendingIntent: PendingIntent,
        launchTaskId: Int?,
    ): ActivityOptions? {
        if (!pendingIntent.isActivity) {
            Log.d(TAG, "Not an activity PendingIntent, no ActivityOptions needed.")
            return null
        }
        val activityOptions = ActivityOptions.makeBasic()
        if (launchTaskId != null) {
            activityOptions.launchTaskId = launchTaskId
        }
        activityOptions.setPendingIntentBackgroundActivityStartMode(
            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
        )
        return activityOptions
    }
}
