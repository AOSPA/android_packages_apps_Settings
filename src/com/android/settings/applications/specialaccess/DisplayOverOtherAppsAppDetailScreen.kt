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

import android.Manifest.permission.SYSTEM_ALERT_WINDOW
import android.app.AppOpsManager
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.UserManager
import android.provider.Settings.ACTION_MANAGE_APP_OVERLAY_PERMISSION
import androidx.core.net.toUri
import com.android.settings.CatalystSettingsActivity
import com.android.settings.R
import com.android.settings.applications.CatalystAppListFragment.Companion.DEFAULT_SHOW_SYSTEM
import com.android.settings.contract.TAG_DEVICE_STATE_PREFERENCE
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.flags.Flags
import com.android.settings.utils.highlightPreference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen

@ProvidePreferenceScreen(DisplayOverOtherAppsAppDetailScreen.KEY, parameterized = true)
open class DisplayOverOtherAppsAppDetailScreen(context: Context, arguments: Bundle) :
    SpecialAccessAppDetailScreen(context, arguments) {

    override val key
        get() = KEY

    override val bindingKey
        get() = "$KEY-$packageName"

    override val screenTitle
        get() = R.string.system_alert_window_settings

    override val op
        get() = AppOpsManager.OP_SYSTEM_ALERT_WINDOW

    override val setModeByUid: Boolean?
        get() = false // set op mode by package

    override val switchPreferenceTitle
        get() = R.string.permit_draw_overlay

    override val footerPreferenceTitle
        get() = R.string.allow_overlay_description

    override fun tags(context: Context) =
        arrayOf(TAG_DEVICE_STATE_SCREEN, TAG_DEVICE_STATE_PREFERENCE)

    override fun isFlagEnabled(context: Context) = Flags.deeplinkApps25q4()

    override fun isAvailable(context: Context) =
        super.isAvailable(context) &&
            !UserManager.get(context).isManagedProfile &&
            hasOverlayPermission(context, packageInfo?.applicationInfo)

    override fun getMetricsCategory() = SettingsEnums.SYSTEM_ALERT_WINDOW_APPS

    override fun getAccessChangeActionMetrics(allowed: Boolean) =
        when (allowed) {
            true -> SettingsEnums.APP_SPECIAL_PERMISSION_APPDRAW_ALLOW
            else -> SettingsEnums.APP_SPECIAL_PERMISSION_APPDRAW_DENY
        }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        Intent(ACTION_MANAGE_APP_OVERLAY_PERMISSION).apply {
            data = "package:$packageName".toUri()
            highlightPreference(arguments, metadata?.bindingKey)
        }

    companion object {
        const val KEY = "sa_draw_overlay_app_detail"

        @JvmStatic fun parameters(context: Context) = parameters(context, DEFAULT_SHOW_SYSTEM)

        fun parameters(context: Context, showSystemApp: Boolean) =
            parameters(context, showSystemApp, ::hasOverlayPermission)

        fun hasOverlayPermission(context: Context, appInfo: ApplicationInfo?): Boolean {
            if (appInfo == null) return false
            try {
                val packageInfo: PackageInfo =
                    context.packageManager.getPackageInfo(
                        appInfo.packageName,
                        PackageManager.GET_PERMISSIONS,
                    )
                val requestedPermissions = packageInfo.requestedPermissions
                return requestedPermissions?.contains(SYSTEM_ALERT_WINDOW) == true
            } catch (e: Exception) {
                return false
            }
        }
    }
}

class DisplayOverOtherAppsAppDetailActivity :
    CatalystSettingsActivity(DisplayOverOtherAppsAppDetailScreen.KEY)
