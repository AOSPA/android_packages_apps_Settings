/*
 * Copyright 2026 The Android Open Source Project
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
import android.companion.virtual.VirtualDeviceManager
import android.companion.virtual.computercontrol.ComputerControlConsentManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.spa.app.specialaccess.ComputerControlAgentPageModel
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
@Suppress("MissingPermission")
class ComputerControlAgentPageProviderTest {

    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()
    @get:Rule val setFlagsRule = SetFlagsRule()

    @Mock private lateinit var appOpsManager: AppOpsManager
    @Mock private lateinit var virtualDeviceManager: VirtualDeviceManager
    @Mock private lateinit var consentManager: ComputerControlConsentManager
    @Mock private lateinit var packageManager: PackageManager

    private lateinit var context: Context
    private lateinit var listModel: ComputerControlAgentPageModel

    @Before
    fun setUp() {
        context = spy(ApplicationProvider.getApplicationContext<Application>())
        whenever(context.getSystemService(AppOpsManager::class.java)).thenReturn(appOpsManager)
        whenever(context.getSystemService(VirtualDeviceManager::class.java))
            .thenReturn(virtualDeviceManager)
        whenever(virtualDeviceManager.computerControlConsentManager).thenReturn(consentManager)
        whenever(context.packageManager).thenReturn(packageManager)
        listModel = ComputerControlAgentPageModel(context)
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
    @DisableFlags(android.companion.virtualdevice.flags.Flags.FLAG_COMPUTER_CONTROL_PER_APP_CONSENT)
    fun getSummary_perAppConsentDisabled_modeAllowed_returnsAlwaysAllowedString() {
        val app = mockApp("test.app", 123, hasPermission = true)
        whenever(
                appOpsManager.checkOpNoThrow(
                    AppOpsManager.OP_COMPUTER_CONTROL,
                    app.uid,
                    app.packageName,
                )
            )
            .thenReturn(AppOpsManager.MODE_ALLOWED)

        val summary = listModel.getSummary(context, app)

        assertThat(summary)
            .isEqualTo(context.getString(R.string.computer_control_automation_always_allow))
    }

    @Test
    @DisableFlags(android.companion.virtualdevice.flags.Flags.FLAG_COMPUTER_CONTROL_PER_APP_CONSENT)
    fun getSummary_perAppConsentDisabled_modeIgnored_returnsDontAllowString() {
        val app = mockApp("test.app", 123, hasPermission = true)
        whenever(
                appOpsManager.checkOpNoThrow(
                    AppOpsManager.OP_COMPUTER_CONTROL,
                    app.uid,
                    app.packageName,
                )
            )
            .thenReturn(AppOpsManager.MODE_IGNORED)

        val summary = listModel.getSummary(context, app)

        assertThat(summary)
            .isEqualTo(context.getString(R.string.computer_control_automation_dont_allow))
    }

    @Test
    @DisableFlags(android.companion.virtualdevice.flags.Flags.FLAG_COMPUTER_CONTROL_PER_APP_CONSENT)
    fun getSummary_perAppConsentDisabled_modeDefault_returnsAskString() {
        val app = mockApp("test.app", 123, hasPermission = true)
        whenever(
                appOpsManager.checkOpNoThrow(
                    AppOpsManager.OP_COMPUTER_CONTROL,
                    app.uid,
                    app.packageName,
                )
            )
            .thenReturn(AppOpsManager.MODE_DEFAULT)

        val summary = listModel.getSummary(context, app)

        assertThat(summary)
            .isEqualTo(context.getString(R.string.computer_control_automation_ask_every_time))
    }

    @Test
    @EnableFlags(android.companion.virtualdevice.flags.Flags.FLAG_COMPUTER_CONTROL_PER_APP_CONSENT)
    fun getSummary_perAppConsentEnabled_noAppsAllowed_returnsNoAppsAllowedString() {
        val app = mockApp("test.app", 123, hasPermission = true)
        whenever(consentManager.getAutomatableAppListForAgent(app.uid, app.packageName))
            .thenReturn(arrayOf())

        val summary = listModel.getSummary(context, app)

        assertThat(summary)
            .isEqualTo(context.getString(R.string.computer_control_automation_no_apps_allowed))
    }

    @Test
    @EnableFlags(android.companion.virtualdevice.flags.Flags.FLAG_COMPUTER_CONTROL_PER_APP_CONSENT)
    fun getSummary_perAppConsentEnabled_oneAppAllowed_returnsOneAppAllowedString() {
        val app = mockApp("test.app", 123, hasPermission = true)
        whenever(consentManager.getAutomatableAppListForAgent(app.uid, app.packageName))
            .thenReturn(arrayOf("target.app"))

        val summary = listModel.getSummary(context, app)

        assertThat(summary).contains("1 app allowed")
    }

    @Test
    @EnableFlags(android.companion.virtualdevice.flags.Flags.FLAG_COMPUTER_CONTROL_PER_APP_CONSENT)
    fun getSummary_perAppConsentEnabled_multipleAppsAllowed_returnsCountString() {
        val app = mockApp("test.app", 123, hasPermission = true)
        whenever(consentManager.getAutomatableAppListForAgent(app.uid, app.packageName))
            .thenReturn(arrayOf("target.app1", "target.app2"))

        val summary = listModel.getSummary(context, app)

        assertThat(summary).contains("2 apps allowed")
    }
}
