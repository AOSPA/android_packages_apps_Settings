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

package com.android.settings.applications

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ApplicationInfoFlags
import android.content.pm.PackageManager.PackageInfoFlags
import android.multiuser.Flags
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import androidx.annotation.RequiresApi
import com.android.settingslib.metadata.R
import com.android.settingslib.metadata.preferencesapi.types.FiniteOptionsType

/**
 * Any package installed on the device.
 *
 * This implementation lives within Settings because most library clients do not possess the
 * required permissions to list all packages on the device.
 *
 * @param flags - The flags used to query apps when retrieving all the options.
 * @param heldPermissions - Permissions which must be held by the packages.
 */
sealed class InstalledPackageName(
    private val flags: Long? = null,
    private val heldPermissions: Array<String>? = null,
) : FiniteOptionsType<String> {

    override fun getType(): Class<String> = String::class.java

    override fun getDescription(context: Context): String =
        context.getString(R.string.installed_package_name_type_description)

    override fun getKey(): String = "InstalledPackageName:${flags ?: 0}"

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override suspend fun getOptions(context: Context): List<Pair<String, String>> {
        val pm = context.packageManager
        val appList: List<ApplicationInfo> =
            if (heldPermissions != null) {
                pm.getPackagesHoldingPermissions(heldPermissions, PackageInfoFlags.of(flags ?: 0L))
                    .mapNotNull { it.applicationInfo }
                    .filter { it.enabled }
            } else {
                pm.getInstalledApplications(
                    ApplicationInfoFlags.of(flags ?: getDefaultFlags(context))
                )
            }

        return appList.map { appInfo ->
            appInfo.packageName to (appInfo.loadLabel(pm)?.toString() ?: appInfo.packageName)
        }
    }

    private fun getDefaultFlags(context: Context): Long {
        val um = context.getSystemService(UserManager::class.java)
        var retrieveFlags =
            PackageManager.MATCH_DISABLED_COMPONENTS or
                PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS
        if (!Flags.dontShowOtherUsersAppsToAdmin() && um.isUserAdmin(UserHandle.myUserId())) {
            retrieveFlags = retrieveFlags or PackageManager.MATCH_ANY_USER
        }
        return retrieveFlags.toLong()
    }

    companion object : InstalledPackageName()
}
