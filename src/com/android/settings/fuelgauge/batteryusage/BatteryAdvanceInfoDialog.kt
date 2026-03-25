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

import android.app.settings.SettingsEnums
import com.android.settings.core.instrumentation.InstrumentedDialogFragment

/** Dialog for battery advance info. */
open class BatteryAdvanceInfoDialog : InstrumentedDialogFragment() {

    override fun getMetricsCategory() = SettingsEnums.OPEN_BATTERY_USAGE

    companion object {
        @JvmField
        val BATTERY_ADVANCE_INFO_SETTINGS = "battery_advance_info_enabled"
        @JvmField
        val DEFAULT = -1
        @JvmField
        val ENABLE = 1
        @JvmField
        val DISABLE = 0
    }
}