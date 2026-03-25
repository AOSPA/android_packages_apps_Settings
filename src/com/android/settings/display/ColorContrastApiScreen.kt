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

import android.app.UiModeManager
import android.provider.Settings.Secure.CONTRAST_LEVEL
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.types.CustomEnum
import com.android.settingslib.metadata.preferencesapi.types.EType
import com.android.settingslib.metadata.preferencesapi.types.EnumApiWithRes

// LINT.IfChange
@ProvidePreferenceScreen(ColorContrastApiScreen.KEY)
class ColorContrastApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.DISPLAY,
        fragment = ColorContrastFragment::class,
        purpose = R.string.color_contrast_screen_purpose,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }

        preference(
            key = "color_contrast_selector",
            type =
                CustomEnum(
                    ContrastLevelApiWithRes::class,
                    R.string.contrast_level_enum_description,
                ),
            purpose = R.string.color_contrast_selector_preference_purpose,
        ) {
            get {
                execute {
                    SettingsSecureStore.get(context).getFloat(CONTRAST_LEVEL)?.let { contrast ->
                        val level = UiModeManager.ContrastUtils.toContrastLevel(contrast)
                        ContrastLevelApiWithRes.entries.find { it.asApiValue == level }
                    } ?: ContrastLevelApiWithRes.DEFAULT
                }
            }

            set {
                execute { value ->
                    val contrast = UiModeManager.ContrastUtils.fromContrastLevel(value.asApiValue)
                    SettingsSecureStore.get(context).setFloat(CONTRAST_LEVEL, contrast)
                }
            }
        }
    }

    companion object {
        const val KEY = "color_contrast_screen"
    }
}

enum class ContrastLevelApiWithRes(override val asApiValue: Int, override val purpose: Int) :
    EnumApiWithRes<Int> {
    DEFAULT(0, R.string.contrast_default),
    MEDIUM(1, R.string.contrast_medium),
    HIGH(2, R.string.contrast_high),
}
// LINT.ThenChange(ColorContrastFragment.java,
//                 ContrastSelectorPreferenceController.java,
//                 ContrastPreferenceController.java)
