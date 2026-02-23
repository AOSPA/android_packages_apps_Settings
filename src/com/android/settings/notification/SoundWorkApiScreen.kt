/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law-or-agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.notification

import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_NOTIFICATIONS

// LINT.IfChange
@ProvidePreferenceScreen(SoundWorkApiScreen.KEY)
class SoundWorkApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.SOUND,
        fragment = SoundWorkSettings::class,
        purpose = R.string.sound_work_screen_purpose,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }

        tags(APP_FUNCTION_NOTIFICATIONS)

        preconditions(R.string.sound_work_screen_preconditions) {
            if (SoundWorkSettings.isSupportWorkProfileSound(context)) {
                Allowed
            } else {
                Custom(R.string.sound_work_screen_not_supported)
            }
        }
    }

    companion object {
        const val KEY = "sound_work_screen"
    }
}
// LINT.ThenChange(SoundWorkSettings.java,
//                 WorkSoundsPreferenceController.java)
