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

package com.android.settings.deviceinfo.batteryinfo

import android.content.Context
import android.os.BatteryManager
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.fuelgauge.BatteryUtils
import com.android.settings.overlay.FeatureFactory
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_BATTERY
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.types.AnyString
import java.util.concurrent.TimeUnit

// LINT.IfChange
@ProvidePreferenceScreen(BatteryInfoApiScreen.KEY)
class BatteryInfoApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.ABOUT_DEVICE,
        fragment = BatteryInfoFragment::class,
        purpose = R.string.battery_info_screen_purpose,
    ) {

    init {
        flag { Flags.catalystMigration26q2() }
        tags(APP_FUNCTION_BATTERY)

        preconditions(R.string.battery_info_screen_preconditions) {
            if (context.isBatteryInfoEnabled()) {
                Allowed
            } else {
                Custom(R.string.battery_info_screen_battery_information_feature_disabled)
            }
        }

        preference(
            key = MANUFACTURE_DATE_KEY,
            purpose = R.string.battery_info_manufacture_date_purpose,
            type = AnyString,
        ) {
            get {
                execute {
                    val mBatteryManager =
                        context.getSystemService(BatteryManager::class.java)
                            ?: error("BatteryManager is not available.")

                    val manufactureEpoch =
                        mBatteryManager.getLongProperty(
                            BatteryManager.BATTERY_PROPERTY_MANUFACTURING_DATE
                        )
                    val mManufactureDateInMs =
                        TimeUnit.MILLISECONDS.convert(manufactureEpoch, TimeUnit.SECONDS)

                    BatteryUtils.getBatteryInfoFormattedDate(mManufactureDateInMs).toString()
                }
            }
        }

        preference(
            key = FIRST_USE_DATE_KEY,
            purpose = R.string.battery_info_first_usage_date_purpose,
            type = AnyString,
        ) {
            get {
                execute {
                    val mBatteryManager =
                        context.getSystemService(BatteryManager::class.java)
                            ?: error("BatteryManager is not available.")

                    val firstUseDateInSec =
                        mBatteryManager.getLongProperty(
                            BatteryManager.BATTERY_PROPERTY_FIRST_USAGE_DATE
                        )

                    val firstUseDateMs =
                        TimeUnit.MILLISECONDS.convert(firstUseDateInSec, TimeUnit.SECONDS)

                    BatteryUtils.getBatteryInfoFormattedDate(firstUseDateMs).toString()
                }
            }
        }

        preference(
            key = CYCLE_COUNT_KEY,
            purpose = R.string.battery_info_cycle_count_purpose,
            type = AnyString,
        ) {
            get {
                execute {
                    val batteryIntent = BatteryUtils.getBatteryIntent(context)

                    val cycleCount: Int =
                        batteryIntent.getIntExtra(
                            BatteryManager.EXTRA_CYCLE_COUNT,
                            INVALID_CYCLE_COUNT,
                        )

                    if (cycleCount == INVALID_CYCLE_COUNT) {
                        context.getString(R.string.battery_cycle_count_not_available)
                    } else {
                        cycleCount.toString()
                    }
                }
            }
        }
    }

    companion object {
        const val KEY = "battery_info_screen"
        const val MANUFACTURE_DATE_KEY = "battery_info_manufacture_date"
        const val FIRST_USE_DATE_KEY = "battery_info_first_use_date"
        const val CYCLE_COUNT_KEY = "battery_info_cycle_count"

        const val INVALID_CYCLE_COUNT = -1

        fun Context.isBatteryInfoEnabled() =
            FeatureFactory.featureFactory.batterySettingsFeatureProvider.isBatteryInfoEnabled(this)
    }
}
// LINT.ThenChange(BatteryInfoFragment.java,
//                 BatteryInfoPreferenceController.java)
