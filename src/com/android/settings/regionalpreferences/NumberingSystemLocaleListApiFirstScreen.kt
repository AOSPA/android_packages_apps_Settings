/**
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

package com.android.settings.regionalpreferences

import android.os.LocaleList
import com.android.internal.app.LocaleStore
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.regionalpreferences.RegionalPreferencesDataUtils.getNumberingSystemLocales
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import java.util.Locale

// LINT.IfChange
@ProvidePreferenceScreen(NumberingSystemLocaleListApiFirstScreen.KEY)
class NumberingSystemLocaleListApiFirstScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.SYSTEM,
        fragment = NumberingSystemLocaleListFragment::class,
        purpose = R.string.regional_preference_numbering_system_purpose,
    ) {

    init {
        flag { Flags.catalystMigration26q2() }

        preconditions(R.string.numbering_system_screen_preconditions) {
            if (getNumberingSystemLocales().isNotEmpty()) {
                Allowed
            } else {
                Custom(R.string.numbering_system_screen_unavailable)
            }
        }
    }

    companion object {
        const val KEY = "regional_preference_numbering_system"
    }
}
// LINT.ThenChange(NumberingSystemLocaleListFragment.java)
