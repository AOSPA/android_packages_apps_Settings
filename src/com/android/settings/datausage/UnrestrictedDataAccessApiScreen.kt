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

package com.android.settings.datausage

import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category

// LINT.IfChange
@ProvidePreferenceScreen(UnrestrictedDataAccessApiScreen.KEY)
class UnrestrictedDataAccessApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.NETWORK,
        fragment = UnrestrictedDataAccess::class,
        purpose = R.string.unrestricted_data_settings_purpose,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }

        // TODO(b/474055726) CatalystApi: migrate the preferences
    }

    companion object {
        const val KEY = "unrestricted_data_screen"
    }
}
// LINT.ThenChange(UnrestrictedDataAccess.java)
