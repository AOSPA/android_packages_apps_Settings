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

import android.content.Context
import com.android.settings.accessibility.ColorAndMotionScreen
import com.android.settings.accessibility.VibrationIntensityScreen
import com.android.settings.accessibility.VibrationScreen
import com.android.settings.connecteddevice.BluetoothDashboardScreen
import com.android.settings.datausage.DataSaverScreen
import com.android.settings.deviceinfo.aboutphone.MyDeviceInfoScreen
import com.android.settings.deviceinfo.firmwareversion.FirmwareVersionScreen
import com.android.settings.deviceinfo.hardwareinfo.DeviceModelPreference
import com.android.settings.deviceinfo.hardwareinfo.HardwareInfoScreen
import com.android.settings.deviceinfo.hardwareinfo.HardwareVersionPreference
import com.android.settings.deviceinfo.legal.LegalSettingsScreen
import com.android.settings.deviceinfo.legal.ModuleLicensesScreen
import com.android.settings.deviceinfo.storage.StoragePreferenceScreen
import com.android.settings.display.AutoBrightnessScreen
import com.android.settings.display.DisplayScreen
import com.android.settings.display.ScreenTimeoutScreen
import com.android.settings.display.darkmode.DarkModeScreen
import com.android.settings.dream.ScreensaverScreen
import com.android.settings.fuelgauge.batterysaver.BatterySaverScreen
import com.android.settings.fuelgauge.batteryusage.PowerUsageSummaryScreen
import com.android.settings.gestures.SystemNavigationGestureScreen
import com.android.settings.language.LanguageAndRegionScreen
import com.android.settings.location.LocationScreen
import com.android.settings.location.RecentLocationAccessScreen
import com.android.settings.network.AdaptiveConnectivityScreen
import com.android.settings.network.MobileNetworkListScreen
import com.android.settings.network.NetworkDashboardScreen
import com.android.settings.network.NetworkProviderScreen
import com.android.settings.network.tether.TetherScreen
import com.android.settings.notification.SoundScreen
import com.android.settings.notification.modes.devicestate.ZenModeBedtimeScreen
import com.android.settings.notification.modes.devicestate.ZenModeButtonPreference
import com.android.settings.notification.modes.devicestate.ZenModeDndDisplayScreen
import com.android.settings.notification.modes.devicestate.ZenModeDndScreen
import com.android.settings.security.LockScreenPreferenceScreen
import com.android.settings.spa.app.catalyst.AllAppsScreen
import com.android.settings.spa.app.catalyst.AppInfoAllFilesAccessScreen
import com.android.settings.spa.app.catalyst.AppInfoFullScreenIntentScreen
import com.android.settings.spa.app.catalyst.AppInfoInteractAcrossProfilesScreen
import com.android.settings.spa.app.catalyst.AppInfoNotificationAccessScreen
import com.android.settings.spa.app.catalyst.AppInfoStorageScreen
import com.android.settings.spa.app.catalyst.AppInteractAcrossProfilesAppListScreen
import com.android.settings.spa.app.catalyst.AppStorageAppListScreen
import com.android.settings.spa.app.catalyst.AppsAllFilesAccessAppListScreen
import com.android.settings.spa.app.catalyst.AppsFullScreenIntentAppListScreen
import com.android.settings.spa.app.catalyst.AppsNotificationAccessScreen
import com.android.settings.supervision.SupervisionDashboardScreen
import com.android.settings.supervision.SupervisionPinManagementScreen
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.getPreferenceSummary

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
    val enabled: Boolean = true,
    val settingKey: String,
    val settingScreenKey: String,
    // TODO hint text should come from a "description" field, which currently only exists on Screens
    val hintText: (Context, PreferenceMetadata) -> String? = { _, _ -> null },
)

/**
 * Configuration of a screen converting to device states.
 *
 * @param enabled whether expose device states on this screen to App Functions.
 * @param screenKey the unique ID for the screen.
 * @param category the device state category of the screen. The default is UNCATEGORIZED.
 */
data class PerScreenCatalystConfig(
    val enabled: Boolean,
    val screenKey: String,
    // TODO(b/405344827): map categories to PreferenceMetadata#tags
    val category: Set<DeviceStateCategory> = setOf(DeviceStateCategory.UNCATEGORIZED),
)

fun getCatalystScreenConfigs() =
    listOf(
        PerScreenCatalystConfig(enabled = true, screenKey = DarkModeScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = ColorAndMotionScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = AdaptiveConnectivityScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = "adaptive_vibration_settings_page"),
        PerScreenCatalystConfig(enabled = true, screenKey = AutoBrightnessScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = BatterySaverScreen.KEY,
            category = setOf(DeviceStateCategory.BATTERY),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = BluetoothDashboardScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = LockScreenPreferenceScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = "touch_sensitivity_settings_page"),
        PerScreenCatalystConfig(enabled = true, screenKey = DisplayScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = FirmwareVersionScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = NetworkProviderScreen.KEY,
            category = setOf(DeviceStateCategory.MOBILE_DATA),
        ),
        PerScreenCatalystConfig(enabled = false, screenKey = LanguageAndRegionScreen.KEY),
        PerScreenCatalystConfig(enabled = false, screenKey = ModuleLicensesScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = LegalSettingsScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = LocationScreen.KEY,
            category = setOf(DeviceStateCategory.UNCATEGORIZED),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = RecentLocationAccessScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = AppsAllFilesAccessAppListScreen.KEY,
            category = setOf(DeviceStateCategory.UNCATEGORIZED),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = AppsFullScreenIntentAppListScreen.KEY,
            category = setOf(DeviceStateCategory.UNCATEGORIZED),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = MobileNetworkListScreen.KEY,
            category = setOf(DeviceStateCategory.MOBILE_DATA),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = MyDeviceInfoScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = DataSaverScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = NetworkDashboardScreen.KEY,
            category = setOf(DeviceStateCategory.MOBILE_DATA),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = PowerUsageSummaryScreen.KEY,
            category = setOf(DeviceStateCategory.BATTERY),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = "reverse_charging"),
        PerScreenCatalystConfig(enabled = true, screenKey = ScreenTimeoutScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = SoundScreen.KEY),
        PerScreenCatalystConfig(enabled = false, screenKey = SupervisionPinManagementScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = TetherScreen.KEY,
            category = setOf(DeviceStateCategory.MOBILE_DATA),
        ),
        PerScreenCatalystConfig(enabled = false, screenKey = SupervisionDashboardScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = VibrationIntensityScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = VibrationScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = AppStorageAppListScreen.KEY,
            category = setOf(DeviceStateCategory.STORAGE),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = AllAppsScreen.KEY,
            category = setOf(DeviceStateCategory.STORAGE),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = StoragePreferenceScreen.KEY,
            category = setOf(DeviceStateCategory.STORAGE),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = ZenModeDndScreen.KEY,
            category = setOf(DeviceStateCategory.UNCATEGORIZED),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = ZenModeDndDisplayScreen.KEY,
            category = setOf(DeviceStateCategory.UNCATEGORIZED),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = ZenModeBedtimeScreen.KEY,
            category = setOf(DeviceStateCategory.UNCATEGORIZED),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = AppInteractAcrossProfilesAppListScreen.KEY,
            category = setOf(DeviceStateCategory.UNCATEGORIZED),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = AppsNotificationAccessScreen.KEY,
            category = setOf(DeviceStateCategory.UNCATEGORIZED),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = ScreensaverScreen.KEY,
            category = setOf(DeviceStateCategory.UNCATEGORIZED),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = SystemNavigationGestureScreen.KEY,
            category = setOf(DeviceStateCategory.UNCATEGORIZED),
        ),
    )

fun getDeviceStateItemList() =
    listOf(
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "daltonizer_preference",
            settingScreenKey = ColorAndMotionScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "toggle_inversion_preference",
            settingScreenKey = ColorAndMotionScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "dark_ui_mode",
            settingScreenKey = ColorAndMotionScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "animator_duration_scale",
            settingScreenKey = ColorAndMotionScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "adaptive_connectivity_enabled",
            settingScreenKey = AdaptiveConnectivityScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "adaptive_vibration_top_intro",
            settingScreenKey = "adaptive_vibration_settings_page",
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "adaptive_vibration_illustration",
            settingScreenKey = "adaptive_vibration_settings_page",
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "adaptive_haptics_v2_enable",
            settingScreenKey = "adaptive_vibration_settings_page",
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "adaptive_vibration_footer",
            settingScreenKey = "adaptive_vibration_settings_page",
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "battery_saver",
            settingScreenKey = BatterySaverScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "basic_battery_saver",
            settingScreenKey = BatterySaverScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "extreme_battery_saver",
            settingScreenKey = BatterySaverScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "adaptive_battery_top_intro",
            settingScreenKey = BatterySaverScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "adaptive_battery_management_enabled",
            settingScreenKey = BatterySaverScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "use_bluetooth",
            settingScreenKey = BluetoothDashboardScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "bluetooth_screen_footer",
            settingScreenKey = BluetoothDashboardScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "ambient_display_always_on",
            settingScreenKey = LockScreenPreferenceScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "touch_sensitivity_settings_top_intro",
            settingScreenKey = "touch_sensitivity_settings_page",
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "touch_sensitivity_illustration",
            settingScreenKey = "touch_sensitivity_settings_page",
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "adaptive_touch_sensitivity_enabled",
            settingScreenKey = "touch_sensitivity_settings_page",
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "touch_sensitivity_enabled",
            settingScreenKey = "touch_sensitivity_settings_page",
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "brightness",
            settingScreenKey = DisplayScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "auto_brightness_entry",
            settingScreenKey = DisplayScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "lockscreen_from_display_settings",
            settingScreenKey = DisplayScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "dark_ui_mode",
            settingScreenKey = DisplayScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "touch_sensitivity_settings_page",
            settingScreenKey = DisplayScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "peak_refresh_rate",
            settingScreenKey = DisplayScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "os_firmware_version",
            settingScreenKey = FirmwareVersionScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "security_key",
            settingScreenKey = FirmwareVersionScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "module_version",
            settingScreenKey = FirmwareVersionScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "base_band",
            settingScreenKey = FirmwareVersionScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "kernel_version",
            settingScreenKey = FirmwareVersionScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "os_build_number",
            settingScreenKey = FirmwareVersionScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "main_toggle_wifi",
            settingScreenKey = NetworkProviderScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "copyright",
            settingScreenKey = LegalSettingsScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "license",
            settingScreenKey = LegalSettingsScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "terms",
            settingScreenKey = LegalSettingsScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "module_license",
            settingScreenKey = LegalSettingsScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "webview_license",
            settingScreenKey = LegalSettingsScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "legal_source_code",
            settingScreenKey = LegalSettingsScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "wallpaper_attributions",
            settingScreenKey = LegalSettingsScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "mobile_data",
            settingScreenKey = MobileNetworkListScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "use_data_saver",
            settingScreenKey = DataSaverScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "mobile_network_list",
            settingScreenKey = NetworkDashboardScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "airplane_mode_on",
            settingScreenKey = NetworkDashboardScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "restrict_background_parent_entry",
            settingScreenKey = NetworkDashboardScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "battery_header",
            settingScreenKey = PowerUsageSummaryScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "status_bar_show_battery_percent",
            settingScreenKey = PowerUsageSummaryScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "reverse_charging_summary",
            settingScreenKey = "reverse_charging",
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "reverse_charging_illustration",
            settingScreenKey = "reverse_charging",
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "reverse_charging_detail_footer",
            settingScreenKey = "reverse_charging",
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "adaptive_sleep",
            settingScreenKey = ScreenTimeoutScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "media_volume",
            settingScreenKey = SoundScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "call_volume",
            settingScreenKey = SoundScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "separate_ring_volume",
            settingScreenKey = SoundScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "dtmf_tone",
            settingScreenKey = SoundScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "supervision_pin_recovery",
            settingScreenKey = SupervisionPinManagementScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "supervision_change_pin",
            settingScreenKey = SupervisionPinManagementScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "wifi_tether",
            settingScreenKey = TetherScreen.KEY,
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
            settingScreenKey = VibrationIntensityScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "vibrate_on",
            settingScreenKey = VibrationScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = ZenModeButtonPreference.KEY,
            settingScreenKey = ZenModeDndScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = ZenModeButtonPreference.KEY,
            settingScreenKey = ZenModeBedtimeScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = DeviceModelPreference.KEY,
            settingScreenKey = HardwareInfoScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = HardwareVersionPreference.KEY,
            settingScreenKey = HardwareInfoScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = AppInfoStorageScreen.KEY,
            settingScreenKey = AppStorageAppListScreen.KEY,
            hintText = { context, metadata ->
                metadata.extras(context)?.getString(AppInfoStorageScreen.KEY_EXTRA_PACKAGE_NAME)
            },
        ),
        // AppList summaries for each permission types
        DeviceStateItemConfig(
            enabled = true,
            settingKey = AppInfoAllFilesAccessScreen.KEY,
            settingScreenKey = AppsAllFilesAccessAppListScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = AppInfoFullScreenIntentScreen.KEY,
            settingScreenKey = AppsFullScreenIntentAppListScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = StoragePreferenceScreen.KEY_SUMMARY_USED,
            settingScreenKey = StoragePreferenceScreen.KEY,
            hintText = { _, _ -> "Total device storage currently used" },
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = StoragePreferenceScreen.KEY_SUMMARY_TOTAL,
            settingScreenKey = StoragePreferenceScreen.KEY,
            hintText = { _, _ -> "Total device storage" },
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = StoragePreferenceScreen.KEY_FREE_UP_SPACE,
            settingScreenKey = StoragePreferenceScreen.KEY,
            hintText = { context, metadata -> metadata.getPreferenceSummary(context).toString() },
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = StoragePreferenceScreen.KEY_PREF_APPS,
            settingScreenKey = StoragePreferenceScreen.KEY,
            hintText = { _, _ -> "Total device storage used by apps" },
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = StoragePreferenceScreen.KEY_PREF_GAMES,
            settingScreenKey = StoragePreferenceScreen.KEY,
            hintText = { _, _ -> "Total device storage used by games" },
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = StoragePreferenceScreen.KEY_PREF_DOCUMENTS,
            settingScreenKey = StoragePreferenceScreen.KEY,
            hintText = { _, _ -> "Total device storage used by document files" },
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = StoragePreferenceScreen.KEY_PREF_VIDEOS,
            settingScreenKey = StoragePreferenceScreen.KEY,
            hintText = { _, _ -> "Total device storage used by video files" },
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = StoragePreferenceScreen.KEY_PREF_AUDIO,
            settingScreenKey = StoragePreferenceScreen.KEY,
            hintText = { _, _ -> "Total device storage used by audio files" },
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = StoragePreferenceScreen.KEY_PREF_IMAGES,
            settingScreenKey = StoragePreferenceScreen.KEY,
            hintText = { _, _ -> "Total device storage used by image files" },
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = StoragePreferenceScreen.KEY_PREF_TRASH,
            settingScreenKey = StoragePreferenceScreen.KEY,
            hintText = { _, _ -> "Total device storage used by files in trash" },
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = StoragePreferenceScreen.KEY_PREF_OTHER,
            settingScreenKey = StoragePreferenceScreen.KEY,
            hintText = { _, _ -> "Total device storage used by other files" },
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = StoragePreferenceScreen.KEY_PREF_SYSTEM,
            settingScreenKey = StoragePreferenceScreen.KEY,
            hintText = { _, _ -> "Total device storage used by the operating system" },
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = StoragePreferenceScreen.KEY_PREF_TEMP,
            settingScreenKey = StoragePreferenceScreen.KEY,
            hintText = { _, _ -> "Total device storage used by temporary system files" },
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = AppInfoInteractAcrossProfilesScreen.KEY,
            settingScreenKey = AppInteractAcrossProfilesAppListScreen.KEY,
            hintText = { context, metadata ->
                metadata
                    .extras(context)
                    ?.getString(AppInfoInteractAcrossProfilesScreen.KEY_EXTRA_PACKAGE_NAME)
            },
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = AppInfoNotificationAccessScreen.KEY,
            settingScreenKey = AppsNotificationAccessScreen.KEY,
            hintText = { context, metadata ->
                metadata
                    .extras(context)
                    ?.getString(AppInfoNotificationAccessScreen.KEY_EXTRA_PACKAGE_NAME)
            },
        ),
    )
