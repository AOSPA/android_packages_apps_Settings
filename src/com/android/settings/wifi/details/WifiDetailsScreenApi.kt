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
import com.android.settings.wifi.details.WifiNetworkDetailsFragment.KEY_CHOSEN_WIFIENTRY_KEY
import com.android.settings.wifi.repository.SavedNetworkInfo
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category

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

    init {
        flag { Flags.catalystMigration26q2() }

        parameters {
            parameter(
                name = PARAMETER_KEY,
                purpose = R.string.wifi_details_parameter_purpose,
                type = SavedNetwork
            )

            prepareScreenExtras { keyParameters, extras ->
                val savedNetwork = keyParameters.getTyped<SavedNetworkInfo>(PARAMETER_KEY) ?: return@prepareScreenExtras
                extras.putString(KEY_CHOSEN_WIFIENTRY_KEY, savedNetwork.key)
            }
        }
    }

    companion object {
        private const val TAG = "WifiDetailsScreenApi"
        const val KEY = "wifi_details_settings"
        const val PARAMETER_KEY = "PARAMETER_KEY"
    }
}
