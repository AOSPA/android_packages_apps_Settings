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

import com.android.server.connectivity.Flags as ConnectivityFlags
import com.android.settings.IccLockApiScreen
import com.android.settings.TrustedCredentialsScreenApi
import com.android.settings.UserCredentialsScreenApi
import com.android.settings.accessibility.AccessibilityScreen
import com.android.settings.accessibility.VibrationIntensityScreen
import com.android.settings.accessibility.VibrationIntensityScreenApi
import com.android.settings.accessibility.VibrationScreen
import com.android.settings.accessibility.VibrationScreenApi
import com.android.settings.accessibility.a11yactivity.ui.A11yActivityScreen
import com.android.settings.accessibility.a11yservice.ui.A11yServiceScreen
import com.android.settings.accessibility.actiontimeout.ui.ActionTimeoutSettingsScreen
import com.android.settings.accessibility.audioadjustment.ui.AudioAdjustmentScreen
import com.android.settings.accessibility.autoclick.ui.AutoclickScreen
import com.android.settings.accessibility.buttonshortcutsetting.ui.ButtonShortcutSettingScreen
import com.android.settings.accessibility.captionpreferences.ui.CaptioningAppearanceScreen
import com.android.settings.accessibility.captionpreferences.ui.CaptioningMoreOptionsScreen
import com.android.settings.accessibility.captionpreferences.ui.CaptioningPropertiesScreen
import com.android.settings.accessibility.colorandmotion.ui.ColorAndMotionScreen
import com.android.settings.accessibility.colorcorrection.ui.ColorCorrectionApiFirstScreen
import com.android.settings.accessibility.colorcorrection.ui.ColorCorrectionScreen
import com.android.settings.accessibility.colorinversion.ui.ColorInversionScreen
import com.android.settings.accessibility.extradim.ui.ExtraDimScreen
import com.android.settings.accessibility.flashnotifications.ui.FlashNotificationsScreen
import com.android.settings.accessibility.hearingdevices.ui.HearingDevicesScreen
import com.android.settings.accessibility.hearingdevices.ui.PairHearingDeviceScreen
import com.android.settings.accessibility.screenmagnification.ui.MagnificationScreen
import com.android.settings.accessibility.shortcuts.ui.EditShortcutsScreen
import com.android.settings.accessibility.shortcutssettings.ui.ShortcutsSettingsScreen
import com.android.settings.accessibility.systemcontrols.ui.SystemControlsScreen
import com.android.settings.accessibility.textreading.ui.TextReadingScreen
import com.android.settings.accessibility.timingcontrols.ui.TimingControlsScreen
import com.android.settings.accounts.AccountDetailApiScreen
import com.android.settings.accounts.AccountScreen
import com.android.settings.accounts.ManageAccountsScreen
import com.android.settings.accounts.ManagedProfileApiScreen
import com.android.settings.applications.AppDashboardScreen
import com.android.settings.applications.AppStorageSettingsScreenApi
import com.android.settings.applications.contacts.ContactsStorageApiScreen
import com.android.settings.applications.intentpicker.AppLaunchApiScreen
import com.android.settings.applications.managedomainurls.OpeningLinksApiScreen
import com.android.settings.applications.specialaccess.AlarmsAndRemindersAppDetailScreen
import com.android.settings.applications.specialaccess.AlarmsAndRemindersAppListScreen
import com.android.settings.applications.specialaccess.AllFilesAccessAppDetailScreen
import com.android.settings.applications.specialaccess.AllFilesAccessAppListScreen
import com.android.settings.applications.specialaccess.DisplayOverOtherAppsAppDetailScreen
import com.android.settings.applications.specialaccess.DisplayOverOtherAppsAppListScreen
import com.android.settings.applications.specialaccess.FullScreenNotificationsAppDetailScreen
import com.android.settings.applications.specialaccess.FullScreenNotificationsAppListScreen
import com.android.settings.applications.specialaccess.InstallUnknownAppsAppDetailScreen
import com.android.settings.applications.specialaccess.InstallUnknownAppsAppListScreen
import com.android.settings.applications.specialaccess.InteractAcrossProfilesAppDetailScreen
import com.android.settings.applications.specialaccess.InteractAcrossProfilesAppListScreen
import com.android.settings.applications.specialaccess.ManageWriteSettingsAppDetailScreen
import com.android.settings.applications.specialaccess.ManageWriteSettingsAppListScreen
import com.android.settings.applications.specialaccess.SpecialAccessSettingsScreen
import com.android.settings.applications.specialaccess.WifiControlAppDetailScreen
import com.android.settings.applications.specialaccess.WifiControlAppListScreen
import com.android.settings.applications.specialaccess.WriteSystemPreferencesAppDetailScreen
import com.android.settings.applications.specialaccess.WriteSystemPreferencesAppListScreen
import com.android.settings.applications.specialaccess.deviceadmin.DeviceAdminApiScreen
import com.android.settings.applications.specialaccess.notificationaccess.AppInfoNotificationAccessScreen
import com.android.settings.applications.specialaccess.notificationaccess.AppsNotificationAccessScreen
import com.android.settings.applications.specialaccess.pictureinpicture.PictureInPictureAppDetailScreen
import com.android.settings.applications.specialaccess.pictureinpicture.PictureInPictureAppListScreen
import com.android.settings.applications.specialaccess.zenaccess.ZenAccessDetailsApiScreen
import com.android.settings.backup.AccountsAndBackupScreen
import com.android.settings.connecteddevice.AdvancedConnectedDeviceApiScreen
import com.android.settings.connecteddevice.AdvancedConnectedDeviceScreen
import com.android.settings.connecteddevice.BluetoothDashboardScreen
import com.android.settings.connecteddevice.BluetoothDashboardScreenApi
import com.android.settings.connecteddevice.ConnectedDeviceDashboardScreen
import com.android.settings.connecteddevice.NfcAndPaymentScreen
import com.android.settings.connecteddevice.PreviouslyConnectedDeviceScreen
import com.android.settings.connecteddevice.display.ResolutionRefreshRateApiScreen
import com.android.settings.connecteddevice.display.TabbedDisplayApiScreen
import com.android.settings.connecteddevice.stylus.StylusUsiDetailsApiScreen
import com.android.settings.connecteddevice.usb.UsbDetailsApiScreen
import com.android.settings.datausage.AppDataUsageScreenApi
import com.android.settings.datausage.BillingCycleScreen
import com.android.settings.datausage.DataSaverScreen
import com.android.settings.datausage.DataUsageAppDetailScreen
import com.android.settings.datausage.DataUsageListScreen
import com.android.settings.datausage.UnrestrictedDataAccessApiScreen
import com.android.settings.datetime.DateTimeSettingsApiScreen
import com.android.settings.datetime.DateTimeSettingsScreen
import com.android.settings.deviceinfo.aboutphone.MyDeviceInfoApiFirstScreen
import com.android.settings.deviceinfo.aboutphone.MyDeviceInfoScreen
import com.android.settings.deviceinfo.batteryinfo.BatteryInfoApiScreen
import com.android.settings.deviceinfo.firmwareversion.FirmwareVersionScreen
import com.android.settings.deviceinfo.hardwareinfo.HardwareInfoApiScreen
import com.android.settings.deviceinfo.hardwareinfo.HardwareInfoScreen
import com.android.settings.deviceinfo.legal.LegalSettingsScreen
import com.android.settings.deviceinfo.legal.ModuleLicensesScreen
import com.android.settings.deviceinfo.storage.StoragePreferenceScreen
import com.android.settings.display.AmbientDisplayAlwaysOnPreferenceScreen
import com.android.settings.display.AutoBrightnessApiScreen
import com.android.settings.display.AutoBrightnessScreen
import com.android.settings.display.ColorContrastApiScreen
import com.android.settings.display.ColorModeApiScreen
import com.android.settings.display.ColorModeScreen
import com.android.settings.display.DeviceStateAutoRotateApiScreen
import com.android.settings.display.DisplayApiScreen
import com.android.settings.display.DisplayScreen
import com.android.settings.display.HdrBrightnessApiScreen
import com.android.settings.display.NightDisplayApiScreen
import com.android.settings.display.NightDisplayScreen
import com.android.settings.display.ScreenResolutionApiScreen
import com.android.settings.display.ScreenTimeoutScreen
import com.android.settings.display.SmartAutoRotateApiScreen
import com.android.settings.display.darkmode.DarkModeApiFirstScreen
import com.android.settings.display.darkmode.DarkModeScreen
import com.android.settings.dream.DreamSettingsApiScreen
import com.android.settings.dream.ScreensaverScreen
import com.android.settings.emergency.EmergencyDashboardScreen
import com.android.settings.fuelgauge.AdvancedPowerUsageDetailScreen
import com.android.settings.fuelgauge.PowerBackgroundUsageDetailScreen
import com.android.settings.fuelgauge.batterysaver.BatterySaverScreen
import com.android.settings.fuelgauge.batteryusage.PowerUsageAdvancedApiScreen
import com.android.settings.fuelgauge.batteryusage.PowerUsageAdvancedScreen
import com.android.settings.fuelgauge.batteryusage.PowerUsageSummaryApiScreen
import com.android.settings.fuelgauge.batteryusage.PowerUsageSummaryScreen
import com.android.settings.gestures.ButtonNavigationSettingsScreen
import com.android.settings.gestures.DoubleTapApiScreen
import com.android.settings.gestures.DoubleTapPowerApiFirstScreen
import com.android.settings.gestures.DoubleTapPowerScreen
import com.android.settings.gestures.DoubleTwistGestureApiFirstScreen
import com.android.settings.gestures.GestureSettingsApiScreen
import com.android.settings.gestures.OneHandedApiScreen
import com.android.settings.gestures.PickupGestureApiScreen
import com.android.settings.gestures.PowerMenuSettingsScreenApi
import com.android.settings.gestures.SwipeToNotificationApiScreen
import com.android.settings.gestures.SystemNavigationGestureScreen
import com.android.settings.gestures.TapScreenGestureApiScreen
import com.android.settings.inputmethod.AvailableVirtualKeyboardApiScreen
import com.android.settings.inputmethod.SpellCheckerApiScreen
import com.android.settings.inputmethod.UserDictionaryListApiScreen
import com.android.settings.language.LanguageAndRegionApiFirstScreen
import com.android.settings.language.LanguageAndRegionScreen
import com.android.settings.localepicker.SystemLocalePickerApiFirstScreen
import com.android.settings.localepicker.TermsOfAddressApiFirstScreen
import com.android.settings.location.BluetoothScanningApiScreen
import com.android.settings.location.LocationScreen
import com.android.settings.location.LocationServicesScreen
import com.android.settings.location.LocationServicesScreenApi
import com.android.settings.location.LocationSettingsScreenApi
import com.android.settings.location.RecentLocationAccessScreen
import com.android.settings.location.WifiScanningApiScreen
import com.android.settings.network.AdaptiveConnectivityApiScreen
import com.android.settings.network.AdaptiveConnectivityScreen
import com.android.settings.network.AirplaneModeSettingsScreen
import com.android.settings.network.MobileNetworkListScreen
import com.android.settings.network.NetworkDashboardScreen
import com.android.settings.network.NetworkProviderScreen
import com.android.settings.network.apn.ApnSettingsScreen
import com.android.settings.network.telephony.CellularSecurityScreenApi
import com.android.settings.network.telephony.MobileNetworkScreen
import com.android.settings.network.telephony.MobileNetworkScreenApi
import com.android.settings.network.tether.TetherApiScreen
import com.android.settings.network.tether.TetherScreen
import com.android.settings.nfc.NfcAndPaymentApiScreen
import com.android.settings.notification.BubbleNotificationScreen
import com.android.settings.notification.PoliteNotificationsApiScreen
import com.android.settings.notification.SoundApiScreen
import com.android.settings.notification.SoundScreen
import com.android.settings.notification.SpatialAudioApiScreen
import com.android.settings.notification.app.AppNotificationSettingsApiScreen
import com.android.settings.notification.app.ConversationListScreen
import com.android.settings.notification.modes.ZenModeApiScreen
import com.android.settings.notification.modes.ZenModesListScreen
import com.android.settings.print.PrintServiceApiScreen
import com.android.settings.print.PrintSettingsApiScreen
import com.android.settings.regionalpreferences.FirstDayOfWeekApiFirstScreen
import com.android.settings.regionalpreferences.MeasurementSystemApiFirstScreen
import com.android.settings.regionalpreferences.NumberingSystemLocaleListApiFirstScreen
import com.android.settings.regionalpreferences.RegionPickerApiFirstScreen
import com.android.settings.regionalpreferences.TemperatureUnitApiFirstScreen
import com.android.settings.safetycenter.ui.AccountSecuritySubpageScreenApi
import com.android.settings.safetycenter.ui.AppSecurityScreenApi
import com.android.settings.safetycenter.ui.DeviceUnlockApiScreen
import com.android.settings.safetycenter.ui.MoreSecurityPrivacyScreenApi
import com.android.settings.safetycenter.ui.PrivacyControlsScreenApi
import com.android.settings.safetycenter.ui.SafetyCenterApiScreen
import com.android.settings.safetycenter.ui.SystemAndUpdatesScreenApi
import com.android.settings.security.ContentProtectionScreenApi
import com.android.settings.security.CredentialManagementAppScreenApi
import com.android.settings.security.EncryptionAndCredentialScreenApi
import com.android.settings.security.InstallCertificateFromStorageScreenApi
import com.android.settings.security.LockScreenPreferenceScreen
import com.android.settings.security.trustagent.TrustAgentApiScreen
import com.android.settings.sound.MediaControlsScreen
import com.android.settings.spa.app.appcompat.UserAspectRatioAppApiScreen
import com.android.settings.spa.app.appcompat.UserAspectRatioAppsApiScreen
import com.android.settings.spa.app.battery.AppBatteryUsageListApiScreen
import com.android.settings.spa.app.catalyst.AllAppsScreen
import com.android.settings.spa.app.catalyst.AppInfoScreen
import com.android.settings.spa.app.catalyst.AppInfoScreenApiFirst
import com.android.settings.spa.app.catalyst.AppInfoStorageScreen
import com.android.settings.spa.app.catalyst.AppStorageAppListScreen
import com.android.settings.spa.app.specialaccess.LongBackgroundTasksAppsApiScreen
import com.android.settings.spa.app.specialaccess.MediaManagementAppsApiScreen
import com.android.settings.spa.app.specialaccess.MediaRoutingControlApiScreen
import com.android.settings.spa.app.specialaccess.NfcTagAppsSettingsApiScreen
import com.android.settings.spa.app.specialaccess.TurnScreenOnAppsApiScreen
import com.android.settings.spa.app.specialaccess.UsageDataAppListApiScreen
import com.android.settings.supervision.SupervisionDashboardScreen
import com.android.settings.supervision.appstorefilters.SupervisionAppStoreFiltersScreen
import com.android.settings.supervision.credentialmanagement.SupervisionPinManagementScreen
import com.android.settings.supervision.webcontentfilters.SupervisionWebContentFiltersBrowserSupportedAppsScreen
import com.android.settings.supervision.webcontentfilters.SupervisionWebContentFiltersScreen
import com.android.settings.supervision.webcontentfilters.SupervisionWebContentFiltersSearchSupportedAppsScreen
import com.android.settings.system.ResetDashboardScreen
import com.android.settings.system.SystemDashboardScreen
import com.android.settings.tts.TextToSpeechApiScreen
import com.android.settings.users.UserDetailsSettingsScreenApi
import com.android.settings.users.UserSettingsScreenApi
import com.android.settings.vpn2.VpnSettingsScreen
import com.android.settings.wfd.WifiDisplayScreen
import com.android.settings.wifi.ConfigureWifiApiScreen
import com.android.settings.wifi.ConfigureWifiScreen
import com.android.settings.wifi.WifiAppDataUsageScreenApi
import com.android.settings.wifi.WifiDataUsageScreenApi
import com.android.settings.wifi.calling.WifiCallingScreen
import com.android.settings.wifi.details.WifiDetailsScreenApi
import com.android.settings.wifi.details.WifiNetworkDetailsScreen
import com.android.settings.wifi.details2.WifiPrivacyScreenApi
import com.android.settings.wifi.p2p.WifiDirectApiScreen
import com.android.settings.wifi.savedaccesspoints2.SavedAccessPointsWifiScreen
import com.android.settings.wifi.tether.WifiHotspotScreen
import com.android.settings.wifi.tether.WifiHotspotSecurityApiScreen
import com.android.settings.wifi.tether.WifiHotspotSpeedApiScreen

/**
 * Configuration of a screen converting to device states.
 *
 * @param enabled whether expose device states on this screen to App Functions.
 * @param screenKey the unique ID for the screen.
 * @param appFunctionTypes the app function associated with this screen. The default is
 *   UNCATEGORIZED.
 */
data class PerScreenCatalystConfig(
    val enabled: Boolean,
    val screenKey: String,
    // TODO(b/405344827): map categories to PreferenceMetadata#tags
    val appFunctionTypes: Set<DeviceStateAppFunctionType> =
        setOf(DeviceStateAppFunctionType.GET_UNCATEGORIZED),
)

/**
 * Configuration of the device state app functions.
 *
 * @param screenConfigs a list of catalyst screen configurations
 */
data class CatalystConfig(val screenConfigs: List<PerScreenCatalystConfig>)

fun getSettingsCatalystConfig() = CatalystConfig(screenConfigs = getCatalystScreenConfigs())

private fun getCatalystScreenConfigs() =
    listOf(
        PerScreenCatalystConfig(enabled = true, screenKey = NumberingSystemLocaleListApiFirstScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = WifiNetworkDetailsScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = DarkModeScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = DarkModeApiFirstScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = ColorAndMotionScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = ColorInversionScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = ColorCorrectionApiFirstScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = ColorCorrectionScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = ExtraDimScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = AdaptiveConnectivityScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = AdaptiveConnectivityApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = AutoBrightnessScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = AutoBrightnessApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = HdrBrightnessApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = DeviceStateAutoRotateApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = SmartAutoRotateApiScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = BatterySaverScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_BATTERY),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = BluetoothDashboardScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = LockScreenPreferenceScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = DisplayScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = FirmwareVersionScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = NetworkProviderScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_MOBILE_DATA),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = LanguageAndRegionScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = LanguageAndRegionApiFirstScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = RegionPickerApiFirstScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = ModuleLicensesScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = LegalSettingsScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = LocationServicesScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = LocationScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = RecentLocationAccessScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = MobileNetworkListScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_MOBILE_DATA),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = ApnSettingsScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_MOBILE_DATA),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = ConfigureWifiApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = DateTimeSettingsApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = TetherApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = ContentProtectionScreenApi.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = SystemLocalePickerApiFirstScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = FirstDayOfWeekApiFirstScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = MeasurementSystemApiFirstScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = TemperatureUnitApiFirstScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = TermsOfAddressApiFirstScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = MyDeviceInfoApiFirstScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = MyDeviceInfoScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = BatteryInfoApiScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_BATTERY),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = DataSaverScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = NetworkDashboardScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_MOBILE_DATA),
        ),
        PerScreenCatalystConfig(
            enabled = ConnectivityFlags.syncAirplaneModeWithWatches(),
            screenKey = AirplaneModeSettingsScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_MOBILE_DATA),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = PowerUsageSummaryScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_BATTERY),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = PowerUsageSummaryApiScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_BATTERY),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = PowerUsageAdvancedScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_BATTERY),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = PowerUsageAdvancedApiScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_BATTERY),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = AppBatteryUsageListApiScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_BATTERY),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = PowerBackgroundUsageDetailScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_BATTERY),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = AdvancedPowerUsageDetailScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_BATTERY),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = ScreenTimeoutScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = SoundScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_NOTIFICATIONS),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = SoundApiScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_NOTIFICATIONS),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = SpatialAudioApiScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_NOTIFICATIONS),
        ),
        PerScreenCatalystConfig(enabled = false, screenKey = SupervisionPinManagementScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = SupervisionWebContentFiltersSearchSupportedAppsScreen.KEY,
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = SupervisionWebContentFiltersBrowserSupportedAppsScreen.KEY,
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = SupervisionAppStoreFiltersScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = WriteSystemPreferencesAppDetailScreen.KEY,
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = WifiControlAppDetailScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = PictureInPictureAppDetailScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = ManageWriteSettingsAppDetailScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = InstallUnknownAppsAppDetailScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = FullScreenNotificationsAppDetailScreen.KEY,
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = DisplayOverOtherAppsAppDetailScreen.KEY,
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = AllFilesAccessAppDetailScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = AlarmsAndRemindersAppDetailScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = TetherScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_MOBILE_DATA),
        ),
        PerScreenCatalystConfig(enabled = false, screenKey = SupervisionDashboardScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = VibrationIntensityScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = VibrationIntensityScreenApi.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = VibrationScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = VibrationScreenApi.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = AccountsAndBackupScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = AppStorageAppListScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_STORAGE),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = AllAppsScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_APPS),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = StoragePreferenceScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_STORAGE),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = ScreensaverScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = DreamSettingsApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = SystemNavigationGestureScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = A11yActivityScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = A11yServiceScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = AmbientDisplayAlwaysOnPreferenceScreen.KEY,
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = DataUsageAppDetailScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_MOBILE_DATA),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = AppDataUsageScreenApi.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_MOBILE_DATA),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = InstallUnknownAppsAppListScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = InstallUnknownAppsAppDetailScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = ManageWriteSettingsAppListScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = ManageWriteSettingsAppDetailScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = AppLaunchApiScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_APPS),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = ColorModeScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = ColorContrastApiScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = ConfigureWifiScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_MOBILE_DATA),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = AdvancedConnectedDeviceScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = AdvancedConnectedDeviceApiScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = ConversationListScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_NOTIFICATIONS),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = DateTimeSettingsScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = DeviceAdminApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = TrustAgentApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = HardwareInfoScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = AppInfoStorageScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_STORAGE),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = FlashNotificationsScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_NOTIFICATIONS),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = DoubleTapPowerApiFirstScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = DoubleTapPowerScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = HearingDevicesScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = PairHearingDeviceScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = AppInfoScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = MagnificationScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = ActionTimeoutSettingsScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = AudioAdjustmentScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = AutoclickScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = ButtonShortcutSettingScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = SpellCheckerApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = CaptioningPropertiesScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = CaptioningAppearanceScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = CaptioningMoreOptionsScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = EditShortcutsScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = ShortcutsSettingsScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = SystemControlsScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = TimingControlsScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = MediaControlsScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = MobileNetworkScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_MOBILE_DATA),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = MobileNetworkScreenApi.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_MOBILE_DATA),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = NfcAndPaymentScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = NfcAndPaymentApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = NightDisplayScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = NightDisplayApiScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = BubbleNotificationScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_NOTIFICATIONS),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = PoliteNotificationsApiScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_NOTIFICATIONS),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = ResetDashboardScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = DisplayOverOtherAppsAppListScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = DisplayOverOtherAppsAppDetailScreen.KEY,
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = FullScreenNotificationsAppListScreen.KEY,
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = FullScreenNotificationsAppDetailScreen.KEY,
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = InteractAcrossProfilesAppListScreen.KEY,
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = InteractAcrossProfilesAppDetailScreen.KEY,
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = PictureInPictureAppListScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = PictureInPictureAppDetailScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = WifiControlAppListScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = WifiControlAppDetailScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = WriteSystemPreferencesAppListScreen.KEY,
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = AllFilesAccessAppListScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = AllFilesAccessAppDetailScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = AlarmsAndRemindersAppListScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = AlarmsAndRemindersAppDetailScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = SpecialAccessSettingsScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = MediaManagementAppsApiScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_APPS),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = MediaRoutingControlApiScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_APPS),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = LongBackgroundTasksAppsApiScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_APPS),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = NfcTagAppsSettingsApiScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_APPS),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = TurnScreenOnAppsApiScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_APPS),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = UsageDataAppListApiScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_APPS),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = ZenAccessDetailsApiScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_APPS),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = SupervisionWebContentFiltersScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = TextReadingScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = AccessibilityScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = AccountScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = AccountDetailApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = ManagedProfileApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = AppDashboardScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = ConnectedDeviceDashboardScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = EmergencyDashboardScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = AppNotificationSettingsApiScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_NOTIFICATIONS),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = ZenModesListScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_NOTIFICATIONS),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = ZenModeApiScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_NOTIFICATIONS),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = SystemDashboardScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = UserDictionaryListApiScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = VpnSettingsScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_MOBILE_DATA),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = WifiCallingScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_MOBILE_DATA),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = WifiHotspotScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_MOBILE_DATA),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = WifiDisplayScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = AppsNotificationAccessScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_NOTIFICATIONS),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = ManageAccountsScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = AppInfoNotificationAccessScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_NOTIFICATIONS),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = PreviouslyConnectedDeviceScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = AppInfoNotificationAccessScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = ButtonNavigationSettingsScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = SavedAccessPointsWifiScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_MOBILE_DATA),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = DataUsageListScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_MOBILE_DATA),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = BillingCycleScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_MOBILE_DATA),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = GestureSettingsApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = DoubleTwistGestureApiFirstScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = SwipeToNotificationApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = PickupGestureApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = TapScreenGestureApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = DoubleTapApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = WifiDirectApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = ContactsStorageApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = PowerMenuSettingsScreenApi.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = WifiHotspotSecurityApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = WifiHotspotSpeedApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = UserSettingsScreenApi.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = UserDetailsSettingsScreenApi.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = PrintSettingsApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = PrintServiceApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = UsbDetailsApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = UnrestrictedDataAccessApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = SafetyCenterApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = AccountSecuritySubpageScreenApi.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = AppSecurityScreenApi.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = SystemAndUpdatesScreenApi.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = MoreSecurityPrivacyScreenApi.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = PrivacyControlsScreenApi.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = TrustedCredentialsScreenApi.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = UserCredentialsScreenApi.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = UserAspectRatioAppsApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = UserAspectRatioAppApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = CredentialManagementAppScreenApi.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = EncryptionAndCredentialScreenApi.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = InstallCertificateFromStorageScreenApi.KEY,
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = IccLockApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = BluetoothScanningApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = DateTimeSettingsScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = DeviceUnlockApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = AvailableVirtualKeyboardApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = WifiDataUsageScreenApi.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = WifiAppDataUsageScreenApi.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = OpeningLinksApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = WifiDetailsScreenApi.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = HardwareInfoApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = TabbedDisplayApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = DisplayApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = ColorModeApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = ResolutionRefreshRateApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = ScreenResolutionApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = OneHandedApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = WifiScanningApiScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = AppStorageSettingsScreenApi.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_APPS),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = StylusUsiDetailsApiScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = BluetoothDashboardScreenApi.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = CellularSecurityScreenApi.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = LocationSettingsScreenApi.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = LocationServicesScreenApi.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = TextToSpeechApiScreen.KEY),
    )
