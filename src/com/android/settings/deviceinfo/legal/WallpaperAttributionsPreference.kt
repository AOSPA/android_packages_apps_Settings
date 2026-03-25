/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.settings.deviceinfo.legal

import android.content.Context
import androidx.preference.Preference
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.preference.PreferenceBinding

// LINT.IfChange
class WallpaperAttributionsPreference :
    PreferenceMetadata, PreferenceBinding, PreferenceAvailabilityProvider {
    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.wallpaper_attributions_purpose

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override val title: Int
        get() = R.string.wallpaper_attributions

    override val summary: Int
        get() = R.string.wallpaper_attributions_values

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.isSelectable = false
    }

    override val availabilityDescription = UI_ONLY_PREFERENCE

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context) =
        context.resources.getBoolean(R.bool.config_show_wallpaper_attribution)

    companion object {
        const val KEY = "wallpaper_attributions"
    }
}
// LINT.ThenChange(WallpaperAttributionsPreferenceController.java)
