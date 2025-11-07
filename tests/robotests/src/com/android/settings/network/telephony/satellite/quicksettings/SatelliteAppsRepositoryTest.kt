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

package com.android.settings.network.telephony.satellite.quicksettings

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.net.Uri
import android.provider.Settings
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SatelliteAppsRepositoryTest {
    private lateinit var context: Context

    @Mock private lateinit var packageManager: PackageManager
    @Mock private lateinit var resources: Resources

    private lateinit var satelliteAppsRepository: SatelliteAppsRepository

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        context = spy(RuntimeEnvironment.getApplication())
        `when`(context.packageManager).thenReturn(packageManager)
        `when`(context.resources).thenReturn(resources)
        satelliteAppsRepository = SatelliteAppsRepository(context)
    }

    @Test
    fun getAppsPackagesForNbNtnLandingPage_returnsCorrectPackages() {
        setupPackageManagerForApps(installedApps = listOf("com.app1", "com.app2"))
        `when`(resources.getStringArray(R.array.config_satellite_apps_for_nbntn_landing_page))
            .thenReturn(arrayOf("com.app1", "com.app2"))

        val packages = satelliteAppsRepository.getAppsPackagesForNbNtnLandingPage()

        assertThat(packages).containsExactly("com.app1", "com.app2").inOrder()
    }

    @Test
    fun getAppsPackagesForLteLandingPage_returnsCorrectPackages() {
        setupPackageManagerForApps(installedApps = listOf("com.app1", "com.app2"))
        `when`(resources.getStringArray(R.array.config_satellite_apps_for_lte_landing_page))
            .thenReturn(arrayOf("com.app1", "com.app2"))

        val packages = satelliteAppsRepository.getAppsPackagesForLteLandingPage()

        assertThat(packages).containsExactly("com.app1", "com.app2").inOrder()
    }

    @Test
    fun getAppsPackagesForNbNtnLandingPage_oneAppNotInstalled_returnsOnlyInstalledApps() {
        setupPackageManagerForApps(installedApps = listOf("com.app1"))
        `when`(resources.getStringArray(R.array.config_satellite_apps_for_nbntn_landing_page))
            .thenReturn(arrayOf("com.app1", "com.app2"))

        val packages = satelliteAppsRepository.getAppsPackagesForNbNtnLandingPage()

        assertThat(packages).containsExactly("com.app1")
    }

    @Test
    fun getEmergencySosIntent_resolvable_returnsIntent() {
        val resolveInfo =
            ResolveInfo().apply {
                activityInfo =
                    ActivityInfo().apply {
                        applicationInfo =
                            ApplicationInfo().apply { packageName = "com.example.dialer" }
                        name = "DialerActivity"
                    }
            }
        `when`(
                packageManager.resolveActivity(
                    argThat { it.action == Intent.ACTION_DIAL },
                    anyInt(),
                )
            )
            .thenReturn(resolveInfo)

        val result = satelliteAppsRepository.getEmergencySosIntent()

        assertThat(result).isNotNull()
        assertThat(result!!.action).isEqualTo(Intent.ACTION_DIAL)
        assertThat(result.data).isEqualTo(Uri.parse("tel:911"))
    }

    @Test
    fun getEmergencySosIntent_notResolvable_returnsNull() {
        `when`(
                packageManager.resolveActivity(
                    argThat { it.action == Intent.ACTION_DIAL },
                    anyInt(),
                )
            )
            .thenReturn(null)

        val result = satelliteAppsRepository.getEmergencySosIntent()

        assertThat(result).isNull()
    }

    @Test
    fun getSettingsIntent_resolvable_returnsIntent() {
        val resolveInfo =
            ResolveInfo().apply {
                activityInfo =
                    ActivityInfo().apply {
                        applicationInfo =
                            ApplicationInfo().apply { packageName = "com.android.settings" }
                        name = "SatelliteSettingsActivity"
                    }
            }
        `when`(
                packageManager.resolveActivity(
                    argThat { it.action == Settings.ACTION_SATELLITE_SETTING },
                    anyInt(),
                )
            )
            .thenReturn(resolveInfo)

        val result = satelliteAppsRepository.getSettingsIntent()

        assertThat(result).isNotNull()
        assertThat(result!!.action).isEqualTo(Settings.ACTION_SATELLITE_SETTING)
    }

    @Test
    fun getSettingsIntent_notResolvable_returnsNull() {
        `when`(
                packageManager.resolveActivity(
                    argThat { it.action == Settings.ACTION_SATELLITE_SETTING },
                    anyInt(),
                )
            )
            .thenReturn(null)

        val result = satelliteAppsRepository.getSettingsIntent()

        assertThat(result).isNull()
    }

    private fun setupPackageManagerForApps(installedApps: List<String>) {
        `when`(
                packageManager.getPackageInfo(
                    org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.anyInt(),
                )
            )
            .thenAnswer { invocation ->
                val packageName = invocation.arguments[0] as String
                if (installedApps.contains(packageName)) {
                    PackageInfo()
                } else {
                    throw PackageManager.NameNotFoundException()
                }
            }
    }
}
