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
import android.os.Looper
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.satellite.SatelliteManager
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowSatelliteManager
import org.robolectric.shadows.ShadowSubscriptionManager

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SatelliteLandingPageViewModelTest {
    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    private lateinit var context: Application
    private lateinit var shadowSatelliteManager: ShadowSatelliteManager
    private val SUB_ID = 1

    @Mock private lateinit var subInfo: SubscriptionInfo
    @Mock private lateinit var packageManager: PackageManager
    @Mock private lateinit var appsRepository: SatelliteAppsRepository
    @Mock private lateinit var satelliteStateRepository: SatelliteStateRepository

    private val satelliteStatusFlow = MutableStateFlow(SatelliteStatus.NOT_AVAILABLE)

    @Before
    fun setUp() {
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

        `when`(satelliteStateRepository.satelliteStatus).thenReturn(satelliteStatusFlow)
    }

    @Test
    fun areAppsEnabled_whenSatelliteNotAvailable_isFalse() = runTest {
        satelliteStatusFlow.value = SatelliteStatus.NOT_AVAILABLE
        val viewModel = createViewModel()

        val job = launch { viewModel.areAppsEnabled.collect {} }
        advanceUntilIdle() // starts the listener
        shadowOf(Looper.getMainLooper()).idle() // executes work on main thread and updates flow

        assertThat(viewModel.areAppsEnabled.value).isFalse()
        job.cancel()
    }

    @Test
    fun areAppsEnabled_whenSatelliteActive_isTrue() = runTest {
        satelliteStatusFlow.value = SatelliteStatus.ACTIVE
        val viewModel = createViewModel()

        val job = launch { viewModel.areAppsEnabled.collect {} }
        advanceUntilIdle() // starts the listener
        shadowOf(Looper.getMainLooper()).idle() // executes work on main thread and updates flow

        assertThat(viewModel.areAppsEnabled.value).isTrue()
        job.cancel()
    }

    @Test
    fun areAppsEnabled_whenSatelliteAvailable_isTrue() = runTest {
        satelliteStatusFlow.value = SatelliteStatus.AVAILABLE
        val viewModel = createViewModel()

        val job = launch { viewModel.areAppsEnabled.collect {} }
        advanceUntilIdle() // starts the listener
        shadowOf(Looper.getMainLooper()).idle() // executes work on main thread and updates flow

        assertThat(viewModel.areAppsEnabled.value).isTrue()
        job.cancel()
    }

    @Test
    fun satelliteAppItems_whenLteNtnSupported_loadsLteApps() {
        setLteNtnSupported(true)
        `when`(appsRepository.getAppsPackagesForLteLandingPage()).thenReturn(listOf("com.app1"))
        mockAppsRepositoryIntents()
        setupPackageManagerForApp("com.app1", "App1", Intent("app1.intent"))
        setupCommonPackageManagerApps()

        val items = createViewModelAndGetItems()

        assertThat(items).hasSize(3) // SOS, App1, Settings
        assertThat(items.map { it.getAppLabel(packageManager) })
            .containsExactly(
                context.getString(com.android.settings.R.string.satellite_emergency_sos),
                "App1",
                "Settings",
            )
            .inOrder()
    }

    @Test
    fun satelliteAppItems_whenLteNtnNotSupported_loadsNbIotApps() {
        setLteNtnSupported(false)
        `when`(appsRepository.getAppsPackagesForNbNtnLandingPage()).thenReturn(listOf("com.app2"))
        mockAppsRepositoryIntents()
        setupPackageManagerForApp("com.app2", "App2", Intent("app2.intent"))
        setupCommonPackageManagerApps()

        val items = createViewModelAndGetItems()

        assertThat(items).hasSize(3) // SOS, App2, Settings
        assertThat(items.map { it.getAppLabel(packageManager) })
            .containsExactly(
                context.getString(com.android.settings.R.string.satellite_emergency_sos),
                "App2",
                "Settings",
            )
            .inOrder()
    }

    @Test
    fun satelliteAppItems_whenAppNotFound_doesNotLoadApp() {
        setLteNtnSupported(true)
        val missingPackage = "com.missing.app"
        `when`(appsRepository.getAppsPackagesForLteLandingPage()).thenReturn(listOf(missingPackage))
        mockAppsRepositoryIntents()
        setupCommonPackageManagerApps()

        `when`(packageManager.getApplicationInfo(missingPackage, 0))
            .thenThrow(PackageManager.NameNotFoundException())

        val items = createViewModelAndGetItems()

        assertThat(items).hasSize(2) // SOS, Settings
        assertThat(items.map { it.getAppLabel(packageManager) })
            .containsExactly(
                context.getString(com.android.settings.R.string.satellite_emergency_sos),
                "Settings",
            )
            .inOrder()
    }

    @Test
    fun satelliteAppItems_whenAppLaunchIntentNull_doesNotLoadApp() {
        setLteNtnSupported(true)
        val noIntentPackage = "com.nointent.app"
        `when`(appsRepository.getAppsPackagesForLteLandingPage())
            .thenReturn(listOf(noIntentPackage))
        mockAppsRepositoryIntents()
        setupCommonPackageManagerApps()

        val appInfo = mock(ApplicationInfo::class.java)
        doReturn(appInfo).`when`(packageManager).getApplicationInfo(noIntentPackage, 0)
        `when`(packageManager.getLaunchIntentForPackage(noIntentPackage)).thenReturn(null)

        val items = createViewModelAndGetItems()

        assertThat(items).hasSize(2) // SOS, Settings
    }

    @Test
    fun satelliteAppItems_whenSosIntentNull_doesNotLoadSos() {
        setLteNtnSupported(true)
        `when`(appsRepository.getAppsPackagesForLteLandingPage()).thenReturn(emptyList())
        mockAppsRepositoryIntents(sosIntent = null)
        setupCommonPackageManagerApps()

        val items = createViewModelAndGetItems()

        assertThat(items).hasSize(1) // Only Settings
        assertThat(items[0].getAppLabel(packageManager)).isEqualTo("Settings")
    }

    @Test
    fun satelliteAppItems_whenSettingsIntentNull_doesNotLoadSettings() {
        setLteNtnSupported(true)
        `when`(appsRepository.getAppsPackagesForLteLandingPage()).thenReturn(emptyList())
        mockAppsRepositoryIntents(settingsIntent = null)
        setupCommonPackageManagerApps()

        val items = createViewModelAndGetItems()

        assertThat(items).hasSize(1) // Only SOS
        assertThat(items[0].getAppLabel(packageManager))
            .isEqualTo(context.getString(com.android.settings.R.string.satellite_emergency_sos))
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

    private fun createViewModel(): SatelliteLandingPageViewModel {
        return SatelliteLandingPageViewModel(
            context,
            appsRepository,
            packageManager,
            satelliteStateRepository,
        )
    }

    private fun createViewModelAndGetItems(): List<SatelliteAppItem> {
        val viewModel = createViewModel()
        viewModel.loadSatelliteAppItems()
        return viewModel.satelliteAppItems.value!!
    }

    private fun mockAppsRepositoryIntents(
        sosIntent: Intent? = Intent("sos"),
        settingsIntent: Intent? = Intent("settings"),
    ) {
        `when`(appsRepository.getEmergencySosIntent()).thenReturn(sosIntent)
        `when`(appsRepository.getSettingsIntent()).thenReturn(settingsIntent)
    }

    private fun setupCommonPackageManagerApps() {
        setupPackageManagerForApp(
            SatelliteAppsRepository.PACKAGE_NAME_SAFETY_HUB,
            "SOS",
            Intent("sos"),
        )
        setupPackageManagerForApp(
            SatelliteAppsRepository.PACKAGE_NAME_SETTINGS,
            "Settings",
            Intent("settings"),
        )
    }
}
