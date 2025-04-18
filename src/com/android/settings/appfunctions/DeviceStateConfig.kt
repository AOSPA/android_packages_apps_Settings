/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.appfunctions

import com.android.settings.accessibility.ColorAndMotionScreen
import com.android.settings.accessibility.VibrationIntensityScreen
import com.android.settings.accessibility.VibrationScreen
import com.android.settings.connecteddevice.BluetoothDashboardScreen
import com.android.settings.datausage.DataSaverScreen
import com.android.settings.deviceinfo.aboutphone.MyDeviceInfoScreen
import com.android.settings.deviceinfo.firmwareversion.FirmwareVersionScreen
import com.android.settings.deviceinfo.legal.LegalSettingsScreen
import com.android.settings.deviceinfo.legal.ModuleLicensesScreen
import com.android.settings.display.AutoBrightnessScreen
import com.android.settings.display.DisplayScreen
import com.android.settings.display.ScreenTimeoutScreen
import com.android.settings.display.darkmode.DarkModeScreen
import com.android.settings.fuelgauge.batterysaver.BatterySaverScreen
import com.android.settings.fuelgauge.batteryusage.PowerUsageSummaryScreen
import com.android.settings.language.LanguageSettingScreen
import com.android.settings.location.LocationScreen
import com.android.settings.network.AdaptiveConnectivityScreen
import com.android.settings.network.MobileNetworkListScreen
import com.android.settings.network.NetworkDashboardScreen
import com.android.settings.network.NetworkProviderScreen
import com.android.settings.network.tether.TetherScreen
import com.android.settings.notification.SoundScreen
import com.android.settings.security.LockScreenPreferenceScreen
import com.android.settings.supervision.SupervisionDashboardScreen
import com.android.settings.supervision.SupervisionPinManagementScreen

enum class DeviceStateCategory(val functionId: String) {
    UNCATEGORIZED("getUncategorizedDeviceState"),
    STORAGE("getStorageDeviceState"),
    BATTERY("getBatteryDeviceState"),
    PERMISSION("getPermissionsDeviceState"),
    MOBILE_DATA("getMobileDataUsageDeviceState");

    companion object {
        fun fromId(functionId: String): DeviceStateCategory? {
            return entries.firstOrNull {it.functionId == functionId}
        }
    }
}

/**
 * Configuration of a single setting for the device state app functions. It controls how the setting
 * is presented in the device state results.
 *
 * @param enabled whether expose the device state to App Functions
 * @param settingKey the unique ID of the device state
 * @param settingScreenKey the ID of the screen that the device state is associated with
 * @param hintText additional context about the device state
 */
data class DeviceStateItemConfig(
    val enabled: Boolean,
    val settingKey: String,
    val settingScreenKey: String,
    val hintText: String = "",
)

/**
 * Configuration of a screen converting to device states.
 *
 * @param enabled whether expose device states on this screen to App Functions.
 * @param screenKey the unique ID for the screen.
 * @param category the device state category of the screen. The default is UNCATEGORIZED.
 */
data class PerScreenConfig(
    val enabled: Boolean,
    val screenKey: String,
    // TODO(b/405344827): map categories to PreferenceMetadata#tags
    val category: Set<DeviceStateCategory> = setOf(DeviceStateCategory.UNCATEGORIZED)
)

fun getScreenConfigs() = listOf(
    PerScreenConfig(
        enabled = true,
        screenKey = DarkModeScreen.KEY,
    ),
    PerScreenConfig(
        enabled = true,
        screenKey = ColorAndMotionScreen.KEY,
    ),
    PerScreenConfig(
        enabled = true,
        screenKey = AdaptiveConnectivityScreen.KEY,
    ),
    PerScreenConfig(
        enabled = true,
        screenKey = "adaptive_vibration_settings_page",
    ),
    PerScreenConfig(
        enabled = true,
        screenKey = AutoBrightnessScreen.KEY,
    ),
    PerScreenConfig(
        enabled = true,
        screenKey = BatterySaverScreen.KEY,
        category = setOf(DeviceStateCategory.BATTERY)
    ),
    PerScreenConfig(
        enabled = true,
        screenKey = BluetoothDashboardScreen.KEY,
    ),
    PerScreenConfig(
        enabled = true,
        screenKey = LockScreenPreferenceScreen.KEY,
    ),
    PerScreenConfig(
        enabled = true,
        screenKey = "touch_sensitivity_settings_page",
    ),
    PerScreenConfig(
        enabled = true,
        screenKey = DisplayScreen.KEY,
    ),
    PerScreenConfig(
        enabled = true,
        screenKey = FirmwareVersionScreen.KEY,
    ),
    PerScreenConfig(
        enabled = true,
        screenKey = NetworkProviderScreen.KEY,
        category = setOf(DeviceStateCategory.MOBILE_DATA)
    ),
    PerScreenConfig(
        enabled = false,
        screenKey = LanguageSettingScreen.KEY,
    ),
    PerScreenConfig(
        enabled = false,
        screenKey = ModuleLicensesScreen.KEY,
    ),
    PerScreenConfig(
        enabled = true,
        screenKey = LegalSettingsScreen.KEY,
    ),
    PerScreenConfig(
        enabled = true,
        screenKey = LocationScreen.KEY,
        category = setOf(DeviceStateCategory.PERMISSION)
    ),
    PerScreenConfig(
        enabled = true,
        screenKey = MobileNetworkListScreen.KEY,
        category = setOf(DeviceStateCategory.MOBILE_DATA)
    ),
    PerScreenConfig(
        enabled = true,
        screenKey = MyDeviceInfoScreen.KEY,
    ),
    PerScreenConfig(
        enabled = true,
        screenKey = DataSaverScreen.KEY,
    ),
    PerScreenConfig(
        enabled = true,
        screenKey = NetworkDashboardScreen.KEY,
        category = setOf(DeviceStateCategory.MOBILE_DATA)
    ),
    PerScreenConfig(
        enabled = true,
        screenKey = PowerUsageSummaryScreen.KEY,
        category = setOf(DeviceStateCategory.BATTERY)
    ),
    PerScreenConfig(
        enabled = true,
        screenKey = "reverse_charging",
    ),
    PerScreenConfig(
        enabled = true,
        screenKey = ScreenTimeoutScreen.KEY,
    ),
    PerScreenConfig(
        enabled = true,
        screenKey = SoundScreen.KEY,
    ),
    PerScreenConfig(
        enabled = false,
        screenKey = SupervisionPinManagementScreen.KEY,
    ),
    PerScreenConfig(
        enabled = true,
        screenKey = TetherScreen.KEY,
        category = setOf(DeviceStateCategory.MOBILE_DATA)
    ),
    PerScreenConfig(
        enabled = false,
        screenKey = SupervisionDashboardScreen.KEY
    ),
    PerScreenConfig(
        enabled = true,
        screenKey = VibrationIntensityScreen.KEY,
    ),
    PerScreenConfig(
        enabled = true,
        screenKey = VibrationScreen.KEY,
    )
)

fun getDeviceStateItemList() = listOf(
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "daltonizer_preference",
        settingScreenKey = ColorAndMotionScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "toggle_inversion_preference",
        settingScreenKey = ColorAndMotionScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "dark_ui_mode",
        settingScreenKey = ColorAndMotionScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "animator_duration_scale",
        settingScreenKey = ColorAndMotionScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "adaptive_connectivity_enabled",
        settingScreenKey = AdaptiveConnectivityScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "adaptive_vibration_top_intro",
        settingScreenKey = "adaptive_vibration_settings_page"
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "adaptive_vibration_illustration",
        settingScreenKey = "adaptive_vibration_settings_page"
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "adaptive_haptics_v2_enable",
        settingScreenKey = "adaptive_vibration_settings_page"
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "adaptive_vibration_footer",
        settingScreenKey = "adaptive_vibration_settings_page"
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "battery_saver",
        settingScreenKey = BatterySaverScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "basic_battery_saver",
        settingScreenKey = BatterySaverScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "extreme_battery_saver",
        settingScreenKey = BatterySaverScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "adaptive_battery_top_intro",
        settingScreenKey = BatterySaverScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "adaptive_battery_management_enabled",
        settingScreenKey = BatterySaverScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "use_bluetooth",
        settingScreenKey = BluetoothDashboardScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "bluetooth_screen_footer",
        settingScreenKey = BluetoothDashboardScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "ambient_display_always_on",
        settingScreenKey = LockScreenPreferenceScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "touch_sensitivity_settings_top_intro",
        settingScreenKey = "touch_sensitivity_settings_page"
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "touch_sensitivity_illustration",
        settingScreenKey = "touch_sensitivity_settings_page"
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "adaptive_touch_sensitivity_enabled",
        settingScreenKey = "touch_sensitivity_settings_page"
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "touch_sensitivity_enabled",
        settingScreenKey = "touch_sensitivity_settings_page"
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "brightness",
        settingScreenKey = DisplayScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "auto_brightness_entry",
        settingScreenKey = DisplayScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "lockscreen_from_display_settings",
        settingScreenKey = DisplayScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "dark_ui_mode",
        settingScreenKey = DisplayScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "touch_sensitivity_settings_page",
        settingScreenKey = DisplayScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "peak_refresh_rate",
        settingScreenKey = DisplayScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "os_firmware_version",
        settingScreenKey = FirmwareVersionScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "security_key",
        settingScreenKey = FirmwareVersionScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "module_version",
        settingScreenKey = FirmwareVersionScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "base_band",
        settingScreenKey = FirmwareVersionScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "kernel_version",
        settingScreenKey = FirmwareVersionScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "os_build_number",
        settingScreenKey = FirmwareVersionScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "main_toggle_wifi",
        settingScreenKey = NetworkProviderScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "copyright",
        settingScreenKey = LegalSettingsScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "license",
        settingScreenKey = LegalSettingsScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "terms",
        settingScreenKey = LegalSettingsScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "module_license",
        settingScreenKey = LegalSettingsScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "webview_license",
        settingScreenKey = LegalSettingsScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "legal_source_code",
        settingScreenKey = LegalSettingsScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "wallpaper_attributions",
        settingScreenKey = LegalSettingsScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "mobile_data",
        settingScreenKey = MobileNetworkListScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "use_data_saver",
        settingScreenKey = DataSaverScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "mobile_network_list",
        settingScreenKey = NetworkDashboardScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "airplane_mode_on",
        settingScreenKey = NetworkDashboardScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "restrict_background_parent_entry",
        settingScreenKey = NetworkDashboardScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "battery_header",
        settingScreenKey = PowerUsageSummaryScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "status_bar_show_battery_percent",
        settingScreenKey = PowerUsageSummaryScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "reverse_charging_summary",
        settingScreenKey = "reverse_charging"
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "reverse_charging_illustration",
        settingScreenKey = "reverse_charging"
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "reverse_charging_detail_footer",
        settingScreenKey = "reverse_charging"
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "adaptive_sleep",
        settingScreenKey = ScreenTimeoutScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "media_volume",
        settingScreenKey = SoundScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "call_volume",
        settingScreenKey = SoundScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "separate_ring_volume",
        settingScreenKey = SoundScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "dtmf_tone",
        settingScreenKey = SoundScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "supervision_pin_recovery",
        settingScreenKey = SupervisionPinManagementScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "supervision_change_pin",
        settingScreenKey = SupervisionPinManagementScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "wifi_tether",
        settingScreenKey = TetherScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "device_supervision_switch",
        settingScreenKey = SupervisionDashboardScreen.KEY,
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "supervision_pin_management",
        settingScreenKey = SupervisionDashboardScreen.KEY,
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "vibrate_on",
        settingScreenKey = VibrationIntensityScreen.KEY
    ),
    DeviceStateItemConfig(
        enabled = true,
        settingKey = "vibrate_on",
        settingScreenKey = VibrationScreen.KEY
    ),
)
