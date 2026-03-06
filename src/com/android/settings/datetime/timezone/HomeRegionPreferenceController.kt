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

package com.android.settings.datetime.timezone

import android.content.Context
import androidx.preference.Preference

class HomeRegionPreferenceController(context: Context, preferenceKey: String) :
    RegionPreferenceController(context, preferenceKey) {

    private var mHomeTimeZoneSettings: HomeTimeZoneSettings? = null

    fun setParentFragment(fragment: HomeTimeZoneSettings) {
        mHomeTimeZoneSettings = fragment
    }

    override fun updateState(preference: Preference) {
        super.updateState(preference)
        preference.isEnabled = (mHomeTimeZoneSettings?.isHomeTimeZoneEnabled() ?: false)
    }
}
