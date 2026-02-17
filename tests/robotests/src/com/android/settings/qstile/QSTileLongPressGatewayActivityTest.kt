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

package com.android.settings.qstile

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.android.settings.SubSettings
import com.android.settings.development.AdbWirelessDebuggingPreferenceController
import com.android.settings.development.qstile.AdbWirelessDebuggingDevelopmentTile
import com.android.settings.development.qstile.DevelopmentTiles
import com.android.settings.network.telephony.satellite.quicksettings.SatelliteLandingPageActivity
import com.android.settings.network.telephony.satellite.quicksettings.SatelliteTileService
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@RunWith(RobolectricTestRunner::class)
@Config(
    shadows =
        [QSTileLongPressGatewayActivityTest.ShadowAdbWirelessDebuggingPreferenceController::class]
)
class QSTileLongPressGatewayActivityTest {

    @Before
    fun setUp() {
        ShadowAdbWirelessDebuggingPreferenceController.reset()
    }

    @Test
    fun onCreate_withSatelliteTileComponent_launchesLandingPage() {
        val intent =
            Intent().apply {
                putExtra(
                    Intent.EXTRA_COMPONENT_NAME,
                    ComponentName("com.android.settings", SatelliteTileService::class.java.name),
                )
            }

        val activity =
            Robolectric.buildActivity(QSTileLongPressGatewayActivity::class.java, intent)
                .create()
                .get()

        val shadowApp = shadowOf(RuntimeEnvironment.getApplication())
        val nextStartedActivity = shadowApp.nextStartedActivity
        assertThat(nextStartedActivity).isNotNull()
        assertThat(nextStartedActivity.component?.className)
            .isEqualTo(SatelliteLandingPageActivity::class.java.name)
        assertThat(activity.isFinishing).isTrue()
    }

    @Test
    fun onCreate_withWirelessDebuggingTile_whenAvailable_launchesWirelessDebuggingFragment() {
        ShadowAdbWirelessDebuggingPreferenceController.sIsAvailable = true
        val intent =
            Intent().apply {
                putExtra(
                    Intent.EXTRA_COMPONENT_NAME,
                    ComponentName(
                        "com.android.settings",
                        AdbWirelessDebuggingDevelopmentTile::class.java.name,
                    ),
                )
            }

        val activity =
            Robolectric.buildActivity(QSTileLongPressGatewayActivity::class.java, intent)
                .create()
                .get()

        val shadowApp = shadowOf(RuntimeEnvironment.getApplication())
        val nextStartedActivity = shadowApp.nextStartedActivity
        assertThat(nextStartedActivity).isNotNull()
        assertThat(nextStartedActivity.component?.className).isEqualTo(SubSettings::class.java.name)
        assertThat(activity.isFinishing).isTrue()
    }

    @Test
    fun onCreate_withWirelessDebuggingTile_whenNotAvailable_launchesDeveloperOptions() {
        ShadowAdbWirelessDebuggingPreferenceController.sIsAvailable = false
        val intent =
            Intent().apply {
                putExtra(
                    Intent.EXTRA_COMPONENT_NAME,
                    ComponentName(
                        "com.android.settings",
                        AdbWirelessDebuggingDevelopmentTile::class.java.name,
                    ),
                )
            }

        val activity =
            Robolectric.buildActivity(QSTileLongPressGatewayActivity::class.java, intent)
                .create()
                .get()

        val shadowApp = shadowOf(RuntimeEnvironment.getApplication())
        val nextStartedActivity = shadowApp.nextStartedActivity
        assertThat(nextStartedActivity).isNotNull()
        assertThat(nextStartedActivity.action)
            .isEqualTo(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        assertThat(activity.isFinishing).isTrue()
    }

    @Test
    fun onCreate_withOtherDeveloperTile_launchesDeveloperOptions() {
        val intent =
            Intent().apply {
                putExtra(
                    Intent.EXTRA_COMPONENT_NAME,
                    ComponentName(
                        "com.android.settings",
                        DevelopmentTiles.ShowTaps::class.java.name,
                    ),
                )
            }

        val activity =
            Robolectric.buildActivity(QSTileLongPressGatewayActivity::class.java, intent)
                .create()
                .get()

        val shadowApp = shadowOf(RuntimeEnvironment.getApplication())
        val nextStartedActivity = shadowApp.nextStartedActivity
        assertThat(nextStartedActivity).isNotNull()
        assertThat(nextStartedActivity.action)
            .isEqualTo(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
        assertThat(activity.isFinishing).isTrue()
    }

    @Test
    fun onCreate_withExternalTileComponent_doesNothing() {
        val intent =
            Intent().apply {
                putExtra(
                    Intent.EXTRA_COMPONENT_NAME,
                    ComponentName("com.example.other", "com.example.other.OtherTileService"),
                )
            }

        val activity =
            Robolectric.buildActivity(QSTileLongPressGatewayActivity::class.java, intent)
                .create()
                .get()

        val shadowApp = shadowOf(RuntimeEnvironment.getApplication())
        val nextStartedActivity = shadowApp.nextStartedActivity
        assertThat(nextStartedActivity).isNull()
        assertThat(activity.isFinishing).isTrue()
    }

    @Test
    fun onCreate_withNullTileComponent_doesNothing() {
        val intent = Intent() // No component extra

        val activity =
            Robolectric.buildActivity(QSTileLongPressGatewayActivity::class.java, intent)
                .create()
                .get()

        val shadowApp = shadowOf(RuntimeEnvironment.getApplication())
        val nextStartedActivity = shadowApp.nextStartedActivity
        assertThat(nextStartedActivity).isNull()
        assertThat(activity.isFinishing).isTrue()
    }

    @Implements(AdbWirelessDebuggingPreferenceController::class)
    class ShadowAdbWirelessDebuggingPreferenceController {
        companion object {
            @JvmStatic var sIsAvailable = true

            @Implementation @JvmStatic fun isAvailable(context: Context): Boolean = sIsAvailable

            @JvmStatic
            fun reset() {
                sIsAvailable = true
            }
        }
    }
}
