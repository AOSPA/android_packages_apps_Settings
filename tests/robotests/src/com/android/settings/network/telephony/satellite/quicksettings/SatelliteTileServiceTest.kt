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

import android.app.PendingIntent
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.service.quicksettings.Tile
import android.telephony.ServiceState
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.telephony.satellite.SatelliteDisallowedReasonsCallback
import android.telephony.satellite.SatelliteManager
import android.telephony.satellite.SatelliteModemStateCallback
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnit
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class SatelliteTileServiceTest {

    @get:Rule val mocks = MockitoJUnit.rule()

    @Mock private lateinit var telephonyManager: TelephonyManager
    @Mock private lateinit var satelliteTilePromptUtils: SatelliteTilePromptUtils
    @Mock private lateinit var satelliteManager: SatelliteManager
    @Mock private lateinit var connectivityManager: ConnectivityManager

    @Captor private lateinit var telephonyCallbackCaptor: ArgumentCaptor<TelephonyCallback>
    @Captor
    private lateinit var modemStateCallbackCaptor: ArgumentCaptor<SatelliteModemStateCallback>
    @Captor
    private lateinit var disallowedReasonsCallbackCaptor:
        ArgumentCaptor<SatelliteDisallowedReasonsCallback>
    @Captor private lateinit var pendingIntentCaptor: ArgumentCaptor<PendingIntent>
    @Captor
    private lateinit var networkCallbackCaptor: ArgumentCaptor<ConnectivityManager.NetworkCallback>

    private lateinit var context: Context
    private lateinit var service: SatelliteTileService

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        service = spy(Robolectric.setupService(SatelliteTileService::class.java))
        service.satelliteTilePromptUtils = satelliteTilePromptUtils

        doReturn(telephonyManager).`when`(service).getSystemService(TelephonyManager::class.java)
        doReturn(satelliteManager).`when`(service).getSystemService(SatelliteManager::class.java)
        doReturn(connectivityManager)
            .`when`(service)
            .getSystemService(ConnectivityManager::class.java)

        service.onCreate()
    }

    // 1. Active State (Priority)
    @Test
    fun updateTile_active_whenModemConnected() {
        service.onStartListening()
        val callback = getSatelliteModemStateCallback()

        callback.onSatelliteModemStateChanged(SatelliteManager.SATELLITE_MODEM_STATE_CONNECTED)

        assertTileIsActive()
    }

    @Test
    fun updateTile_active_whenCarrierNtnActive() {
        service.onStartListening()
        val callback = getCarrierRoamingNtnCallback()

        callback.onCarrierRoamingNtnModeChanged(true)

        assertTileIsActive()
    }

    // 2. Available State (Standard)
    @Test
    fun updateTile_available_whenNoTerrestrialAndAllowed() {
        service.onStartListening()
        // Ensure no internet
        setInternetConnected(false)

        // Cellular Out of Service
        val serviceStateCallback = getServiceStateCallback()
        val serviceState = ServiceState()
        serviceState.state = ServiceState.STATE_OUT_OF_SERVICE
        serviceStateCallback.onServiceStateChanged(serviceState)

        // Allowed (Eligible or OEM Allowed)
        val disallowedCallback = getSatelliteDisallowedReasonsCallback()
        disallowedCallback.onSatelliteDisallowedReasonsChanged(intArrayOf()) // Empty = Allowed

        assertTileIsAvailable()
    }

    // 3. Available State (No SIM)
    @Test
    fun updateTile_available_whenServiceStateNull() {
        // If ServiceState is null, we treat it as no cellular service (false)
        doReturn(null).`when`(telephonyManager).serviceState
        service.onStartListening() // Triggers refreshCellularState
        setInternetConnected(false)

        // Allowed
        val disallowedCallback = getSatelliteDisallowedReasonsCallback()
        disallowedCallback.onSatelliteDisallowedReasonsChanged(intArrayOf())

        assertTileIsAvailable()
    }

    // 4. Not Available (Wi-Fi/Internet Block)
    @Test
    fun updateTile_notAvailable_whenInternetConnected() {
        service.onStartListening()
        // Cellular Out of Service
        val serviceStateCallback = getServiceStateCallback()
        val serviceState = ServiceState()
        serviceState.state = ServiceState.STATE_OUT_OF_SERVICE
        serviceStateCallback.onServiceStateChanged(serviceState)

        // Allowed
        val disallowedCallback = getSatelliteDisallowedReasonsCallback()
        disallowedCallback.onSatelliteDisallowedReasonsChanged(intArrayOf())

        // BUT Internet is Connected
        setInternetConnected(true)

        assertTileIsNotAvailable()
    }

    // 5. Not Available (Cellular Block)
    @Test
    fun updateTile_notAvailable_whenCellularAvailable() {
        service.onStartListening()
        // Internet disconnected
        setInternetConnected(false)

        // Allowed
        val disallowedCallback = getSatelliteDisallowedReasonsCallback()
        disallowedCallback.onSatelliteDisallowedReasonsChanged(intArrayOf())

        // BUT Cellular is IN_SERVICE
        val serviceStateCallback = getServiceStateCallback()
        val serviceState = ServiceState()
        serviceState.state = ServiceState.STATE_IN_SERVICE
        serviceStateCallback.onServiceStateChanged(serviceState)

        assertTileIsNotAvailable()
    }

    // 6. Not Available (Restricted)
    @Test
    fun updateTile_notAvailable_whenDisallowed() {
        service.onStartListening()
        // No Terrestrial Connectivity
        setInternetConnected(false)
        val serviceStateCallback = getServiceStateCallback()
        val serviceState = ServiceState()
        serviceState.state = ServiceState.STATE_OUT_OF_SERVICE
        serviceStateCallback.onServiceStateChanged(serviceState)

        // BUT Disallowed (Restricted)
        val disallowedCallback = getSatelliteDisallowedReasonsCallback()
        disallowedCallback.onSatelliteDisallowedReasonsChanged(
            intArrayOf(SatelliteManager.SATELLITE_RESULT_NOT_SUPPORTED)
        )

        assertTileIsNotAvailable()
    }

    // --- Cleanup & Interactions ---

    @Test
    fun cleanup_callbackUnregistered() {
        // Need to ensure callbacks are captured first
        val carrierCallback = getCarrierRoamingNtnCallback()
        val serviceStateCallback = getServiceStateCallback()
        val modemCallback = getSatelliteModemStateCallback()
        val disallowedCallback = getSatelliteDisallowedReasonsCallback()
        verify(connectivityManager)
            .registerDefaultNetworkCallback(
                any(ConnectivityManager.NetworkCallback::class.java),
                any(Handler::class.java),
            )
        val networkCallback = getNetworkCallback()

        service.cleanup()

        verify(telephonyManager).unregisterTelephonyCallback(carrierCallback as TelephonyCallback)
        verify(telephonyManager)
            .unregisterTelephonyCallback(serviceStateCallback as TelephonyCallback)
        verify(satelliteManager).unregisterForModemStateChanged(modemCallback)
        verify(satelliteManager).unregisterForSatelliteDisallowedReasonsChanged(disallowedCallback)
        verify(connectivityManager).unregisterNetworkCallback(networkCallback)
    }

    @Test
    fun onClick_startsActivity() {
        doNothing().`when`(service).startActivityAndCollapse(any(PendingIntent::class.java))

        service.onClick()

        verify(service).startActivityAndCollapse(pendingIntentCaptor.capture())
        val capturedIntent = shadowOf(pendingIntentCaptor.value).savedIntent
        assertThat(capturedIntent).isNotNull()
        assertThat(capturedIntent.component?.className)
            .isEqualTo(SatelliteLandingPageActivity::class.java.name)
    }

    @Test
    fun onTileAdded_setsPromptShown() {
        service.onTileAdded()
        verify(satelliteTilePromptUtils).setAddTilePromptShown(service, true)
    }

    @Test
    fun onTileRemoved_setsPromptShown() {
        service.onTileRemoved()
        verify(satelliteTilePromptUtils).setAddTilePromptShown(service, false)
    }

    // --- Helpers ---

    private fun setInternetConnected(connected: Boolean) {
        val callback = getNetworkCallback()
        val caps = mock(NetworkCapabilities::class.java)
        doReturn(connected).`when`(caps).hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        callback.onCapabilitiesChanged(mock(Network::class.java), caps)
    }

    private fun getNetworkCallback(): ConnectivityManager.NetworkCallback {
        verify(connectivityManager)
            .registerDefaultNetworkCallback(
                networkCallbackCaptor.capture(),
                any(Handler::class.java),
            )
        return networkCallbackCaptor.value
    }

    private fun getCarrierRoamingNtnCallback(): TelephonyCallback.CarrierRoamingNtnListener {
        // Capture all registered callbacks.
        // Note: The service registers 2 callbacks in onCreate.
        verify(telephonyManager, org.mockito.Mockito.atLeast(1))
            .registerTelephonyCallback(any(), telephonyCallbackCaptor.capture())
        return telephonyCallbackCaptor.allValues
            .filterIsInstance<TelephonyCallback.CarrierRoamingNtnListener>()
            .first()
    }

    private fun getServiceStateCallback(): TelephonyCallback.ServiceStateListener {
        verify(telephonyManager, org.mockito.Mockito.atLeast(1))
            .registerTelephonyCallback(any(), telephonyCallbackCaptor.capture())
        return telephonyCallbackCaptor.allValues
            .filterIsInstance<TelephonyCallback.ServiceStateListener>()
            .first()
    }

    private fun getSatelliteModemStateCallback(): SatelliteModemStateCallback {
        verify(satelliteManager)
            .registerForModemStateChanged(any(), modemStateCallbackCaptor.capture())
        return modemStateCallbackCaptor.value
    }

    private fun getSatelliteDisallowedReasonsCallback(): SatelliteDisallowedReasonsCallback {
        verify(satelliteManager)
            .registerForSatelliteDisallowedReasonsChanged(
                any(),
                disallowedReasonsCallbackCaptor.capture(),
            )
        return disallowedReasonsCallbackCaptor.value
    }

    private fun assertTileIsActive() {
        val tile = service.qsTile
        assertThat(tile.subtitle).isEqualTo(context.getString(R.string.satellite_tile_subtitle_on))
        assertThat(tile.state).isEqualTo(Tile.STATE_ACTIVE)
    }

    private fun assertTileIsAvailable() {
        val tile = service.qsTile
        assertThat(tile.subtitle)
            .isEqualTo(context.getString(R.string.satellite_tile_subtitle_available))
        assertThat(tile.state).isEqualTo(Tile.STATE_INACTIVE)
    }

    private fun assertTileIsNotAvailable() {
        val tile = service.qsTile
        assertThat(tile.subtitle)
            .isEqualTo(context.getString(R.string.satellite_tile_subtitle_not_available))
        assertThat(tile.state).isEqualTo(Tile.STATE_INACTIVE)
    }
}
