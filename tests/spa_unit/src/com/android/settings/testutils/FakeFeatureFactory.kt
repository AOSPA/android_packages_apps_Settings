/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.testutils

import android.content.Context
import com.android.settings.accessibility.AccessibilityFeedbackFeatureProvider
import com.android.settings.accessibility.AccessibilityPageIdFeatureProvider
import com.android.settings.accessibility.AccessibilitySearchFeatureProvider
import com.android.settings.accounts.AccountFeatureProvider
import com.android.settings.applications.ApplicationFeatureProvider
import com.android.settings.biometrics.BiometricsFeatureProvider
import com.android.settings.biometrics.face.FaceFeatureProvider
import com.android.settings.biometrics.fingerprint.FingerprintFeatureProvider
import com.android.settings.bluetooth.BluetoothFeatureProvider
import com.android.settings.connecteddevice.audiosharing.AudioSharingFeatureProvider
import com.android.settings.connecteddevice.fastpair.FastPairFeatureProvider
import com.android.settings.connecteddevice.stylus.StylusFeatureProvider
import com.android.settings.connecteddevice.threadnetwork.ThreadNetworkFeatureProvider
import com.android.settings.connecteddevice.usb.UsbFeatureProvider
import com.android.settings.dashboard.DashboardFeatureProvider
import com.android.settings.dashboard.suggestions.SuggestionFeatureProvider
import com.android.settings.deviceinfo.hardwareinfo.HardwareInfoFeatureProvider
import com.android.settings.display.DisplayFeatureProvider
import com.android.settings.enterprise.EnterprisePrivacyFeatureProvider
import com.android.settings.fuelgauge.BatterySettingsFeatureProvider
import com.android.settings.fuelgauge.BatteryStatusFeatureProvider
import com.android.settings.fuelgauge.PowerUsageFeatureProvider
import com.android.settings.homepage.contextualcards.ContextualCardFeatureProvider
import com.android.settings.inputmethod.KeyboardSettingsFeatureProvider
import com.android.settings.localepicker.LocaleFeatureProvider
import com.android.settings.network.telephony.TelephonyFeatureProvider
import com.android.settings.notification.syncacrossdevices.SyncAcrossDevicesFeatureProvider
import com.android.settings.overlay.DockUpdaterFeatureProvider
import com.android.settings.overlay.FeatureFactory
import com.android.settings.panel.PanelFeatureProvider
import com.android.settings.privatespace.PrivateSpaceLoginFeatureProvider
import com.android.settings.search.SearchFeatureProvider
import com.android.settings.security.SecurityFeatureProvider
import com.android.settings.security.SecuritySettingsFeatureProvider
import com.android.settings.slices.SlicesFeatureProvider
import com.android.settings.users.UserFeatureProvider
import com.android.settings.vpn2.AdvancedVpnFeatureProvider
import com.android.settings.wifi.WifiTrackerLibProvider
import com.android.settings.wifi.factory.WifiFeatureProvider
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider
import org.mockito.kotlin.mock

class FakeFeatureFactory : FeatureFactory() {

    val mockApplicationFeatureProvider = mock<ApplicationFeatureProvider>()

    init {
        setFactory(appContext, this)
    }

    override val suggestionFeatureProvider: SuggestionFeatureProvider =
        mock<SuggestionFeatureProvider>()

    override val hardwareInfoFeatureProvider: HardwareInfoFeatureProvider =
        mock<HardwareInfoFeatureProvider>()

    override val metricsFeatureProvider = mock<MetricsFeatureProvider>()

    override val powerUsageFeatureProvider = mock<PowerUsageFeatureProvider>()

    override val batteryStatusFeatureProvider: BatteryStatusFeatureProvider =
        mock<BatteryStatusFeatureProvider>()

    override val batterySettingsFeatureProvider: BatterySettingsFeatureProvider =
        mock<BatterySettingsFeatureProvider>()

    override val dashboardFeatureProvider: DashboardFeatureProvider =
        mock<DashboardFeatureProvider>()

    override val dockUpdaterFeatureProvider: DockUpdaterFeatureProvider =
        mock<DockUpdaterFeatureProvider>()

    override val applicationFeatureProvider = mockApplicationFeatureProvider

    override val localeFeatureProvider: LocaleFeatureProvider = mock<LocaleFeatureProvider>()

    override val enterprisePrivacyFeatureProvider: EnterprisePrivacyFeatureProvider =
        mock<EnterprisePrivacyFeatureProvider>()

    override val searchFeatureProvider: SearchFeatureProvider = mock<SearchFeatureProvider>()

    override fun getSurveyFeatureProvider(context: Context) = null

    override val securityFeatureProvider: SecurityFeatureProvider = mock<SecurityFeatureProvider>()

    override val userFeatureProvider: UserFeatureProvider = mock<UserFeatureProvider>()

    override val slicesFeatureProvider: SlicesFeatureProvider = mock<SlicesFeatureProvider>()

    override val accountFeatureProvider: AccountFeatureProvider = mock<AccountFeatureProvider>()

    override val panelFeatureProvider: PanelFeatureProvider = mock<PanelFeatureProvider>()

    override fun getContextualCardFeatureProvider(context: Context): ContextualCardFeatureProvider {
        return mock<ContextualCardFeatureProvider>()
    }

    override val bluetoothFeatureProvider: BluetoothFeatureProvider =
        mock<BluetoothFeatureProvider>()

    override val biometricsFeatureProvider: BiometricsFeatureProvider =
        mock<BiometricsFeatureProvider>()

    override val faceFeatureProvider: FaceFeatureProvider = mock<FaceFeatureProvider>()

    override val fingerprintFeatureProvider: FingerprintFeatureProvider =
        mock<FingerprintFeatureProvider>()

    override val wifiTrackerLibProvider: WifiTrackerLibProvider = mock<WifiTrackerLibProvider>()

    override val securitySettingsFeatureProvider: SecuritySettingsFeatureProvider =
        mock<SecuritySettingsFeatureProvider>()

    override val accessibilityFeedbackFeatureProvider: AccessibilityFeedbackFeatureProvider =
        mock<AccessibilityFeedbackFeatureProvider>()

    override val accessibilitySearchFeatureProvider: AccessibilitySearchFeatureProvider =
        mock<AccessibilitySearchFeatureProvider>()

    override val accessibilityPageIdFeatureProvider: AccessibilityPageIdFeatureProvider =
        mock<AccessibilityPageIdFeatureProvider>()

    override val advancedVpnFeatureProvider: AdvancedVpnFeatureProvider =
        mock<AdvancedVpnFeatureProvider>()

    override val wifiFeatureProvider: WifiFeatureProvider = mock<WifiFeatureProvider>()

    override val keyboardSettingsFeatureProvider: KeyboardSettingsFeatureProvider =
        mock<KeyboardSettingsFeatureProvider>()

    override val stylusFeatureProvider: StylusFeatureProvider = mock<StylusFeatureProvider>()

    override val threadNetworkFeatureProvider: ThreadNetworkFeatureProvider =
        mock<ThreadNetworkFeatureProvider>()

    override val fastPairFeatureProvider: FastPairFeatureProvider = mock<FastPairFeatureProvider>()

    override val privateSpaceLoginFeatureProvider: PrivateSpaceLoginFeatureProvider =
        mock<PrivateSpaceLoginFeatureProvider>()

    override val displayFeatureProvider: DisplayFeatureProvider = mock<DisplayFeatureProvider>()

    override val syncAcrossDevicesFeatureProvider: SyncAcrossDevicesFeatureProvider =
        mock<SyncAcrossDevicesFeatureProvider>()

    override val audioSharingFeatureProvider: AudioSharingFeatureProvider =
        mock<AudioSharingFeatureProvider>()

    override val usbFeatureProvider: UsbFeatureProvider = mock<UsbFeatureProvider>()

    override val telephonyFeatureProvider: TelephonyFeatureProvider =
        mock<TelephonyFeatureProvider>()
}
