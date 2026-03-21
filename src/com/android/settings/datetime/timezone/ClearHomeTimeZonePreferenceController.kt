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
import android.provider.Settings
import android.timezone.flags.Flags
import com.android.settings.R
import com.android.settings.core.TogglePreferenceController

class ClearHomeTimeZonePreferenceController(context: Context, preferenceKey: String) :
    TogglePreferenceController(context, preferenceKey) {

    private var mHomeTimeZoneSettings: HomeTimeZoneSettings? = null

    fun setParentFragment(fragment: HomeTimeZoneSettings) {
        mHomeTimeZoneSettings = fragment
    }

    override fun isChecked(): Boolean {
        val currentZone =
            Settings.Global.getString(mContext.contentResolver, Settings.Global.HOME_TIME_ZONE_ID)
        return "" != currentZone
    }

    override fun getAvailabilityStatus(): Int {
        return if (Flags.enableHomeTimeZoneApi()) AVAILABLE else UNSUPPORTED_ON_DEVICE
    }

    override fun setChecked(isChecked: Boolean): Boolean {
        if (!isChecked) {
            mHomeTimeZoneSettings?.setHomeTimeZone("")
        } else {
            mHomeTimeZoneSettings?.setHomeTimeZone(null)
        }

        return true
    }

    override fun getSliceHighlightMenuRes(): Int {
        return R.string.menu_key_system
    }
}
