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

package com.android.settings.connecteddevice.usb

import android.hardware.usb.UsbManager
import android.hardware.usb.UsbPortStatus.DATA_ROLE_DEVICE
import android.hardware.usb.UsbPortStatus.POWER_ROLE_NONE
import android.hardware.usb.UsbPortStatus.POWER_ROLE_SINK
import android.hardware.usb.UsbPortStatus.POWER_ROLE_SOURCE
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.overlay.FeatureFactory
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean

// LINT.IfChange
@ProvidePreferenceScreen(UsbDetailsApiScreen.KEY)
class UsbDetailsApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.CONNECTED_DEVICES,
        fragment = UsbDetailsFragment::class,
        purpose = R.string.usb_details_fragment_purpose,
    ) {
    private val usbDetailsRepository =
        FeatureFactory.featureFactory.usbFeatureProvider.usbDetailsRepository

    init {
        flag { Flags.catalystMigration26q2() }

        // TODO(b/480956777): add api for "Connected devices" preference.

        preference(
            key = PREFERENCE_KEY_CONVERT_VIDEOS_TO_AVC,
            purpose = R.string.usb_transcode_files_purpose,
            type = AnyBoolean,
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)

            preconditions(R.string.usb_transcode_files_preconditions) {
                val currentFunctions = usbDetailsRepository.getCurrentFunction()
                val isMtpEnabled = (currentFunctions and UsbManager.FUNCTION_MTP) != 0L
                val isPtpEnabled = (currentFunctions and UsbManager.FUNCTION_PTP) != 0L
                val isDataRole = usbDetailsRepository.getDataRole() == DATA_ROLE_DEVICE
                when {
                    isDataRole && (isMtpEnabled || isPtpEnabled) -> Allowed
                    else -> Custom(R.string.usb_transcode_files_unavailable, stability = PreconditionStability.UNSTABLE)
                }
            }

            get { execute { usbDetailsRepository.isMtpTranscodeEnabled() } }

            set { execute { value -> usbDetailsRepository.setMtpTranscodeState(value) } }
        }

        preference(
            key = PREFERENCE_USB_DETAILS_POWER_ROLE,
            purpose = R.string.usb_charge_connected_device,
            type = AnyBoolean,
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)

            preconditions(R.string.usb_charge_connected_device_preconditions) {
                if (
                    usbDetailsRepository.powerRole != POWER_ROLE_NONE &&
                        usbDetailsRepository.isSupportAllRoles
                )
                    Allowed
                else
                    Custom(
                        R.string.usb_charge_connected_device_unavailable,
                        stability = PreconditionStability.UNSTABLE,
                    )
            }

            get { execute { usbDetailsRepository.powerRole == POWER_ROLE_SOURCE } }

            set {
                execute { value ->
                    if (value) usbDetailsRepository.powerRole = POWER_ROLE_SOURCE
                    else usbDetailsRepository.powerRole = POWER_ROLE_SINK
                }
            }
        }
    }

    companion object {
        const val KEY = "usb_details_fragment"
        const val PREFERENCE_KEY_CONVERT_VIDEOS_TO_AVC = "usb_transcode_files"
        const val PREFERENCE_USB_DETAILS_POWER_ROLE = "usb_details_power_role"
    }
}
// LINT.ThenChange(UsbDetailsFragment.java)
