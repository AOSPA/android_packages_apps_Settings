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
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.service.quicksettings.Tile
import android.telephony.satellite.SatelliteManager
import com.android.settings.R
import com.android.settings.network.telephony.TelephonyFeatureProvider
import com.android.settings.network.telephony.satellite.SatelliteSettingsRepository
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils.MetricsRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowSubscriptionManager

@RunWith(RobolectricTestRunner::class)
class SatelliteTileServiceTest {

    @get:Rule val mocks = MockitoJUnit.rule()
    @get:Rule val metricsRule = MetricsRule()

    @Mock private lateinit var repository: SatelliteStateRepository
    @Mock private lateinit var satelliteTilePromptUtils: SatelliteTilePromptUtils
    @Captor private lateinit var pendingIntentCaptor: ArgumentCaptor<PendingIntent>

    private lateinit var context: Context
    private lateinit var service: SatelliteTileService
    private val satelliteStatusFlow = MutableStateFlow(SatelliteStatus.NOT_AVAILABLE)

    @Mock private lateinit var mockSatelliteAppsRepository: SatelliteAppsRepository

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()

        // Mock the Repository singleton
        doReturn(satelliteStatusFlow).`when`(repository).satelliteStatus
        SatelliteStateRepository.setInstance(repository)

        service = spy(Robolectric.setupService(SatelliteTileService::class.java))
        service.satelliteTilePromptUtils = satelliteTilePromptUtils

        SatelliteUtils.satelliteAppsRepositoryProvider = { mockSatelliteAppsRepository }

        service.onCreate()
    }

    @After
    fun tearDown() {
        // Reset singleton to avoid leaking state between tests
        SatelliteStateRepository.setInstance(null)
        SatelliteUtils.satelliteAppsRepositoryProvider = { SatelliteAppsRepository(it) }
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

    @Test
    fun onClick_unconstrained_startsSettingsActivity() {
        val subId = 1
        ShadowSubscriptionManager.setActiveDataSubscriptionId(subId)
        doReturn(SatelliteManager.SATELLITE_DATA_SUPPORT_UNCONSTRAINED)
            .`when`(repository)
            .getSatelliteDataSupportMode(subId)

        // Mock Carrier Roaming NTN supported
        val mockSatelliteSettingsRepository = mock(SatelliteSettingsRepository::class.java)
        `when`(mockSatelliteSettingsRepository.isSatelliteAttachSupported(subId)).thenReturn(true)

        val fakeFeatureFactory = FakeFeatureFactory.setupForTest()
        `when`(fakeFeatureFactory.mTelephonyFeatureProvider.satelliteSettingsRepository)
            .thenReturn(mockSatelliteSettingsRepository)

        val expectedIntent = Intent(android.provider.Settings.ACTION_SATELLITE_SETTING)
        `when`(mockSatelliteAppsRepository.getSettingsIntent(true)).thenReturn(expectedIntent)

        doNothing().`when`(service).startActivityAndCollapse(any(PendingIntent::class.java))

        service.onClick()

        verify(service).startActivityAndCollapse(pendingIntentCaptor.capture())
        val capturedIntent = shadowOf(pendingIntentCaptor.value).savedIntent
        assertThat(capturedIntent).isEqualTo(expectedIntent)
    }

    @Test
    fun onTileAdded_logsTileAdded() {
        service.onTileAdded()

        verify(metricsRule.metricsFeatureProvider)
            .action(any(), eq(SettingsEnums.QS_SATELLITE_TILE_ADDED))
    }

    @Test
    fun onTileRemoved_logsTileRemoved() {
        service.onTileRemoved()

        verify(metricsRule.metricsFeatureProvider)
            .action(any(), eq(SettingsEnums.QS_SATELLITE_TILE_REMOVED))
    }

    private fun assertTileState(expectedState: Int, expectedSubtitleResId: Int) {
        // Ensure UI loop has run
        shadowOf(Looper.getMainLooper()).idle()

        val tile = service.qsTile
        assertThat(tile.state).isEqualTo(expectedState)
        assertThat(tile.subtitle).isEqualTo(context.getString(expectedSubtitleResId))
    }
}
