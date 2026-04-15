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

package com.android.settings.accessibility.buttonshortcutsetting.ui

import android.app.settings.SettingsEnums
import android.content.Context
import android.view.accessibility.AccessibilityManager
import com.android.internal.accessibility.common.ShortcutConstants
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityButtonFragment
import com.android.settings.accessibility.Flags
import com.android.settings.core.PreferenceScreenMixin
import com.android.settingslib.metadata.PreferenceIndexableProvider
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability


/** Provides the preference screen for the Accessibility Button shortcut settings. */
@ProvidePreferenceScreen(ButtonShortcutSettingScreen.KEY)
open class ButtonShortcutSettingScreen :
    PreferenceScreenMixin, PreferenceSummaryProvider, PreferenceIndexableProvider {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_UNCATEGORIZED)

    override val title: Int
        get() = R.string.accessibility_button_title

    override val highlightMenuKey: Int
        get() = R.string.menu_key_accessibility

    override fun isFlagEnabled(context: Context): Boolean =
        Flags.catalystA11yButtonShortcutSetting()

    override fun fragmentClass() = AccessibilityButtonFragment::class.java

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +ButtonShortcutTopIntroPreference()
            +ButtonShortcutIllustrationPreference()
            +ButtonLocationPreference(context)
            +ButtonSizePreference(context)
            +FloatingMenuFadePreference()
            +FloatingMenuTransparencyPreference(context)
            +ButtonShortcutSettingFooterPreference()
        }

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.a11y_button_shortcut_setting_screen_purpose

    override fun getMetricsCategory(): Int = SettingsEnums.ACCESSIBILITY_BUTTON_SETTINGS

    override fun isEnabled(context: Context): Boolean {
        return context
            .getSystemService(AccessibilityManager::class.java)
            ?.getAccessibilityShortcutTargets(ShortcutConstants.UserShortcutType.SOFTWARE)
            ?.isNotEmpty() ?: false
    }

    override fun getEnabledDescription() = "At least one accessibility feature must be assigned to the accessibility button."

    override fun getEnabledStability() = PreconditionStability.UNSTABLE

    override fun isIndexable(context: Context): Boolean {
        return isEnabled(context)
    }

    override fun getSummary(context: Context): CharSequence? =
        if (!isEnabled(context)) {
            context.getString(R.string.a11y_button_shortcut_unassigned_setting_unavailable_summary)
        } else {
            ""
        }

    companion object {
        const val KEY = "accessibility_button_preference"
    }
}
