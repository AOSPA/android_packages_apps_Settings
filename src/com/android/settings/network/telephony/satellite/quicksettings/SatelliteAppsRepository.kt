/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.network.telephony.satellite.quicksettings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.DeviceConfig
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.telephony.satellite.SatelliteManager
import android.util.Log
import com.android.settings.R

/** A repository for getting the list of satellite apps for the landing page. */
open class SatelliteAppsRepository(private val context: Context) {
    companion object {
        const val PACKAGE_NAME_SETTINGS = "com.android.settings"
        const val PACKAGE_NAME_SAFETY_HUB = "com.google.android.apps.safetyhub"
        private const val TAG = "SatelliteAppsRepository"
        private const val EXTRA_SHOW_FRAGMENT_AS_SUBSETTING =
            ":settings:show_fragment_as_subsetting"
        private const val EXTRA_SUB_ID = "sub_id"
        private const val OVERRIDE_SATELLITE_APPS_FOR_LTE_LANDING_PAGE_WITH_CONFIG_KEY =
            "override_satellite_apps_for_lte_landing_page_with_config"
        private const val DEFAULT_OVERRIDE_SATELLITE_APPS_FOR_LTE_LANDING_PAGE_WITH_CONFIG_VALUE =
            false
    }

    /** Returns the intent for the Emergency SOS app. */
    open fun getEmergencySosIntent(): Intent? {
        // TODO(434793872): Add a config for the emergency number. Note that the emergency number
        // may be different in different countries.
        val sosIntent = Intent(Intent.ACTION_DIAL).setData(Uri.parse("tel:911"))
        if (sosIntent.resolveActivity(context.packageManager) == null) {
            Log.d(TAG, "Intent for Emergency SOS cannot be resolved.")
            return null
        }
        return sosIntent
    }

    /** Returns the intent for the Settings app for satellite settings. */
    open fun getSettingsIntent(): Intent? {
        val settingsIntent = Intent(Settings.ACTION_SATELLITE_SETTING)
        settingsIntent.putExtra(EXTRA_SHOW_FRAGMENT_AS_SUBSETTING, true)
        settingsIntent.putExtra(EXTRA_SUB_ID, SubscriptionManager.getActiveDataSubscriptionId())
        if (settingsIntent.resolveActivity(context.packageManager) == null) {
            Log.d(TAG, "Intent for Settings cannot be resolved.")
            return null
        }
        return settingsIntent
    }

    /** Returns the list of satellite app package names for the NB-NTN landing page. */
    open fun getAppsPackagesForNbNtnLandingPage(): List<String> {
        val packages =
            context.resources.getStringArray(R.array.config_satellite_apps_for_nbntn_landing_page)
        return packages.filter { it.isNotEmpty() && isPackageInstalled(it) }
    }

    /** Returns the list of satellite app package names for the LTE-based landing page. */
    open fun getAppsPackagesForLteLandingPage(): List<String> {
        val overrideSatelliteAppsForLteLandingPageWithConfig =
            DeviceConfig.getBoolean(
                DeviceConfig.NAMESPACE_TELEPHONY,
                OVERRIDE_SATELLITE_APPS_FOR_LTE_LANDING_PAGE_WITH_CONFIG_KEY,
                DEFAULT_OVERRIDE_SATELLITE_APPS_FOR_LTE_LANDING_PAGE_WITH_CONFIG_VALUE,
            )
        Log.d(
            TAG,
            "getAppsPackagesForLteLandingPage: overrideSatelliteAppsForLteLandingPageWithConfig=$overrideSatelliteAppsForLteLandingPageWithConfig",
        )

        val packages =
            if (overrideSatelliteAppsForLteLandingPageWithConfig) {
                context.resources
                    .getStringArray(R.array.config_satellite_apps_for_lte_landing_page)
                    .toList()
            } else {
                // By default, use the satellite data optimized apps from SatelliteManager.
                getSatelliteDataOptimizedApps(context)
            }
        return packages.filter { it.isNotEmpty() && isPackageInstalled(it) }
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d(TAG, "$packageName is not installed.")
            false
        }
    }

    private fun getSatelliteDataOptimizedApps(context: Context): List<String> {
        val satelliteManager = context.getSystemService(SatelliteManager::class.java)
        if (satelliteManager == null) {
            return emptyList()
        }
        try {
            return satelliteManager.getSatelliteDataOptimizedApps()
        } catch (e: IllegalStateException) {
            Log.d(TAG, "getSatelliteDataOptimizedApps failed due to $e")
        }
        return emptyList()
    }
}
