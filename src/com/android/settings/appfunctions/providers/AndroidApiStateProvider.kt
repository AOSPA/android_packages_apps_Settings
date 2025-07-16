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

package com.android.settings.appfunctions.providers

import android.app.INotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.ServiceManager
import com.android.settings.appfunctions.DeviceStateCategory
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItem
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenDeviceStates

/**
 * A [DeviceStateProvider] that gathers device state information directly from Android APIs
 * rather than using Catalyst.
 *
 * @param context The application context.
 */
class AndroidApiStateProvider(private val context: Context) : DeviceStateProvider {
    override suspend fun provide(
        requestCategory: DeviceStateCategory
    ): DeviceStateProviderResult {
        val states = mutableListOf<PerScreenDeviceStates>()
        if (requestCategory in setOf(DeviceStateCategory.UNCATEGORIZED)) {
            states.add(buildNotificationsScreenStates())
        }
        return DeviceStateProviderResult(states = states)
    }

    // TODO: prototype implementation, to be replaced in b/426005115
    private fun buildNotificationsScreenStates(): PerScreenDeviceStates {
        val packageManager = context.packageManager
        val notificationManager = INotificationManager.Stub.asInterface(
            ServiceManager.getService(Context.NOTIFICATION_SERVICE)
        )
        val disabledComponentsFlag =
            (PackageManager.MATCH_DISABLED_COMPONENTS or
                    PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS)
                .toLong()
        val regularFlags = PackageManager.ApplicationInfoFlags.of(disabledComponentsFlag)
        val installedPackages = packageManager.getInstalledApplications(regularFlags)
        val deviceStateItems = ArrayList<DeviceStateItem>(installedPackages.size)
        for (info in installedPackages) {
            val packageName = info.packageName
            val appName = packageManager.getApplicationLabel(info.applicationInfo)
            val uid = info.applicationInfo.uid
            val areNotificationsEnabled =
                notificationManager?.areNotificationsEnabledForPackage(packageName, uid) ?: false
            deviceStateItems.add(
                DeviceStateItem(
                    key = "notifications_enabled_package_$packageName",
                    hintText = "App: $appName",
                    jsonValue = areNotificationsEnabled.toString(),
                )
            )
        }

        return PerScreenDeviceStates(
            description =
                "Notifications Settings Screen. Note that to get to the notification settings for a given package, the intent uri is intent:#Intent;action=android.settings.APP_NOTIFICATION_SETTINGS;S.android.provider.extra.APP_PACKAGE=\$packageName;end",
            intentUri = "intent:#Intent;action=android.settings.NOTIFICATION_SETTINGS;end",
            deviceStateItems = deviceStateItems,
        )
    }
}