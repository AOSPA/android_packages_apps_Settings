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
package com.android.settings.network

import android.provider.Settings
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean

@ProvidePreferenceScreen(AdaptiveConnectivityApiScreen.KEY)
class AdaptiveConnectivityApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.NETWORK,
        fragment = AdaptiveConnectivitySettings::class,
        purpose = R.string.adaptive_connectivity_purpose,
        alreadyPartiallyMigrated = AdaptiveConnectivityScreen::class,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }
        preference(
            key = PREF_KEY_WIFI,
            purpose = R.string.adaptive_connectivity_wifi_enabled_purpose,
            type = AnyBoolean,
        ) {
            get {
                execute { Settings.Secure.getInt(context.contentResolver, PREF_KEY_WIFI, 1) == 1 }
            }
            set {
                execute { value ->
                    Settings.Secure.putInt(
                        context.contentResolver,
                        PREF_KEY_WIFI,
                        if (value) 1 else 0,
                    )
                }
            }
        }

        preference(
            key = PREF_KEY_MOBILE,
            purpose = R.string.adaptive_connectivity_mobile_network_enabled_purpose,
            type = AnyBoolean,
        ) {
            get {
                execute { Settings.Secure.getInt(context.contentResolver, PREF_KEY_MOBILE, 1) == 1 }
            }
            set {
                execute { value ->
                    Settings.Secure.putInt(
                        context.contentResolver,
                        PREF_KEY_MOBILE,
                        if (value) 1 else 0,
                    )
                }
            }
        }
    }

    companion object {
        const val KEY = "api_adaptive_connectivity"
        const val PREF_KEY_WIFI = "adaptive_connectivity_wifi_enabled"
        const val PREF_KEY_MOBILE = "adaptive_connectivity_mobile_network_enabled"
    }
}
