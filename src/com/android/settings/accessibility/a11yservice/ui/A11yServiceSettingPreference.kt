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

package com.android.settings.accessibility.a11yservice.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.android.settings.R
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE

/** Represents the preference for navigating to the settings screen of an accessibility service. */
class A11yServiceSettingPreference(private val serviceInfo: AccessibilityServiceInfo) :
    PreferenceMetadata, PreferenceAvailabilityProvider {
    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.accessibility_service_settings_purpose

    override val title: Int
        get() = R.string.accessibility_menu_item_settings

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override val indexable
        get() = false

    override fun intent(context: Context): Intent? {
        val settingsActivityName = serviceInfo.settingsActivityName ?: return null

        val settingsIntent =
            Intent(Intent.ACTION_MAIN)
                .setComponent(
                    ComponentName(serviceInfo.componentName.packageName, settingsActivityName)
                )

        // Check if there's an activity that can handle this intent
        val resolveInfo = context.packageManager?.resolveActivity(settingsIntent, 0)
        return if (resolveInfo != null) {
            settingsIntent
        } else {
            null
        }
    }

    override val availabilityDescription =
        "The device must not be during setup and there must be an activity that display the activity service settings."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context): Boolean {
        return !context.isInSetupWizard() && intent(context) != null
    }

    companion object {
        private const val KEY = "accessibility_service_settings"
    }
}
