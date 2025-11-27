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

package com.android.settings.widget

import android.content.Context
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.android.settingslib.development.DevelopmentSettingsEnabler

/**
 * A specific Category Controller for System > Advanced.
 *
 * It handles the asynchronous nature of Developer Options by synchronously checking the Global
 * Settings provider. This prevents the category from flickering or hiding while the
 * DeveloperOptionsController is loading.
 */
class SystemAdvancedCategoryController(context: Context, key: String) :
    PreferenceCategoryController(context, key) {

    override fun getAvailabilityStatus(): Int {
        return AVAILABLE
    }

    override fun updateState(preference: Preference?) {
        // Smart cast: If it's not a category, stop here.
        val category = preference as? PreferenceCategory ?: return

        // 1. Check if Developer Options logic mandates visibility.
        val showDevOptions = DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(mContext)

        // 2. Check if there are ANY visible children in the category.
        var hasVisibleChildren = false
        for (i in 0 until category.preferenceCount) {
            val child = category.getPreference(i)
            // We found a child that is visible.
            if (child.isVisible) {
                hasVisibleChildren = true
                break
            }
        }

        // 3. The category is visible if Dev Options is enabled OR if we found other content.
        category.isVisible = showDevOptions || hasVisibleChildren
    }
}
