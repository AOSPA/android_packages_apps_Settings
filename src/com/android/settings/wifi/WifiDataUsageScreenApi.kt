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

package com.android.settings.wifi

import android.util.Log
import com.android.settings.R
import com.android.settings.datausage.DataUsageList
import com.android.settings.flags.Flags
import com.android.settings.overlay.FeatureFactory
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Disallowed

@ProvidePreferenceScreen(WifiDataUsageScreenApi.KEY)
class WifiDataUsageScreenApi :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.NETWORK,
        fragment = DataUsageList::class,
        purpose = R.string.non_carrier_data_usage_screen_purpose,
    ) {
    private val repository =
        FeatureFactory.featureFactory.wifiFeatureProvider.wifiDataUsageRepository

    init {
        flag { Flags.catalystMigration26q2() }

        preconditions(R.string.wifi_data_usage_screen_preconditions) {
            if (repository.isAvailable) Allowed
            else {
                Log.w(TAG, "Wifi Data Usage Screen is unavailable!")
                Disallowed(R.string.wifi_data_usage_screen_unavailable)
            }
        }

        // TODO(b/474034849) CatalystApi: migrate the preferences
    }

    companion object {
        private const val TAG = "WifiDataUsageScreenApi"
        const val KEY = "non_carrier_data_usage_settings"
    }
}
