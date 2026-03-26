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
import android.companion.virtual.VirtualDeviceManager
import android.companion.virtual.computercontrol.ComputerControlConsentManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.spa.app.specialaccess.ComputerControlConsentController
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
@Suppress("MissingPermission")
class ComputerControlAutomationTest {

    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Mock private lateinit var appOpsManager: AppOpsManager
    @Mock private lateinit var virtualDeviceManager: VirtualDeviceManager
    @Mock private lateinit var consentManager: ComputerControlConsentManager
    @Mock private lateinit var packageManager: PackageManager

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = spy(ApplicationProvider.getApplicationContext<Application>())
        whenever(context.getSystemService(AppOpsManager::class.java)).thenReturn(appOpsManager)
        whenever(context.getSystemService(VirtualDeviceManager::class.java))
            .thenReturn(virtualDeviceManager)
        whenever(virtualDeviceManager.computerControlConsentManager).thenReturn(consentManager)
        whenever(context.packageManager).thenReturn(packageManager)
    }

    private fun mockApp(packageName: String, uid: Int): ApplicationInfo {
        val packageInfo =
            PackageInfo().apply {
                this.packageName = packageName
                this.applicationInfo =
                    ApplicationInfo().apply {
                        this.packageName = packageName
                        this.uid = uid
                    }
            }
        return packageInfo.applicationInfo!!
    }

    @Test
    fun consentController_setAppOpMode_callsAppOpsManager() {
        val app = mockApp("test.app", 123)
        val controller = ComputerControlConsentController(context, app)

        controller.setAppOpMode(AppOpsManager.MODE_ALLOWED)

        verify(appOpsManager)
            .setMode(
                AppOpsManager.OP_COMPUTER_CONTROL,
                app.uid,
                app.packageName,
                AppOpsManager.MODE_ALLOWED,
            )
    }

    @Test
    fun consentController_getAutomatablePackages_callsConsentManager() {
        val app = mockApp("test.app", 123)
        val controller = ComputerControlConsentController(context, app)
        val expectedPackages = arrayOf("com.example.app1", "com.example.app2")
        whenever(consentManager.getAutomatableAppListForAgent(app.uid, app.packageName))
            .thenReturn(expectedPackages)

        val packages = controller.getAutomatablePackages()

        assertThat(packages).containsExactlyElementsIn(expectedPackages)
    }

    @Test
    fun consentController_clearAutomatablePackages_callsConsentManager() {
        val app = mockApp("test.app", 123)
        val controller = ComputerControlConsentController(context, app)

        controller.clearAutomatablePackages()

        verify(consentManager).clearAutomatableAppListForAgent(app.uid, app.packageName)
    }

    @Test
    fun consentController_removeAutomatablePackage_callsConsentManager() {
        val app = mockApp("test.app", 123)
        val controller = ComputerControlConsentController(context, app)
        val targetPackage = "com.example.target"

        controller.removeAutomatablePackage(targetPackage)

        verify(consentManager)
            .removeAppFromAutomatableAppListForAgent(app.uid, app.packageName, targetPackage)
    }
}
