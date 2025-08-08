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

package com.android.settings.appfunctions.sources

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.verify.domain.DomainVerificationManager
import com.android.settings.appfunctions.DeviceStateCategory
import com.android.settings.applications.intentpicker.IntentPickerUtils
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItem
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenDeviceStates

class OpenByDefaultStateSource : DeviceStateSource {
    override val category: DeviceStateCategory = DeviceStateCategory.UNCATEGORIZED

    override suspend fun get(
        context: Context,
        sharedDeviceStateData: SharedDeviceStateData,
    ): PerScreenDeviceStates {
        val packageManager = context.packageManager
        val installedApplications =
            packageManager.getInstalledApplications(PackageManager.MATCH_DISABLED_COMPONENTS)

        val deviceStateItems = mutableListOf<DeviceStateItem>()
        for (app in installedApplications) {
            val appName = packageManager.getApplicationLabel(app.applicationInfo)
            val packageName = app.packageName
            val domainVerificationManager =
                context.getSystemService(DomainVerificationManager::class.java)
            val userState =
                IntentPickerUtils.getDomainVerificationUserState(
                    domainVerificationManager,
                    app.packageName,
                )
            val isEnabled = userState?.isLinkHandlingAllowed ?: false
            deviceStateItems.add(
                DeviceStateItem(
                    key = "preferred_settings_enabled_package_$packageName",
                    jsonValue = isEnabled.toString(),
                    hintText = "App: $appName",
                )
            )
        }

        return PerScreenDeviceStates(
            description = "Open by default",
            deviceStateItems = deviceStateItems,
        )
    }
}
