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
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.multiusers.ManagementScope.PROFILE_GROUP

@ProvidePreferenceScreen(LocationServicesScreenApi.KEY)
open class LocationServicesScreenApi :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.LOCATION,
        fragment = LocationServices::class,
        purpose = R.string.location_services_purpose,
        alreadyPartiallyMigrated = LocationServicesScreen::class,
        // TODO(b/491740982): There are LocationServicesForPrivateProfile::class and
        //  LocationServicesForWork::class for personal and work profile land page fragments.
        canManage = PROFILE_GROUP
    ) {
    init {
        flag { Flags.catalystMigration26q2() }

        // exclude this screen from api result since we don't have any preferences in it, and it is
        // redundant with the location_services screen in the output. We can remove this tag when
        // we'll add preferences.
        tags(UI_ONLY_PREFERENCE)
    }

    companion object {
        const val KEY = "api_location_services"
    }
}
