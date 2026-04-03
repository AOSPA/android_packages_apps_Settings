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

package com.android.settings.notification.app

import android.Manifest.permission.STATUS_BAR_SERVICE
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.ApplicationInfo.FLAG_INSTALLED
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.applications.AppInfoBase.ARG_PACKAGE_NAME
import com.android.settings.flags.Flags.FLAG_CATALYST_MIGRATION_26Q2
import com.android.settings.notification.NotificationBackend
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.MissingPermissionException
import com.android.settings.testutils2.Parameters
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class AppNotificationSettingsApiScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private lateinit var context: Context
    private lateinit var tester: ApiTester
    private val packageManager = mock<PackageManager>()
    private lateinit var backend: NotificationBackend

    @Before
    fun setUp() {
        // Sadly we cannot simply do shadowOf(packageManager).installPackage(packageInfo) because
        // AppListRepository calls hidden APIs not supported by Robolectric. Mock stuff instead.
        context =
            spy(ApplicationProvider.getApplicationContext()) {
                on { packageManager } doReturn packageManager
            }
        val appInfo =
            ApplicationInfo().apply {
                packageName = INSTALLED_PACKAGE
                this.enabled = true
                this.flags = FLAG_INSTALLED
            }
        val pkgInfo =
            PackageInfo().apply {
                packageName = INSTALLED_PACKAGE
                applicationInfo = appInfo
            }
        packageManager.stub {
            on {
                getInstalledApplicationsAsUser(any<PackageManager.ApplicationInfoFlags>(), anyInt())
            } doReturn listOf(appInfo)
            on { getPackageInfo(anyString(), any<PackageManager.PackageInfoFlags>()) } doReturn
                pkgInfo
            on { getPackageInfo(anyString(), anyInt()) } doReturn pkgInfo
            on {
                getPackageUid(eq(INSTALLED_PACKAGE), any<PackageManager.PackageInfoFlags>())
            } doReturn INSTALLED_UID
            on { getPackageUid(eq(INSTALLED_PACKAGE), anyInt()) } doReturn INSTALLED_UID
        }

        backend = mock<NotificationBackend>()
        val screen = AppNotificationSettingsApiScreen()
        screen.backend = backend
        tester = ApiTester(screen, context)

        shadowOf(context as Application).grantPermissions(STATUS_BAR_SERVICE)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_isNotNull() {
        tester.initializeScreenParameters(Parameters(ARG_PACKAGE_NAME to INSTALLED_PACKAGE))
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getLaunchScreenExtras_mapsExtras() {
        tester.initializeScreenParameters(Parameters(ARG_PACKAGE_NAME to INSTALLED_PACKAGE))

        val extras = tester.getLaunchScreenExtras()

        assertThat(extras.keySet().size).isEqualTo(1)
        assertThat(extras.getString(ARG_PACKAGE_NAME)).isEqualTo(INSTALLED_PACKAGE)
    }

    @Test
    @DisableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagDisabled_isNull() {
        tester.initializeScreenParameters(Parameters(ARG_PACKAGE_NAME to INSTALLED_PACKAGE))
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getPermissionToggle_returnsCorrectValue() {
        tester.initializeScreenParameters(Parameters(ARG_PACKAGE_NAME to INSTALLED_PACKAGE))
        backend.stub { on { getNotificationsBanned(eq(INSTALLED_PACKAGE), any()) } doReturn false }

        val value: Boolean = tester.get("permission_toggle")

        assertThat(value).isTrue()
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun setPermissionToggle_updatesBackend() {
        tester.initializeScreenParameters(Parameters(ARG_PACKAGE_NAME to INSTALLED_PACKAGE))

        tester.set("permission_toggle", false)

        verify(backend)
            .setNotificationsEnabledForPackage(eq(INSTALLED_PACKAGE), eq(INSTALLED_UID), eq(false))
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getPermissionToggle_noPermission_fails() {
        tester.initializeScreenParameters(Parameters(ARG_PACKAGE_NAME to INSTALLED_PACKAGE))
        backend.stub { on { getNotificationsBanned(eq(INSTALLED_PACKAGE), any()) } doReturn false }
        shadowOf(context as Application).denyPermissions(STATUS_BAR_SERVICE)

        assertFailsWith<MissingPermissionException> { tester.get("permission_toggle") }
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun setPermissionToggle_noPermission_fails() {
        tester.initializeScreenParameters(Parameters(ARG_PACKAGE_NAME to INSTALLED_PACKAGE))
        shadowOf(context as Application).denyPermissions(STATUS_BAR_SERVICE)

        assertFailsWith<MissingPermissionException> { tester.set("permission_toggle", false) }
    }

    companion object {
        private const val INSTALLED_PACKAGE = "com.example.app"
        private const val INSTALLED_UID = 12345
    }
}
