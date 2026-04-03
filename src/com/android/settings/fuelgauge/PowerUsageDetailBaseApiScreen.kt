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

import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.fuelgauge.PowerBackgroundUsageDetail.EXTRA_PACKAGE_NAME
import com.android.settingslib.metadata.preferencesapi.ApiOperationContext
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.multiusers.ManagementScope
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.ApiPreconditions
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import kotlin.reflect.KClass

/** Base class for per-app api-screen that has the precondition about battery optimization mode. */
open class PowerUsageDetailBaseApiScreen(key: String, fragment: KClass<out Fragment>) :
    PreferencesApiScreen(
        key = key,
        topLevelSettingsCategory = Category.BATTERY,
        fragment = fragment,
        purpose = R.string.power_background_usage_details_screen_purpose,
        canManage = ManagementScope.PROFILE_GROUP,
    ) {
    protected lateinit var batteryOptimizeUtils: BatteryOptimizeUtils

    protected fun ApiOperationContext.ensureBatteryOptimizeUtilsInitialized() {
        if (::batteryOptimizeUtils.isInitialized) return

        val packageName = parameters.getRequired(EXTRA_PACKAGE_NAME)
        val uid = context.packageManager.getPackageUidAsUser(packageName, context.userId)
        batteryOptimizeUtils = BatteryOptimizeUtils(context, uid, packageName)
    }

    protected fun ApiOperationContext.requireBatteryOptimizationModeMutable(): ApiPreconditions {
        ensureBatteryOptimizeUtilsInitialized()
        return when {
            batteryOptimizeUtils.isDisabledForOptimizeModeOnly -> {
                Custom(
                    context.getString(
                        R.string.manager_battery_usage_footer_limited,
                        context.getString(R.string.manager_battery_usage_optimized_only),
                    ),
                    stability = PreconditionStability.STABLE_UNTIL_APK_UPDATE,
                )
            }

            batteryOptimizeUtils.isSystemOrDefaultApp -> {
                Custom(
                    context.getString(
                        R.string.manager_battery_usage_footer_limited,
                        context.getString(R.string.manager_battery_usage_unrestricted_only),
                    ),
                    stability = PreconditionStability.STABLE_UNTIL_APK_UPDATE,
                )
            }

            else -> Allowed
        }
    }
}
