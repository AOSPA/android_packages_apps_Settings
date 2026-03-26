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

import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import android.Manifest.permission.CHANGE_WIFI_STATE
import android.Manifest.permission.NETWORK_SETTINGS
import android.app.AppOpsManager
import android.app.settings.SettingsEnums
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.core.net.toUri
import com.android.settings.R
import com.android.settings.applications.CatalystAppListFragment.Companion.DEFAULT_SHOW_SYSTEM
import com.android.settings.applications.getPackageInfoWithPermissions
import com.android.settings.applications.isPermissionRequested
import com.android.settings.flags.Flags
import com.android.settings.utils.highlightPreference
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.android.settingslib.metadata.KeyParametersSchema
import com.android.settingslib.metadata.ParameterizedPreferenceScreenArgumentsFactory
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.ValidatedKeyParameters

/**
 * The app detail catalyst screen for "Wi-Fi control" special app access.
 *
 * This screen is accessible from: Settings > Apps > Special app access > Wi-Fi control > [app name]
 */
@ProvidePreferenceScreen(WifiControlAppDetailScreen.KEY, parameterized = true)
open class WifiControlAppDetailScreen : SpecialAccessAppDetailScreen {

    @Deprecated(
        "This constructor will be removed once the catalyst framework stops passing the arguments as a bundle. Use the other constructor instead."
    )
    constructor(context: Context, arguments: Bundle) : super(context, arguments)

    constructor(
        context: Context,
        keyArguments: ValidatedKeyParameters,
    ) : super(context, keyArguments)

    override val key
        get() = KEY

    override val keyParametersSchema: KeyParametersSchema
        get() = parametersSchema

    //TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.special_access_wifi_control_app_detail_purpose

    override val bindingKey
        get() = "$KEY-$packageName"

    override val screenTitle
        get() = R.string.change_wifi_state_title

    override val op
        get() = AppOpsManager.OP_CHANGE_WIFI_STATE

    override val permission: String?
        get() = PERMISSION

    override val switchPreferenceTitle
        get() = R.string.change_wifi_state_app_detail_switch

    override val footerPreferenceTitle
        get() = R.string.change_wifi_state_app_detail_summary

    override val availabilityDescription =
        "The app must be enabled, and must have requested change wifi state permission."

    // Edge case: what if the app's change wifi state permission is revoked/granted

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE
    override fun isAvailable(context: Context) =
        super.isAvailable(context) && wifiStateControlFilter(context, packageInfo?.applicationInfo)

    override fun getMetricsCategory() = SettingsEnums.CONFIGURE_WIFI

    override fun isFlagEnabled(context: Context) = Flags.deeplinkApps25q4()

    override fun getAccessChangeActionMetrics(allowed: Boolean): Int =
        when (allowed) {
            true -> SettingsEnums.APP_SPECIAL_PERMISSION_SETTINGS_CHANGE_ALLOW
            else -> SettingsEnums.APP_SPECIAL_PERMISSION_SETTINGS_CHANGE_DENY
        }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        Intent().apply {
            component =
                ComponentName(
                    "com.android.settings",
                    "com.android.settings.WifiControlAppDetailIntent",
                )
            data = "package:$packageName".toUri()
            if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
                highlightPreference(keyParameters!!, metadata?.bindingKey)
            } else {
                highlightPreference(arguments!!, metadata?.bindingKey)
            }
        }

    companion object :
        ParameterizedPreferenceScreenArgumentsFactory by SpecialAccessAppDetailScreen.Companion {
        const val KEY = "special_access_wifi_control_app_detail"
        const val BROADER_PERMISSION = NETWORK_SETTINGS
        const val PERMISSION = CHANGE_WIFI_STATE

        @JvmStatic
        override fun keyParameters(context: Context) = keyParameters(context, DEFAULT_SHOW_SYSTEM)

        fun keyParameters(context: Context, showSystemApp: Boolean) =
            keyParameters(context, showSystemApp, ::wifiStateControlFilter)

        @JvmStatic
        @Deprecated(
            "This method will be removed once the catalyst framework stops passing the arguments as a bundle. Use keyParameters instead."
        )
        fun parameters(context: Context) = parameters(context, DEFAULT_SHOW_SYSTEM)

        @Deprecated(
            "This method will be removed once the catalyst framework stops passing the arguments as a bundle. Use keyParameters instead."
        )
        fun parameters(context: Context, showSystemApp: Boolean) =
            parameters(context, showSystemApp, ::wifiStateControlFilter)

        private fun wifiStateControlFilter(context: Context, appInfo: ApplicationInfo?): Boolean {
            if (appInfo == null) return false
            val packageInfo =
                context.getPackageInfoWithPermissions(appInfo.packageName) ?: return false

            val isChangeable =
                isPermissionRequested(packageInfo, PERMISSION) &&
                    !isPermissionRequested(packageInfo, BROADER_PERMISSION)

            return isChangeable
        }
    }
}
