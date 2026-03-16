/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.fuelgauge.batteryusage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE
import com.android.settings.fuelgauge.batteryusage.BatteryChartPreferenceController.SlotUpdateSource.INITIAL_REFRESH
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BatteryAdvanceInfoControllerTest {

    private lateinit var context: Context
    private lateinit var controller: BatteryAdvanceInfoController

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        controller = BatteryAdvanceInfoController(context)
    }

    @Test
    fun getAvailabilityStatus_returnConditionallyUnavailable() {
        assertThat(controller.availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    fun getPreferenceKey_returnCorrectKey() {
        assertThat(controller.preferenceKey).isEqualTo("battery_advance_info_category")
    }

    @Test
    fun onBatteryUsageUpdated_invokeOnBatteryUsageUpdatedWithoutCrash() {
        // Test with null data
        controller.onBatteryUsageUpdated(
            batteryDiffDataMap = null,
            selectedDailyIndex = 0,
            slotUpdateSource = INITIAL_REFRESH,
            description = "test"
        )

        // Test with empty data
        val emptyData = mutableMapOf<Int?, MutableMap<Int?, BatteryDiffData?>?>()
        controller.onBatteryUsageUpdated(
            batteryDiffDataMap = emptyData,
            selectedDailyIndex = 1,
            slotUpdateSource = INITIAL_REFRESH,
            description = null
        )
    }
}
