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

package com.android.settings.language

import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category

// LINT.IfChange
@ProvidePreferenceScreen(LanguageAndRegionApiFirstScreen.KEY)
class LanguageAndRegionApiFirstScreen : PreferencesApiScreen(
    key = KEY,
    topLevelSettingsCategory = Category.SYSTEM,
    fragment = LanguageAndRegionSettings::class,
    purpose = R.string.language_and_region_settings_purpose,
    alreadyPartiallyMigrated = LanguageAndRegionScreen::class
) {

    init {
        flag { Flags.catalystMigration26q2() }
    }

    companion object {
        const val KEY = "api_language_and_region_settings"
    }
}
// LINT.ThenChange(LanguageAndRegionSettings.java, LanguageAndRegionPreferenceController.java)
