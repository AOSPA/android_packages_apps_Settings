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

import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported

@ProvidePreferenceScreen(PowerUsageAdvancedApiScreen.KEY)
class PowerUsageAdvancedApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.BATTERY,
        fragment = PowerUsageAdvanced::class,
        purpose = R.string.battery_usage_summary_purpose,
        alreadyPartiallyMigrated = PowerUsageAdvancedScreen::class,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }

        tags(APP_FUNCTION_BATTERY)

        preconditions(R.string.battery_usage_screen_preconditions) {
            if (featureFactory.powerUsageFeatureProvider.isBatteryUsageEnabled()) {
                Allowed
            } else {
                HardwareUnsupported(R.string.battery_usage_screen_feature_disabled)
            }
        }
    }

    companion object {
        const val KEY = "api_battery_usage_summary"
    }
}
