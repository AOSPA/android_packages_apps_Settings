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

import android.app.ActivityManager
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.supervision.SupervisionSessionController.Companion.SESSION_TIMEOUT_MILLIS
import com.android.settings.supervision.SupervisionSessionController.Companion.SUPERVISION_AUTH_SESSION_KEY
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when` as whenever
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSystemClock

@RunWith(AndroidJUnit4::class)
class SupervisionSessionControllerTest {
    private val mockActivityManager = mock<ActivityManager>()
    private val mockRoleManager = mock<RoleManager>()
    private var mockContext = mock<Context>()

    @get:Rule val setFlagsRule = SetFlagsRule()

    @Before
    fun setUp() {
        SupervisionSessionController.instance = null
        whenever(mockContext.getSystemService(ActivityManager::class.java))
            .thenReturn(mockActivityManager)
        whenever(mockContext.getSystemService(RoleManager::class.java)).thenReturn(mockRoleManager)
        whenever(mockContext.applicationContext).thenReturn(mockContext)
        mockRoleManager.stub {
            on { getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION) } doReturn
                listOf(SUPERVISION_PACKAGE_NAME)
        }
        ShadowLooper.idleMainLooper()
    }

    @Test
    fun currentTaskIsNotRunning_authSessionIsNotActive() {
        setUpTaskActivity(
            listOf(RUNNING_SUPERVISION_TASK_INFO, FOCUSED_SUPERVISION_DASHBOARD_TASK_INFO)
        )

        withSupervisionSessionsRegistered {
            setUpTaskActivity(
                listOf(NOT_RUNNING_SUPERVISION_TASK_INFO, FOCUSED_SUPERVISION_DASHBOARD_TASK_INFO)
            )
            onTaskStackChanged(this)
            assertThat(isSessionActive(SUPERVISION_AUTH_SESSION_KEY)).isFalse()
        }
    }

    @Test
    fun currentTaskIsNotRunning_otherSessionIsNotActive() {
        withSupervisionSessionsRegistered {
            setUpTaskActivity(
                listOf(NOT_RUNNING_SUPERVISION_TASK_INFO, FOCUSED_SUPERVISION_DASHBOARD_TASK_INFO)
            )
            onTaskStackChanged(this)
            assertThat(isSessionActive(SESSION_KEY)).isFalse()
        }
    }

    @Test
    fun currentTaskIsRemoved_authSessionIsNotActive() {
        withSupervisionSessionsRegistered {
            setUpTaskActivity(emptyList())
            onTaskStackChanged(this)
            assertThat(isSessionActive(SUPERVISION_AUTH_SESSION_KEY)).isFalse()
        }
    }

    @Test
    fun screenOff_sessionInvalidated() {
        withSupervisionSessionsRegistered {
            val broadcastReceiverCaptor = argumentCaptor<BroadcastReceiver>()
            val intentFilterCaptor = argumentCaptor<IntentFilter>()
            verify(mockContext)
                .registerReceiver(broadcastReceiverCaptor.capture(), intentFilterCaptor.capture())

            val screenOffReceiver: BroadcastReceiver = broadcastReceiverCaptor.firstValue
            screenOffReceiver.onReceive(mockContext, Intent(Intent.ACTION_SCREEN_OFF))
            ShadowLooper.idleMainLooper()
            assertThat(isSessionActive(SESSION_KEY)).isFalse()
            verify(mockContext).unregisterReceiver(screenOffReceiver)
        }
    }

    @Test
    fun sessionTimesOut_sessionInvalidated() {
        withSupervisionSessionsRegistered {
            val timeoutMillis = SESSION_TIMEOUT_MILLIS + 1
            ShadowSystemClock.advanceBy(Duration.ofMillis(timeoutMillis))
            ShadowLooper.idleMainLooper()

            assertThat(isSessionActive(SESSION_KEY)).isFalse()
        }
    }

    private fun withSupervisionSessionsRegistered(action: SupervisionSessionController.() -> Unit) {
        val sessionsRepository = SupervisionSessionController.getInstance(mockContext)
        assertThat(sessionsRepository.isSessionActive(SESSION_KEY)).isFalse()
        sessionsRepository.startSession(TASK_ID, SESSION_KEY)
        assertThat(sessionsRepository.isSessionActive(SESSION_KEY)).isTrue()
        try {
            action(sessionsRepository)
        } finally {
            sessionsRepository.stopSession(SESSION_KEY)
            assertThat(sessionsRepository.isSessionActive(SESSION_KEY)).isFalse()
        }
    }

    private fun onTaskStackChanged(sessionsRepository: SupervisionSessionController) {
        sessionsRepository.taskStackListener.onTaskStackChanged()
        ShadowLooper.idleMainLooper()
    }

    private fun setUpTaskActivity(runningTasks: List<ActivityManager.RecentTaskInfo>) {
        val tasks = mutableListOf<ActivityManager.AppTask>()
        for (task in runningTasks) {
            val mockTask = mock<ActivityManager.AppTask>().stub { on { taskInfo } doReturn task }
            tasks.add(mockTask)
        }
        whenever(mockActivityManager.appTasks).thenReturn(tasks)
    }

    private companion object {
        const val TASK_ID = 100
        const val DASHBOARD_TASK_ID = 100
        val SUPERVISION_PACKAGE_NAME = "com.android.supervision"
        val SESSION_KEY = "session key"
        val RUNNING_SUPERVISION_TASK_INFO =
            ActivityManager.RecentTaskInfo().apply {
                taskId = TASK_ID
                isRunning = true
                isFocused = true
                topActivity = ComponentName(SUPERVISION_PACKAGE_NAME, "SomeSupervisionActivity")
            }
        val NOT_RUNNING_SUPERVISION_TASK_INFO =
            ActivityManager.RecentTaskInfo().apply {
                taskId = TASK_ID
                isRunning = false
                isFocused = false
                topActivity = ComponentName(SUPERVISION_PACKAGE_NAME, "SomeSupervisionActivity")
            }
        val FOCUSED_SUPERVISION_DASHBOARD_TASK_INFO =
            ActivityManager.RecentTaskInfo().apply {
                taskId = DASHBOARD_TASK_ID
                isRunning = true
                isFocused = true
                topActivity =
                    ComponentName(
                        "com.android.settings",
                        SupervisionDashboardActivity::class.java.name,
                    )
            }
    }
}
