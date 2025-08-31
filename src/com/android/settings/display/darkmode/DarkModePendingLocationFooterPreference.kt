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

package com.android.settings.display.darkmode

import android.app.UiModeManager
import android.content.Context
import android.location.LocationManager
import android.view.accessibility.Flags
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.widget.FooterPreferenceBinding
import com.android.settings.widget.FooterPreferenceMetadata
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.widget.FooterPreference

// LINT.IfChange
class DarkModePendingLocationFooterPreference :
    FooterPreferenceMetadata, FooterPreferenceBinding, PreferenceAvailabilityProvider {

    override val key: String
        get() = KEY

    override val icon: Int
        get() = R.drawable.ic_settings_location_filled

    override fun isAvailable(context: Context): Boolean {
        val uiModeManager = context.getSystemService(UiModeManager::class.java) ?: return false
        val locationManager = context.getSystemService(LocationManager::class.java) ?: return false

        return uiModeManager.nightMode == UiModeManager.MODE_NIGHT_AUTO &&
            locationManager.isLocationEnabled &&
            locationManager.lastLocation == null
    }

    override val title: Int
        get() = R.string.twilight_mode_pending_location

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        val footerPreference = preference as FooterPreference
        if (Flags.forceInvertColor()) {
            footerPreference.order =
                DarkModePreferenceOrderUtil.Order.LOCATION_CONNECTION_FOOTER.value
        }
    }

    companion object {
        const val KEY = "dark_theme_connection_footer"
    }
}
// LINT.ThenChange(DarkModePendingLocationPreferenceController.java)
