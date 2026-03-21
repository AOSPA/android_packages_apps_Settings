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

package com.android.settings.notification.modes

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.app.ActivityManager
import android.provider.Settings
import android.service.notification.ZenModeConfig
import android.service.notification.ZenModeConfig.MANUAL_RULE_ID
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.overlay.FeatureFactory
import com.android.settingslib.R as LibR
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.types.CustomEnum
import com.android.settingslib.metadata.preferencesapi.types.EnumApiWithString
import com.android.settingslib.notification.modes.ZenModesBackend
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability

// LINT.IfChange
@ProvidePreferenceScreen(ZenModeApiScreen.KEY, parameterized = true)
class ZenModeApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.PRIORITY_MODES,
        fragment = ZenModeFragment::class,
        purpose = R.string.zen_mode_screen_purpose,
    ) {

    init {
        flag { Flags.catalystMigration26q2() }
        tags(APP_FUNCTION_NOTIFICATIONS)

        parameters {
            parameter(
                name = MODE_NAME,
                purpose = R.string.zen_mode_screen_parameter_purpose,
                required = true,
                type = ZenModes,
            )

            prepareScreenExtras { keyParameters, extras ->
                val modeName = keyParameters[MODE_NAME]
                val modes = getModes()
                modes
                    .find { it.name == modeName }
                    ?.let { extras.putString(Settings.EXTRA_AUTOMATIC_ZEN_RULE_ID, it.id) }
                    ?: Error("Can't find mode with name $modeName")
            }
        }

        preference(
            key = RADIO_SELECTOR_KEY,
            purpose = R.string.mode_manual_duration_purpose,
            type = CustomEnum(DurationType::class, R.string.mode_manual_duration_enum_description),
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)
            preconditions(R.string.mode_manual_duration_precondition) {
                val modeName = keyParameters?.getRequired(MODE_NAME)
                if (getModes().any { it.name == modeName && it.id == MANUAL_RULE_ID }) {
                    Allowed
                } else {
                    Custom(
                        R.string.mode_manual_duration_not_dnd_mode,
                        stability = PreconditionStability.UNSTABLE)
                }
            }
            get {
                execute {
                    val zenDuration =
                        Settings.Secure.getInt(
                            context.contentResolver,
                            Settings.Secure.ZEN_DURATION,
                            Settings.Secure.ZEN_DURATION_FOREVER,
                        )
                    when (zenDuration) {
                        Settings.Secure.ZEN_DURATION_FOREVER -> DurationType.FOREVER
                        COUNTDOWN_15 -> DurationType.COUNTDOWN_15
                        COUNTDOWN_30 -> DurationType.COUNTDOWN_30
                        COUNTDOWN_45 -> DurationType.COUNTDOWN_45
                        COUNTDOWN_1_HR -> DurationType.COUNTDOWN_1_HR
                        COUNTDOWN_2_HRS -> DurationType.COUNTDOWN_2_HRS
                        COUNTDOWN_3_HRS -> DurationType.COUNTDOWN_3_HRS
                        COUNTDOWN_4_HRS -> DurationType.COUNTDOWN_4_HRS
                        COUNTDOWN_5_HRS -> DurationType.COUNTDOWN_5_HRS
                        COUNTDOWN_6_HRS -> DurationType.COUNTDOWN_6_HRS
                        COUNTDOWN_7_HRS -> DurationType.COUNTDOWN_7_HRS
                        COUNTDOWN_8_HRS -> DurationType.COUNTDOWN_8_HRS
                        COUNTDOWN_9_HRS -> DurationType.COUNTDOWN_9_HRS
                        COUNTDOWN_10_HRS -> DurationType.COUNTDOWN_10_HRS
                        COUNTDOWN_11_HRS -> DurationType.COUNTDOWN_11_HRS
                        COUNTDOWN_12_HRS -> DurationType.COUNTDOWN_12_HRS
                        else -> DurationType.ALWAYSASK
                    }
                }
            }
            set {
                permissions(WRITE_SECURE_SETTINGS)
                execute { value ->
                    Settings.Secure.putInt(
                        context.contentResolver,
                        Settings.Secure.ZEN_DURATION,
                        value.asApiValue,
                    )
                }
            }
        }
    }

    private fun getModes() = ZenModesBackend.getInstance(FeatureFactory.appContext).modes

    companion object {
        const val KEY = "zen_mode_screen"
        const val MODE_NAME = "MODE_NAME"
        internal const val RADIO_SELECTOR_KEY = "mode_manual_duration"
        internal const val FOREVER = Settings.Secure.ZEN_DURATION_FOREVER
        internal const val ALWAYS_ASK = Settings.Secure.ZEN_DURATION_PROMPT
        internal const val COUNTDOWN_15 = 15
        internal const val COUNTDOWN_30 = 30
        internal const val COUNTDOWN_45 = 45
        internal const val COUNTDOWN_1_HR = 60
        internal const val COUNTDOWN_2_HRS = 120
        internal const val COUNTDOWN_3_HRS = 180
        internal const val COUNTDOWN_4_HRS = 240
        internal const val COUNTDOWN_5_HRS = 300
        internal const val COUNTDOWN_6_HRS = 360
        internal const val COUNTDOWN_7_HRS = 420
        internal const val COUNTDOWN_8_HRS = 480
        internal const val COUNTDOWN_9_HRS = 540
        internal const val COUNTDOWN_10_HRS = 600
        internal const val COUNTDOWN_11_HRS = 660
        internal const val COUNTDOWN_12_HRS = 720

        // Returns a human-readable string for the given Zen Mode duration.
        //
        // Uses logic from [ZenModeConfig] and [DndDurationDialogFactory] to
        // determine the appropriate string representation.
        internal fun getDurationString(duration: Int): String {
            val context = FeatureFactory.appContext
            return when (duration) {
                FOREVER -> context.getString(LibR.string.zen_mode_forever)
                ALWAYS_ASK -> context.getString(LibR.string.zen_mode_duration_always_prompt_title)
                else -> {
                    ZenModeConfig.toTimeCondition(
                            context,
                            duration,
                            ActivityManager.getCurrentUser(),
                            false,
                        )
                        .line1
                }
            }
        }
    }
}

// LINT.ThenChange(ZenModeFragment.java,
//                 ManualDurationPreferenceController.java,
//                 /frameworks/base/packages/SettingsLib/src/com/android/settingslib/notification/
//                     modes/DndDurationDialogFactory.java)

/**
 * Total of 17 countdown options:
 * - 15 duration-based (15/30/45 min, and 1-12 hours).
 * - 2 functional types (FOREVER, ALWAYS_ASK).
 */
internal enum class DurationType(override val asApiValue: Int, override val purpose: String) :
    EnumApiWithString<Int> {
    FOREVER(ZenModeApiScreen.FOREVER, ZenModeApiScreen.getDurationString(ZenModeApiScreen.FOREVER)),
    ALWAYSASK(
        ZenModeApiScreen.ALWAYS_ASK,
        ZenModeApiScreen.getDurationString(ZenModeApiScreen.ALWAYS_ASK),
    ),
    COUNTDOWN_15(
        ZenModeApiScreen.COUNTDOWN_15,
        ZenModeApiScreen.getDurationString(ZenModeApiScreen.COUNTDOWN_15),
    ),
    COUNTDOWN_30(
        ZenModeApiScreen.COUNTDOWN_30,
        ZenModeApiScreen.getDurationString(ZenModeApiScreen.COUNTDOWN_30),
    ),
    COUNTDOWN_45(
        ZenModeApiScreen.COUNTDOWN_45,
        ZenModeApiScreen.getDurationString(ZenModeApiScreen.COUNTDOWN_45),
    ),
    COUNTDOWN_1_HR(
        ZenModeApiScreen.COUNTDOWN_1_HR,
        ZenModeApiScreen.getDurationString(ZenModeApiScreen.COUNTDOWN_1_HR),
    ),
    COUNTDOWN_2_HRS(
        ZenModeApiScreen.COUNTDOWN_2_HRS,
        ZenModeApiScreen.getDurationString(ZenModeApiScreen.COUNTDOWN_2_HRS),
    ),
    COUNTDOWN_3_HRS(
        ZenModeApiScreen.COUNTDOWN_3_HRS,
        ZenModeApiScreen.getDurationString(ZenModeApiScreen.COUNTDOWN_3_HRS),
    ),
    COUNTDOWN_4_HRS(
        ZenModeApiScreen.COUNTDOWN_4_HRS,
        ZenModeApiScreen.getDurationString(ZenModeApiScreen.COUNTDOWN_4_HRS),
    ),
    COUNTDOWN_5_HRS(
        ZenModeApiScreen.COUNTDOWN_5_HRS,
        ZenModeApiScreen.getDurationString(ZenModeApiScreen.COUNTDOWN_5_HRS),
    ),
    COUNTDOWN_6_HRS(
        ZenModeApiScreen.COUNTDOWN_6_HRS,
        ZenModeApiScreen.getDurationString(ZenModeApiScreen.COUNTDOWN_6_HRS),
    ),
    COUNTDOWN_7_HRS(
        ZenModeApiScreen.COUNTDOWN_7_HRS,
        ZenModeApiScreen.getDurationString(ZenModeApiScreen.COUNTDOWN_7_HRS),
    ),
    COUNTDOWN_8_HRS(
        ZenModeApiScreen.COUNTDOWN_8_HRS,
        ZenModeApiScreen.getDurationString(ZenModeApiScreen.COUNTDOWN_8_HRS),
    ),
    COUNTDOWN_9_HRS(
        ZenModeApiScreen.COUNTDOWN_9_HRS,
        ZenModeApiScreen.getDurationString(ZenModeApiScreen.COUNTDOWN_9_HRS),
    ),
    COUNTDOWN_10_HRS(
        ZenModeApiScreen.COUNTDOWN_10_HRS,
        ZenModeApiScreen.getDurationString(ZenModeApiScreen.COUNTDOWN_10_HRS),
    ),
    COUNTDOWN_11_HRS(
        ZenModeApiScreen.COUNTDOWN_11_HRS,
        ZenModeApiScreen.getDurationString(ZenModeApiScreen.COUNTDOWN_11_HRS),
    ),
    COUNTDOWN_12_HRS(
        ZenModeApiScreen.COUNTDOWN_12_HRS,
        ZenModeApiScreen.getDurationString(ZenModeApiScreen.COUNTDOWN_12_HRS),
    ),
}
