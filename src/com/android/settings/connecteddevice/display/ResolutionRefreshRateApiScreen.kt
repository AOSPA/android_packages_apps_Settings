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

package com.android.settings.connecteddevice.display

import android.hardware.display.DisplayManager
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability

// LINT.IfChange
@ProvidePreferenceScreen(ResolutionRefreshRateApiScreen.KEY, parameterized = true)
class ResolutionRefreshRateApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.CONNECTED_DEVICES,
        fragment = ResolutionRefreshRatePreferenceFragment::class,
        purpose = R.string.external_display_resolution_screen_purpose,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }

        parameters {
            parameter(
                name = "display_id",
                purpose = R.string.external_display_id_parameter_purpose,
                required = true,
                type = DisplayId(allowInternal = false, allowExternal = true),
            )

            prepareScreenExtras { params, extras ->
                val paramIdString = params["display_id"]
                val paramId =
                    requireNotNull(paramIdString?.toIntOrNull()) {
                        R.string.external_display_invalid_id
                    }

                extras.putInt(ResolutionRefreshRatePreferenceFragment.DISPLAY_ID_ARG, paramId)
            }
        }

        preconditions(R.string.external_display_connected_preconditions) {
            val displayManager = context.getSystemService(DisplayManager::class.java)
            val displayInjector = ConnectedDisplayInjector(context)

            val paramIdString = parameters["display_id"]
            val targetDisplayId = paramIdString?.toInt()
            val targetDisplay =
                if (targetDisplayId != null) {
                    displayManager?.getDisplay(targetDisplayId)
                } else {
                    null
                }

            val isValidTarget =
                targetDisplay != null && displayInjector.isConnectedDisplay(targetDisplay)

            if (isValidTarget) {
                Allowed
            } else {
                Custom(R.string.external_display_connected_unavailable, stability = PreconditionStability.UNSTABLE)
            }
        }
    }

    companion object {
        const val KEY = "external_display_resolution_refresh_rate_screen"
    }
}
// LINT.ThenChange(ResolutionRefreshRatePreferenceFragment.kt)
