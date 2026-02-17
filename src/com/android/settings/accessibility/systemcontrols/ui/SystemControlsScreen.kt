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
package com.android.settings.accessibility.systemcontrols.ui

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.gestures.OneHandedSettingsUtils
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED

// LINT.IfChange
@ProvidePreferenceScreen(SystemControlsScreen.KEY)
open class SystemControlsScreen : PreferenceScreenMixin {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_UNCATEGORIZED)

    override val key: String
        get() = KEY

    // TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.system_controls_preference_screen_purpose

    override val title: Int
        get() = R.string.accessibility_system_controls_title

    override val highlightMenuKey: Int
        get() = R.string.menu_key_accessibility

    override val icon: Int
        get() = R.drawable.ic_system_controls

    override val summary: Int
        get() =
            if (OneHandedSettingsUtils.isSupportOneHandedMode()) {
                R.string.accessibility_system_controls_subtext
            } else {
                R.string.accessibility_system_controls_subtext_one_handed_not_supported
            }

    override fun getMetricsCategory() = SettingsEnums.ACCESSIBILITY_SYSTEM_CONTROLS

    override fun isFlagEnabled(context: Context): Boolean = Flags.catalystMigration26q2()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = SystemControlsFragment::class.java

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) { +PowerButtonEndsCallPreference() }

    companion object {
        const val KEY = "system_controls_preference_screen"
    }
}
// LINT.ThenChange(SystemControlsFragment.java, ../../SystemControlsPreferenceController.kt)
