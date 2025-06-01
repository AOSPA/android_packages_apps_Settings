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
package com.android.settings.supervision

import android.app.ActivityManager
import android.app.ActivityTaskManager
import android.app.TaskStackListener
import android.content.Context
import androidx.annotation.VisibleForTesting
import javax.annotation.concurrent.GuardedBy

/**
 * Manages a supervision authentication session.
 *
 * After the supervising profile is authenticated, the user should not need to authenticate again
 * unless they leave the Settings app. This session should persist across interactions with native
 * settings and those injected from the separate supervision APK.
 *
 * Activities responsible for authentication should start a session only after the user has been
 * successfully authenticated, and the session will be automatically invalidated when the task that
 * started the session stops running or goes into the background.
 */
class SupervisionAuthController private constructor(appContext: Context) {
    private val activityManager = appContext.getSystemService(ActivityManager::class.java)
    @GuardedBy("this") private var currentTaskId: Int? = null

    @VisibleForTesting
    val mTaskStackListener: TaskStackListener =
        object : TaskStackListener() {
            override fun onTaskStackChanged() {
                synchronized(this) {
                    if (currentTaskId != null && !isCurrentTaskFocused()) {
                        invalidateSession()
                    }
                }
            }
        }

    init {
        ActivityTaskManager.getInstance().registerTaskStackListener(mTaskStackListener)
    }

    /**
     * Starts an auth session, indicating that a parent has been authenticated on the current task.
     */
    fun startSession(taskId: Int) {
        synchronized(this) { currentTaskId = taskId }
    }

    /** Returns whether an auth session is currently active on this task. */
    fun isSessionActive(taskId: Int): Boolean {
        synchronized(this) {
            return currentTaskId == taskId
        }
    }

    /**
     * Invalidates the current session. This should only be done if a task with an active session
     * goes into the background.
     */
    @GuardedBy("this")
    private fun invalidateSession() {
        currentTaskId = null
    }

    /** Whether the task with a currently active auth session is running and focused. */
    @GuardedBy("this")
    private fun isCurrentTaskFocused(): Boolean {
        if (currentTaskId == null) return false
        val appTasks = activityManager.appTasks ?: emptyList()
        val task = appTasks.find { it.taskInfo.taskId == currentTaskId }
        if (task == null) return false
        return task.taskInfo.isRunning && task.taskInfo.isFocused
    }

    companion object {
        @Volatile @VisibleForTesting var sInstance: SupervisionAuthController? = null

        fun getInstance(context: Context): SupervisionAuthController {
            return sInstance
                ?: synchronized(this) {
                    sInstance
                        ?: SupervisionAuthController(context.applicationContext).also {
                            sInstance = it
                        }
                }
        }
    }
}
