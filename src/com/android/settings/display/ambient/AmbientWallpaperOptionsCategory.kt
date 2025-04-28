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
package com.android.settings.display.ambient

import android.content.Context
import com.android.internal.R as InternalR
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceCategory
import com.android.systemui.shared.Flags.ambientAod

class AmbientWallpaperOptionsCategory :
    PreferenceCategory("ambient_wallpaperGroup", R.string.doze_always_on_wallpaper_options),
    PreferenceAvailabilityProvider {

    override fun isAvailable(context: Context): Boolean =
        ambientAod() && context.resources.getBoolean(InternalR.bool.config_dozeSupportsAodWallpaper)
}
