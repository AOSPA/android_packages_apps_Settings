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
import android.os.Vibrator
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.VibrationSettingsActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceCategory
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

/** Accessibility settings for vibration. */
// LINT.IfChange
@ProvidePreferenceScreen(VibrationScreen.KEY)
open class VibrationScreen : PreferenceScreenMixin, PreferenceAvailabilityProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.accessibility_vibration_settings_title

    override val keywords: Int
        get() = R.string.keywords_vibration

    override fun getMetricsCategory()= SettingsEnums.ACCESSIBILITY_VIBRATION

    override fun isAvailable(context: Context) =
        context.hasVibrator && context.getSupportedVibrationIntensityLevels() == 1

    override val highlightMenuKey
        get() = R.string.menu_key_accessibility

    override fun isFlagEnabled(context: Context): Boolean = Flags.catalystVibrationIntensityScreen()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = VibrationSettings::class.java

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +VibrationMainSwitchPreference()
            // The preferences below are migrated behind a different flag from the screen migration.
            // They should only be declared in this screen hierarchy if their migration is enabled.
            if (Flags.catalystVibrationIntensityScreen25q4()) {
                +CallVibrationPreferenceCategory() += {
                    +RingVibrationIntensitySwitchPreference(context)
                    +RampingRingerVibrationSwitchPreference(context)
                }
                +NotificationAlarmVibrationPreferenceCategory() += {
                    +NotificationVibrationIntensitySwitchPreference(context)
                    +AlarmVibrationIntensitySwitchPreference(context)
                }
                +InteractiveHapticsPreferenceCategory() += {
                    +TouchVibrationIntensitySwitchPreference(context)
                    +MediaVibrationIntensitySwitchPreference(context)
                    +KeyboardVibrationSwitchPreference()
                }
            }
        }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, VibrationSettingsActivity::class.java, metadata?.key)

    companion object {
        const val KEY = "vibration_screen"
    }
}

/** Call vibration preferences (e.g. ringtone, ramping ringer, etc). */
class CallVibrationPreferenceCategory :
    PreferenceCategory(
        "vibration_category_call",
        R.string.accessibility_call_vibration_category_title,
    )

/** Notification and alarm vibration preferences. */
class NotificationAlarmVibrationPreferenceCategory :
    PreferenceCategory(
        "vibration_category_notification_alarm",
        R.string.accessibility_notification_alarm_vibration_category_title,
    )

/** Interactive haptics preferences (e.g. touch feedback, media, keyboard, etc). */
class InteractiveHapticsPreferenceCategory :
    PreferenceCategory(
        "vibration_category_haptics",
        R.string.accessibility_interactive_haptics_category_title,
    )

/** Returns true if the device has a system vibrator, false otherwise. */
val Context.hasVibrator: Boolean
    get() = getSystemService(Vibrator::class.java)?.hasVibrator() == true

// LINT.ThenChange(VibrationPreferenceController.java)
