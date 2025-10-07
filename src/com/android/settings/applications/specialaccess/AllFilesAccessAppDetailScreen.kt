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

import android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
import android.app.AppOpsManager
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import androidx.core.net.toUri
import com.android.settings.R
import com.android.settings.Settings.ManageExternalStorageActivity
import com.android.settings.applications.CatalystAppListFragment.Companion.DEFAULT_SHOW_SYSTEM
import com.android.settings.contract.TAG_DEVICE_STATE_PREFERENCE
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.flags.Flags
import com.android.settings.utils.highlightPreference
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen

/**
 * The app detail catalyst screen for "All files access" special app access.
 *
 * This screen is accessible from: Settings > Apps > Special app access > All files
 * access > [app name]
 */
@ProvidePreferenceScreen(AllFilesAccessAppDetailScreen.KEY, parameterized = true)
open class AllFilesAccessAppDetailScreen(context: Context, arguments: Bundle) :
    SpecialAccessAppDetailScreen(context, arguments) {

    override val key
        get() = KEY

    override val bindingKey
        get() = "$KEY-$packageName"

    override val screenTitle
        get() = R.string.manage_external_storage_title

    override val op
        get() = AppOpsManager.OP_MANAGE_EXTERNAL_STORAGE

    override val setModeByUid: Boolean?
        get() = true

    override val switchPreferenceTitle
        get() = R.string.permit_manage_external_storage

    override val footerPreferenceTitle
        get() = R.string.allow_manage_external_storage_description

    // Edge case: what if the app's read permission is revoked/granted
    override fun isAvailable(context: Context) =
        super.isAvailable(context) &&
            hasManageExternalStoragePermission(context, packageInfo?.applicationInfo)

    override fun getMetricsCategory() = SettingsEnums.PAGE_UNKNOWN // TODO: correct page id

    override fun isFlagEnabled(context: Context) = Flags.deeplinkApps25q4()

    override fun tags(context: Context) =
        arrayOf(TAG_DEVICE_STATE_SCREEN, TAG_DEVICE_STATE_PREFERENCE)

    override fun getAccessChangeActionMetrics(allowed: Boolean): Int =
        when (allowed) {
            true -> SettingsEnums.APP_SPECIAL_PERMISSION_MANAGE_EXT_STRG_ALLOW
            else -> SettingsEnums.APP_SPECIAL_PERMISSION_MANAGE_EXT_STRG_DENY
        }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, ManageExternalStorageActivity::class.java, metadata?.key).apply {
            data = "package:$packageName".toUri()
            highlightPreference(arguments, metadata?.bindingKey)
        }

    companion object {
        const val KEY = "special_access_all_files_access_app_detail"

        @JvmStatic fun parameters(context: Context) = parameters(context, DEFAULT_SHOW_SYSTEM)

        fun parameters(context: Context, showSystemApp: Boolean) =
            parameters(context, showSystemApp, ::hasManageExternalStoragePermission)

        private fun hasManageExternalStoragePermission(
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

            val index = packageInfo?.requestedPermissions?.indexOf(MANAGE_EXTERNAL_STORAGE) ?: -1
            val flags = if (index >= 0) packageInfo?.requestedPermissionsFlags!![index] else 0

            return (flags and REQUESTED_PERMISSION_GRANTED) != 0 &&
                !isSystemOrRootUid(appInfo) &&
                isNotChangeablePackages(appInfo)
        }

        private fun isSystemOrRootUid(appInfo: ApplicationInfo): Boolean =
            UserHandle.getAppId(appInfo.uid) in listOf(Process.SYSTEM_UID, Process.ROOT_UID)

        private fun isNotChangeablePackages(appInfo: ApplicationInfo): Boolean =
            appInfo.packageName !in setOf("com.android.systemui")
    }
}
