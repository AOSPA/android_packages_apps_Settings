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
import android.service.quicksettings.Tile
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnit
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SatelliteTileServiceTest {

    @get:Rule val mocks = MockitoJUnit.rule()

    @Mock private lateinit var telephonyManager: TelephonyManager

    @Captor private lateinit var telephonyCallbackCaptor: ArgumentCaptor<TelephonyCallback>

    private lateinit var context: Context
    private lateinit var service: SatelliteTileService

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        service = spy(Robolectric.setupService(SatelliteTileService::class.java))

        doReturn(telephonyManager).`when`(service).getSystemService(TelephonyManager::class.java)

        service.onCreate()
    }

    @Test
    fun onStartListening_tileIsNotAvailable() {
        service.onStartListening()

        assertTileIsInactiveAndNotAvailable()
    }

    @Test
    fun onCarrierRoamingNtnEligibleStateChanged_whenEligible_tileIsAvailable() {
        service.onStartListening()
        val callback = getTelephonyCallback() as TelephonyCallback.CarrierRoamingNtnListener

        callback.onCarrierRoamingNtnEligibleStateChanged(true)

        assertTileIsInactiveAndAvailable()
    }

    @Test
    fun onCarrierRoamingNtnEligibleStateChanged_whenNotEligible_tileIsNotAvailable() {
        service.onStartListening()
        val callback = getTelephonyCallback() as TelephonyCallback.CarrierRoamingNtnListener
        // Set to eligible first
        callback.onCarrierRoamingNtnEligibleStateChanged(true)
        assertTileIsInactiveAndAvailable()

        // Then set to not eligible
        callback.onCarrierRoamingNtnEligibleStateChanged(false)

        assertTileIsInactiveAndNotAvailable()
    }

    @Test
    fun onCarrierRoamingNtnModeChanged_whenActive_tileIsActive() {
        service.onStartListening()
        val callback = getTelephonyCallback() as TelephonyCallback.CarrierRoamingNtnListener

        callback.onCarrierRoamingNtnModeChanged(true)

        assertTileIsActive()
    }

    @Test
    fun onCarrierRoamingNtnModeChanged_whenInactive_tileIsInactive() {
        service.onStartListening()
        val callback = getTelephonyCallback() as TelephonyCallback.CarrierRoamingNtnListener
        // Set to active first
        callback.onCarrierRoamingNtnModeChanged(true)
        assertTileIsActive()

        // Then set to not active
        callback.onCarrierRoamingNtnModeChanged(false)

        assertTileIsInactiveAndNotAvailable()
    }

    @Test
    fun cleanup_callbackUnregistered() {
        val callback = getTelephonyCallback()

        service.cleanup()

        verify(telephonyManager).unregisterTelephonyCallback(callback)
    }

    @Test
    fun updateTile_whenModeActive_tileIsActive() {
        // isEligible does not matter when mode is active
        service.isCarrierRoamingNtnModeActive = true
        service.isCarrierRoamingNtnEligible = false

        service.updateTile()

        assertTileIsActive()
    }

    @Test
    fun updateTile_whenEligible_tileIsInactiveAndAvailable() {
        service.isCarrierRoamingNtnModeActive = false
        service.isCarrierRoamingNtnEligible = true

        service.updateTile()

        assertTileIsInactiveAndAvailable()
    }

    @Test
    fun updateTile_whenNotEligibleOrActive_tileIsInactiveAndNotAvailable() {
        service.isCarrierRoamingNtnModeActive = false
        service.isCarrierRoamingNtnEligible = false

        service.updateTile()

        assertTileIsInactiveAndNotAvailable()
    }

    private fun getTelephonyCallback(): TelephonyCallback {
        verify(telephonyManager).registerTelephonyCallback(any(), telephonyCallbackCaptor.capture())
        return telephonyCallbackCaptor.value
    }

    private fun assertTileIsActive() {
        val tile = service.qsTile
        assertThat(tile.subtitle).isEqualTo(context.getString(R.string.satellite_tile_subtitle_on))
        assertThat(tile.state).isEqualTo(Tile.STATE_ACTIVE)
    }

    private fun assertTileIsInactiveAndAvailable() {
        val tile = service.qsTile
        assertThat(tile.subtitle)
            .isEqualTo(context.getString(R.string.satellite_tile_subtitle_available))
        assertThat(tile.state).isEqualTo(Tile.STATE_INACTIVE)
    }

    private fun assertTileIsInactiveAndNotAvailable() {
        val tile = service.qsTile
        assertThat(tile.subtitle)
            .isEqualTo(context.getString(R.string.satellite_tile_subtitle_not_available))
        assertThat(tile.state).isEqualTo(Tile.STATE_INACTIVE)
    }
}
