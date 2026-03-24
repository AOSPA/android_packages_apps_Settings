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

package com.android.settings.accessibility.shortcutssettings.ui

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.ShortcutsSettingsFragment
import com.android.settings.accessibility.buttonshortcutsetting.ui.ButtonShortcutSettingScreen
import com.android.settings.core.PreferenceScreenMixin
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED

@ProvidePreferenceScreen(ShortcutsSettingsScreen.KEY)
open class ShortcutsSettingsScreen : PreferenceScreenMixin {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_UNCATEGORIZED)

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.accessibility_shortcuts_settings_title

    override val summary: Int
        get() = R.string.accessibility_shortcuts_settings_subtext

    override val highlightMenuKey: Int
        get() = R.string.menu_key_accessibility

    override fun fragmentClass(): Class<out Fragment>? = ShortcutsSettingsFragment::class.java

    override fun isFlagEnabled(context: Context) = Flags.catalystA11yShortcutsSettings()

    override val indexable: Boolean = true

    override val purpose: Int
        get() = R.string.a11y_shortcuts_settings_screen_purpose

    override fun getMetricsCategory(): Int = SettingsEnums.ACCESSIBILITY_SHORTCUTS_SETTINGS

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +ButtonShortcutSettingScreen.KEY
            +VolumeKeysShortcutLockScreenPreference()
        }

    companion object {
        const val KEY = "accessibility_shortcuts_preference_screen"
    }
}
