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

package com.android.settings.accessibility

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.AccessibilitySettingsActivity
import com.android.settings.accessibility.textreading.ui.TextReadingScreenOnAccessibility
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceCategory
import com.android.settingslib.metadata.PreferenceIconProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.widget.SettingsThemeHelper.isExpressiveTheme
import kotlinx.coroutines.CoroutineScope
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED

// LINT.IfChange
@ProvidePreferenceScreen(AccessibilityScreen.KEY)
open class AccessibilityScreen :
    PreferenceScreenMixin, PreferenceIconProvider, PreferenceAvailabilityProvider {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_UNCATEGORIZED)

    override val key: String
        get() = KEY

    // TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.top_level_accessibility_purpose

    override val title: Int
        get() = R.string.accessibility_settings

    override val summary: Int
        get() = R.string.accessibility_settings_summary

    override fun getIcon(context: Context) =
        when {
            isExpressiveTheme(context) -> R.drawable.ic_homepage_accessibility
            else -> R.drawable.ic_settings_accessibility_filled
        }

    override val keywords: Int
        get() = R.string.keywords_accessibility

    override val availabilityDescription = "The device must support the accessibility screen."

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context) =
        context.resources.getBoolean(R.bool.config_show_top_level_accessibility)

    override val highlightMenuKey: Int
        get() = R.string.menu_key_accessibility

    override fun getMetricsCategory() = SettingsEnums.ACCESSIBILITY

    override fun isFlagEnabled(context: Context): Boolean = Flags.catalystAccessibilityScreen()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = AccessibilitySettings::class.java

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +PreferenceCategory(
                "display_category",
                purpose = R.string.display_category_purpose,
                R.string.display_category_title,
            ) +=
                {
                    +TextReadingScreenOnAccessibility.KEY
                }
        }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, AccessibilitySettingsActivity::class.java, metadata?.key)

    companion object {
        const val KEY = "top_level_accessibility"
    }
}
// LINT.ThenChange(AccessibilitySettings.java)
