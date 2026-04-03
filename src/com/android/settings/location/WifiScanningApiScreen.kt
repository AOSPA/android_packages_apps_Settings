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

package com.android.settings.location

import android.Manifest.permission.NETWORK_SETTINGS
import android.net.wifi.WifiManager
import android.os.UserManager
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.EnterpriseRestriction
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean

// LINT.IfChange
@ProvidePreferenceScreen(WifiScanningApiScreen.KEY)
open class WifiScanningApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.LOCATION,
        fragment = WifiScanningFragment::class,
        purpose = R.string.wifi_scanning_screen_purpose,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }

        preference(
            key = "wifi_scanning",
            purpose = R.string.wifi_scanning_purpose,
            type = AnyBoolean,
        ) {
            preconditions(R.string.wifi_scanning_hardware_unsupported) {
                if (context.getResources().getBoolean(R.bool.config_show_location_scanning)) {
                    Allowed
                } else {
                    HardwareUnsupported(R.string.wifi_scanning_hardware_unsupported)
                }
            }
            get {
                execute {
                    // Deprecated for apps, but currently used by
                    // WifiScanningMainSwitchPreferenceController.
                    // Retaining for parity with existing Settings logic during Catalyst migration.
                    @Suppress("DEPRECATION")
                    context.getSystemService(WifiManager::class.java)?.isScanAlwaysAvailable == true
                }
            }
            set {
                permissions(NETWORK_SETTINGS)
                preconditions(R.string.wifi_scanning_user_restricted) {
                    val userManager =
                        context.getSystemService(UserManager::class.java)
                            ?: error("UserManager service not found")

                    if (userManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_LOCATION)) {
                        EnterpriseRestriction(R.string.wifi_scanning_user_restricted)
                    } else {
                        Allowed
                    }
                }
                execute { enabled: Boolean ->
                    val wifiManager =
                        context.getSystemService(WifiManager::class.java)
                            ?: error(
                                "WifiManager service not found, could not set scan always available"
                            )

                    wifiManager.setScanAlwaysAvailable(enabled)
                }
            }
        }
    }

    companion object {
        const val KEY = "wifi_always_scanning_screen"
    }
}
// LINT.ThenChange(WifiScanningFragment.java,
//                 WifiScanningMainSwitchPreferenceController.java)
