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

package com.android.settings.applications.specialaccess

import android.Manifest.permission.CHANGE_WIFI_STATE
import android.Manifest.permission.NETWORK_SETTINGS
import android.app.AppOpsManager
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.UserHandle
import com.android.settings.R
import com.android.settings.applications.CatalystAppListFragment.Companion.DEFAULT_SHOW_SYSTEM
import com.android.settingslib.metadata.ProvidePreferenceScreen
import kotlin.collections.indexOf

/**
 * The app detail catalyst screen for "Wi-Fi control" special app access.
 *
 * This screen is accessible from: Settings > Apps > Special app access > Wi-Fi control > [app name]
 */
@ProvidePreferenceScreen(WifiControlAppDetailScreen.KEY, parameterized = true)
open class WifiControlAppDetailScreen(context: Context, arguments: Bundle) :
    SpecialAccessAppDetailScreen(context, arguments) {

    override val key
        get() = KEY

    override val bindingKey
        get() = "$KEY-$packageName"

    override val screenTitle
        get() = R.string.change_wifi_state_title

    override val op
        get() = AppOpsManager.OP_CHANGE_WIFI_STATE

    override val switchPreferenceTitle
        get() = R.string.change_wifi_state_app_detail_switch

    override val footerPreferenceTitle
        get() = R.string.change_wifi_state_app_detail_summary

    // Edge case: what if the app's change wifi state permission is revoked/granted
    override fun isAvailable(context: Context) =
        super.isAvailable(context) &&
            hasChangeWifiStateControlPermission(context, packageInfo?.applicationInfo)

    override fun getMetricsCategory() = SettingsEnums.CONFIGURE_WIFI

    override fun getAccessChangeActionMetrics(allowed: Boolean): Int =
        when (allowed) {
            true -> SettingsEnums.APP_SPECIAL_PERMISSION_SETTINGS_CHANGE_ALLOW
            else -> SettingsEnums.APP_SPECIAL_PERMISSION_SETTINGS_CHANGE_DENY
        }

    companion object {
        const val KEY = "sa_wfc_app_detail"

        @JvmStatic fun parameters(context: Context) = parameters(context, DEFAULT_SHOW_SYSTEM)

        fun parameters(context: Context, showSystemApp: Boolean) =
            parameters(context, showSystemApp, ::hasChangeWifiStateControlPermission)

        private fun hasChangeWifiStateControlPermission(
            context: Context,
            appInfo: ApplicationInfo?,
        ): Boolean {
            if (appInfo == null) return false
            val packageInfo =
                try {
                    context.packageManager.getPackageInfoAsUser(
                        appInfo.packageName,
                        PackageManager.GET_PERMISSIONS,
                        UserHandle.myUserId(),
                    )
                } catch (_: Exception) {
                    return false
                }

            // NETWORK_SETTINGS permission trumps CHANGE_WIFI_CONFIG.
            if (isPermissionGranted(packageInfo, NETWORK_SETTINGS)) {
                return false
            }

            return isPermissionGranted(packageInfo, CHANGE_WIFI_STATE)
        }

        private fun isPermissionGranted(packageInfo: PackageInfo?, permission: String): Boolean {
            val index = packageInfo?.requestedPermissions?.indexOf(permission) ?: -1
            val flags = if (index >= 0) packageInfo?.requestedPermissionsFlags!![index] else 0

            return (flags and REQUESTED_PERMISSION_GRANTED) != 0
        }
    }
}
