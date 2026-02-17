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

package com.android.settings.widget

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import com.android.settings.core.BasePreferenceController

/**
 * A generic Category Controller that auto-hides itself if it has no visible children. It works with
 * both static preferences and dynamic external injections.
 */
class SystemCategoryController(context: Context, key: String) :
    BasePreferenceController(context, key) {
    private val IA_SETTINGS_ACTION = "com.android.settings.action.IA_SETTINGS"
    private val META_DATA_KEY = "com.android.settings.group_key"

    override fun getAvailabilityStatus(): Int {
        return AVAILABLE
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        val category = screen.findPreference<PreferenceCategory>(preferenceKey) ?: return

        if (hasVisibleStaticPreferences(category)) {
            category.isVisible = true
            return
        }

        category.isVisible = hasVisibleDynamicPreferences()
    }

    override fun updateState(preference: Preference?) {
        super.updateState(preference)
        if (preference is PreferenceCategory) {
            val isVisible = hasVisibleStaticPreferences(preference)

            if (preference.isVisible != isVisible) {
                preference.isVisible = isVisible
            }
        }
    }

    private fun hasVisibleStaticPreferences(category: PreferenceCategory): Boolean {
        for (i in 0 until category.preferenceCount) {
            if (category.getPreference(i).isVisible) return true
        }
        return false
    }

    private fun hasVisibleDynamicPreferences(): Boolean {
        val intent = Intent(IA_SETTINGS_ACTION)
        val resolveInfos =
            mContext.packageManager.queryIntentActivities(
                intent,
                PackageManager.GET_META_DATA or PackageManager.MATCH_SYSTEM_ONLY,
            )

        for (info in resolveInfos) {
            val metaData = info.activityInfo.metaData
            if (metaData != null && metaData.containsKey(META_DATA_KEY)) {
                val targetKey = metaData.getString(META_DATA_KEY)
                if (mPreferenceKey == targetKey) {
                    return true
                }
            }
        }
        return false
    }
}
