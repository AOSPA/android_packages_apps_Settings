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

import android.content.Context
import android.content.pm.PackageManager
import android.net.NetworkTemplate
import android.util.Log
import com.android.settings.R
import com.android.settings.applications.AppInfoBase
import com.android.settings.applications.InstalledPackageName
import com.android.settings.datausage.AppDataUsage
import com.android.settings.flags.Flags
import com.android.settings.overlay.FeatureFactory.Companion.appContext
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category

/**
 * The [PreferencesApiScreen] for the Wi-Fi App Data Usage screen.
 *
 * This screen allows users to view and manage Wi-Fi data usage for a specific app.
 */
@ProvidePreferenceScreen(WifiAppDataUsageScreenApi.KEY, parameterized = true)
class WifiAppDataUsageScreenApi :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.APPS,
        fragment = AppDataUsage::class,
        purpose = R.string.non_carrier_app_data_usage_screen_purpose,
    ) {

    init {
        flag { Flags.catalystMigration26q2() }

        parameters {
            parameter(
                name = KEY_APP_PACKAGE_NAME,
                purpose = R.string.app_data_usage_parameter_purpose,
                type = InstalledPackageName,
            )

            prepareScreenExtras { keyParameters, extras ->
                val packageName = keyParameters[KEY_APP_PACKAGE_NAME] ?: return@prepareScreenExtras
                extras.putString(KEY_APP_PACKAGE_NAME, packageName)
                extras.putInt(AppInfoBase.ARG_PACKAGE_UID, appContext.getPackageUid(packageName))
                val template = NetworkTemplate.Builder(NetworkTemplate.MATCH_WIFI).build()
                extras.putParcelable(ARG_NETWORK_TEMPLATE, template)
            }
        }
    }

    companion object {
        private const val TAG = "WifiAppDataUsageScreenApi"
        const val KEY = "wifi_app_data_usage_screen"
        const val KEY_APP_PACKAGE_NAME = "app"
        const val ARG_NETWORK_TEMPLATE = "network_template"

        /**
         * Gets the UID for a given package name.
         *
         * @param packageName The target package name.
         * @return The UID of the package, or -1 if the package is not found.
         */
        fun Context.getPackageUid(packageName: String): Int {
            return try {
                packageManager.getPackageUid(packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e(TAG, "Package not found: $packageName", e)
                -1
            }
        }
    }
}
