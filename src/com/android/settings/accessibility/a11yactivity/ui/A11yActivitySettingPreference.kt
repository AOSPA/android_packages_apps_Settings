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

package com.android.settings.accessibility.a11yactivity.ui

import android.accessibilityservice.AccessibilityShortcutInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.android.settings.R
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE

/** Represents the preference for navigating to the settings screen of an accessibility activity. */
class A11yActivitySettingPreference(private val shortcutInfo: AccessibilityShortcutInfo) :
    PreferenceMetadata, PreferenceAvailabilityProvider {
    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.accessibility_activity_settings_purpose

    override val title: Int
        get() = R.string.accessibility_menu_item_settings

    override val indexable
        get() = false

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override fun intent(context: Context): Intent? {
        val settingsActivityName = shortcutInfo.settingsActivityName ?: return null

        val settingsIntent =
            Intent(Intent.ACTION_MAIN)
                .setComponent(
                    ComponentName(shortcutInfo.componentName.packageName, settingsActivityName)
                )

        // Check if there's an activity that can handle this intent
        val resolveInfo = context.packageManager?.resolveActivity(settingsIntent, 0)
        return if (resolveInfo != null) {
            settingsIntent
        } else {
            null
        }
    }

    override val availabilityDescription = UI_ONLY_PREFERENCE

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context): Boolean {
        return !context.isInSetupWizard() && intent(context) != null
    }

    companion object {
        private const val KEY = "accessibility_activity_settings"
    }
}
