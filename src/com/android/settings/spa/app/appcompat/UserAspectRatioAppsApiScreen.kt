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
import com.android.settings.applications.appcompat.UserAspectRatioManager
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability

// LINT.IfChange
@ProvidePreferenceScreen(UserAspectRatioAppsApiScreen.KEY)
class UserAspectRatioAppsApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.APPS,
        spaRoutePrefix = UserAspectRatioAppsPageProvider.name,
        purpose = R.string.user_aspect_ratio_app_list_purpose,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }

        preconditions(R.string.user_aspect_ratio_apps_screen_preconditions) {
            if (!UserAspectRatioManager.isFeatureEnabled(context)) {
                Custom(
                    R.string.user_aspect_ratio_screen_unavailable,
                    stability = PreconditionStability.STABLE_UNTIL_APK_UPDATE,
                )
            } else {
                Allowed
            }
        }
    }

    companion object {
        const val KEY = "aspect_ratio_apps"
    }
}
// LINT.ThenChange(UserAspectRatioAppsPageProvider.kt)
