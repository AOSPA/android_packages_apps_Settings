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

package com.android.settings.wifi.details

import android.util.Log
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settings.wifi.details.WifiNetworkDetailsFragment.KEY_CHOSEN_WIFIENTRY_KEY
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.types.GeneratedParameterType
import com.android.settingslib.metadata.preferencesapi.types.GeneratedValue

/**
 * The [PreferencesApiScreen] for the Wifi Details screen.
 *
 * This screen allows users to view and manage the details of a saved Wi-Fi network.
 */
@ProvidePreferenceScreen(WifiDetailsScreenApi.KEY, parameterized = true)
class WifiDetailsScreenApi :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.NETWORK,
        fragment = WifiNetworkDetailsFragment::class,
        purpose = R.string.wifi_details_settings_purpose,
    ) {
    private val repository = featureFactory.wifiFeatureProvider.savedNetworkRepository

    init {
        flag { Flags.catalystMigration26q2() }

        parameters {
            parameter(
                name = PARAMETER_KEY,
                purpose = R.string.wifi_details_parameter_purpose,
                type =
                    GeneratedParameterType(R.string.wifi_details_parameter_description) {
                        repository.fetchSavedNetworksInfo().map {
                            GeneratedValue(it.lookupKey, it.ssid)
                        }
                    },
            )

            prepareScreenExtras { keyParameters, extras ->
                val lookupKey = keyParameters[PARAMETER_KEY] ?: return@prepareScreenExtras
                repository.findSavedNetworkInfo(lookupKey)?.let { info ->
                    extras.putString(KEY_CHOSEN_WIFIENTRY_KEY, info.key)
                } ?: Log.e(TAG, "Saved network info not found!")
            }
        }
    }

    companion object {
        private const val TAG = "WifiDetailsScreenApi"
        const val KEY = "wifi_details_settings"
        const val PARAMETER_KEY = "PARAMETER_KEY"
    }
}
