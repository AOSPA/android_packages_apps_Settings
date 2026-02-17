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

package com.android.settings.accessibility.timingcontrols.ui

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.TapAssistanceFragment
import com.android.settings.accessibility.actiontimeout.ui.ActionTimeoutSettingsScreen
import com.android.settings.core.PreferenceScreenMixin
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED

/** Provides the preference screen for Timing Controls settings. */
@ProvidePreferenceScreen(TimingControlsScreen.KEY)
open class TimingControlsScreen : PreferenceScreenMixin {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_UNCATEGORIZED)

    override val highlightMenuKey: Int
        get() = R.string.menu_key_accessibility

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.a11y_timing_controls_screen_purpose

    override val title: Int
        get() = R.string.accessibility_tap_assistance_title

    override val summary: Int
        get() = R.string.accessibility_tap_assistance_subtext

    override val icon: Int
        get() = R.drawable.ic_tap_assistance

    override fun isFlagEnabled(context: Context): Boolean = Flags.catalystTimingControlsScreen()

    override val indexable: Boolean
        get() = true

    override fun fragmentClass(): Class<out Fragment>? {
        return TapAssistanceFragment::class.java
    }

    override fun getMetricsCategory(): Int = SettingsEnums.ACCESSIBILITY_TAP_ASSISTANCE

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +LongPressTimeoutPreference(context)
            +ActionTimeoutSettingsScreen.KEY
        }

    companion object {
        const val KEY = "tap_assistance_preference_screen"
    }
}
