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
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class SupervisionAuthControllerTest {

    private val mockActivityManager = mock<ActivityManager>()
    private val context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getSystemService(name: String): Any =
                when (name) {
                    getSystemServiceName(ActivityManager::class.java) -> mockActivityManager
                    else -> super.getSystemService(name)
                }
        }

    @Before
    fun setUp() {
        SupervisionAuthController.sInstance = null
    }

    @Test
    fun initially_sessionIsNotActive() {
        val mockTask =
            mock<ActivityManager.AppTask>().stub { on { taskInfo } doReturn FOCUSED_TASK_INFO }
        mockActivityManager.stub { on { appTasks } doReturn listOf(mockTask) }

        val authController = SupervisionAuthController.getInstance(context)
        assertThat(authController.isSessionActive(TASK_ID)).isFalse()
    }

    @Test
    fun startSession_sessionIsActive() {
        val mockTask =
            mock<ActivityManager.AppTask>().stub { on { taskInfo } doReturn FOCUSED_TASK_INFO }
        mockActivityManager.stub { on { appTasks } doReturn listOf(mockTask) }

        val authController = SupervisionAuthController.getInstance(context)
        authController.startSession(TASK_ID)
        assertThat(authController.isSessionActive(TASK_ID)).isTrue()
    }

    @Test
    fun taskLosesFocus_sessionInvalidated() {
        val mockTask =
            mock<ActivityManager.AppTask>().stub { on { taskInfo } doReturn FOCUSED_TASK_INFO }
        mockActivityManager.stub { on { appTasks } doReturn listOf(mockTask) }

        val authController = SupervisionAuthController.getInstance(context)
        authController.startSession(TASK_ID)
        assertThat(authController.isSessionActive(TASK_ID)).isTrue()

        mockTask.stub { on { taskInfo } doReturn NOT_FOCUSED_TASK_INFO }
        authController.mTaskStackListener.onTaskStackChanged()
        assertThat(authController.isSessionActive(TASK_ID)).isFalse()
    }

    private companion object {
        const val TASK_ID = 100
        val FOCUSED_TASK_INFO =
            ActivityManager.RecentTaskInfo().apply {
                taskId = TASK_ID
                isRunning = true
                isFocused = true
            }
        val NOT_FOCUSED_TASK_INFO =
            ActivityManager.RecentTaskInfo().apply {
                taskId = TASK_ID
                isRunning = true
                isFocused = false
            }
    }
}
