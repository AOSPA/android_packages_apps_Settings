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
import android.app.NotificationManager
import android.app.StatusBarManager
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.testutils.MetricsRule
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import java.util.function.Consumer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnit
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class AddSatelliteTileActivityTest {
    @get:Rule val mocks = MockitoJUnit.rule()
    @get:Rule val metricsRule = MetricsRule()

    @Mock private lateinit var mockStatusBarManager: StatusBarManager
    @Mock private lateinit var mockNotificationManager: NotificationManager

    private val context: Application = ApplicationProvider.getApplicationContext()
    private var tileServiceCallback: Consumer<Int>? = null

    @Before
    fun setUp() {
        shadowOf(context).setSystemService(Context.STATUS_BAR_SERVICE, mockStatusBarManager)
        shadowOf(context).setSystemService(Context.NOTIFICATION_SERVICE, mockNotificationManager)
        // Reset the shared preferences to ensure test isolation.
        context
            .getSharedPreferences("SatelliteTilePromptPrefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
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
    fun requestAddTileService_whenTileAdded_finishesAndMarksPromptsShown() {
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
        assertThat(satelliteTilePromptUtils.areAllPromptsShown(context)).isTrue()
    }

    @Test
    fun requestAddTileService_whenTileAlreadyAdded_finishesAndMarksPromptsShown() {
        val (satelliteTilePromptUtils, scenario) = setupAndLaunchActivity()

        invokeRequestAddTileServiceCallbackAndClose(
            scenario,
            satelliteTilePromptUtils,
            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED,
        )

        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        assertThat(satelliteTilePromptUtils.areAllPromptsShown(context)).isTrue()
    }

    @Test
    fun requestAddTileService_whenTileNotAdded_finishesAndMarksPromptsShown() {
        val (satelliteTilePromptUtils, scenario) = setupAndLaunchActivity()

        invokeRequestAddTileServiceCallbackAndClose(
            scenario,
            satelliteTilePromptUtils,
            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED,
        )

        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        assertThat(satelliteTilePromptUtils.areAllPromptsShown(context)).isTrue()
    }

    @Test
    fun requestAddTileService_whenDialogDismissed_finishesAndMarksPromptsShown() {
        val (satelliteTilePromptUtils, scenario) = setupAndLaunchActivity()

        invokeRequestAddTileServiceCallbackAndClose(
            scenario,
            satelliteTilePromptUtils,
            StatusBarManager.TILE_ADD_REQUEST_RESULT_DIALOG_DISMISSED,
        )

        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        assertThat(satelliteTilePromptUtils.areAllPromptsShown(context)).isTrue()
    }

    @Test
    fun requestAddTileService_whenError_finishesAndDoesNotMarkPromptsShown() {
        val (satelliteTilePromptUtils, scenario) = setupAndLaunchActivity()

        invokeRequestAddTileServiceCallbackAndClose(
            scenario,
            satelliteTilePromptUtils,
            StatusBarManager.TILE_ADD_REQUEST_ERROR_APP_NOT_IN_FOREGROUND,
        )

        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        assertThat(satelliteTilePromptUtils.areAllPromptsShown(context)).isFalse()
    }

    @Test
    fun requestAddTileService_whenNotCurrentUser_finishesAndMarksPromptsShown() {
        val (satelliteTilePromptUtils, scenario) = setupAndLaunchActivity()

        invokeRequestAddTileServiceCallbackAndClose(
            scenario,
            satelliteTilePromptUtils,
            StatusBarManager.TILE_ADD_REQUEST_ERROR_NOT_CURRENT_USER,
        )

        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        assertThat(satelliteTilePromptUtils.areAllPromptsShown(context)).isTrue()
    }

    @Test
    fun onCreate_withNotificationId_cancelsNotification() {
        val testNotificationId = R.id.satellite_prompt_notification_id
        val intent =
            Intent(context, AddSatelliteTileActivity::class.java).apply {
                putExtra(AddSatelliteTileActivity.EXTRA_NOTIFICATION_ID, testNotificationId)
            }
        mockRequestAddTileService()

        val scenario = ActivityScenario.launch<AddSatelliteTileActivity>(intent)

        verify(mockNotificationManager).cancel(testNotificationId)
        scenario.close()
    }

    @Test
    fun onCreate_withoutNotificationId_doesNotCancelNotification() {
        mockRequestAddTileService()

        val scenario = ActivityScenario.launch(AddSatelliteTileActivity::class.java)

        verify(mockNotificationManager, never()).cancel(anyInt())
        scenario.close()
    }

    @Test
    fun requestAddTileService_whenTileAdded_logsAction() {
        val (satelliteTilePromptUtils, scenario) = setupAndLaunchActivity()

        invokeRequestAddTileServiceCallbackAndClose(
            scenario,
            satelliteTilePromptUtils,
            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED,
        )

        verify(metricsRule.metricsFeatureProvider)
            .action(any(), eq(SettingsEnums.ACTION_SATELLITE_NOTIFICATION_ADD_TILE))
    }

    @Test
    fun requestAddTileService_whenDialogDismissed_doesNotLogAction() {
        val (satelliteTilePromptUtils, scenario) = setupAndLaunchActivity()

        invokeRequestAddTileServiceCallbackAndClose(
            scenario,
            satelliteTilePromptUtils,
            StatusBarManager.TILE_ADD_REQUEST_RESULT_DIALOG_DISMISSED,
        )

        verify(metricsRule.metricsFeatureProvider, never())
            .action(any(), eq(SettingsEnums.ACTION_SATELLITE_NOTIFICATION_ADD_TILE))
    }

    private fun setupAndLaunchActivity():
        Pair<SatelliteTilePromptUtils, ActivityScenario<AddSatelliteTileActivity>> {
        val satelliteTilePromptUtils = spy(SatelliteTilePromptUtils())
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
