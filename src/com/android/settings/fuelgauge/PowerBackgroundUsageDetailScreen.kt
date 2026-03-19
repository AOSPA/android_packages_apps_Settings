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
import com.android.settingslib.metadata.preferencesapi.ApiOperationContext
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.multiusers.ManagementScope
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability

// LINT.IfChange
@ProvidePreferenceScreen(PowerBackgroundUsageDetailScreen.KEY, parameterized = true)
open class PowerBackgroundUsageDetailScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.BATTERY,
        fragment = PowerBackgroundUsageDetail::class,
        purpose = R.string.power_background_usage_details_screen_purpose,
        canManage = ManagementScope.PROFILE_GROUP,
    ) {

    private lateinit var batteryOptimizeUtils: BatteryOptimizeUtils

    init {
        flag { Flags.catalystMigration26q2() }

        tags(APP_FUNCTION_BATTERY)

        parameters {
            parameter(
                name = EXTRA_PACKAGE_NAME,
                purpose = R.string.power_background_usage_app_parameter_purpose,
                required = true,
                type = InstalledPackageName(),
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
            val packageName = parameters.getRequired(EXTRA_PACKAGE_NAME)
            val uid = context.packageManager.getPackageUidAsUser(packageName, userId)
            batteryOptimizeUtils = BatteryOptimizeUtils(context, uid, packageName)
            when {
                batteryOptimizeUtils.isDisabledForOptimizeModeOnly -> {
                    Custom(
                        context.getString(
                            R.string.manager_battery_usage_footer_limited,
                            context.getString(R.string.manager_battery_usage_optimized_only),
                        ),
                        stability = PreconditionStability.UNSTABLE
                    )
                }
                batteryOptimizeUtils.isSystemOrDefaultApp -> {
                    Custom(
                        context.getString(
                            R.string.manager_battery_usage_footer_limited,
                            context.getString(R.string.manager_battery_usage_unrestricted_only),
                        ),
                        stability = PreconditionStability.UNSTABLE
                    )
                }
                else -> Allowed
            }
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

    private fun ApiOperationContext.ensureBatteryOptimizeUtilsInitialized() {
        if (this@PowerBackgroundUsageDetailScreen::batteryOptimizeUtils.isInitialized) return

        val packageName = parameters.getRequired(EXTRA_PACKAGE_NAME)
        val uid = context.packageManager.getPackageUidAsUser(packageName, context.userId)
        batteryOptimizeUtils = BatteryOptimizeUtils(context, uid, packageName)
    }

    companion object {
        const val KEY = "power_background_usage_detail"
        const val SWITCH_KEY = "background_usage_allowability_switch"
    }
}
// LINT.ThenChange(PowerBackgroundUsageDetail.java)
