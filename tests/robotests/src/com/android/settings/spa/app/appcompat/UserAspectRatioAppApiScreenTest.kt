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
package com.android.settings.spa.app.appcompat

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.applications.appinfo.AppInfoDashboardFragment.ARG_PACKAGE_NAME
import com.android.settings.flags.Flags
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.FailedPreconditionException
import com.android.settings.testutils2.Parameters
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as whenever
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowPackageManager
import org.robolectric.shadows.ShadowSystemProperties

@RunWith(AndroidJUnit4::class)
class UserAspectRatioAppApiScreenTest {

    @JvmField @Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()
    @get:Rule val setFlagsRule = SetFlagsRule()
    @Spy private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var tester: ApiTester
    private lateinit var shadowPackageManager: ShadowPackageManager
    @Mock private lateinit var resources: Resources
    @Mock private lateinit var launcherApps: LauncherApps
    @Mock private lateinit var launcherActivities: List<LauncherActivityInfo>

    @Before
    fun setUp() {
        tester = ApiTester(UserAspectRatioAppApiScreen(), context)
        whenever(context.resources).thenReturn(resources)
        whenever(context.getSystemService(Context.LAUNCHER_APPS_SERVICE)).thenReturn(launcherApps)
        whenever(context.getSystemService(LauncherApps::class.java)).thenReturn(launcherApps)
        whenever(launcherApps.getActivityList(anyString(), any())).thenReturn(launcherActivities)
        shadowPackageManager = shadowOf(context.packageManager)
        val packageInfo =
            PackageInfo().apply {
                packageName = PACKAGE_NAME
                applicationInfo = APP_INFO
            }
        shadowPackageManager.installPackage(packageInfo)
        whenever(
                resources.getBoolean(
                    com.android.internal.R.bool.config_appCompatUserAppAspectRatioSettingsIsEnabled
                )
            )
            .thenReturn(true)
        whenever(resources.getStringArray(R.array.config_userAspectRatioOverrideEntries))
            .thenReturn(arrayOf("App default"))
        whenever(resources.getIntArray(R.array.config_userAspectRatioOverrideValues))
            .thenReturn(intArrayOf(PackageManager.USER_MIN_ASPECT_RATIO_UNSET))
    }

    @After
    fun cleanUp() {
        ShadowPackageManager.reset()
        ShadowSystemProperties.reset()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getLaunchIntent_featureDisabled_throwsException() {
        whenever(
                resources.getBoolean(
                    com.android.internal.R.bool.config_appCompatUserAppAspectRatioSettingsIsEnabled
                )
            )
            .thenReturn(false)
        tester.initializeScreenParameters(Parameters(ARG_PACKAGE_NAME to PACKAGE_NAME))
        assertThrows(FailedPreconditionException::class.java) { tester.getLaunchIntent() }
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getLaunchIntent_cannotDisplayAspectRatioUi_throwsException() {
        tester.initializeScreenParameters(Parameters(ARG_PACKAGE_NAME to PACKAGE_NAME))
        whenever(launcherApps.getActivityList(anyString(), any())).thenReturn(emptyList())

        assertThrows(FailedPreconditionException::class.java) { tester.getLaunchIntent() }
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getLaunchIntent_canDisplayAspectRatioUi_isNotNull() {
        tester.initializeScreenParameters(Parameters(ARG_PACKAGE_NAME to PACKAGE_NAME))
        whenever(launcherApps.getActivityList(anyString(), any()))
            .thenReturn(listOf(mock(LauncherActivityInfo::class.java)))

        assertThat(tester.getLaunchIntent()).isNotNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q2)
    fun getLaunchScreenExtras_isNotNull() {
        tester.initializeScreenParameters(Parameters(ARG_PACKAGE_NAME to PACKAGE_NAME))

        val extras = tester.getLaunchScreenExtras()
        assertThat(extras?.getString(ARG_PACKAGE_NAME)).isEqualTo(PACKAGE_NAME)
    }

    private companion object {
        const val PACKAGE_NAME = "com.abc.test"
        val APP_INFO = ApplicationInfo().apply { packageName = PACKAGE_NAME }
    }
}
