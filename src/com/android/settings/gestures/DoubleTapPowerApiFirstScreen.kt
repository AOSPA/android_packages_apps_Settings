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

package com.android.settings.gestures

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.provider.Settings
import android.service.quickaccesswallet.Flags.launchWalletOptionOnPowerDoubleTap
import android.service.quickaccesswallet.QuickAccessWalletClient
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.preferencesapi.types.CustomEnum
import com.android.settingslib.metadata.preferencesapi.types.EType
import com.android.settingslib.metadata.preferencesapi.types.EnumApiWithRes

// LINT.IfChange
@ProvidePreferenceScreen(DoubleTapPowerApiFirstScreen.KEY)
class DoubleTapPowerApiFirstScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.SYSTEM,
        fragment = DoubleTapPowerSettings::class,
        purpose = R.string.gesture_double_tap_power_input_summary_purpose_api,
        alreadyPartiallyMigrated = DoubleTapPowerScreen::class,
    ) {
    init {
        flag { Flags.catalystMigration26q2() }
        preconditions(R.string.api_double_tap_power_screen_preconditions) {
            if (!launchWalletOptionOnPowerDoubleTap()) {
                HardwareUnsupported(R.string.api_double_tap_power_screen_wallet_feature_unsupported)
            } else {
                if (
                    DoubleTapPowerSettingsUtils.isMultiTargetDoubleTapPowerButtonGestureAvailable(
                        context
                    )
                ) {
                    Allowed
                } else {
                    HardwareUnsupported(R.string.api_double_tap_power_screen_gesture_unsupported)
                }
            }
        }
        preference(
            key = RADIO_PREFERENCE_KEY,
            purpose = R.string.gesture_double_power_tap_app_purpose,
            type =
                CustomEnum(
                    PowerButtonLaunchApp::class,
                    R.string.gesture_double_power_tap_app_custom_enum_description,
                ),
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)
            get {
                execute {
                    val launchApp =
                        Settings.Secure.getInt(
                            context.contentResolver,
                            Settings.Secure.DOUBLE_TAP_POWER_BUTTON_GESTURE,
                            context.resources.getInteger(
                                com.android.internal.R.integer
                                    .config_doubleTapPowerGestureMultiTargetDefaultAction
                            ),
                        )
                    if (launchApp == CAMERA_LAUNCH_VALUE) {
                        PowerButtonLaunchApp.CAMERA
                    } else {
                        PowerButtonLaunchApp.WALLET
                    }
                }
            }
            set {
                permissions(WRITE_SECURE_SETTINGS)
                preconditions(R.string.gesture_double_power_tap_app_set_preconditions) {
                    val isDoubleTapPowerButtonGestureEnabled =
                        DoubleTapPowerSettingsUtils.isDoubleTapPowerButtonGestureEnabled(context)
                    val isWalletServiceAvailable =
                        QuickAccessWalletClient.create(context).isWalletServiceAvailable
                    if (isDoubleTapPowerButtonGestureEnabled && isWalletServiceAvailable) {
                        Allowed
                    } else {
                        if (!isDoubleTapPowerButtonGestureEnabled) {
                            Custom(
                                R.string.gesture_double_power_tap_app_set_gesture_disabled,
                                stability = PreconditionStability.UNSTABLE,
                            )
                        } else {
                            Custom(
                                R.string
                                    .gesture_double_power_tap_app_set_wallet_service_not_available,
                                stability = PreconditionStability.UNSTABLE,
                            )
                        }
                    }
                }
                execute { value ->
                    val launchApp =
                        if (value == PowerButtonLaunchApp.CAMERA) {
                            CAMERA_LAUNCH_VALUE
                        } else {
                            WALLET_LAUNCH_VALUE
                        }
                    Settings.Secure.putInt(
                        context.contentResolver,
                        Settings.Secure.DOUBLE_TAP_POWER_BUTTON_GESTURE,
                        launchApp,
                    )
                }
            }
        }
    }

    companion object {
        const val KEY = "api_double_tap_power_screen"
        internal const val RADIO_PREFERENCE_KEY = "gesture_double_power_tap_app"

        internal const val CAMERA_LAUNCH_VALUE = 0
        internal const val WALLET_LAUNCH_VALUE = 1
    }
}

// LINT.ThenChange(
//     DoubleTapPowerPreferenceController.java,
//     DoubleTapPowerSettings.java,
//     DoubleTapPowerForCameraPreferenceController.java,
//     DoubleTapPowerForWalletPreferenceController.java,
// )

internal enum class PowerButtonLaunchApp(override val asApiValue: Int, override val purpose: Int) :
    EnumApiWithRes<Int> {
    CAMERA(
        DoubleTapPowerApiFirstScreen.CAMERA_LAUNCH_VALUE,
        R.string.double_tap_power_camera_action_title,
    ),
    WALLET(
        DoubleTapPowerApiFirstScreen.WALLET_LAUNCH_VALUE,
        R.string.double_tap_power_wallet_action_title,
    ),
}
