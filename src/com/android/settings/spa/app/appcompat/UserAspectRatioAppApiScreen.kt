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
package com.android.settings.spa.app.appcompat

import com.android.settings.R
import com.android.settings.applications.InstalledPackageName
import com.android.settings.applications.appcompat.UserAspectRatioDetails
import com.android.settings.applications.appcompat.UserAspectRatioManager
import com.android.settings.applications.appinfo.AppInfoDashboardFragment.ARG_PACKAGE_NAME
import com.android.settings.applications.getApplicationInfo
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability

// LINT.IfChange
@ProvidePreferenceScreen(UserAspectRatioAppApiScreen.KEY, parameterized = true)
class UserAspectRatioAppApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.APPS,
        fragment = UserAspectRatioDetails::class,
        purpose = R.string.user_aspect_ratio_app_purpose,
    ) {

    init {
        flag { Flags.catalystMigration26q2() }

        parameters {
            parameter(
                name = ARG_PACKAGE_NAME,
                purpose = R.string.user_aspect_ratio_app_parameter_purpose,
                required = true,
                type = InstalledPackageName
            )

            prepareScreenExtras { parameters, extras ->
                extras.putString(ARG_PACKAGE_NAME, parameters[ARG_PACKAGE_NAME])
            }
        }

        preconditions(R.string.user_aspect_ratio_app_screen_preconditions) {
            val packageName = parameters.getRequired(ARG_PACKAGE_NAME)
            val app = context.getApplicationInfo(packageName)
            val manager = UserAspectRatioManager(context)
            if (!UserAspectRatioManager.isFeatureEnabled(context)) {
                Custom(
                    R.string.user_aspect_ratio_screen_unavailable,
                    stability = PreconditionStability.STABLE_UNTIL_APK_UPDATE,
                )
            } else if (app != null && manager.canDisplayAspectRatioUi(app)) {
                Allowed
            } else {
                Custom(
                    R.string.user_aspect_ratio_app_not_launchable,
                    stability = PreconditionStability.UNSTABLE,
                )
            }
        }
    }

    companion object {
        const val KEY = "aspect_ratio_app"
    }
}
// LINT.ThenChange(UserAspectRatioDetails.java)
