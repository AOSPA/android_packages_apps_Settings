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

package com.android.settings.location

import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.multiusers.ManagementScope.OWN_USER

@ProvidePreferenceScreen(LocationSettingsScreenApi.KEY)
open class LocationSettingsScreenApi :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.LOCATION,
        fragment = LocationSettings::class,
        purpose = R.string.location_settings_purpose,
        alreadyPartiallyMigrated = LocationScreen::class,
        // TODO(b/491740982): There are LocationPersonalSettings::class and
        //  LocationWorkProfileSettings::class for personal and work profile land page fragments.
        canManage = OWN_USER
    ) {
    init {
        flag { Flags.catalystMigration26q2() }
    }

    companion object {
        const val KEY = "api_location_settings"
    }
}
