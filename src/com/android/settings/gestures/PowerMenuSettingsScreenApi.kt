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

package com.android.settings.gestures

import android.content.Context
import android.provider.Settings
import androidx.annotation.VisibleForTesting
import com.android.settings.R
import com.android.settings.flags.Flags.catalystMigration26q2
import com.android.settings.gestures.LongPressPowerSensitivityPreferenceController.closestValueIndex
import com.android.settingslib.datastore.SettingsGlobalStore
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported
import com.android.settingslib.metadata.preferencesapi.preconditions.InvalidPreference
import com.android.settingslib.metadata.preferencesapi.types.CustomEnum
import com.android.settingslib.metadata.preferencesapi.types.EType
import com.android.settingslib.metadata.preferencesapi.types.EnumApiWithRes
import com.android.settingslib.metadata.preferencesapi.types.GeneratedType
import com.android.settingslib.metadata.preferencesapi.types.GeneratedValue
import com.android.settingslib.metadata.preferencesapi.safe

@ProvidePreferenceScreen(PowerMenuSettingsScreenApi.KEY)
class PowerMenuSettingsScreenApi() :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.SYSTEM,
        fragment = PowerMenuSettings::class,
        purpose = R.string.long_press_power_pref_purpose,
    ) {
    init {
        flag { catalystMigration26q2() }

        preconditions(R.string.long_press_power_category_precondition) {
            if (PowerMenuSettingsUtils.isLongPressPowerSettingAvailable(context)) {
                Allowed
            } else {
                HardwareUnsupported(R.string.long_press_power_category_unavailable)
            }
        }
        preference(
            key = "gesture_power_menu_long_press_category",
            purpose = R.string.long_press_power_category_purpose,
            type = CustomEnum(
                LongPressPowerActions::class,
                R.string.long_press_power_actions_description
            ),
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)

            get {
                execute {
                    val invokeAssistant =
                        PowerMenuSettingsUtils.isLongPressPowerForAssistantEnabled(context)
                    if (invokeAssistant) {
                        LongPressPowerActions.ASSISTANT
                    } else {
                        LongPressPowerActions.POWER_MENU
                    }
                }
            }

            set {
                execute { value ->
                    when (value) {
                        LongPressPowerActions.POWER_MENU ->
                            PowerMenuSettingsUtils.setLongPressPowerForPowerMenu(context)
                        LongPressPowerActions.ASSISTANT ->
                            PowerMenuSettingsUtils.setLongPressPowerForAssistant(context)
                    }
                }
            }
        }

        preference(
            key = "gesture_power_menu_long_press_for_assist_sensitivity",
            purpose = R.string.long_press_power_assistant_sensitivity_purpose,
            type =
                GeneratedType(
                    description = R.string.long_press_power_assistant_sensitivity_ms_description,
                    unit = "ms",
                    key = "LongPressButtonTime",
                ) {
                    context.resources.getIntArray(DURATIONS_ARRAY_ID).map {
                        it.createSensitivityGeneratedValue(context)
                    }
                },
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)

            preconditions(R.string.long_press_power_sensitivity_precondition) {
                when {
                    !PowerMenuSettingsUtils.isLongPressPowerForAssistantEnabled(context) -> {
                        InvalidPreference(
                            otherPreferenceScreenKey = KEY,
                            otherPreferenceKey = "gesture_power_menu_long_press_category",
                            otherPreferenceParams = null,
                            reason =
                                R.string
                                    .long_press_power_sensitivity_precondition_set_for_power_button,
                        )
                    }
                    context.resources.getIntArray(DURATIONS_ARRAY_ID).size < 2 ->
                        HardwareUnsupported(R.string.long_press_power_sensitivity_no_options)
                    else -> Allowed
                }
            }
            get {
                execute {
                    val settingsValue: Int =
                        SettingsGlobalStore.get(context)
                            .getInt(Settings.Global.POWER_BUTTON_LONG_PRESS_DURATION_MS)
                            ?: context.resources.getInteger(
                                com.android.internal.R.integer.config_longPressOnPowerDurationMs
                            )
                    settingsValue
                }
            }
            set {
                execute { value ->
                    val values = context.resources.getIntArray(DURATIONS_ARRAY_ID)
                    val closestIndex = closestValueIndex(values, value)
                    SettingsGlobalStore.get(context)
                        .setInt(
                            Settings.Global.POWER_BUTTON_LONG_PRESS_DURATION_MS,
                            values[closestIndex],
                        )
                }
            }
        }
    }

    companion object {
        const val KEY = "long_press_power_pref_screen"
        private const val DURATIONS_ARRAY_ID =
            com.android.internal.R.array.config_longPressOnPowerDurationSettings
    }
}

private fun Int.createSensitivityGeneratedValue(context: Context): GeneratedValue<Int> {
    return GeneratedValue(
        this.safe(),
        context.getString(R.string.long_press_power_sensitivity_value_description, this).safe(),
    )
}

@VisibleForTesting
enum class LongPressPowerActions(override val asApiValue: Int, override val purpose: Int) :
    EnumApiWithRes<Int> {
    POWER_MENU(
        PowerMenuSettingsUtils.LONG_PRESS_POWER_GLOBAL_ACTIONS,
        R.string.power_menu_long_press_for_power_menu_title,
    ),
    ASSISTANT(
        PowerMenuSettingsUtils.LONG_PRESS_POWER_ASSISTANT_VALUE,
        R.string.power_menu_long_press_for_assistant_title,
    ),
}
