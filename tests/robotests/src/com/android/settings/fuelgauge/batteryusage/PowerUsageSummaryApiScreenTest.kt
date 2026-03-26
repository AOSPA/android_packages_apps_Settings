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

import android.content.Context
import android.content.res.Resources
import android.os.BatteryManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags
import com.android.settings.fuelgauge.BatteryInfo
import com.android.settings.fuelgauge.BatterySettingsFeatureProvider
import com.android.settings.fuelgauge.batteryusage.PowerUsageSummaryApiScreen.Companion.BATTERY_LEVEL_KEY
import com.android.settings.fuelgauge.batteryusage.PowerUsageSummaryApiScreen.Companion.BATTERY_STATUS_KEY
import com.android.settings.fuelgauge.batteryusage.PowerUsageSummaryApiScreen.Companion.EXPECTED_REMAINING_TIME_KEY
import com.android.settings.fuelgauge.batteryusage.PowerUsageSummaryApiScreen.Companion.PLUGGED_STATUS_KEY
import com.android.settings.fuelgauge.batteryusage.PowerUsageSummaryApiScreen.Companion.TIME_UNTIL_DONE_CHARGING_KEY
import com.android.settings.fuelgauge.batteryusage.PowerUsageSummaryApiScreen.Companion.isChargingOnHoldByDefender
import com.android.settings.fuelgauge.batteryusage.PowerUsageSummaryApiScreen.Companion.isDoneChargingUnderChargingOptimizationMode
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.FailedPreconditionException
import com.android.settings.testutils2.HardwareUnsupportedException
import com.android.settingslib.metadata.preferencesapi.types.TimeDuration
import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class PowerUsageSummaryApiScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val screen = PowerUsageSummaryApiScreen()
    private lateinit var batteryInfo: BatteryInfo
    private lateinit var context: Context
    private lateinit var spyResources: Resources
    private lateinit var tester: ApiTester
    private lateinit var provider: BatterySettingsFeatureProvider

    @Before
    fun setUp() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        context = spy(baseContext)
        spyResources = spy(context.resources)
        tester = ApiTester(screen, context)

        context.stub { on { resources } doReturn spyResources }
        spyResources.stub {
            on { getBoolean(com.android.settings.R.bool.config_show_top_level_battery) } doReturn
                true
        }
        provider = FakeFeatureFactory.setupForTest().batterySettingsFeatureProvider
        provider.stub { on { isChargingOptimizationMode(any(), any()) } doReturn false }
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
        spyResources.stub {
            on { getBoolean(com.android.settings.R.bool.config_show_top_level_battery) } doReturn
                false
        }
        assertThrows(HardwareUnsupportedException::class.java) { tester.getLaunchIntent() }
    }

    @Test
    fun getBatteryStatus_chargingOptModeDoneCharged_returnDoneCharging() {
        batteryInfo = BatteryInfo().apply { batteryStatus = BatteryManager.BATTERY_STATUS_CHARGING }
        setDoneChargingUnderChargingOptimizationMode(batteryInfo, true)
        setChargingStatus(batteryInfo)

        assertThat(tester.get<Int>(BATTERY_LEVEL_KEY)).isEqualTo(80)
        assertThat(tester.get<BatteryStatusType>(BATTERY_STATUS_KEY))
            .isEqualTo(BatteryStatusType.DONE_CHARGING.asApiValue)
    }

    @Test
    fun getBatteryStatus_chargingOptModeStillCharging_returnExpectedResult() {
        batteryInfo =
            BatteryInfo().apply {
                discharging = false
                batteryLevel = 40
                batteryStatus = BatteryManager.BATTERY_STATUS_CHARGING
            }
        setDoneChargingUnderChargingOptimizationMode(batteryInfo, false)
        setChargingStatus(batteryInfo)

        assertThat(tester.get<Int>(BATTERY_LEVEL_KEY)).isEqualTo(40)
        assertThat(tester.get<BatteryStatusType>(BATTERY_STATUS_KEY))
            .isEqualTo(BatteryStatusType.CHARGING.asApiValue)
    }

    @Test
    fun getPluggedStatus_returnExpectedResults() {
        // Unplug to a power source
        batteryInfo = BatteryInfo().apply { pluggedStatus = 0 }
        setChargingStatus(batteryInfo)
        assertThrows(FailedPreconditionException::class.java) {
            tester.get<String>(PLUGGED_STATUS_KEY)
        }

        batteryInfo =
            BatteryInfo().apply { pluggedStatus = BatteryManager.BATTERY_PLUGGED_WIRELESS }
        setChargingStatus(batteryInfo)
        assertThat(tester.get<PluggedStatusType>(PLUGGED_STATUS_KEY))
            .isEqualTo(PluggedStatusType.WIRELESS.asApiValue)
    }

    @Test
    fun getTimeUntilDoneCharging_disallowedPrecondition_throwFailedPreconditionException() {
        // Battery is not charging.
        batteryInfo = BatteryInfo().apply { discharging = true }
        setChargingStatus(batteryInfo)
        assertThrows(FailedPreconditionException::class.java) {
            tester.get<TimeDuration>(TIME_UNTIL_DONE_CHARGING_KEY)
        }

        // Battery defender is on
        batteryInfo = BatteryInfo().apply { discharging = false }
        setPowerChargeLimitedByDefender(batteryInfo, true)
        setChargingStatus(batteryInfo)
        assertThrows(FailedPreconditionException::class.java) {
            tester.get<TimeDuration>(TIME_UNTIL_DONE_CHARGING_KEY)
        }

        // Done charging at 80%
        batteryInfo = BatteryInfo().apply { discharging = false }
        setPowerChargeLimitedByDefender(batteryInfo, false)
        setDoneChargingUnderChargingOptimizationMode(batteryInfo, true)
        setChargingStatus(batteryInfo)
        assertThrows(FailedPreconditionException::class.java) {
            tester.get<TimeDuration>(TIME_UNTIL_DONE_CHARGING_KEY)
        }

        // Fully charging at 100%
        batteryInfo =
            BatteryInfo().apply {
                discharging = false
                batteryStatus = BatteryManager.BATTERY_STATUS_FULL
            }
        setPowerChargeLimitedByDefender(batteryInfo, false)
        setDoneChargingUnderChargingOptimizationMode(batteryInfo, false)
        setChargingStatus(batteryInfo)
        assertThrows(FailedPreconditionException::class.java) {
            tester.get<TimeDuration>(TIME_UNTIL_DONE_CHARGING_KEY)
        }

        // No prediction value
        batteryInfo =
            BatteryInfo().apply {
                discharging = false
                batteryStatus = BatteryManager.BATTERY_STATUS_CHARGING
                remainingTimeUs = 0
            }
        setPowerChargeLimitedByDefender(batteryInfo, false)
        setDoneChargingUnderChargingOptimizationMode(batteryInfo, false)
        setChargingStatus(batteryInfo)
        assertThrows(FailedPreconditionException::class.java) {
            tester.get<TimeDuration>(TIME_UNTIL_DONE_CHARGING_KEY)
        }
    }

    @Test
    fun getTimeUntilDoneCharging_allowedPrecondition_returnExpectedResults() {
        val seconds = (1.hours + 2.minutes).inWholeSeconds.toInt()
        batteryInfo =
            BatteryInfo().apply {
                discharging = false
                batteryStatus = BatteryManager.BATTERY_STATUS_CHARGING
                remainingTimeUs = seconds * 1000_000L
            }
        setPowerChargeLimitedByDefender(batteryInfo, false)
        setDoneChargingUnderChargingOptimizationMode(batteryInfo, false)
        setChargingStatus(batteryInfo)
        assertThat(tester.get<TimeDuration>(EXPECTED_REMAINING_TIME_KEY)).isEqualTo(seconds)
    }

    @Test
    fun getExpectedRemainingTime_disallowedPrecondition_throwFailedPreconditionException() {
        batteryInfo = BatteryInfo().apply { discharging = false }
        setChargingStatus(batteryInfo)
        assertThrows(FailedPreconditionException::class.java) {
            tester.get<TimeDuration>(EXPECTED_REMAINING_TIME_KEY)
        }

        batteryInfo =
            BatteryInfo().apply {
                discharging = true
                remainingTimeUs = 0
            }
        setChargingStatus(batteryInfo)
        assertThrows(FailedPreconditionException::class.java) {
            tester.get<TimeDuration>(EXPECTED_REMAINING_TIME_KEY)
        }
    }

    @Test
    fun getExpectedRemainingTime_allowedPrecondition_returnExpectedResults() {
        val seconds = (1.hours + 2.minutes).inWholeSeconds.toInt()
        batteryInfo =
            BatteryInfo().apply {
                discharging = true
                remainingTimeUs = seconds * 1000_000L
            }
        setChargingStatus(batteryInfo)
        assertThat(tester.get<TimeDuration>(EXPECTED_REMAINING_TIME_KEY)).isEqualTo(seconds)
    }

    private fun setDoneChargingUnderChargingOptimizationMode(
        batteryInfo: BatteryInfo,
        expectedResult: Boolean,
    ) {
        if (expectedResult) {
            batteryInfo.apply {
                discharging = false
                batteryLevel = 80
                batteryStatus = BatteryManager.BATTERY_STATUS_CHARGING
            }
        } else {
            batteryInfo.apply { discharging = true }
        }
        provider.stub { on { isChargingOptimizationMode(any(), any()) } doReturn expectedResult }
        assertThat(batteryInfo.isDoneChargingUnderChargingOptimizationMode(context))
            .isEqualTo(expectedResult)
    }

    private fun setPowerChargeLimitedByDefender(batteryInfo: BatteryInfo, expectedResult: Boolean) {
        if (expectedResult) {
            batteryInfo.apply {
                isBatteryDefender = true
                batteryStatus = BatteryManager.BATTERY_STATUS_NOT_CHARGING
                pluggedStatus = BatteryManager.BATTERY_PLUGGED_USB
            }
        } else {
            batteryInfo.apply {
                isBatteryDefender = false
                pluggedStatus = BatteryManager.BATTERY_PLUGGED_USB
            }
        }
        assertThat(batteryInfo.isChargingOnHoldByDefender(context)).isEqualTo(expectedResult)
    }

    private fun setChargingStatus(batteryInfo: BatteryInfo) {
        val field = PowerUsageSummaryApiScreen::class.java.getDeclaredField("batteryInfo")
        field.isAccessible = true
        field.set(screen, batteryInfo)
    }
}
