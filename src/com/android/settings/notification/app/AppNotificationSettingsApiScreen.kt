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

package com.android.settings.notification.app

import android.Manifest.permission.STATUS_BAR_SERVICE
import android.content.pm.PackageManager
import com.android.settings.R
import com.android.settings.applications.AppInfoBase.ARG_PACKAGE_NAME
import com.android.settings.applications.InstalledPackageName
import com.android.settings.flags.Flags
import com.android.settings.notification.NotificationBackend
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean

@ProvidePreferenceScreen(AppNotificationSettingsApiScreen.KEY, parameterized = true)
class AppNotificationSettingsApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.NOTIFICATIONS,
        fragment = AppNotificationSettings::class,
        purpose = R.string.app_notifications_screen_purpose,
    ) {
    internal var backend = NotificationBackend()

    init {
        flag { Flags.catalystMigration26q2() }

        tags(APP_FUNCTION_NOTIFICATIONS)

        parameters {
            parameter(
                name = ARG_PACKAGE_NAME,
                purpose = R.string.app_parameter_purpose,
                required = true,
                type = InstalledPackageName,
            )

            prepareScreenExtras { keyParameters, extras ->
                extras.putString(ARG_PACKAGE_NAME, keyParameters[ARG_PACKAGE_NAME])
            }
        }

        preference(
            key = "permission_toggle",
            purpose = R.string.app_notifications_allow,
            type = AnyBoolean,
        ) {
            permissions(STATUS_BAR_SERVICE) // SystemUI-only, but not enforced for agents.

            get {
                execute {
                    val packageName = parameters.getRequired(ARG_PACKAGE_NAME)
                    val uid =
                        context.packageManager.getPackageUid(
                            packageName,
                            PackageManager.PackageInfoFlags.of(0),
                        )
                    !backend.getNotificationsBanned(packageName, uid)
                }
            }

            set {
                execute { value ->
                    val packageName = parameters.getRequired(ARG_PACKAGE_NAME)
                    val uid =
                        context.packageManager.getPackageUid(
                            packageName,
                            PackageManager.PackageInfoFlags.of(0),
                        )
                    backend.setNotificationsEnabledForPackage(packageName, uid, value)
                }
            }
        }
    }

    companion object {
        const val KEY = "app_notifications"
    }
}
