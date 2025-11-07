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

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.satellite.SatelliteManager
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowSatelliteManager
import org.robolectric.shadows.ShadowSubscriptionManager

@RunWith(RobolectricTestRunner::class)
class SatelliteLandingPageViewModelTest {
    private lateinit var context: Application
    private lateinit var shadowSatelliteManager: ShadowSatelliteManager
    private val SUB_ID = 1

    @Mock private lateinit var subInfo: SubscriptionInfo
    @Mock private lateinit var packageManager: PackageManager
    @Mock private lateinit var appsRepository: SatelliteAppsRepository

    // Constants for common intents
    private val SOS_INTENT = Intent("sos.intent")
    private val SETTINGS_INTENT = Intent("settings.intent")

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = RuntimeEnvironment.getApplication()
        context.setTheme(com.android.settings.R.style.Theme_Settings)
        shadowSatelliteManager =
            Shadow.extract(context.getSystemService(SatelliteManager::class.java))
        ShadowSubscriptionManager.setActiveDataSubscriptionId(SUB_ID)
        `when`(subInfo.subscriptionId).thenReturn(SUB_ID)
        val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
        shadowOf(subscriptionManager).setActiveSubscriptionInfoList(listOf(subInfo))
        val carrierConfigManager = context.getSystemService(CarrierConfigManager::class.java)!!
        shadowOf(carrierConfigManager).setConfigForSubId(SUB_ID, PersistableBundle())
    }

    @Test
    fun satelliteAppItems_whenLteNtnSupported_loadsLteApps() {
        setLteNtnSupported(true)
        val lteAppPackages = listOf("com.app1", "com.app2")
        `when`(appsRepository.getAppsPackagesForLteLandingPage()).thenReturn(lteAppPackages)
        mockAppsRepositoryIntents()
        setupPackageManagerForApp("com.app1", "App1", Intent("app1.intent"))
        setupPackageManagerForApp("com.app2", "App2", Intent("app2.intent"))
        setupCommonPackageManagerApps()

        val items = createViewModelAndGetItems()

        assertThat(items).hasSize(4)
        assertThat(items.map { it.getAppLabel(packageManager) })
            .containsExactly(
                context.getString(com.android.settings.R.string.satellite_emergency_sos),
                "App1",
                "App2",
                "Settings",
            )
            .inOrder()
    }

    @Test
    fun satelliteAppItems_whenLteNtnNotSupported_loadsNbNtnApps() {
        setLteNtnSupported(false)
        val nbNtnAppPackages = listOf("com.app3")
        `when`(appsRepository.getAppsPackagesForNbNtnLandingPage()).thenReturn(nbNtnAppPackages)
        mockAppsRepositoryIntents()
        setupPackageManagerForApp("com.app3", "App3", Intent("app3.intent"))
        setupCommonPackageManagerApps()

        val items = createViewModelAndGetItems()

        assertThat(items).hasSize(3)
        assertThat(items.map { it.getAppLabel(packageManager) })
            .containsExactly(
                context.getString(com.android.settings.R.string.satellite_emergency_sos),
                "App3",
                "Settings",
            )
            .inOrder()
    }

    @Test
    fun satelliteAppItems_whenAppNotFound_isNotAdded() {
        setLteNtnSupported(true)
        val lteAppPackages = listOf("com.app1", "com.app.notfound")
        `when`(appsRepository.getAppsPackagesForLteLandingPage()).thenReturn(lteAppPackages)
        mockAppsRepositoryIntents()
        setupPackageManagerForApp("com.app1", "App1", Intent("app1.intent"))
        `when`(packageManager.getApplicationInfo("com.app.notfound", 0))
            .thenThrow(PackageManager.NameNotFoundException())
        `when`(packageManager.getLaunchIntentForPackage("com.app.notfound"))
            .thenReturn(Intent("notfound.intent"))
        setupCommonPackageManagerApps()

        val items = createViewModelAndGetItems()

        assertThat(items).hasSize(3)
        assertThat(items.map { it.getAppLabel(packageManager) })
            .containsExactly(
                context.getString(com.android.settings.R.string.satellite_emergency_sos),
                "App1",
                "Settings",
            )
            .inOrder()
    }

    @Test
    fun satelliteAppItems_whenIntentIsNull_isNotAdded() {
        setLteNtnSupported(true)
        val lteAppPackages = listOf("com.app1")
        `when`(appsRepository.getAppsPackagesForLteLandingPage())
            .thenReturn(lteAppPackages) // Null intent
        mockAppsRepositoryIntents(sosIntent = null)
        setupPackageManagerForApp("com.app1", "App1", Intent("app1.intent"))
        setupCommonPackageManagerApps(sosIntent = null)

        val items = createViewModelAndGetItems()

        assertThat(items).hasSize(2)
        assertThat(items.map { it.getAppLabel(packageManager) })
            .containsExactly("App1", "Settings")
            .inOrder()
    }

    private fun setupPackageManagerForApp(packageName: String, appName: String, intent: Intent?) {
        val appInfo = mock(ApplicationInfo::class.java)
        `when`(appInfo.loadLabel(packageManager)).thenReturn(appName)
        doReturn(appInfo).`when`(packageManager).getApplicationInfo(packageName, 0)
        `when`(packageManager.getLaunchIntentForPackage(packageName)).thenReturn(intent)
    }

    private fun setLteNtnSupported(isSupported: Boolean) {
        val reasons = if (isSupported) emptySet() else setOf(1)
        shadowSatelliteManager.setAttachRestrictionReasonsForCarrier(SUB_ID, reasons)

        val config =
            PersistableBundle().apply {
                putBoolean(CarrierConfigManager.KEY_SATELLITE_ATTACH_SUPPORTED_BOOL, isSupported)
                if (isSupported) {
                    putInt(
                        CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
                        CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
                    )
                }
            }
        val carrierConfigManager = context.getSystemService(CarrierConfigManager::class.java)!!
        shadowOf(carrierConfigManager).setConfigForSubId(SUB_ID, config)
    }

    private fun createViewModelAndGetItems(): List<SatelliteAppItem> {
        val viewModel = SatelliteLandingPageViewModel(context, appsRepository, packageManager)
        return viewModel.satelliteAppItems.value!!
    }

    private fun mockAppsRepositoryIntents(
        sosIntent: Intent? = SOS_INTENT,
        settingsIntent: Intent? = SETTINGS_INTENT,
    ) {
        `when`(appsRepository.getEmergencySosIntent()).thenReturn(sosIntent)
        `when`(appsRepository.getSettingsIntent()).thenReturn(settingsIntent)
    }

    private fun setupCommonPackageManagerApps(
        sosIntent: Intent? = SOS_INTENT,
        settingsIntent: Intent? = SETTINGS_INTENT,
    ) {
        setupPackageManagerForApp(SatelliteAppsRepository.PACKAGE_NAME_SAFETY_HUB, "SOS", sosIntent)
        setupPackageManagerForApp(
            SatelliteAppsRepository.PACKAGE_NAME_SETTINGS,
            "Settings",
            settingsIntent,
        )
    }
}
