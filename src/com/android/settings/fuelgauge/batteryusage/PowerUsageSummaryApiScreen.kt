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
import android.os.BatteryManager
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.fuelgauge.BatteryInfo
import com.android.settings.fuelgauge.BatteryUtils
import com.android.settings.overlay.FeatureFactory
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.ApiOperationContext
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.preferencesapi.types.CustomEnum
import com.android.settingslib.metadata.preferencesapi.types.EnumApiWithRes
import com.android.settingslib.metadata.preferencesapi.types.PercentageInt
import com.android.settingslib.metadata.preferencesapi.types.TimeDuration
import kotlin.time.Duration.Companion.microseconds
import kotlinx.coroutines.CompletableDeferred

@ProvidePreferenceScreen(PowerUsageSummaryApiScreen.KEY)
class PowerUsageSummaryApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.BATTERY,
        fragment = PowerUsageSummary::class,
        purpose = R.string.power_usage_summary_screen_purpose,
        alreadyPartiallyMigrated = PowerUsageSummaryScreen::class,
    ) {

    private lateinit var batteryInfo: BatteryInfo

    init {
        flag { Flags.catalystMigration26q3() }

        tags(APP_FUNCTION_BATTERY)

        preconditions(R.string.battery_settings_screen_preconditions) {
            if (context.resources.getBoolean(R.bool.config_show_top_level_battery)) {
                Allowed
            } else {
                HardwareUnsupported(R.string.battery_settings_screen_feature_disabled)
            }
        }

        preference(
            key = BATTERY_LEVEL_KEY,
            purpose = R.string.battery_level_purpose,
            type = PercentageInt,
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)
            get {
                execute {
                    ensureBatteryInfoInitialized()
                    batteryInfo.batteryLevel
                }
            }
        }

        preference(
            key = BATTERY_STATUS_KEY,
            purpose = R.string.battery_status_purpose,
            type = CustomEnum(BatteryStatusType::class, R.string.battery_status_purpose),
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)
            get {
                execute {
                    ensureBatteryInfoInitialized()
                    if (batteryInfo.isDoneChargingUnderChargingOptimizationMode(context)) {
                        return@execute BatteryStatusType.DONE_CHARGING
                    }
                    val status = batteryInfo.batteryStatus
                    BatteryStatusType.entries.find { it.asApiValue == status }
                        ?: BatteryStatusType.UNKNOWN
                }
            }
        }

        preference(
            key = PLUGGED_STATUS_KEY,
            purpose = R.string.plugged_status_purpose,
            type = CustomEnum(PluggedStatusType::class, R.string.plugged_status_purpose),
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)
            get {
                preconditions(R.string.plugged_status_preconditions) {
                    ensureBatteryInfoInitialized()
                    if (batteryInfo.pluggedStatus == 0) {
                        Custom(
                            R.string.plugged_status_unsupported_value,
                            stability = PreconditionStability.UNSTABLE,
                        )
                    } else {
                        Allowed
                    }
                }
                execute {
                    ensureBatteryInfoInitialized()
                    val status = batteryInfo.pluggedStatus
                    PluggedStatusType.entries.find { it.asApiValue == status }
                        ?: PluggedStatusType.UNKNOWN
                }
            }
        }

        preference(
            key = TIME_UNTIL_DONE_CHARGING_KEY,
            purpose = R.string.time_until_full_purpose,
            type = TimeDuration,
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)
            get {
                preconditions(R.string.charging_status_preconditions) {
                    ensureBatteryInfoInitialized()
                    if (batteryInfo.discharging) {
                        Custom(
                            R.string.charging_related_value_unsupported,
                            stability = PreconditionStability.UNSTABLE,
                        )
                    } else if (batteryInfo.isChargingOnHoldByDefender(context)) {
                        Custom(
                            R.string.power_charge_limited_reason,
                            stability = PreconditionStability.UNSTABLE,
                        )
                    } else if (batteryInfo.isDoneChargingUnderChargingOptimizationMode(context)) {
                        Custom(
                            R.string.done_charging_purpose,
                            stability = PreconditionStability.UNSTABLE,
                        )
                    } else if (batteryInfo.batteryStatus == BatteryManager.BATTERY_STATUS_FULL) {
                        Custom(
                            "The device is fully charged.",
                            stability = PreconditionStability.UNSTABLE,
                        )
                    } else if (batteryInfo.remainingTimeUs <= 0L) {
                        Custom(
                            R.string.remaining_time_estimate_value_unsupported,
                            stability = PreconditionStability.UNSTABLE,
                        )
                    } else {
                        Allowed
                    }
                }
                execute {
                    ensureBatteryInfoInitialized()
                    batteryInfo.remainingTimeUs.microseconds
                }
            }
        }

        preference(
            key = EXPECTED_REMAINING_TIME_KEY,
            purpose = R.string.expected_remaining_time_purpose,
            type = TimeDuration,
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)
            get {
                preconditions(R.string.discharging_status_preconditions) {
                    ensureBatteryInfoInitialized()
                    if (!batteryInfo.discharging) {
                        Custom(
                            R.string.discharging_related_value_unsupported,
                            stability = PreconditionStability.UNSTABLE,
                        )
                    } else if (batteryInfo.remainingTimeUs <= 0L) {
                        Custom(
                            R.string.remaining_time_estimate_value_unsupported,
                            stability = PreconditionStability.UNSTABLE,
                        )
                    } else {
                        Allowed
                    }
                }
                execute {
                    ensureBatteryInfoInitialized()
                    batteryInfo.remainingTimeUs.microseconds
                }
            }
        }
    }

    private suspend fun ApiOperationContext.ensureBatteryInfoInitialized() {
        if (this@PowerUsageSummaryApiScreen::batteryInfo.isInitialized) return

        val deferredResult = CompletableDeferred<BatteryInfo>()
        BatteryInfo.getBatteryInfo(
            context,
            { info -> deferredResult.complete(info) },
            /* shortString= */ false,
        )
        batteryInfo = deferredResult.await()
        Log.d(TAG, "batteryInfo: $batteryInfo")
    }

    companion object {
        const val TAG = "PowerUsageSummaryApiScreen"
        const val KEY = "api_power_usage_summary_screen"
        private const val CHARGE_LIMIT_PERCENTAGE = 80

        @VisibleForTesting const val BATTERY_LEVEL_KEY = "battery_level"
        @VisibleForTesting const val BATTERY_STATUS_KEY = "battery_status"
        @VisibleForTesting const val PLUGGED_STATUS_KEY = "plugged_status"
        @VisibleForTesting const val TIME_UNTIL_DONE_CHARGING_KEY = "time_until_done_charging"
        @VisibleForTesting const val EXPECTED_REMAINING_TIME_KEY = "expected_remaining_time"

        fun BatteryInfo.isDoneChargingUnderChargingOptimizationMode(context: Context): Boolean =
            !this.discharging &&
                FeatureFactory.featureFactory.batterySettingsFeatureProvider
                    .isChargingOptimizationMode(context, this.isLongLife) &&
                this.batteryLevel >= CHARGE_LIMIT_PERCENTAGE

        fun BatteryInfo.isChargingOnHoldByDefender(context: Context): Boolean {
            val dockDefenderMode = BatteryUtils.getCurrentDockDefenderMode(context, this)
            return ((this.isBatteryDefender &&
                this.batteryStatus != BatteryManager.BATTERY_STATUS_FULL &&
                dockDefenderMode == BatteryUtils.DockDefenderMode.DISABLED) ||
                dockDefenderMode == BatteryUtils.DockDefenderMode.ACTIVE)
        }
    }
}

enum class BatteryStatusType(override val asApiValue: Int, override val purpose: Int) :
    EnumApiWithRes<Int> {
    UNKNOWN(
        BatteryManager.BATTERY_STATUS_UNKNOWN,
        com.android.settingslib.R.string.battery_info_status_unknown,
    ),
    CHARGING(
        BatteryManager.BATTERY_STATUS_CHARGING,
        com.android.settingslib.R.string.battery_info_status_charging,
    ),
    DISCHARGING(
        BatteryManager.BATTERY_STATUS_DISCHARGING,
        com.android.settingslib.R.string.battery_info_status_discharging,
    ),
    NOT_CHARGING(
        BatteryManager.BATTERY_STATUS_NOT_CHARGING,
        com.android.settingslib.R.string.battery_info_status_not_charging,
    ),
    FULL(
        BatteryManager.BATTERY_STATUS_FULL,
        com.android.settingslib.R.string.battery_info_status_full_charged,
    ),
    DONE_CHARGING(6, R.string.done_charging_purpose),
}

enum class PluggedStatusType(override val asApiValue: Int, override val purpose: Int) :
    EnumApiWithRes<Int> {
    UNKNOWN(0, R.string.plugged_type_unknown_purpose),
    AC(BatteryManager.BATTERY_PLUGGED_AC, R.string.plugged_type_ac_purpose),
    USB(BatteryManager.BATTERY_PLUGGED_USB, R.string.plugged_type_usb_purpose),
    WIRELESS(BatteryManager.BATTERY_PLUGGED_WIRELESS, R.string.plugged_type_wireless_purpose),
    DOCK(BatteryManager.BATTERY_PLUGGED_DOCK, R.string.plugged_type_dock_purpose),
}
