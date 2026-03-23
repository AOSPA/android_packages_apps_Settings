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
import android.os.Build
import android.os.UserHandle
import androidx.annotation.RequiresApi
import com.android.settingslib.metadata.R
import com.android.settingslib.metadata.preferencesapi.types.DirectFiniteOptionsType
import com.android.settingslib.metadata.preferencesapi.types.EType
import com.android.settingslib.metadata.preferencesapi.safe
import com.android.settingslib.metadata.preferencesapi.unsafe
import com.android.settingslib.metadata.preferencesapi.SafetyAnnotated
import com.android.settingslib.spaprivileged.model.app.AppListRepository
import com.android.settingslib.spaprivileged.model.app.AppListRepositoryImpl

/**
 * Any package installed on the device.
 *
 * This implementation lives within Settings because most library clients do not possess the
 * required permissions to list all packages on the device.
 *
 * @param heldPermissions - Permissions which must be held by the packages.
 * @param excludeSystemApps - Whether to exclude system apps from the list.
 */
// This is only open to enable the companion object to call the constructor.
// We should close this down but for now no point in changing all the current
// callers.
open class InstalledPackageName(
    private val heldPermissions: Array<String>? = null,
    private val excludeSystemApps: Boolean = true
) : DirectFiniteOptionsType<String> {

    override val externalType: EType<String> = EType.String

    override fun getDescription(context: Context): String =
        context.getString(R.string.installed_package_name_type_description)

    override fun getKey(): String {
        val appsFilter = if (excludeSystemApps) "non-system" else "all"
        val permissionsRequired =
            heldPermissions?.takeIf { it.isNotEmpty() }?.sorted()?.joinToString(",")
                ?: "no-permissions"
        return "InstalledPackageName:$permissionsRequired:$appsFilter"
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override suspend fun getOptions(context: Context): List<Pair<SafetyAnnotated<String>, SafetyAnnotated<String>>> {
        val pm = context.packageManager
        val repository: AppListRepository = AppListRepositoryImpl(context)

        var appList: List<ApplicationInfo> = repository.loadAndMaybeExcludeSystemApps(
            UserHandle.myUserId(),
            this.excludeSystemApps
        )

        if (heldPermissions != null) {
            appList = appList.filter { appInfo ->
                try {
                    val packageInfo =
                        pm.getPackageInfo(appInfo.packageName, PackageManager.GET_PERMISSIONS)
                    packageInfo.requestedPermissions?.let { requestedPermissions ->
                        heldPermissions.all { requiredPerm ->
                            requestedPermissions.contains(
                                requiredPerm
                            )
                        }
                    } ?: false
                } catch (_: PackageManager.NameNotFoundException) {
                    false
                }
            }
        }

        return appList.map { appInfo ->
            appInfo.packageName.safe() to (appInfo.loadLabel(pm)?.toString()?.unsafe()
                ?: appInfo.packageName.safe())
        }
    }

    companion object : InstalledPackageName()
}
