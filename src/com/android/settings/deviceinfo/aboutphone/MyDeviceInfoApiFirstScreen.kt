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

package com.android.settings.deviceinfo.aboutphone

import android.Manifest.permission.BLUETOOTH_ADMIN
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.NETWORK_SETTINGS
import android.Manifest.permission.OVERRIDE_WIFI_CONFIG
import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.os.Build
import android.os.SystemClock
import android.provider.Settings.Global.DEVICE_NAME
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.wifi.tether.WifiDeviceNameTextValidator
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.datastore.SettingsGlobalStore
import com.android.settingslib.datastore.and
import com.android.settingslib.metadata.MUSTPASS
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.preferencesapi.types.AnyString
import com.android.settingslib.utils.StringUtil

// LINT.IfChange
@ProvidePreferenceScreen(MyDeviceInfoApiFirstScreen.KEY)
class MyDeviceInfoApiFirstScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.ABOUT_DEVICE,
        fragment = MyDeviceInfoFragment::class,
        purpose = R.string.my_device_info_pref_screen_purpose_api,
        alreadyPartiallyMigrated = MyDeviceInfoScreen::class,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }
        preference(
            key = DEVICE_NAME_KEY,
            purpose = R.string.device_name_purpose,
            type = AnyString,
        ) {
            sensitivityLevel(SensitivityLevel.REQUIRES_CONFIRMATION)

            tags(MUSTPASS)
            preconditions(R.string.device_name_preconditions) {
                if (context.resources.getBoolean(R.bool.config_show_device_name)) {
                    Allowed
                } else {
                    HardwareUnsupported(R.string.device_name_not_available)
                }
            }
            get {
                execute { SettingsGlobalStore.get(context).getString(DEVICE_NAME) ?: Build.MODEL }
            }
            set {
                permissions(
                    Permissions.allOf(WRITE_SECURE_SETTINGS, BLUETOOTH_ADMIN, BLUETOOTH_CONNECT) and
                        Permissions.anyOf(OVERRIDE_WIFI_CONFIG, NETWORK_SETTINGS)
                )
                valuePreconditions(
                    "Valid device name is required. A valid name is any non-whitespace string between 1 and 16 characters."
                ) { value ->
                    val validator = WifiDeviceNameTextValidator()
                    if (!validator.isTextValid(value)) {
                        Custom("New device name is invalid. ${validator.getErrorMessage(value)}", stability = PreconditionStability.UNSTABLE)
                    } else {
                        Allowed
                    }
                }
                execute { value -> context.updateDeviceName(value) }
            }
        }

        preference(
            key = BUILD_NUMBER_KEY,
            purpose = R.string.my_device_info_build_number_purpose,
            type = AnyString,
        ) {
            sensitivityLevel(SensitivityLevel.DO_NOT_EXPOSE)

            get { execute { Build.DISPLAY } }
        }

        preference(
            key = UPTIME_KEY,
            purpose = R.string.my_device_info_uptime_purpose,
            type = AnyString,
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)

            get {
                execute {
                    val uptimeMillis = SystemClock.elapsedRealtime()
                    StringUtil.formatElapsedTime(context, uptimeMillis.toDouble(), false, false)
                        .toString()
                }
            }
        }
    }

    companion object {
        const val KEY = "api_my_device_info_pref_screen"
        const val DEVICE_NAME_KEY = "device_name"
        const val BUILD_NUMBER_KEY = "build_number"
        const val UPTIME_KEY = "uptime"
    }
}
// LINT.ThenChange(MyDeviceInfoFragment.java,
//                 MyDeviceInfoScreen.kt,
//                 ../DeviceNamePreferenceController.java,
//                 ../BuildNumberPreferenceController.java,
//                 ../UptimePreferenceController.java,
//                 ../PhoneNumberPreferenceController.java)
