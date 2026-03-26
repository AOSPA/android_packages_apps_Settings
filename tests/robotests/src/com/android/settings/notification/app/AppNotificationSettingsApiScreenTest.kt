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
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.Parameters
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class AppNotificationSettingsApiScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private lateinit var context: Context
    private lateinit var tester: ApiTester
    private val packageManager = mock<PackageManager>()

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
        }

        tester = ApiTester(AppNotificationSettingsApiScreen(), context)
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

    companion object {
        private const val INSTALLED_PACKAGE = "com.example.app"
    }
}
