/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.VibrationIntensitySettingsActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy

/** Accessibility settings for vibration intensities. */
// TODO(b/368360218): investigate if we still need this screen once we finish the migration.
//  We might be able to consolidate this into VibrationScreen with PreferenceHierarchy choosing
//  between toggle or slider preferences based on device config, depending on how overlays are done.
// LINT.IfChange
@ProvidePreferenceScreen(VibrationIntensityScreen.KEY)
open class VibrationIntensityScreen : PreferenceScreenMixin, PreferenceAvailabilityProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.accessibility_vibration_settings_title

    override val keywords: Int
        get() = R.string.keywords_vibration

    override fun getMetricsCategory() = SettingsEnums.ACCESSIBILITY_VIBRATION

    override fun isAvailable(context: Context) =
        context.hasVibrator && context.getSupportedVibrationIntensityLevels() > 1

    override val highlightMenuKey
        get() = R.string.menu_key_accessibility

    override fun isFlagEnabled(context: Context): Boolean = Flags.catalystVibrationIntensityScreen()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? =
        VibrationIntensitySettingsFragment::class.java

    override fun getPreferenceHierarchy(context: Context) =
        preferenceHierarchy(context, this) {
            +VibrationMainSwitchPreference()
            // The preferences below are migrated behind a different flag from the screen migration.
            // They should only be declared in this screen hierarchy if their migration is enabled.
            // TODO(b/420767832): Remove the Flags.deviceState() condition
            if (Flags.catalystVibrationIntensityScreen25q4() || Flags.deviceState()) {
                +CallVibrationPreferenceCategory() += {
                    +RingVibrationIntensitySliderPreference(context)
                }
                +NotificationAlarmVibrationPreferenceCategory() += {
                    +NotificationVibrationIntensitySliderPreference(context)
                    +AlarmVibrationIntensitySliderPreference(context)
                }
                +InteractiveHapticsPreferenceCategory() += {
                    +TouchVibrationIntensitySliderPreference(context)
                    +MediaVibrationIntensitySliderPreference(context)
                    +KeyboardVibrationSwitchPreference()
                }
            }
        }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        if (Flags.deviceState()) {
            makeLaunchIntent(context, VibrationIntensitySettingsActivity::class.java, metadata?.key)
        } else {
            super.getLaunchIntent(context, metadata)
        }

    companion object {
        const val KEY = "vibration_intensity_screen"
    }
}
// LINT.ThenChange(VibrationPreferenceController.java)
