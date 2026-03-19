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

package com.android.settings.safetycenter.ui

import android.hardware.SensorPrivacyManager
import android.provider.Settings
import com.android.settings.R
import com.android.settings.utils.SensorPrivacyManagerHelper
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean

/**
 * API definition for the Privacy Controls screen. This class exposes the screen and its preferences
 * to the Settings API framework.
 */
@ProvidePreferenceScreen(PrivacyControlsScreenApi.KEY)
class PrivacyControlsScreenApi :
    SubpageScreenApi(
        key = KEY,
        topLevelSettingsCategory = Category.SAFETY_CENTER,
        fragment = PrivacyControlsFragment::class,
        purpose = R.string.privacy_controls_screen_purpose,
        subpageRegistryKey = SafetyCenterSubpageRegistry.PRIVACY_CONTROLS_SUBPAGE_KEY,
    ) {

    init {
        // Camera Access toggle
        preference(
            key = "privacy_camera_toggle",
            purpose = R.string.privacy_camera_toggle_purpose,
            type = AnyBoolean,
        ) {
            sensitivityLevel(SensitivityLevel.REQUIRES_CONFIRMATION)

            preconditions(R.string.sensor_toggle_unsupported) {
                val helper = SensorPrivacyManagerHelper.getInstance(context)!!
                if (helper.supportsSensorToggle(SensorPrivacyManager.Sensors.CAMERA)) {
                    Allowed
                } else {
                    HardwareUnsupported(R.string.sensor_toggle_unsupported)
                }
            }
            get {
                execute {
                    val helper = SensorPrivacyManagerHelper.getInstance(context)!!
                    !helper.isSensorBlocked(sensor = SensorPrivacyManager.Sensors.CAMERA)
                }
            }
            set {
                execute { value ->
                    val helper = SensorPrivacyManagerHelper.getInstance(context)!!
                    helper.setSensorBlocked(SensorPrivacyManager.Sensors.CAMERA, !value)
                }
            }
        }

        // Microphone Access toggle
        preference(
            key = "privacy_mic_toggle",
            purpose = R.string.privacy_mic_toggle_purpose,
            type = AnyBoolean,
        ) {
            sensitivityLevel(SensitivityLevel.REQUIRES_CONFIRMATION)

            preconditions(R.string.sensor_toggle_unsupported) {
                val helper = SensorPrivacyManagerHelper.getInstance(context)!!
                if (helper.supportsSensorToggle(SensorPrivacyManager.Sensors.MICROPHONE)) {
                    Allowed
                } else {
                    HardwareUnsupported(R.string.sensor_toggle_unsupported)
                }
            }
            get {
                execute {
                    val helper = SensorPrivacyManagerHelper.getInstance(context)!!
                    !helper.isSensorBlocked(sensor = SensorPrivacyManager.Sensors.MICROPHONE)
                }
            }
            set {
                execute { value ->
                    val helper = SensorPrivacyManagerHelper.getInstance(context)!!
                    helper.setSensorBlocked(SensorPrivacyManager.Sensors.MICROPHONE, !value)
                }
            }
        }

        // Show clipboard access notification toggle
        preference(
            key = "show_clip_access_notification",
            purpose = R.string.show_clip_access_notification_purpose,
            type = AnyBoolean,
        ) {
            sensitivityLevel(SensitivityLevel.REQUIRES_CONFIRMATION)

            get {
                execute {
                    Settings.Secure.getInt(
                        context.contentResolver,
                        Settings.Secure.CLIPBOARD_SHOW_ACCESS_NOTIFICATIONS,
                        0,
                    ) != 0
                }
            }
            set {
                execute { value ->
                    Settings.Secure.putInt(
                        context.contentResolver,
                        Settings.Secure.CLIPBOARD_SHOW_ACCESS_NOTIFICATIONS,
                        if (value) 1 else 0,
                    )
                }
            }
        }

        // Show passwords toggle
        preference(
            key = "show_password",
            purpose = R.string.show_password_purpose,
            type = AnyBoolean,
        ) {
            sensitivityLevel(SensitivityLevel.REQUIRES_CONFIRMATION)

            get {
                execute {
                    Settings.System.getInt(
                        context.contentResolver,
                        Settings.System.TEXT_SHOW_PASSWORD,
                        1,
                    ) != 0
                }
            }
            set {
                execute { value ->
                    Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.TEXT_SHOW_PASSWORD,
                        if (value) 1 else 0,
                    )
                }
            }
        }

        // Health Connect deeplink
        preference(
            key = "health_connect",
            purpose = R.string.health_connect_purpose,
            type = AnyBoolean,
        ) {
            preconditions(R.string.safety_source_unavailable) {
                if (isSafetySourceVisible(context, "AndroidHealthConnect")) {
                    Allowed
                } else {
                    Custom(
                        R.string.safety_source_unavailable,
                        stability = PreconditionStability.UNSTABLE,
                    )
                }
            }
            get { execute { true } }
        }

        // App function access deeplink
        preference(
            key = "app_function_access",
            purpose = R.string.app_function_access_purpose,
            type = AnyBoolean,
        ) {
            preconditions(R.string.safety_source_unavailable) {
                if (isSafetySourceVisible(context, "AndroidAppFunctionAccess")) {
                    Allowed
                } else {
                    Custom(
                        R.string.safety_source_unavailable,
                        stability = PreconditionStability.UNSTABLE,
                    )
                }
            }
            get { execute { true } }
        }

        // Data sharing updates for location deeplink
        preference(
            key = "location_data_sharing_updates",
            purpose = R.string.location_data_sharing_updates_purpose,
            type = AnyBoolean,
        ) {
            preconditions(R.string.safety_source_unavailable) {
                if (isSafetySourceVisible(context, "AndroidPrivacyAppDataSharingUpdates")) {
                    Allowed
                } else {
                    Custom(
                        R.string.safety_source_unavailable,
                        stability = PreconditionStability.UNSTABLE,
                    )
                }
            }
            get { execute { true } }
        }

        // Location access deeplink
        preference(
            key = "privacy_location_access",
            purpose = R.string.privacy_location_access_purpose,
            type = AnyBoolean,
        ) {
            get { execute { true } }
        }
    }

    companion object {
        const val KEY = "privacy_controls_screen"
    }
}
