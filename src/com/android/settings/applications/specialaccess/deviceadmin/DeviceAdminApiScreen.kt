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

package com.android.settings.applications.specialaccess.deviceadmin

import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported

// LINT.IfChange
@ProvidePreferenceScreen(DeviceAdminApiScreen.KEY)
class DeviceAdminApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.SECURITY,
        fragment = DeviceAdminSettings::class,
        purpose = R.string.device_admin_settings_screen_purpose,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }
        preconditions(R.string.device_admin_settings_screen_preconditions) {
            if (context.resources.getBoolean(R.bool.config_show_manage_device_admin)) {
                Allowed
            } else {
                HardwareUnsupported(R.string.device_admin_settings_screen_not_available)
            }
        }
    }

    companion object {
        const val KEY = "device_admin_settings"
    }
}
// LINT.ThenChange(DeviceAdminSettings.java,
//                 ../../../enterprise/ManageDeviceAdminPreferenceController.java)
