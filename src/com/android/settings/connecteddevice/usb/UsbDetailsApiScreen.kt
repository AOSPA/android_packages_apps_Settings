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
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.overlay.FeatureFactory
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Disallowed
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

        preference(
            key = PREFERENCE_KEY_CONVERT_VIDEOS_TO_AVC,
            purpose = R.string.usb_transcode_files_purpose,
            type = AnyBoolean,
        ) {
            preconditions(R.string.usb_transcode_files_preconditions) {
                val currentFunctions = usbDetailsRepository.getCurrentFunction()
                val isMtpEnabled = (currentFunctions and UsbManager.FUNCTION_MTP) != 0L
                val isPtpEnabled = (currentFunctions and UsbManager.FUNCTION_PTP) != 0L
                val isDataRole = usbDetailsRepository.getDataRole() == DATA_ROLE_DEVICE
                when {
                    isDataRole && (isMtpEnabled || isPtpEnabled) -> Allowed
                    else -> Disallowed(R.string.usb_transcode_files_unavailable)
                }
            }

            get { execute { usbDetailsRepository.isMtpTranscodeEnabled() } }

            set { execute { value -> usbDetailsRepository.setMtpTranscodeState(value) } }
        }
    }

    companion object {
        const val KEY = "usb_details_fragment"
        const val PREFERENCE_KEY_CONVERT_VIDEOS_TO_AVC = "usb_transcode_files"
    }
}
// LINT.ThenChange(UsbDetailsFragment.java)
