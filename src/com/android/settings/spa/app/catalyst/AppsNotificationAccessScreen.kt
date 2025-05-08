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

package com.android.settings.spa.app.catalyst

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
import android.service.notification.NotificationListenerService
import com.android.settings.R
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.flags.Flags
import com.android.settingslib.applications.ServiceListing
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceFragment
import com.android.settingslib.preference.PreferenceScreenCreator

/** "Apps" -> "Special app access" -> "Notification read, reply & control" */
@ProvidePreferenceScreen(AppsNotificationAccessScreen.KEY)
class AppsNotificationAccessScreen : PreferenceScreenCreator {

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.manage_notification_access_title

    override fun tags(context: Context) = arrayOf(TAG_DEVICE_STATE_SCREEN)

    override fun isFlagEnabled(context: Context) = Flags.catalystAppList() || Flags.deviceState()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass() = PreferenceFragment::class.java

    // TODO: kurtismelby - app highlighting should be supported when there is a
    //   a PreferenceMetadata for a specific app.
    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent? =
        Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS)

    override fun getPreferenceHierarchy(context: Context) =
        preferenceHierarchy(context, this) {
            val services = loadNotificationListenerServices(context)
            for (service in services) {
                val arguments =
                    Bundle(1).apply {
                        putString("app", service.packageName)
                        putString("serviceName", service.name)
                    }
                +(AppInfoNotificationAccessScreen.KEY args arguments)
            }
        }

    companion object {
        const val KEY = "device_state_apps_notification_access"

        @JvmStatic
        fun loadNotificationListenerServices(context: Context): List<ServiceInfo> {
            val serviceListing =
                ServiceListing.Builder(context)
                    .setIntentAction(NotificationListenerService.SERVICE_INTERFACE)
                    .setPermission(Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE)
                    .setNoun("notification listener")
                    .setSetting(Settings.Secure.ENABLED_NOTIFICATION_LISTENERS)
                    .setTag(TAG_DEVICE_STATE_SCREEN)
                    .build()
            val services = serviceListing.load()
            return services
        }
    }
}
