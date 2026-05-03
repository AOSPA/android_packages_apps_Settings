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
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.provider.Settings
import android.telephony.ServiceState
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.telephony.satellite.SatelliteDisallowedReasonsCallback
import android.telephony.satellite.SatelliteManager
import android.telephony.satellite.SatelliteModemStateCallback
import androidx.test.core.app.ApplicationProvider
import com.android.settings.network.telephony.TelephonyFeatureProvider
import com.android.settings.network.telephony.satellite.SatelliteSettingsRepository
import com.android.settings.testutils.FakeFeatureFactory
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSubscriptionManager

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SatelliteStateRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Mock private lateinit var telephonyManager: TelephonyManager
    @Mock private lateinit var satelliteManager: SatelliteManager
    @Mock private lateinit var connectivityManager: ConnectivityManager

    private lateinit var shadowSubscriptionManager: ShadowSubscriptionManager

    private lateinit var fakeFeatureFactory: FakeFeatureFactory
    @Mock private lateinit var mockTelephonyFeatureProvider: TelephonyFeatureProvider
    @Mock private lateinit var mockSatelliteSettingsRepository: SatelliteSettingsRepository

    // UnconfinedTestDispatcher executes coroutines eagerly (synchronously) when they are launched.
    // This ensures that when launchIn(backgroundScope) is called, the entire chain (subscription ->
    // stateIn -> combine -> upstream flows) executes immediately up to the first suspension point.
    private val testScope = TestScope(UnconfinedTestDispatcher())

    private lateinit var repository: SatelliteStateRepository

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        fakeFeatureFactory = FakeFeatureFactory.setupForTest()
        `when`(fakeFeatureFactory.telephonyFeatureProvider.satelliteSettingsRepository)
            .thenReturn(mockSatelliteSettingsRepository)

        shadowSubscriptionManager =
            shadowOf(context.getSystemService(SubscriptionManager::class.java))

        doAnswer { invocation ->
                val callback = invocation.getArgument<TelephonyCallback>(1)
                if (callback is TelephonyCallback.CarrierRoamingNtnListener) {
                    // Trigger an emission by changing the state from its default (false, false)
                    callback.onCarrierRoamingNtnEligibleStateChanged(true)
                    callback.onCarrierRoamingNtnEligibleStateChanged(false)
                }
                null
            }
            .`when`(telephonyManager)
            .registerTelephonyCallback(any(), any())
    }

    private fun createRepository(
        scope: kotlinx.coroutines.CoroutineScope,
        isLteNtnSupported: Boolean = false,
    ): SatelliteStateRepository {
        return SatelliteStateRepository(
            context,
            telephonyManager,
            satelliteManager,
            connectivityManager,
            scope,
            isLteNtnSupportedChecker = { _, _ -> isLteNtnSupported },
        )
    }

    @Test
    fun satelliteStatus_defaultsToNotAvailable() =
        testScope.runTest {
            repository = createRepository(backgroundScope)
            val status = repository.satelliteStatus.value
            assertThat(status).isEqualTo(SatelliteStatus.NOT_AVAILABLE)
        }

    @Test
    fun satelliteStatus_whenCarrierNtnActive_returnsActive() =
        testScope.runTest {
            repository = createRepository(backgroundScope)
            val values = mutableListOf<SatelliteStatus>()
            // Use backgroundScope to ensure the collection is cancelled at the end of the test
            repository.satelliteStatus.onEach { values.add(it) }.launchIn(backgroundScope)
            advanceUntilIdle()

            // Trigger Carrier NTN Active
            val callback = captureCarrierRoamingNtnListener()
            callback?.onCarrierRoamingNtnModeChanged(true)
            advanceUntilIdle()

            assertThat(values.last()).isEqualTo(SatelliteStatus.ACTIVE)
        }

    @Test
    fun satelliteStatus_whenOemSatelliteActive_returnsActive() =
        testScope.runTest {
            repository = createRepository(backgroundScope)
            val values = mutableListOf<SatelliteStatus>()
            repository.satelliteStatus.onEach { values.add(it) }.launchIn(backgroundScope)
            advanceUntilIdle()

            val callback = captureSatelliteModemStateCallback()
            callback.onSatelliteModemStateChanged(SatelliteManager.SATELLITE_MODEM_STATE_CONNECTED)
            advanceUntilIdle()

            assertThat(values.last()).isEqualTo(SatelliteStatus.ACTIVE)
        }

    @Test
    fun activeSubIdFlow_whenSubscriptionChanges_emitsNewSubId() =
        testScope.runTest {
            ShadowSubscriptionManager.setActiveDataSubscriptionId(1)
            repository = createRepository(backgroundScope)
            val values = mutableListOf<Int>()
            repository.activeSubIdFlow.onEach { values.add(it) }.launchIn(backgroundScope)
            advanceUntilIdle()

            assertThat(values.last()).isEqualTo(1)

            ShadowSubscriptionManager.setActiveDataSubscriptionId(2)
            // Trigger the listener by updating the subscription info list
            shadowSubscriptionManager.setActiveSubscriptionInfoList(emptyList())
            advanceUntilIdle()

            assertThat(values.last()).isEqualTo(2)
        }

    @Test
    fun satelliteStatus_whenCarrierEligibleAndNoTerrestrial_returnsAvailable() =
        testScope.runTest {
            val subId = 1
            ShadowSubscriptionManager.setActiveDataSubscriptionId(subId)
            setCarrierSupported(subId, true)
            repository = createRepository(backgroundScope)
            val values = mutableListOf<SatelliteStatus>()
            repository.satelliteStatus.onEach { values.add(it) }.launchIn(backgroundScope)
            advanceUntilIdle()
            // Ensure terrestrial is not available
            setCellularAvailable(false)
            setInternetConnected(false)

            // Trigger Carrier Eligible
            val callback = captureCarrierRoamingNtnListener()
            callback?.onCarrierRoamingNtnEligibleStateChanged(true)
            advanceUntilIdle()

            assertThat(values.last()).isEqualTo(SatelliteStatus.AVAILABLE)
        }

    @Test
    fun satelliteStatus_whenOemAllowedAndNoTerrestrial_returnsAvailable() =
        testScope.runTest {
            repository = createRepository(backgroundScope)
            val values = mutableListOf<SatelliteStatus>()
            repository.satelliteStatus.onEach { values.add(it) }.launchIn(backgroundScope)
            advanceUntilIdle()
            setCellularAvailable(false)
            setInternetConnected(false)

            val callback = captureSatelliteDisallowedReasonsCallback()
            callback.onSatelliteDisallowedReasonsChanged(IntArray(0))
            advanceUntilIdle()

            assertThat(values.last()).isEqualTo(SatelliteStatus.AVAILABLE)
        }

    @Test
    fun satelliteStatus_whenCarrierEligibleButCellularAvailable_returnsNotAvailable() =
        testScope.runTest {
            val subId = 1
            ShadowSubscriptionManager.setActiveDataSubscriptionId(subId)
            setCarrierSupported(subId, true)
            repository = createRepository(backgroundScope)
            val values = mutableListOf<SatelliteStatus>()
            repository.satelliteStatus.onEach { values.add(it) }.launchIn(backgroundScope)
            advanceUntilIdle()
            // Set Carrier Eligible
            val telephonyCallback = captureCarrierRoamingNtnListener()
            telephonyCallback?.onCarrierRoamingNtnEligibleStateChanged(true)

            // Set Cellular Available
            setCellularAvailable(true)
            advanceUntilIdle()

            assertThat(values.last()).isEqualTo(SatelliteStatus.NOT_AVAILABLE)
        }

    @Test
    fun satelliteStatus_whenOemAllowedButInternetConnected_returnsNotAvailable() =
        testScope.runTest {
            repository = createRepository(backgroundScope)
            val values = mutableListOf<SatelliteStatus>()
            repository.satelliteStatus.onEach { values.add(it) }.launchIn(backgroundScope)
            advanceUntilIdle()
            // Set OEM Allowed
            val satelliteCallback = captureSatelliteDisallowedReasonsCallback()
            satelliteCallback.onSatelliteDisallowedReasonsChanged(IntArray(0))

            // Set Internet Connected
            setInternetConnected(true)
            advanceUntilIdle()

            assertThat(values.last()).isEqualTo(SatelliteStatus.NOT_AVAILABLE)
        }

    @Test
    fun satelliteStatus_whenActive_overridesAvailable() =
        testScope.runTest {
            val subId = 1
            ShadowSubscriptionManager.setActiveDataSubscriptionId(subId)
            setCarrierSupported(subId, true)
            repository = createRepository(backgroundScope)
            val values = mutableListOf<SatelliteStatus>()
            repository.satelliteStatus.onEach { values.add(it) }.launchIn(backgroundScope)
            advanceUntilIdle()
            // Set Carrier Eligible (Available)
            val telephonyCallback = captureCarrierRoamingNtnListener()
            telephonyCallback?.onCarrierRoamingNtnEligibleStateChanged(true)
            setCellularAvailable(false)
            setInternetConnected(false)
            advanceUntilIdle()
            assertThat(values.last()).isEqualTo(SatelliteStatus.AVAILABLE)

            // Set OEM Active (Active)
            val modemCallback = captureSatelliteModemStateCallback()
            modemCallback.onSatelliteModemStateChanged(
                SatelliteManager.SATELLITE_MODEM_STATE_CONNECTED
            )
            advanceUntilIdle()

            assertThat(values.last()).isEqualTo(SatelliteStatus.ACTIVE)
        }

    @Test
    fun satelliteStatus_whenLteNtnSupported_returnsNotAvailable() =
        testScope.runTest {
            val subId = 1
            ShadowSubscriptionManager.setActiveDataSubscriptionId(subId)
            setCarrierSupported(subId, true)
            repository = createRepository(backgroundScope, isLteNtnSupported = true)
            val values = mutableListOf<SatelliteStatus>()
            repository.satelliteStatus.onEach { values.add(it) }.launchIn(backgroundScope)
            advanceUntilIdle()

            // Set Carrier Eligible (would be Available, but since LTE NTN is supported, it is not)
            val telephonyCallback = captureCarrierRoamingNtnListener()
            telephonyCallback?.onCarrierRoamingNtnEligibleStateChanged(true)
            setCellularAvailable(false)
            setInternetConnected(false)
            advanceUntilIdle()

            assertThat(values.last()).isEqualTo(SatelliteStatus.NOT_AVAILABLE)
        }

    @Test
    fun satelliteStatus_whenAirplaneModeOn_returnsNotAvailable() =
        testScope.runTest {
            val subId = 1
            ShadowSubscriptionManager.setActiveDataSubscriptionId(subId)
            setCarrierSupported(subId, true)
            repository = createRepository(backgroundScope)
            val values = mutableListOf<SatelliteStatus>()
            repository.satelliteStatus.onEach { values.add(it) }.launchIn(backgroundScope)
            advanceUntilIdle()

            // Set conditions for AVAILABLE state
            setCellularAvailable(false)
            setInternetConnected(false)
            val callback = captureCarrierRoamingNtnListener()
            callback?.onCarrierRoamingNtnEligibleStateChanged(true)
            advanceUntilIdle()
            assertThat(values.last()).isEqualTo(SatelliteStatus.AVAILABLE)

            // Turn on Airplane Mode
            setAirplaneMode(true)
            advanceUntilIdle()

            assertThat(values.last()).isEqualTo(SatelliteStatus.NOT_AVAILABLE)

            // Turn off Airplane Mode
            setAirplaneMode(false)
            advanceUntilIdle()

            assertThat(values.last()).isEqualTo(SatelliteStatus.AVAILABLE)
        }

    @Test
    fun satelliteDisallowedReasons_phoneProcessNotReady_doesNotCrash() =
        testScope.runTest {
            doThrow(IllegalStateException("Telephony service is null"))
                .`when`(satelliteManager)
                .registerForSatelliteDisallowedReasonsChanged(any(), any())

            repository = createRepository(backgroundScope)

            // Act: Collect the flow
            // We expect this NOT to throw an exception.
            try {
                repository.satelliteDisallowedReasons.launchIn(backgroundScope)
                advanceUntilIdle()
            } catch (e: Exception) {
                org.junit.Assert.fail(
                    "Repository crashed when phone process was down: ${e.message}"
                )
            }
        }

    @Test
    fun getSatelliteDataSupportMode_returnsCorrectMode() =
        testScope.runTest {
            val subId = 1
            doReturn(SatelliteManager.SATELLITE_DATA_SUPPORT_RESTRICTED)
                .`when`(satelliteManager)
                .getSatelliteDataSupportMode(subId)

            repository = createRepository(backgroundScope)

            val mode = repository.getSatelliteDataSupportMode(subId)
            assertThat(mode).isEqualTo(SatelliteManager.SATELLITE_DATA_SUPPORT_RESTRICTED)
        }

    @Test
    fun getSatelliteDataSupportMode_invalidSubId_returnsUnknown() =
        testScope.runTest {
            repository = createRepository(backgroundScope)

            val mode = repository.getSatelliteDataSupportMode(SubscriptionManager.INVALID_SUBSCRIPTION_ID)
            assertThat(mode).isEqualTo(SatelliteManager.SATELLITE_DATA_SUPPORT_UNKNOWN)
        }

    @Test
    fun getSatelliteDataSupportMode_exception_returnsUnknown() =
        testScope.runTest {
            val subId = 1
            doThrow(IllegalStateException("Modem exception"))
                .`when`(satelliteManager)
                .getSatelliteDataSupportMode(subId)

            repository = createRepository(backgroundScope)

            val mode = repository.getSatelliteDataSupportMode(subId)
            assertThat(mode).isEqualTo(SatelliteManager.SATELLITE_DATA_SUPPORT_UNKNOWN)
        }

    @Test
    fun satelliteStatus_whenOemSatelliteNotConnected_returnsActive() =
        testScope.runTest {
            repository = createRepository(backgroundScope)
            val values = mutableListOf<SatelliteStatus>()
            repository.satelliteStatus.onEach { values.add(it) }.launchIn(backgroundScope)
            advanceUntilIdle()

            val callback = captureSatelliteModemStateCallback()
            callback.onSatelliteModemStateChanged(
                SatelliteManager.SATELLITE_MODEM_STATE_NOT_CONNECTED
            )
            advanceUntilIdle()

            assertThat(values.last()).isEqualTo(SatelliteStatus.ACTIVE)
        }

    // Helpers to capture callbacks and trigger updates

    private fun setCarrierSupported(subId: Int, supported: Boolean) {
        `when`(mockSatelliteSettingsRepository.isSatelliteAttachSupported(subId))
            .thenReturn(supported)
    }

    private fun setAirplaneMode(airplaneModeOn: Boolean) {
        Settings.Global.putInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON,
            if (airplaneModeOn) 1 else 0,
        )
        context.contentResolver.notifyChange(
            Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON),
            null,
        )
        ShadowLooper.idleMainLooper()
    }

    private fun captureCarrierRoamingNtnListener(): TelephonyCallback.CarrierRoamingNtnListener? {
        val captor = ArgumentCaptor.forClass(TelephonyCallback::class.java)
        verify(telephonyManager, atLeastOnce()).registerTelephonyCallback(any(), captor.capture())
        return captor.allValues.find { it is TelephonyCallback.CarrierRoamingNtnListener }
            as? TelephonyCallback.CarrierRoamingNtnListener
    }

    private fun captureSatelliteModemStateCallback(): SatelliteModemStateCallback {
        val captor = ArgumentCaptor.forClass(SatelliteModemStateCallback::class.java)
        verify(satelliteManager, atLeastOnce())
            .registerForModemStateChanged(any(), captor.capture())
        return captor.value
    }

    private fun captureSatelliteDisallowedReasonsCallback(): SatelliteDisallowedReasonsCallback {
        val captor = ArgumentCaptor.forClass(SatelliteDisallowedReasonsCallback::class.java)
        verify(satelliteManager, atLeastOnce())
            .registerForSatelliteDisallowedReasonsChanged(any(), captor.capture())
        return captor.value
    }

    private fun captureNetworkCallback(): ConnectivityManager.NetworkCallback {
        val captor = ArgumentCaptor.forClass(ConnectivityManager.NetworkCallback::class.java)
        verify(connectivityManager, atLeastOnce())
            .registerDefaultNetworkCallback(captor.capture(), any())
        return captor.value
    }

    private fun setCellularAvailable(available: Boolean) {
        val captor = ArgumentCaptor.forClass(TelephonyCallback::class.java)
        verify(telephonyManager, atLeastOnce()).registerTelephonyCallback(any(), captor.capture())
        // Find the ServiceStateListener among captured callbacks
        val callback =
            captor.allValues.find { it is TelephonyCallback.ServiceStateListener }
                as? TelephonyCallback.ServiceStateListener

        val serviceState = mock(ServiceState::class.java)
        doReturn(
                if (available) ServiceState.STATE_IN_SERVICE else ServiceState.STATE_OUT_OF_SERVICE
            )
            .`when`(serviceState)
            .state
        doReturn(false).`when`(serviceState).isUsingNonTerrestrialNetwork

        callback?.onServiceStateChanged(serviceState)
    }

    private fun setInternetConnected(connected: Boolean) {
        val callback = captureNetworkCallback()
        val network = mock(Network::class.java)
        if (connected) {
            val caps = mock(NetworkCapabilities::class.java)
            doReturn(true).`when`(caps).hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            callback.onCapabilitiesChanged(network, caps)
        } else {
            callback.onLost(network)
        }
    }
}
