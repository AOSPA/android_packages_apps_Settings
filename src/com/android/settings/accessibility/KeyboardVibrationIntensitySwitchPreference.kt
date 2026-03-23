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
package com.android.settings.accessibility

import android.content.Context
import android.os.VibrationAttributes
import android.os.vibrator.Flags
import android.provider.Settings.System.KEYBOARD_VIBRATION_INTENSITY
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.ReadWritePermit

/** Accessibility settings for keyboard vibration, as a switch toggle. */
// LINT.IfChange
class KeyboardVibrationIntensitySwitchPreference(
    context: Context,
    key: String,
    purpose: Int,
    mainSwitchPreferenceKey: String,
) :
    VibrationIntensitySwitchPreference(
        context = context,
        key = key,
        purpose = purpose,
        settingsProviderKey = KEYBOARD_VIBRATION_INTENSITY,
        mainSwitchPreferenceKey = mainSwitchPreferenceKey,
        vibrationUsage = VibrationAttributes.USAGE_IME_FEEDBACK,
        title = R.string.accessibility_keyboard_vibration_title,
    ),
    PreferenceAvailabilityProvider {
    override val keywords: Int
        get() = R.string.keywords_keyboard_vibration

    override fun getWritePermit(
        context: Context,
        value: Boolean?,
        callingPid: Int,
        callingUid: Int,
    ) = ReadWritePermit.ALLOW

    override val supportsWrite = true

    override val availabilityDescription =
        "The device must support keyboard vibration intensity settings."

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context): Boolean {
        val isVibrationSupported = context.resources.getBoolean(
            com.android.internal.R.bool.config_keyboardVibrationSettingsSupported
        )
        val isIntensitySupported = context.resources.getBoolean(
            com.android.internal.R.bool.config_keyboardVibrationSettingsIntensitySupported
        )
        return isVibrationSupported && isIntensitySupported && Flags.keyboardIntensitySliderEnabled()
    }
}
// LINT.ThenChange(KeyboardVibrationIntensitySwitchPreferenceController.java)
