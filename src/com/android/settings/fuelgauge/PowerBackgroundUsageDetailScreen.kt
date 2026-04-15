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

package com.android.settings.fuelgauge

import android.os.Process
import com.android.settings.R
import com.android.settings.applications.InstalledPackageName
import com.android.settings.flags.Flags
import com.android.settings.fuelgauge.BatteryOptimizeHistoricalLogEntry.Action
import com.android.settings.fuelgauge.BatteryOptimizeUtils.MODE_OPTIMIZED
import com.android.settings.fuelgauge.BatteryOptimizeUtils.MODE_RESTRICTED
import com.android.settings.fuelgauge.BatteryOptimizeUtils.MODE_UNRESTRICTED
import com.android.settings.fuelgauge.PowerBackgroundUsageDetail.EXTRA_LAUNCH_SOURCE
import com.android.settings.fuelgauge.PowerBackgroundUsageDetail.EXTRA_PACKAGE_NAME
import com.android.settings.fuelgauge.PowerBackgroundUsageDetail.EXTRA_UID
import com.android.settings.fuelgauge.PowerBackgroundUsageDetail.LaunchSourceType
import com.android.settings.overlay.FeatureFactory
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean

// LINT.IfChange
@ProvidePreferenceScreen(PowerBackgroundUsageDetailScreen.KEY, parameterized = true)
open class PowerBackgroundUsageDetailScreen :
    PowerUsageDetailBaseApiScreen(key = KEY, fragment = PowerBackgroundUsageDetail::class) {
    init {
        flag { Flags.catalystMigration26q2() }

        tags(APP_FUNCTION_BATTERY)

        parameters {
            parameter(
                name = EXTRA_PACKAGE_NAME,
                purpose = R.string.power_background_usage_app_parameter_purpose,
                required = true,
                type = InstalledPackageName,
            )
            prepareScreenExtras { parameters, extras ->
                val packageName = parameters[EXTRA_PACKAGE_NAME] ?: return@prepareScreenExtras
                val userId = Process.myUserHandle().identifier
                val uid =
                    FeatureFactory.appContext.packageManager.getPackageUidAsUser(
                        packageName,
                        userId,
                    )
                extras.putString(EXTRA_PACKAGE_NAME, packageName)
                extras.putInt(EXTRA_UID, uid)
                extras.putString(EXTRA_LAUNCH_SOURCE, LaunchSourceType.SETTINGS_API.name)
            }
        }

        preconditions(R.string.power_background_usage_detail_screen_preconditions) {
            requireBatteryOptimizationModeMutable()
        }

        preference(
            key = SWITCH_KEY,
            purpose = R.string.power_background_usage_toggle_purpose,
            type = AnyBoolean,
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)

            get {
                execute {
                    ensureBatteryOptimizeUtilsInitialized()
                    val mode = batteryOptimizeUtils.appOptimizationMode
                    return@execute mode != MODE_RESTRICTED
                }
            }
            set {
                execute { allowed ->
                    ensureBatteryOptimizeUtilsInitialized()
                    val currentMode = batteryOptimizeUtils.appOptimizationMode
                    val targetMode =
                        when (allowed) {
                            false -> MODE_RESTRICTED
                            true ->
                                if (currentMode == MODE_UNRESTRICTED) MODE_UNRESTRICTED
                                else MODE_OPTIMIZED
                        }
                    if (currentMode != targetMode) {
                        batteryOptimizeUtils.setAppUsageState(targetMode, Action.SETTINGS_API_APPLY)
                    }
                }
            }
        }
    }

    companion object {
        const val KEY = "power_background_usage_detail"
        const val SWITCH_KEY = "background_usage_allowability_switch"
    }
}
// LINT.ThenChange(PowerBackgroundUsageDetail.java)
