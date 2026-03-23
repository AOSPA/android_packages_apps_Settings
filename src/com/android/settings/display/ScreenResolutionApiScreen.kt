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

package com.android.settings.display

import android.Manifest.permission.MODIFY_USER_PREFERRED_DISPLAY_MODE
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported
import com.android.settingslib.metadata.preferencesapi.types.CustomEnum
import com.android.settingslib.metadata.preferencesapi.types.EnumApiWithRes

enum class ScreenResolutionOption(
    override val asApiValue: Int,
    override val purpose: Int,
) : EnumApiWithRes<Int> {
    HIGH(0, R.string.screen_resolution_option_high),
    FULL(1, R.string.screen_resolution_option_full),
}

// LINT.IfChange
@ProvidePreferenceScreen(ScreenResolutionApiScreen.KEY)
class ScreenResolutionApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.DISPLAY,
        fragment = ScreenResolutionFragment::class,
        purpose = R.string.screen_resolution_screen_purpose,
    ) {
    init {
        flag {
            Flags.catalystMigration26q2() &&
                com.android.server.display.feature.flags.Flags.displaySettingsApiScreenSupport()
        }
        preconditions(R.string.screen_resolution_preconditions) {
            if (context.checkSupportedResolutions()) {
                Allowed
            } else {
                HardwareUnsupported(R.string.screen_resolution_unsupported)
            }
        }

        preference(
            key = PREFERENCE_KEY,
            purpose = R.string.screen_resolution_purpose,
            type = CustomEnum(
                ScreenResolutionOption::class,
                R.string.screen_resolution_purpose
            )
        ) {
            get {
                execute {
                    val currentWidth = context.getDisplayWidth()
                    if (currentWidth == context.getHighResolutionDisplayWidth()) {
                        ScreenResolutionOption.HIGH
                    } else {
                        ScreenResolutionOption.FULL
                    }
                }
            }
            set {
                permissions(MODIFY_USER_PREFERRED_DISPLAY_MODE)
                execute { value ->
                    val width =
                        if (value == ScreenResolutionOption.HIGH) {
                            context.getHighResolutionDisplayWidth()
                        } else {
                            context.getFullResolutionDisplayWidth()
                        }
                    context.setDisplayModeByWidth(width)
                }
            }
        }
    }

    companion object {
        const val KEY = "screen_resolution"
        const val PREFERENCE_KEY = "screen_resolution_preference"
    }
}
// LINT.ThenChange(ScreenResolutionFragment.java, ScreenResolutionController.java)
