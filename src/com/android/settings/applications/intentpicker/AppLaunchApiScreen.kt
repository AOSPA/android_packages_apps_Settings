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

package com.android.settings.applications.intentpicker

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.UserHandle
import com.android.settings.R
import com.android.settings.applications.AppInfoBase.ARG_PACKAGE_NAME
import com.android.settings.applications.InstalledPackageName
import com.android.settings.flags.Flags
import com.android.settingslib.applications.AppUtils
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_APPS
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability

// LINT.IfChange
@ProvidePreferenceScreen(AppLaunchApiScreen.KEY, parameterized = true)
class AppLaunchApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.APPS,
        fragment = AppLaunchSettings::class,
        purpose = R.string.app_launch_screen_purpose,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }

        // UI-only as this has no preferences and the OpenByDefaultStateSource provides this information.
        tags(APP_FUNCTION_APPS, UI_ONLY_PREFERENCE)

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

        preconditions(R.string.app_launch_settings_screen_preconditions) {
            val packageName = parameters.getRequired(ARG_PACKAGE_NAME)
            val packageInfo =
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(
                        PackageManager.MATCH_DISABLED_COMPONENTS.toLong() or
                            PackageManager.MATCH_ANY_USER.toLong() or
                            PackageManager.GET_SIGNING_CERTIFICATES.toLong() or
                            PackageManager.GET_PERMISSIONS.toLong() or
                            PackageManager.MATCH_ARCHIVED_PACKAGES
                    ),
                )
            val appInfo = packageInfo.applicationInfo
            when {
                appInfo == null -> {
                    Custom(
                        R.string.app_launch_screen_app_unavailable,
                        stability = PreconditionStability.UNSTABLE,
                    )
                }
                appInfo.flags and ApplicationInfo.FLAG_INSTALLED == 0 -> {
                    Custom(
                        R.string.app_launch_screen_app_not_installed,
                        stability = PreconditionStability.UNSTABLE,
                    )
                }
                !appInfo.enabled -> {
                    Custom(
                        R.string.app_launch_screen_app_disabled,
                        stability = PreconditionStability.UNSTABLE,
                    )
                }
                AppUtils.isInstant(appInfo) -> {
                    // Technically unstable but unlikely to change.
                    Custom(
                        R.string.app_launch_screen_app_instant,
                        stability = PreconditionStability.UNSTABLE,
                    )
                }
                AppUtils.isBrowserApp(context, packageName, UserHandle.myUserId()) -> {
                    // Technically unstable but unlikely to change.
                    Custom(
                        R.string.app_launch_screen_app_browser,
                        stability = PreconditionStability.UNSTABLE,
                    )
                }
                else -> Allowed
            }
        }
    }

    companion object {
        const val KEY = "launch_by_default"
    }
}
// LINT.ThenChange(AppLaunchSettings.java, ../appinfo/AppOpenByDefaultPreferenceController.java)
