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
import android.content.pm.PackageInfo
import android.content.pm.PackageManager.PackageInfoFlags
import android.os.Build
import androidx.annotation.RequiresApi
import com.android.settingslib.metadata.R
import com.android.settingslib.metadata.preferencesapi.types.FiniteOptionsType

/**
 * Any package installed on the device.
 *
 * This implementation lives within Settings because most library clients do not possess the
 * required permissions to list all packages on the device.
 *
 * @param flags - The flags used to query the underlying `getInstalledPackages` method from the
 *   package manager, when retrieving all the options.
 */
class InstalledPackageName(private val flags: PackageInfoFlags) : FiniteOptionsType<String> {

    override fun getDescription(context: Context): String =
        context.getString(R.string.installed_package_name_type_description)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun getOptions(context: Context): List<Pair<String, String>> =
        context.packageManager.getInstalledPackages(flags).map { packageInfo: PackageInfo ->
            packageInfo.packageName to
                (packageInfo.applicationInfo?.name ?: packageInfo.packageName)
        }
}
