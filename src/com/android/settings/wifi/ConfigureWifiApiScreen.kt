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

import android.Manifest.permission.NETWORK_SETTINGS
import android.Manifest.permission.NETWORK_SETUP_WIZARD
import android.net.wifi.WifiManager
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.datastore.Permissions.Companion.anyOf
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean
import com.android.settingslib.utils.ThreadUtils

// LINT.IfChange
@ProvidePreferenceScreen(ConfigureWifiApiScreen.KEY)
class ConfigureWifiApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.NETWORK,
        fragment = ConfigureWifiSettings::class,
        purpose = R.string.configure_network_settings_purpose,
        alreadyPartiallyMigrated = ConfigureWifiScreen::class,
    ) {

    init {
        flag { Flags.catalystMigration26q2() }

        preference(
            key = "wifi_notify_open_networks",
            purpose = R.string.wifi_notify_open_networks_purpose,
            type = AnyBoolean,
        ) {
            get {
                execute {
                    val helper = OpenNetworkNotifierHelper.getInstance(context.applicationContext)
                    if (WifiUtils.isWifiMultiuserEnabled()) {
                        val wifiManager = context.getSystemService(WifiManager::class.java)
                        val bgExecutor = ThreadUtils.getBackgroundExecutor()

                        kotlinx.coroutines.runBlocking {
                            kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                                helper.loadValue(wifiManager, bgExecutor, bgExecutor) {
                                    if (continuation.isActive) {
                                        continuation.resumeWith(Result.success(Unit))
                                    }
                                }
                            }
                        }
                    }

                    helper.isEnabled
                }
            }
            set {
                permissions(anyOf(NETWORK_SETTINGS, NETWORK_SETUP_WIZARD))
                execute { enabled: Boolean ->
                    val wifiManager = context.getSystemService(WifiManager::class.java)
                    val bgExecutor = ThreadUtils.getBackgroundExecutor()

                    OpenNetworkNotifierHelper.getInstance(context.applicationContext)
                        .setEnabled(wifiManager, bgExecutor, enabled)
                }
            }
        }
    }

    companion object {
        const val KEY = "api_configure_network_settings"
    }
}
// LINT.ThenChange(ConfigureWifiSettings.java, ConfigureWifiScreen.kt)
