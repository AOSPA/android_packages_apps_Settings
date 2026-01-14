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
import android.os.Looper
import android.service.quicksettings.Tile
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
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

    @Mock private lateinit var repository: SatelliteStateRepository
    @Mock private lateinit var satelliteTilePromptUtils: SatelliteTilePromptUtils
    @Captor private lateinit var pendingIntentCaptor: ArgumentCaptor<PendingIntent>

    private lateinit var context: Context
    private lateinit var service: SatelliteTileService
    private val satelliteStatusFlow = MutableStateFlow(SatelliteStatus.NOT_AVAILABLE)

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()

        // Mock the Repository singleton
        doReturn(satelliteStatusFlow).`when`(repository).satelliteStatus
        SatelliteStateRepository.setInstance(repository)

        service = spy(Robolectric.setupService(SatelliteTileService::class.java))
        service.satelliteTilePromptUtils = satelliteTilePromptUtils

        service.onCreate()
    }

    @After
    fun tearDown() {
        // Reset singleton to avoid leaking state between tests
        SatelliteStateRepository.setInstance(null)
    }

    @Test
    fun updateTile_active_setsActiveState() {
        service.onStartListening()

        satelliteStatusFlow.value = SatelliteStatus.ACTIVE

        assertTileState(Tile.STATE_ACTIVE, R.string.satellite_tile_subtitle_on)
    }

    @Test
    fun updateTile_available_setsInactiveStateWithSubtitle() {
        service.onStartListening()

        satelliteStatusFlow.value = SatelliteStatus.AVAILABLE

        assertTileState(Tile.STATE_INACTIVE, R.string.satellite_tile_subtitle_available)
    }

    @Test
    fun updateTile_notAvailable_setsInactiveState() {
        service.onStartListening()

        satelliteStatusFlow.value = SatelliteStatus.NOT_AVAILABLE

        assertTileState(Tile.STATE_INACTIVE, R.string.satellite_tile_subtitle_not_available)
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

    private fun assertTileState(expectedState: Int, expectedSubtitleResId: Int) {
        // Ensure UI loop has run
        shadowOf(Looper.getMainLooper()).idle()

        val tile = service.qsTile
        assertThat(tile.state).isEqualTo(expectedState)
        assertThat(tile.subtitle).isEqualTo(context.getString(expectedSubtitleResId))
    }
}
