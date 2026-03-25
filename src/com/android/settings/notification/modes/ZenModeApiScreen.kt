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

package com.android.settings.notification.modes

import android.provider.Settings
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.overlay.FeatureFactory
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.notification.modes.ZenModesBackend

// LINT.IfChange
@ProvidePreferenceScreen(ZenModeApiScreen.KEY, parameterized = true)
class ZenModeApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.PRIORITY_MODES,
        fragment = ZenModeFragment::class,
        purpose = R.string.zen_mode_screen_purpose,
    ) {

    init {
        flag { Flags.catalystMigration26q2() }
        tags(APP_FUNCTION_NOTIFICATIONS)

        parameters {
            parameter(
                name = MODE_NAME,
                purpose = R.string.zen_mode_screen_parameter_purpose,
                required = true,
                type = ZenModes,
            )

            prepareScreenExtras { keyParameters, extras ->
                val modeName = keyParameters[MODE_NAME]
                val modes = getModes()
                modes
                    .find { it.name == modeName }
                    ?.let { extras.putString(Settings.EXTRA_AUTOMATIC_ZEN_RULE_ID, it.id) }
                    ?: Error("Can't find mode with name $modeName")
            }
        }
    }

    private fun getModes() = ZenModesBackend.getInstance(FeatureFactory.appContext).modes

    companion object {
        const val KEY = "zen_mode_screen"
        const val MODE_NAME = "MODE_NAME"
    }
}
// LINT.ThenChange(ZenModeFragment.java)
