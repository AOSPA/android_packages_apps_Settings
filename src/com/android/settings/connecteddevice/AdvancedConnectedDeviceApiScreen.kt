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

package com.android.settings.connecteddevice

import android.Manifest.permission
import android.content.Context
import android.os.UserManager
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.network.AirplaneModePreference
import com.android.settings.network.AirplaneModeSettingsScreen
import com.android.settings.uwb.UwbPreferenceController
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.EnterpriseRestriction
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported
import com.android.settingslib.metadata.preferencesapi.preconditions.InvalidPreference
import com.android.settingslib.metadata.preferencesapi.preconditions.RegionalRestriction
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean

// LINT.IfChange
@ProvidePreferenceScreen(AdvancedConnectedDeviceApiScreen.KEY)
class AdvancedConnectedDeviceApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.CONNECTED_DEVICES,
        fragment = AdvancedConnectedDeviceDashboardFragment::class,
        purpose = R.string.connection_preferences_purpose,
        alreadyPartiallyMigrated = AdvancedConnectedDeviceScreen::class,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }

        preference(
            key = UWB_KEY,
            type = AnyBoolean,
            purpose = R.string.connection_preferences_uwb_purpose,
        ) {
            preconditions(R.string.uwb_settings_general_precondition) {
                if (isUwbUserRestricted(context)) {
                    EnterpriseRestriction(R.string.uwb_settings_unavailable_user_restriction)
                } else {
                    val uwbController = getUwbController(context)
                    when (uwbController.availabilityStatus) {
                        UwbPreferenceController.UNSUPPORTED_ON_DEVICE ->
                            HardwareUnsupported(R.string.uwb_settings_unavailable_hardware)
                        UwbPreferenceController.CONDITIONALLY_UNAVAILABLE ->
                            RegionalRestriction(R.string.uwb_settings_unavailable_regulation)
                        else -> Allowed
                    }
                }
            }
            get { execute { getUwbController(context).isChecked } }
            set {
                permissions(permission.UWB_PRIVILEGED)
                preconditions(R.string.uwb_settings_set_precondition) {
                    val uwbController = getUwbController(context)
                    when (uwbController.availabilityStatus) {
                        UwbPreferenceController.DISABLED_DEPENDENT_SETTING ->
                            InvalidPreference(
                                AirplaneModeSettingsScreen.KEY,
                                AirplaneModePreference.KEY,
                                R.string.uwb_settings_unavailable_airplane_mode,
                            )
                        else -> Allowed
                    }
                }
                execute { value -> getUwbController(context).setChecked(value) }
            }
        }
    }

    companion object {
        const val KEY = "api_connection_preferences"
        const val UWB_KEY = "uwb_settings"

        private fun getUwbController(context: Context) = UwbPreferenceController(context, UWB_KEY)

        private fun isUwbUserRestricted(context: Context): Boolean {
            val userManager = context.getSystemService(UserManager::class.java)
            return userManager?.hasUserRestriction(UserManager.DISALLOW_ULTRA_WIDEBAND_RADIO) ==
                true
        }
    }
}
// LINT.ThenChange(AdvancedConnectedDeviceDashboardFragment.java,
// AdvancedConnectedDeviceController.java)
