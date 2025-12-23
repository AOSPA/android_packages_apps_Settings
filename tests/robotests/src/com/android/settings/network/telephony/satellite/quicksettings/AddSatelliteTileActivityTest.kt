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
import android.app.StatusBarManager
import android.content.Context
import android.graphics.drawable.Icon
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import java.util.function.Consumer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnit
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class AddSatelliteTileActivityTest {
    @get:Rule val mocks = MockitoJUnit.rule()

    @Mock private lateinit var mockStatusBarManager: StatusBarManager

    private val context: Application = ApplicationProvider.getApplicationContext()
    private var tileServiceCallback: Consumer<Int>? = null

    @Before
    fun setUp() {
        shadowOf(context).setSystemService(Context.STATUS_BAR_SERVICE, mockStatusBarManager)
    }

    private fun mockRequestAddTileService() {
        val tileLabel = context.getString(R.string.satellite_tile_label)
        doAnswer {
                tileServiceCallback = it.getArgument<Consumer<Int>>(4)
                null
            }
            .`when`(mockStatusBarManager)
            .requestAddTileService(
                any(), // ComponentName
                eq(tileLabel),
                any(Icon::class.java),
                any(Executor::class.java),
                any(),
            )
    }

    @Test
    fun requestAddTileService_whenTileAdded_finishesAndSetsPromptShown() {
        val (satelliteTilePromptUtils, scenario) = setupAndLaunchActivity()

        invokeRequestAddTileServiceCallbackAndClose(
            scenario,
            satelliteTilePromptUtils,
            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED,
        )

        verify(mockStatusBarManager)
            .requestAddTileService(
                any(),
                eq(context.getString(R.string.satellite_tile_label)),
                any(Icon::class.java),
                any(Executor::class.java),
                any(),
            )
        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        assertThat(satelliteTilePromptUtils.hasAddTilePromptBeenShown(context)).isTrue()
    }

    @Test
    fun requestAddTileService_whenTileAlreadyAdded_finishesAndSetsPromptShown() {
        val (satelliteTilePromptUtils, scenario) = setupAndLaunchActivity()

        invokeRequestAddTileServiceCallbackAndClose(
            scenario,
            satelliteTilePromptUtils,
            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED,
        )

        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        assertThat(satelliteTilePromptUtils.hasAddTilePromptBeenShown(context)).isTrue()
    }

    @Test
    fun requestAddTileService_whenTileNotAdded_finishesAndSetsPromptShown() {
        val (satelliteTilePromptUtils, scenario) = setupAndLaunchActivity()

        invokeRequestAddTileServiceCallbackAndClose(
            scenario,
            satelliteTilePromptUtils,
            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED,
        )

        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        assertThat(satelliteTilePromptUtils.hasAddTilePromptBeenShown(context)).isTrue()
    }

    @Test
    fun requestAddTileService_whenDialogDismissed_finishesAndSetsPromptShown() {
        val (satelliteTilePromptUtils, scenario) = setupAndLaunchActivity()

        invokeRequestAddTileServiceCallbackAndClose(
            scenario,
            satelliteTilePromptUtils,
            StatusBarManager.TILE_ADD_REQUEST_RESULT_DIALOG_DISMISSED,
        )

        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        assertThat(satelliteTilePromptUtils.hasAddTilePromptBeenShown(context)).isTrue()
    }

    @Test
    fun requestAddTileService_whenError_finishesAndDoesNotSetPromptShown() {
        val (satelliteTilePromptUtils, scenario) = setupAndLaunchActivity()

        invokeRequestAddTileServiceCallbackAndClose(
            scenario,
            satelliteTilePromptUtils,
            StatusBarManager.TILE_ADD_REQUEST_ERROR_APP_NOT_IN_FOREGROUND,
        )

        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        assertThat(satelliteTilePromptUtils.hasAddTilePromptBeenShown(context)).isFalse()
    }

    private fun setupAndLaunchActivity():
        Pair<SatelliteTilePromptUtils, ActivityScenario<AddSatelliteTileActivity>> {
        val satelliteTilePromptUtils = spy(SatelliteTilePromptUtils())
        satelliteTilePromptUtils.setAddTilePromptShown(context, false)
        mockRequestAddTileService()
        val scenario = ActivityScenario.launch(AddSatelliteTileActivity::class.java)
        ShadowLooper.idleMainLooper()
        assertThat(tileServiceCallback).isNotNull()
        return Pair(satelliteTilePromptUtils, scenario)
    }

    private fun invokeRequestAddTileServiceCallbackAndClose(
        scenario: ActivityScenario<AddSatelliteTileActivity>,
        satelliteTilePromptUtils: SatelliteTilePromptUtils,
        result: Int,
    ) {
        scenario.onActivity { activity ->
            activity.satelliteTilePromptUtils = satelliteTilePromptUtils
            tileServiceCallback!!.accept(result)
        }
        scenario.close()
    }
}
