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

package com.android.settings.wifi.details2

import android.Manifest.permission
import android.net.wifi.WifiConfiguration
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settings.wifi.repository.SavedNetworkInfo
import com.android.settingslib.datastore.Permissions.Companion.anyOf
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel.Companion.REQUIRES_CONFIRMATION
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean
import com.android.settings.wifi.details.SavedNetwork

/**
 * The [PreferencesApiScreen] for the Wifi Privacy screen.
 *
 * This screen allows users to view and manage the privacy settings of a saved Wi-Fi network.
 */
@ProvidePreferenceScreen(WifiPrivacyScreenApi.KEY, parameterized = true)
class WifiPrivacyScreenApi :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.NETWORK,
        purpose = R.string.wifi_privacy_settings_purpose,
    ) {
    private val repository = featureFactory.wifiFeatureProvider.savedNetworkRepository
    private val network: SavedNetworkInfo?
        get() = keyParameters?.getTyped<SavedNetworkInfo>(PARAMETER_KEY)
    private val wifiConfiguration: WifiConfiguration?
        get() = network?.let { repository.getWifiConfiguration(it.lookupKey) }

    init {
        flag { Flags.catalystMigration26q2() }

        // TODO(b/501330847): Re-add this screen to state provider config
        tags(APP_FUNCTION_NONE)

        parameters {
            parameter(
                name = PARAMETER_KEY,
                purpose = R.string.wifi_privacy_parameter_purpose,
                type = SavedNetwork,
            )

            prepareSpaRoute { keyParameters ->
                val savedNetwork = keyParameters.getTyped<SavedNetworkInfo>(PARAMETER_KEY) ?: return@prepareSpaRoute ""
                WifiPrivacyPageProvider.getRoute(savedNetwork.key)
            }
        }

        preference(SEND_DEVICES_NAME_SWITCH_KEY, SEND_DEVICES_NAME_PURPOSE, AnyBoolean) {
            sensitivityLevel(REQUIRES_CONFIRMATION)

            get {
                permissions(SEND_DEVICES_NAME_READ_PERMISSIONS)
                execute { wifiConfiguration?.let { it.isSendDhcpHostnameEnabled } ?: false }
            }
            set {
                permissions(SEND_DEVICES_NAME_WRITE_PERMISSIONS)
                execute { value ->
                    network?.let {
                        repository.setWifiConfiguration(it.lookupKey) { config ->
                            config.isSendDhcpHostnameEnabled = value
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "WifiPrivacyScreenApi"
        const val KEY = "wifi_privacy_screen"
        const val PARAMETER_KEY = "PARAMETER_KEY"

        const val SEND_DEVICES_NAME_SWITCH_KEY = "wifi_privacy_send_device_name_switch"
        private val SEND_DEVICES_NAME_PURPOSE = R.string.wifi_privacy_send_device_name_purpose
        private val SEND_DEVICES_NAME_READ_PERMISSIONS =
            anyOf(
                permission.ACCESS_FINE_LOCATION,
                permission.NEARBY_WIFI_DEVICES,
                permission.ACCESS_WIFI_STATE,
                permission.READ_WIFI_CREDENTIAL,
            )
        private val SEND_DEVICES_NAME_WRITE_PERMISSIONS =
            anyOf(
                permission.NETWORK_SETTINGS,
                permission.NETWORK_SETUP_WIZARD,
                permission.NETWORK_STACK,
            )
    }
}
