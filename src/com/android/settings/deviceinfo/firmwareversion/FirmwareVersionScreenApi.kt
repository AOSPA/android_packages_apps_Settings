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

package com.android.settings.deviceinfo.firmwareversion

import android.os.Build
import android.os.SystemProperties
import com.android.settings.R
import com.android.settings.deviceinfo.firmwareversion.BasebandVersionPreference.Companion.BASEBAND_PROPERTY
import com.android.settings.flags.Flags
import com.android.settingslib.DeviceInfoUtils
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.types.AnyString

@ProvidePreferenceScreen(FirmwareVersionScreenApi.KEY)
class FirmwareVersionScreenApi :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.ABOUT_DEVICE,
        fragment = FirmwareVersionSettings::class,
        purpose = R.string.firmware_version_purpose,
    ) {

    init {
        flag { Flags.catalystMigration26q2() }

        preference(
            key = OS_FIRMWARE_VERSION_KEY,
            purpose = R.string.os_firmware_version_purpose,
            type = AnyString,
        ) {
            get { execute { Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY } }
        }

        preference(
            key = BASEBAND_VERSION_KEY,
            purpose = R.string.base_band_purpose,
            type = AnyString,
        ) {
            get {
                execute {
                    SystemProperties.get(
                        BASEBAND_PROPERTY,
                        context.getString(R.string.device_info_default),
                    )
                }
            }
        }

        preference(
            key = KERNEL_VERSION_KEY,
            purpose = R.string.kernel_version_purpose,
            type = AnyString,
        ) {
            get { execute { DeviceInfoUtils.getFormattedKernelVersion(context) } }
        }

        preference(
            key = OS_BUILD_NUMBER_KEY,
            purpose = R.string.os_build_number_purpose,
            type = AnyString,
        ) {
            get { execute { Build.DISPLAY } }
        }
    }

    companion object {
        const val KEY = "firmware_version_screen"
        const val OS_FIRMWARE_VERSION_KEY = "os_firmware_version"
        const val BASEBAND_VERSION_KEY = "base_band"
        const val KERNEL_VERSION_KEY = "kernel_version"
        const val OS_BUILD_NUMBER_KEY = "os_build_number"
    }
}
