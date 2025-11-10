/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.applications.specialaccess.screenautomation

import android.app.AppOpsManager
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.spa.app.specialaccess.ComputerAutomationController
import com.android.settings.spa.app.specialaccess.ComputerControlAppRecord
import com.android.settings.spa.app.specialaccess.ComputerControlAutomationAppListModel
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class ComputerControlAutomationTest {

    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Mock private lateinit var appOpsManager: AppOpsManager

    private lateinit var context: Context
    private lateinit var listModel: ComputerControlAutomationAppListModel

    @Before
    fun setUp() {
        context = spy(ApplicationProvider.getApplicationContext<Application>())
        whenever(context.getSystemService(AppOpsManager::class.java)).thenReturn(appOpsManager)

        listModel = ComputerControlAutomationAppListModel()
    }

    private fun mockApp(packageName: String, uid: Int, hasPermission: Boolean): ApplicationInfo {
        val packageInfo =
            PackageInfo().apply {
                this.packageName = packageName
                this.applicationInfo =
                    ApplicationInfo().apply {
                        this.packageName = packageName
                        this.uid = uid
                    }
                this.requestedPermissions =
                    if (hasPermission) {
                        arrayOf(listModel.permission)
                    } else {
                        arrayOf("some.other.permission")
                    }
            }
        return packageInfo.applicationInfo!!
    }

    @Test
    fun getSummary_modeAllowed_returnsAllowedString() {
        val app = mockApp("test.app", 123, hasPermission = true)
        whenever(appOpsManager.checkOpNoThrow(listModel.appOps.op, app.uid, app.packageName))
            .thenReturn(AppOpsManager.MODE_ALLOWED)

        val summary = listModel.getSummary(context, ComputerControlAppRecord(app))

        assertThat(summary)
            .isEqualTo(context.getString(R.string.computer_control_automation_always_allowed))
    }

    @Test
    fun getSummary_modeIgnored_returnsNotAllowedString() {
        val app = mockApp("test.app", 123, hasPermission = true)
        whenever(appOpsManager.checkOpNoThrow(listModel.appOps.op, app.uid, app.packageName))
            .thenReturn(AppOpsManager.MODE_IGNORED)

        val summary = listModel.getSummary(context, ComputerControlAppRecord(app))

        assertThat(summary)
            .isEqualTo(context.getString(R.string.computer_control_automation_dont_allow))
    }

    @Test
    fun getSummary_modeDefault_returnsAskString() {
        val app = mockApp("test.app", 123, hasPermission = true)
        whenever(appOpsManager.checkOpNoThrow(listModel.appOps.op, app.uid, app.packageName))
            .thenReturn(AppOpsManager.MODE_DEFAULT)

        val summary = listModel.getSummary(context, ComputerControlAppRecord(app))

        assertThat(summary)
            .isEqualTo(context.getString(R.string.computer_control_automation_ask_every_time))
    }

    @Test
    fun controller_setMode_callsAppOpsManager() {
        val app = mockApp("test.app", 123, hasPermission = true)
        val controller = ComputerAutomationController(context, app, listModel.appOps)

        controller.setMode(AppOpsManager.MODE_ALLOWED)

        verify(appOpsManager)
            .setMode(
                AppOpsManager.OP_COMPUTER_CONTROL,
                app.uid,
                app.packageName,
                AppOpsManager.MODE_ALLOWED,
            )
    }
}
