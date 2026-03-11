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
import android.provider.DeviceConfig
import android.provider.Settings
import android.telephony.satellite.SatelliteManager
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
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowSatelliteManager

@RunWith(RobolectricTestRunner::class)
class SatelliteAppsRepositoryTest {
    private lateinit var context: Context

    @Mock private lateinit var packageManager: PackageManager
    @Mock private lateinit var resources: Resources

    private companion object {
        private const val APP1_PACKAGE = "com.app1"
        private const val APP2_PACKAGE = "com.app2"
        private const val OVERRIDE_SATELLITE_APPS_FOR_LTE_LANDING_PAGE_WITH_CONFIG =
            "override_satellite_apps_for_lte_landing_page_with_config"
    }

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
        setupPackageManagerForApps(installedApps = listOf(APP1_PACKAGE, APP2_PACKAGE))
        `when`(resources.getStringArray(R.array.config_satellite_apps_for_nbntn_landing_page))
            .thenReturn(arrayOf(APP1_PACKAGE, APP2_PACKAGE))

        val packages = satelliteAppsRepository.getAppsPackagesForNbNtnLandingPage()

        assertThat(packages).containsExactly(APP1_PACKAGE, APP2_PACKAGE).inOrder()
    }

    @Test
    fun getAppsPackagesForLteLandingPage_returnsCorrectPackages() {
        DeviceConfig.setProperty(
            DeviceConfig.NAMESPACE_TELEPHONY,
            OVERRIDE_SATELLITE_APPS_FOR_LTE_LANDING_PAGE_WITH_CONFIG,
            "true",
            false,
        )
        setupPackageManagerForApps(installedApps = listOf(APP1_PACKAGE, APP2_PACKAGE))
        `when`(resources.getStringArray(R.array.config_satellite_apps_for_lte_landing_page))
            .thenReturn(arrayOf(APP1_PACKAGE, APP2_PACKAGE))

        val packages = satelliteAppsRepository.getAppsPackagesForLteLandingPage()

        assertThat(packages).containsExactly(APP1_PACKAGE, APP2_PACKAGE).inOrder()
    }

    @Test
    fun getAppsPackagesForLteLandingPage_restrictedMode_returnsOnlyMessagesApp() {
        DeviceConfig.setProperty(
            DeviceConfig.NAMESPACE_TELEPHONY,
            OVERRIDE_SATELLITE_APPS_FOR_LTE_LANDING_PAGE_WITH_CONFIG,
            "true",
            false,
        )
        val messagesPackage = "com.google.android.apps.messaging"
        `when`(context.getString(R.string.config_satellite_messages_app_package))
            .thenReturn(messagesPackage)
        setupPackageManagerForApps(installedApps = listOf(APP1_PACKAGE, messagesPackage))
        `when`(resources.getStringArray(R.array.config_satellite_apps_for_lte_landing_page))
            .thenReturn(arrayOf(APP1_PACKAGE, messagesPackage))

        val packages =
            satelliteAppsRepository.getAppsPackagesForLteLandingPage(isRestrictedMode = true)

        assertThat(packages).containsExactly(messagesPackage)
    }

    @Test
    fun getAppsPackagesForNbNtnLandingPage_oneAppNotInstalled_returnsOnlyInstalledApps() {
        setupPackageManagerForApps(installedApps = listOf(APP1_PACKAGE))
        `when`(resources.getStringArray(R.array.config_satellite_apps_for_nbntn_landing_page))
            .thenReturn(arrayOf(APP1_PACKAGE, APP2_PACKAGE))

        val packages = satelliteAppsRepository.getAppsPackagesForNbNtnLandingPage()

        assertThat(packages).containsExactly(APP1_PACKAGE)
    }

    @Test
    fun getDialerIntent_resolvable_returnsIntent() {
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

        val result = satelliteAppsRepository.getDialerIntent()

        assertThat(result).isNotNull()
        assertThat(result!!.action).isEqualTo(Intent.ACTION_DIAL)
        assertThat(result.data).isNull()
    }

    @Test
    fun getDialerIntent_notResolvable_returnsNull() {
        `when`(
                packageManager.resolveActivity(
                    argThat { it.action == Intent.ACTION_DIAL },
                    anyInt(),
                )
            )
            .thenReturn(null)

        val result = satelliteAppsRepository.getDialerIntent()

        assertThat(result).isNull()
    }

    @Test
    fun getSettingsIntent_carrierRoamingSupported_returnsSettingsIntent() {
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

        val result = satelliteAppsRepository.getSettingsIntent(true)

        assertThat(result).isNotNull()
        assertThat(result!!.action).isEqualTo(Settings.ACTION_SATELLITE_SETTING)
    }

    @Test
    fun getSettingsIntent_carrierRoamingNotSupported_returnsSconeIntent() {
        val sconePackage = "com.google.android.apps.scone"
        val sconeActivity =
            "com.google.android.apps.scone.satellite.settings.gateway.SettingsGatewayActivity"
        `when`(context.getString(R.string.config_satellite_settings_gateway_package))
            .thenReturn(sconePackage)
        `when`(context.getString(R.string.config_satellite_settings_gateway_class))
            .thenReturn(sconeActivity)

        val resolveInfo =
            ResolveInfo().apply {
                activityInfo =
                    ActivityInfo().apply {
                        applicationInfo = ApplicationInfo().apply { packageName = sconePackage }
                        name = "SettingsGatewayActivity"
                    }
            }
        `when`(
                packageManager.resolveActivity(
                    argThat {
                        it.component?.packageName == sconePackage &&
                            it.component?.className == sconeActivity
                    },
                    anyInt(),
                )
            )
            .thenReturn(resolveInfo)

        val result = satelliteAppsRepository.getSettingsIntent(false)

        assertThat(result).isNotNull()
        assertThat(result!!.component?.packageName).isEqualTo(sconePackage)
        assertThat(result.component?.className).isEqualTo(sconeActivity)
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

        val result = satelliteAppsRepository.getSettingsIntent(true)

        assertThat(result).isNull()
    }

    @Test
    fun getAppsPackagesForLteLandingPage_useSatelliteDataOptimizedApps_returnsOptimizedApps() {
        setupPackageManagerForApps(installedApps = listOf(APP1_PACKAGE, APP2_PACKAGE))
        DeviceConfig.setProperty(
            DeviceConfig.NAMESPACE_TELEPHONY,
            OVERRIDE_SATELLITE_APPS_FOR_LTE_LANDING_PAGE_WITH_CONFIG,
            "false",
            false,
        )
        val shadowSatelliteManager: ShadowSatelliteManager =
            Shadow.extract(context.getSystemService(SatelliteManager::class.java))
        shadowSatelliteManager.setSatelliteDataOptimizedApps(listOf(APP1_PACKAGE))

        val apps = satelliteAppsRepository.getAppsPackagesForLteLandingPage()

        assertThat(apps).containsExactly(APP1_PACKAGE)
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
