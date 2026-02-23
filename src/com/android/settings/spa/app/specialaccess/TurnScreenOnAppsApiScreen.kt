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
package com.android.settings.spa.app.specialaccess

import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_APPS

@ProvidePreferenceScreen(TurnScreenOnAppsApiScreen.KEY)
class TurnScreenOnAppsApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.APPS,
        spaRoutePrefix = TurnScreenOnAppsAppListProvider.getAppListRoute(),
        purpose = R.string.special_access_turn_screen_on_apps_list_purpose,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }

        tags(APP_FUNCTION_APPS)
    }

    companion object {
        const val KEY = "turn_screen_on_apps"
    }
}
