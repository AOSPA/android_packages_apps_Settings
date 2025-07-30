/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.appfunctions.sources

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

/** Provides data shared by multiple [DeviceStateSource]s, which is computed lazily. */
class SharedDeviceStateData(private val context: Context) {
    val installedApplications: List<InstalledApplication> by lazy {
        val packageManager = context.packageManager
        val installedApplications =
            packageManager.getInstalledApplications(PackageManager.MATCH_DISABLED_COMPONENTS)
        installedApplications.map { info ->
            InstalledApplication(
                info = info,
                label = packageManager.getApplicationLabel(info).toString(),
            )
        }
    }

    data class InstalledApplication(val info: ApplicationInfo, val label: String)
}
