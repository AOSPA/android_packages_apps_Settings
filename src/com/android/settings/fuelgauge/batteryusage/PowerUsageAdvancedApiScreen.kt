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
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.fuelgauge.batteryusage.BatteryChartViewModel.SELECTED_INDEX_ALL
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported
import com.android.settingslib.metadata.preferencesapi.types.TimeDuration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CompletableDeferred

@ProvidePreferenceScreen(PowerUsageAdvancedApiScreen.KEY)
class PowerUsageAdvancedApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.BATTERY,
        fragment = PowerUsageAdvanced::class,
        purpose = R.string.battery_usage_summary_purpose,
        alreadyPartiallyMigrated = PowerUsageAdvancedScreen::class,
    ) {

    @set:VisibleForTesting
    var batteryUsageDataFetcher: suspend (Context) -> BatteryDiffData? = {
        getBatteryDiffDataSinceLastFullCharge(it)
    }

    init {
        flag { Flags.catalystMigration26q3() }

        tags(APP_FUNCTION_BATTERY)

        preconditions(R.string.battery_usage_screen_preconditions) {
            if (featureFactory.powerUsageFeatureProvider.isBatteryUsageEnabled()) {
                Allowed
            } else {
                HardwareUnsupported(R.string.battery_usage_screen_feature_disabled)
            }
        }

        preference(
            key = SCREEN_TIME_SINCE_LAST_FULL_CHARGE_KEY,
            purpose = R.string.screen_time_since_last_full_charge_purpose,
            type = TimeDuration,
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)
            get {
                execute {
                    val screenOnTimeMs = batteryUsageDataFetcher(context)?.screenOnTime ?: 0
                    screenOnTimeMs.milliseconds
                }
            }
        }
    }

    companion object {
        const val TAG = "PowerUsageAdvancedApiScreen"
        const val KEY = "api_battery_usage_summary"

        @VisibleForTesting
        const val SCREEN_TIME_SINCE_LAST_FULL_CHARGE_KEY = "screen_on_time_since_last_full_charge"

        suspend fun getBatteryDiffDataSinceLastFullCharge(context: Context): BatteryDiffData? {
            val deferredResult = CompletableDeferred<Map<Long, BatteryDiffData>?>()
            val batteryLevelData =
                DataProcessManager.getBatteryLevelData(
                    context,
                    null,
                    UserIdsSeries(context, /* isNonUIRequest= */ false),
                    /* isFromPeriodJob= */ false,
                ) { diffDataMap ->
                    deferredResult.complete(diffDataMap)
                }
            Log.d(TAG, "batteryLevelData: $batteryLevelData")

            val batteryDiffDataMap = deferredResult.await()
            Log.d(TAG, "batteryDiffDataMap: $batteryDiffDataMap")

            if (batteryLevelData == null || batteryDiffDataMap == null) {
                Log.w(TAG, "Battery level data or diff data map is null")
                return null
            }
            val batteryUsageMap =
                DataProcessor.generateBatteryUsageMap(context, batteryDiffDataMap, batteryLevelData)
            if (batteryUsageMap == null) {
                Log.w(TAG, "Failed to generate batteryUsageMap")
                return null
            }
            return batteryUsageMap[SELECTED_INDEX_ALL]?.get(SELECTED_INDEX_ALL)
        }
    }
}
