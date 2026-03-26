/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.fuelgauge.batteryusage

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.text.format.DateUtils
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags
import com.android.settings.fuelgauge.batteryusage.PowerUsageAdvancedApiScreen.Companion.SCREEN_TIME_SINCE_LAST_FULL_CHARGE_KEY
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.HardwareUnsupportedException
import com.android.settingslib.metadata.preferencesapi.types.TimeDuration
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class PowerUsageAdvancedApiScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private lateinit var context: Context
    private lateinit var featureFactory: FakeFeatureFactory
    private lateinit var tester: ApiTester
    private var fakeData: BatteryDiffData? = null

    @Before
    fun setUp() {
        val baseContext = ApplicationProvider.getApplicationContext<Application>()
        context =
            baseContext.createConfigurationContext(
                Configuration(baseContext.resources.configuration).also {
                    it.setLocale(Locale.ENGLISH)
                }
            )

        val screen = PowerUsageAdvancedApiScreen()
        screen.batteryUsageDataFetcher = { fakeData }
        tester = ApiTester(screen, context)
        featureFactory = FakeFeatureFactory.setupForTest()
        featureFactory.stub {
            on { powerUsageFeatureProvider.isBatteryUsageEnabled() } doReturn true
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q3)
    fun getScreen_flagEnabled_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_MIGRATION_26Q3)
    fun getScreen_flagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    @Test
    fun getLaunchIntent_hasIntent() {
        assertThat(tester.getLaunchIntent()).isNotNull()
    }

    @Test
    fun getLaunchIntent_configDisabled_throwHardwareUnsupportedException() {
        featureFactory.stub {
            on { powerUsageFeatureProvider.isBatteryUsageEnabled() } doReturn false
        }

        assertThrows(HardwareUnsupportedException::class.java) { tester.getLaunchIntent() }
    }

    @Test
    fun getScreenTimeSinceLastFullCharge_noBatteryDiffData_returnZero() {
        assertThat(tester.get<TimeDuration>(SCREEN_TIME_SINCE_LAST_FULL_CHARGE_KEY)).isEqualTo(0)
    }

    @Test
    fun getScreenTimeSinceLastFullCharge_screenTimeLessThanHalfSeconds_returnZero() {
        setBatteryDiffDataWithScreenTime(screenTimeMs = 499L)
        assertThat(tester.get<TimeDuration>(SCREEN_TIME_SINCE_LAST_FULL_CHARGE_KEY)).isEqualTo(0)
    }

    @Test
    fun getScreenTimeSinceLastFullCharge_screenTimeRoundToOneSecond_returnOneSecond() {
        setBatteryDiffDataWithScreenTime(screenTimeMs = 500L)

        assertThat(tester.get<TimeDuration>(SCREEN_TIME_SINCE_LAST_FULL_CHARGE_KEY)).isEqualTo(1)
    }

    @Test
    fun getScreenTimeSinceLastFullCharge_screenTimeLargerThanOneMinutes_returnExpectedInt() {
        setBatteryDiffDataWithScreenTime(
            screenTimeMs = DateUtils.HOUR_IN_MILLIS + DateUtils.MINUTE_IN_MILLIS * 12L
        )

        assertThat(tester.get<TimeDuration>(SCREEN_TIME_SINCE_LAST_FULL_CHARGE_KEY))
            .isEqualTo(60 * 60 + 12 * 60)
    }

    private fun setBatteryDiffDataWithScreenTime(screenTimeMs: Long) {
        fakeData =
            BatteryDiffData(
                context,
                /* startTimestamp= */ 100L,
                /* endTimestamp= */ 200L,
                /* startBatteryLevel= */ 10,
                /* endBatteryLevel= */ 5,
                /* screenOnTime= */ screenTimeMs,
                /* appDiffEntries= */ mutableListOf(),
                /* systemDiffEntries= */ mutableListOf(),
                /* systemAppsPackageNames= */ emptySet(),
                /* systemAppsUids= */ emptySet(),
                /* isAccumulated= */ true,
            )
    }
}
