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

import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.types.GeneratedParameterType
import com.android.settingslib.metadata.preferencesapi.types.GeneratedValue

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

    init {
        flag { Flags.catalystMigration26q2() }

        parameters {
            parameter(
                name = PARAMETER_KEY,
                purpose = R.string.wifi_privacy_parameter_purpose,
                type =
                    GeneratedParameterType(R.string.wifi_privacy_parameter_description) {
                        repository.fetchSavedNetworksInfo().map {
                            GeneratedValue(it.lookupKey, it.ssid)
                        }
                    },
            )

            prepareSpaRoute { keyParameters ->
                val lookupKey = keyParameters[PARAMETER_KEY] ?: return@prepareSpaRoute ""
                repository.findSavedNetworkInfo(lookupKey)?.let { info ->
                    WifiPrivacyPageProvider.getRoute(info.key)
                } ?: ""
            }
        }
    }

    companion object {
        private const val TAG = "WifiPrivacyScreenApi"
        const val KEY = "wifi_privacy_screen"
        const val PARAMETER_KEY = "PARAMETER_KEY"
    }
}
