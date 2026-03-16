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
import com.android.settings.core.BasePreferenceController

/** Controller for battery advance info preference. */
open class BatteryAdvanceInfoController(
    val context: Context
) : BasePreferenceController(context, PREFERENCE_KEY) {

    override fun getAvailabilityStatus() = CONDITIONALLY_UNAVAILABLE

    /**
     * Called when the battery usage data is updated.
     *
     * @param batteryDiffDataMap The data of battery usage.
     * @param selectedDailyIndex The index of the selected daily slot.
     * @param slotUpdateSource The source of the slot update event.
     * @param description The localized text of the selected battery usage slot.
     */
    open fun onBatteryUsageUpdated(
        batteryDiffDataMap: MutableMap<Int?, MutableMap<Int?, BatteryDiffData?>?>?,
        selectedDailyIndex: Int,
        slotUpdateSource: Int,
        description: String?) {}

    private companion object {
        const val PREFERENCE_KEY = "battery_advance_info_category"
    }
}