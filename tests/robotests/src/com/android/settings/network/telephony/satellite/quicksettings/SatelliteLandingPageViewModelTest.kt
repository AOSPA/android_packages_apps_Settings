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
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.satellite.SatelliteManager
import com.android.settings.R
import com.android.settings.network.telephony.TelephonyFeatureProvider
import com.android.settings.network.telephony.satellite.SatelliteSettingsRepository
import com.android.settings.testutils.FakeFeatureFactory
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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

    private lateinit var subInfo: SubscriptionInfo
    @Mock private lateinit var packageManager: PackageManager
    @Mock private lateinit var appsRepository: SatelliteAppsRepository
    @Mock private lateinit var satelliteStateRepository: SatelliteStateRepository

    private val satelliteStatusFlow = MutableStateFlow(SatelliteStatus.NOT_AVAILABLE)
    private val isTerrestrialConnectedFlow = MutableStateFlow(true)
    private val satelliteDisallowedReasonsFlow = MutableStateFlow(intArrayOf())
    private val activeSubIdFlow = MutableStateFlow(SUB_ID)

    private lateinit var fakeFeatureFactory: FakeFeatureFactory
    @Mock private lateinit var mockTelephonyFeatureProvider: TelephonyFeatureProvider
    @Mock private lateinit var mockSatelliteSettingsRepository: SatelliteSettingsRepository

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        fakeFeatureFactory = FakeFeatureFactory.setupForTest()
        `when`(fakeFeatureFactory.telephonyFeatureProvider.satelliteSettingsRepository)
            .thenReturn(mockSatelliteSettingsRepository)

        shadowSatelliteManager =
            Shadow.extract(context.getSystemService(SatelliteManager::class.java))
        ShadowSubscriptionManager.setActiveDataSubscriptionId(SUB_ID)

        subInfo = SubscriptionInfo.Builder().setId(SUB_ID).build()
        val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
        shadowOf(subscriptionManager).setActiveSubscriptionInfoList(listOf(subInfo))

        `when`(satelliteStateRepository.satelliteStatus).thenReturn(satelliteStatusFlow)
        `when`(satelliteStateRepository.isTerrestrialConnected)
            .thenReturn(isTerrestrialConnectedFlow)
        `when`(satelliteStateRepository.satelliteDisallowedReasons)
            .thenReturn(satelliteDisallowedReasonsFlow)
        `when`(satelliteStateRepository.activeSubIdFlow).thenReturn(activeSubIdFlow)
        `when`(satelliteStateRepository.getAttachRestrictionReasons(SUB_ID)).thenReturn(emptySet())
        `when`(satelliteStateRepository.getSatelliteDataSupportMode(SUB_ID))
            .thenReturn(SatelliteManager.SATELLITE_DATA_SUPPORT_UNKNOWN)
    }

    @Test
    fun areAppsEnabled_whenSatelliteNotAvailable_isFalse() = runTest {
        satelliteStatusFlow.value = SatelliteStatus.NOT_AVAILABLE
        val viewModel = createViewModel()

        val job = launch { viewModel.areAppsEnabled.collect {} }
        waitForAsync()

        assertThat(viewModel.areAppsEnabled.value).isFalse()
        job.cancel()
    }

    @Test
    fun isTryADemoButtonEnabled_whenSatelliteActive_isFalse() = runTest {
        satelliteStatusFlow.value = SatelliteStatus.ACTIVE
        val viewModel = createViewModel()

        val job = launch { viewModel.isTryADemoButtonEnabled.collect {} }
        waitForAsync()

        assertThat(viewModel.isTryADemoButtonEnabled.value).isFalse()
        job.cancel()
    }

    @Test
    fun isTryADemoButtonEnabled_whenSatelliteAvailable_isTrue() = runTest {
        satelliteStatusFlow.value = SatelliteStatus.AVAILABLE
        val viewModel = createViewModel()

        val job = launch { viewModel.isTryADemoButtonEnabled.collect {} }
        waitForAsync()

        assertThat(viewModel.isTryADemoButtonEnabled.value).isTrue()
        job.cancel()
    }

    @Test
    fun isTryADemoButtonEnabled_whenSatelliteNotAvailable_isTrue() = runTest {
        satelliteStatusFlow.value = SatelliteStatus.NOT_AVAILABLE
        val viewModel = createViewModel()

        val job = launch { viewModel.isTryADemoButtonEnabled.collect {} }
        waitForAsync()

        assertThat(viewModel.isTryADemoButtonEnabled.value).isTrue()
        job.cancel()
    }

    @Test
    fun isTryADemoButtonEnabled_whenLoading_isFalse() = runTest {
        satelliteStatusFlow.value = SatelliteStatus.AVAILABLE
        // Use a separate dispatcher to control background work execution
        val delayedDispatcher = StandardTestDispatcher()
        val viewModel =
            SatelliteLandingPageViewModel(
                context,
                appsRepository,
                packageManager,
                satelliteStateRepository,
                delayedDispatcher,
            )

        val job = launch { viewModel.isTryADemoButtonEnabled.collect {} }

        // Execute main thread work (init block), but background work will be suspended
        waitForAsync()

        // Button should be disabled while loading
        assertThat(viewModel.isTryADemoButtonEnabled.value).isFalse()

        // Complete the background work. There are two background operations in refreshInternal,
        // so we need to drain the queue twice.
        delayedDispatcher.scheduler.advanceUntilIdle()
        waitForAsync()

        delayedDispatcher.scheduler.advanceUntilIdle()
        waitForAsync()

        // Button should be enabled now
        assertThat(viewModel.isTryADemoButtonEnabled.value).isTrue()
        job.cancel()
    }

    @Test
    fun isTryADemoButtonEnabled_whenRegionRestricted_isFalse() = runTest {
        setLteNtnSupported(true)
        `when`(satelliteStateRepository.getAttachRestrictionReasons(SUB_ID))
            .thenReturn(
                setOf(SatelliteManager.SATELLITE_COMMUNICATION_RESTRICTION_REASON_GEOLOCATION)
            )

        satelliteStatusFlow.value = SatelliteStatus.AVAILABLE
        val viewModel = createViewModel()

        val job = launch { viewModel.isTryADemoButtonEnabled.collect {} }
        waitForAsync()

        assertThat(viewModel.isTryADemoButtonEnabled.value).isFalse()
        job.cancel()
    }

    @Test
    fun areAppsEnabled_whenSatelliteActive_isTrue() = runTest {
        satelliteStatusFlow.value = SatelliteStatus.ACTIVE
        val viewModel = createViewModel()

        val job = launch { viewModel.areAppsEnabled.collect {} }
        waitForAsync()

        assertThat(viewModel.areAppsEnabled.value).isTrue()
        job.cancel()
    }

    @Test
    fun areAppsEnabled_whenSatelliteAvailable_isTrue() = runTest {
        satelliteStatusFlow.value = SatelliteStatus.AVAILABLE
        val viewModel = createViewModel()

        val job = launch { viewModel.areAppsEnabled.collect {} }
        waitForAsync()

        assertThat(viewModel.areAppsEnabled.value).isTrue()
        job.cancel()
    }

    @Test
    fun satelliteAppItems_whenLteNtnSupported_loadsLteApps() = runTest {
        setLteNtnSupported(true)
        `when`(appsRepository.getAppsPackagesForLteLandingPage(false))
            .thenReturn(listOf("com.app1"))
        mockAppsRepositoryIntents()
        setupPackageManagerForApp("com.app1", "App1", Intent("app1.intent"))
        setupCommonPackageManagerApps()

        val items = createViewModelAndGetItems()
        waitForAsync()

        assertThat(items).hasSize(2) // Phone, App1
        assertThat(items.map { it.getAppLabel(packageManager) })
            .containsExactly("Phone", "App1")
            .inOrder()
        assertThat(items[0].summary)
            .isEqualTo(context.getString(com.android.settings.R.string.satellite_phone_summary))
    }

    @Test
    fun satelliteAppItems_whenLteNtnSupportedAndRestricted_loadsRestrictedApps() = runTest {
        setLteNtnSupported(true)
        val messagesPackage = context.getString(R.string.config_satellite_messages_app_package)
        `when`(satelliteStateRepository.getSatelliteDataSupportMode(SUB_ID))
            .thenReturn(SatelliteManager.SATELLITE_DATA_SUPPORT_RESTRICTED)
        `when`(appsRepository.getAppsPackagesForLteLandingPage(true))
            .thenReturn(listOf(messagesPackage))
        mockAppsRepositoryIntents()
        setupPackageManagerForApp(messagesPackage, "Messages", Intent("messages.intent"))
        setupCommonPackageManagerApps()

        val items = createViewModelAndGetItems()
        waitForAsync()

        assertThat(items).hasSize(2) // Phone, Messages
        assertThat(items.map { it.getAppLabel(packageManager) })
            .containsExactly("Phone", "Messages")
            .inOrder()
    }

    @Test
    fun satelliteAppItems_whenLteNtnNotSupported_loadsNbIotApps() = runTest {
        setLteNtnSupported(false)
        `when`(appsRepository.getAppsPackagesForNbNtnLandingPage()).thenReturn(listOf("com.app2"))
        mockAppsRepositoryIntents()
        setupPackageManagerForApp("com.app2", "App2", Intent("app2.intent"))
        setupCommonPackageManagerApps()

        val items = createViewModelAndGetItems()
        waitForAsync()

        assertThat(items).hasSize(2) // Phone, App2
        assertThat(items.map { it.getAppLabel(packageManager) })
            .containsExactly("Phone", "App2")
            .inOrder()
        assertThat(items[0].summary)
            .isEqualTo(context.getString(com.android.settings.R.string.satellite_phone_summary))
    }

    @Test
    fun satelliteAppItems_whenAppNotFound_doesNotLoadApp() = runTest {
        setLteNtnSupported(true)
        val missingPackage = "com.missing.app"
        `when`(appsRepository.getAppsPackagesForLteLandingPage(false))
            .thenReturn(listOf(missingPackage))
        mockAppsRepositoryIntents()
        setupCommonPackageManagerApps()

        `when`(packageManager.getApplicationInfo(missingPackage, 0))
            .thenThrow(PackageManager.NameNotFoundException())

        val items = createViewModelAndGetItems()
        waitForAsync()

        assertThat(items).hasSize(1) // Phone
        assertThat(items[0].getAppLabel(packageManager)).isEqualTo("Phone")
    }

    @Test
    fun satelliteAppItems_whenAppLaunchIntentNull_doesNotLoadApp() = runTest {
        setLteNtnSupported(true)
        val noIntentPackage = "com.nointent.app"
        `when`(appsRepository.getAppsPackagesForLteLandingPage(false))
            .thenReturn(listOf(noIntentPackage))
        mockAppsRepositoryIntents()
        setupCommonPackageManagerApps()
        setupPackageManagerForApp(noIntentPackage, "NoIntentApp", null)

        val items = createViewModelAndGetItems()
        waitForAsync()

        assertThat(items).hasSize(1) // Phone
        assertThat(items[0].getAppLabel(packageManager)).isEqualTo("Phone")
    }

    @Test
    fun satelliteAppItems_whenDialerIntentNull_doesNotLoadPhone() = runTest {
        setLteNtnSupported(false)
        `when`(appsRepository.getAppsPackagesForNbNtnLandingPage()).thenReturn(emptyList())
        mockAppsRepositoryIntents(dialerIntent = null)
        setupCommonPackageManagerApps()

        val items = createViewModelAndGetItems()
        waitForAsync()

        assertThat(items).isEmpty()
    }

    @Test
    fun refresh_updatesSupportedStates() = runTest {
        setLteNtnSupported(true)
        val viewModel = createViewModel()

        viewModel.refresh()
        waitForAsync()

        assertThat(viewModel.isLteBasedNtnSupported.value).isTrue()
        assertThat(viewModel.isCarrierRoamingNtnSupported.value).isTrue()
    }

    @Test
    fun getSettingsIntent_delegatesToRepository() = runTest {
        setupCommonPackageManagerApps()
        val viewModel = createViewModel()
        val expectedIntent = Intent("expected")
        viewModel.refresh()
        `when`(appsRepository.getSettingsIntent(false)).thenReturn(expectedIntent)

        assertThat(viewModel.getSettingsIntent()).isEqualTo(expectedIntent)
    }

    @Test
    fun refresh_lteSupported_userRestricted_setsBannerStateCorrectly() = runTest {
        setLteNtnSupported(true)
        `when`(satelliteStateRepository.getAttachRestrictionReasons(SUB_ID))
            .thenReturn(setOf(SatelliteManager.SATELLITE_COMMUNICATION_RESTRICTION_REASON_USER))

        val viewModel = createViewModel()
        viewModel.refresh()
        waitForAsync()

        val bannerState = viewModel.bannerState.value
        assertThat(bannerState.isUserRestricted).isTrue()
        assertThat(bannerState.isSatelliteAvailableInRegion).isTrue()
        assertThat(bannerState.isEntitled).isTrue()
    }

    private fun setupPackageManagerForApp(packageName: String, appName: String, intent: Intent?) {
        val appInfo = mock(ApplicationInfo::class.java)
        `when`(appInfo.loadLabel(packageManager)).thenReturn(appName)
        doReturn(appInfo).`when`(packageManager).getApplicationInfo(packageName, 0)
        `when`(packageManager.getLaunchIntentForPackage(packageName)).thenReturn(intent)
    }

    private fun setLteNtnSupported(isSupported: Boolean) {
        `when`(mockSatelliteSettingsRepository.isSatelliteAttachSupported(SUB_ID))
            .thenReturn(isSupported)
        if (isSupported) {
            `when`(mockSatelliteSettingsRepository.getSatelliteNtnConnectType(SUB_ID))
                .thenReturn(CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC)
        }
    }

    private fun TestScope.createViewModel(): SatelliteLandingPageViewModel {
        return SatelliteLandingPageViewModel(
            context,
            appsRepository,
            packageManager,
            satelliteStateRepository,
            StandardTestDispatcher(testScheduler),
        )
    }

    private fun TestScope.createViewModelAndGetItems(): List<SatelliteAppItem> {
        val viewModel = createViewModel()
        // refresh() is called from init, but we call it again to ensure test consistency
        viewModel.refresh()
        waitForAsync()
        return viewModel.satelliteAppItems.value
    }

    @Test
    fun activeSubId_change_refreshesViewModel() = runTest {
        setLteNtnSupported(false) // isCarrierRoamingNtnSupported is false for SUB_ID
        val viewModel = createViewModel()
        waitForAsync() // for init

        assertThat(viewModel.isCarrierRoamingNtnSupported.value).isFalse()

        // Set up a new subscription with carrier support
        val newSubId = 2
        val newSubInfo = SubscriptionInfo.Builder().setId(newSubId).build()
        val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
        shadowOf(subscriptionManager).setActiveSubscriptionInfoList(listOf(subInfo, newSubInfo))
        `when`(mockSatelliteSettingsRepository.isSatelliteAttachSupported(newSubId))
            .thenReturn(true)

        // Trigger subscription change
        activeSubIdFlow.value = newSubId
        waitForAsync()

        assertThat(viewModel.isCarrierRoamingNtnSupported.value).isTrue()
    }

    private fun TestScope.waitForAsync() {
        advanceUntilIdle() // start the listeners
        shadowOf(Looper.getMainLooper()).idle() // executes work on main thread and updates flow
    }

    private fun mockAppsRepositoryIntents(
        dialerIntent: Intent? = Intent("dialer"),
        settingsIntent: Intent? = Intent("settings"),
    ) {
        `when`(appsRepository.getDialerIntent()).thenReturn(dialerIntent)
        `when`(appsRepository.getSettingsIntent(org.mockito.ArgumentMatchers.anyBoolean()))
            .thenReturn(settingsIntent)
    }

    private fun setupCommonPackageManagerApps() {
        val phonePackage = context.getString(R.string.config_satellite_phone_app_package)
        setupPackageManagerForApp(phonePackage, "Phone", Intent("dialer"))
    }
}
