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

package com.android.settings.dream

import android.content.Context
import androidx.annotation.StringRes
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.dream.DreamBackend
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean
import com.android.settingslib.metadata.preferencesapi.types.CustomEnum
import com.android.settingslib.metadata.preferencesapi.types.EnumApiWithRes
import com.android.settingslib.metadata.preferencesapi.types.FiniteOptionsType
import com.android.settingslib.metadata.preferencesapi.types.EType
import com.android.settingslib.metadata.preferencesapi.safe
import com.android.settingslib.metadata.preferencesapi.SafetyAnnotated
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability

// LINT.IfChange
@ProvidePreferenceScreen(DreamSettingsApiScreen.KEY)
class DreamSettingsApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.DISPLAY,
        fragment = DreamSettings::class,
        purpose = R.string.screensaver_purpose,
        alreadyPartiallyMigrated = ScreensaverScreen::class,
    ) {

    enum class WhenToDream(
        override val asApiValue: Int,
        @param:StringRes @get:StringRes override val purpose: Int
    ) : EnumApiWithRes<Int> {
        WHILE_CHARGING(
            DreamBackend.WHILE_CHARGING,
            R.string.screensaver_settings_summary_sleep
        ),
        WHILE_DOCKED(
            DreamBackend.WHILE_DOCKED,
            R.string.screensaver_settings_summary_dock
        ),
        WHILE_CHARGING_OR_DOCKED(
            DreamBackend.WHILE_CHARGING_OR_DOCKED,
            R.string.screensaver_settings_summary_either_long
        ),
        WHILE_POSTURED(
            DreamBackend.WHILE_POSTURED,
            R.string.screensaver_settings_summary_postured
        ),
        NEVER(
            DreamBackend.NEVER,
            R.string.screensaver_settings_summary_never
        );

        companion object {
            fun from(backend: DreamBackend): WhenToDream {
                if (!backend.isEnabled) {
                    return NEVER
                }
                return entries.firstOrNull {
                    it.asApiValue == backend.whenToDreamSetting
                } ?: NEVER
            }
        }
    }

    private object WhenToDreamType : FiniteOptionsType<WhenToDream, Int> {
        override val externalType: EType<Int> = EType.Int

        override suspend fun getOptions(context: Context): List<Pair<SafetyAnnotated<Int>, SafetyAnnotated<String>>> {
            val validValues = DreamUtils.getWhenToDreamOptions(context.resources).toSet()
            val posturingSupported =
                context.resources.getBoolean(R.bool.config_posturing_supported)
            return WhenToDream.entries
                .filter { it.asApiValue in validValues }
                .filter { it != WhenToDream.WHILE_POSTURED || posturingSupported }
                .map { it.asApiValue.safe() to context.getString(it.purpose).safe() }
        }

        override fun getDescription(context: Context): String =
            context.getString(R.string.when_to_dream_description)

        override fun getKey(): String = "WhenToDream"

        override fun convertInternalToExternal(internalValue: WhenToDream): Int = internalValue.asApiValue

        override fun convertExternalToInternal(externalValue: Int): WhenToDream =
            WhenToDream.entries.firstOrNull { it.asApiValue == externalValue } ?: WhenToDream.NEVER
    }

    init {
        flag { Flags.catalystMigration26q2() }

        preference(
            key = PREF_KEY_WHEN_TO_DREAM,
            purpose = R.string.when_to_dream_purpose,
            type = WhenToDreamType,
        ) {
            preconditions(R.string.when_to_dream_preconditions) {
                if (context.resources.getBoolean(
                        com.android.internal.R.bool.config_dreamsShowWhenToDreamSetting)
                ) {
                    Allowed
                } else {
                    Custom(R.string.when_to_dream_unavailable, stability = PreconditionStability.STABLE_UNTIL_APK_UPDATE)
                }
            }

            get {
                execute {
                    WhenToDream.from(DreamBackend.getInstance(context))
                }
            }
            set {
                execute { whenToDreamSetting ->
                    val backend = DreamBackend.getInstance(context)
                    backend.setWhenToDream(whenToDreamSetting.asApiValue)
                }
            }
        }

        preference(
            key = PREF_KEY_LOW_LIGHT_MODE,
            purpose = R.string.low_light_mode_purpose,
            type = AnyBoolean,
        ) {
            flag { android.os.Flags.lowLightDreamBehavior() }

            preconditions(R.string.low_light_mode_preconditions) {
                if (android.os.Flags.lowLightDreamBehavior() &&
                    !DreamUtils.getLowLightBehaviors(context.resources).isEmpty()) {
                    Allowed
                } else {
                    Custom(R.string.screensaver_unavailable_due_to_mode, stability = PreconditionStability.UNSTABLE)
                }
            }

            get {
                execute {
                    DreamBackend.getInstance(context).lowLightDisplayBehaviorEnabled
                }
            }
            set {
                execute { value ->
                    DreamBackend.getInstance(context).lowLightDisplayBehaviorEnabled = value
                }
            }
        }
    }

    companion object {
        const val KEY = "api_screensaver"
        const val PREF_KEY_WHEN_TO_DREAM = "when_to_start"
        const val PREF_KEY_LOW_LIGHT_MODE = "low_light_mode"
    }
}
// LINT.ThenChange(DreamSettings.java)
