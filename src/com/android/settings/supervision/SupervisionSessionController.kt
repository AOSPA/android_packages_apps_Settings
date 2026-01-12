/*
 * Copyright (C) 2026 The Android Open Source Project
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

import android.annotation.MainThread
import android.app.ActivityManager
import android.app.ActivityTaskManager
import android.app.TaskStackListener
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import androidx.annotation.VisibleForTesting
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Manages supervision settings sessions.
 *
 * This class is used to keep track of temporary system states within supervision settings, such as
 * a temporarily lifted user restriction or an ongoing authentication flow. Sessions are identified
 * by a unique key and are associated with a specific task ID.
 *
 * All active sessions are automatically canceled when:
 * - The screen turns off.
 * - A session times out after [SESSION_TIMEOUT_MILLIS].
 * - Task lifecycle changes: 1. For auth sessions -- when no SupervisionDashboardActivity is running
 *   and the session-associated task is backgrounded or exited. 2. For other sessions -- when the
 *   associated task is backgrounded or exited.
 */
class SupervisionSessionController private constructor(private val appContext: Context) {

    private val handler = Handler(Looper.getMainLooper())
    private val activityManager = appContext.getSystemService(ActivityManager::class.java)
    private var sessions: MutableMap<String, SupervisionSession> = mutableMapOf()

    // Runnable to handle the delayed unregistration
    private val cleanupRunnable = Runnable {
        if (sessions.isEmpty()) {
            unregisterEventAndTaskListeners()
        }
    }

    val screenOffReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                handler.post { clearAllSessions() }
            }
        }

    @VisibleForTesting
    val taskStackListener: TaskStackListener =
        object : TaskStackListener() {
            override fun onTaskStackChanged() {
                handler.post {
                    val keysToProcess = sessions.keys.toList()
                    keysToProcess.forEach { sessionKey ->
                        sessions[sessionKey]?.let { session ->
                            if (
                                isTaskExit(session.taskId, activityManager.appTasks ?: emptyList())
                            ) {
                                stopSession(sessionKey)
                            }
                        }
                    }
                }
            }
        }

    @MainThread
    fun startSession(taskId: Int, sessionKey: String) {
        handler.removeCallbacks(cleanupRunnable)

        registerEventAndTaskListeners()

        // Cancel existing timeout if session is being refreshed.
        sessions[sessionKey]?.let { handler.removeCallbacks(it.timeoutRunnable) }

        val timeoutRunnable = SessionTimeoutRunnable(sessionKey)
        sessions[sessionKey] = SupervisionSession(taskId, timeoutRunnable)

        handler.postDelayed(timeoutRunnable, SESSION_TIMEOUT_MILLIS)
    }

    @MainThread
    fun isSessionActive(sessionKey: String): Boolean {
        return sessions.containsKey(sessionKey)
    }

    @MainThread
    fun stopSession(sessionKey: String) {
        sessions.remove(sessionKey)?.let { handler.removeCallbacks(it.timeoutRunnable) }
        if (sessions.isEmpty()) {
            handler.removeCallbacks(cleanupRunnable)
            handler.postDelayed(cleanupRunnable, CLEANUP_DELAY_MILLIS)
        }
    }

    @MainThread
    private fun clearAllSessions() {
        sessions.values.forEach { handler.removeCallbacks(it.timeoutRunnable) }
        sessions.clear()
        unregisterEventAndTaskListeners()
        handler.removeCallbacks(cleanupRunnable)
    }

    private fun registerEventAndTaskListeners() {
        if (sessions.isEmpty()) {
            ActivityTaskManager.getInstance().registerTaskStackListener(taskStackListener)
            val offFilter = IntentFilter(Intent.ACTION_SCREEN_OFF)
            offFilter.addAction(Intent.ACTION_USER_PRESENT)
            appContext.registerReceiver(screenOffReceiver, offFilter)
        }
    }

    private fun unregisterEventAndTaskListeners() {
        ActivityTaskManager.getInstance().unregisterTaskStackListener(taskStackListener)
        try {
            appContext.unregisterReceiver(screenOffReceiver)
        } catch (e: IllegalArgumentException) {
            // Ignore exception if receiver is not registered.
        }
    }

    private fun isTaskExit(taskId: Int, appTasks: List<ActivityManager.AppTask>): Boolean {
        val task = appTasks.find { it.taskInfo?.taskId == taskId }
        if (task == null) return true
        return (task.taskInfo?.isRunning != true) || (task.taskInfo?.isFocused != true)
    }

    private data class SupervisionSession(
        val taskId: Int,
        val timeoutRunnable: SessionTimeoutRunnable,
    )

    private inner class SessionTimeoutRunnable(val sessionKey: String) : Runnable {
        override fun run() {
            stopSession(sessionKey)
        }
    }

    companion object {
        const val SUPERVISION_AUTH_SESSION_KEY = "supervision_auth_session_key"
        const val FACTORY_RESET_RESTRICTION_KEY = "factory_reset_restriction_key"
        val SESSION_TIMEOUT_MILLIS = 10.minutes.inWholeMilliseconds
        val CLEANUP_DELAY_MILLIS = 1.seconds.inWholeSeconds

        @JvmStatic
        fun getInstance(context: Context): SupervisionSessionController {
            return instance
                ?: synchronized(this) {
                    instance
                        ?: SupervisionSessionController(context.applicationContext).also {
                            instance = it
                        }
                }
        }

        @Volatile @VisibleForTesting var instance: SupervisionSessionController? = null
    }
}
