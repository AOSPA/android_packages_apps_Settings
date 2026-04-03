/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.settings.language

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.LanguageAndRegionSettingsActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED

// LINT.IfChange
@ProvidePreferenceScreen(LanguageAndRegionScreen.KEY)
open class LanguageAndRegionScreen : PreferenceScreenMixin {
    override fun tags(context: Context) = arrayOf(
        APP_FUNCTION_UNCATEGORIZED,
        // exclude this screen from api result since we have the data in api_language_and_region_settings screen
        UI_ONLY_PREFERENCE,
    )

    override val key: String
        get() = KEY

    // TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.language_and_region_settings_purpose

    override val title: Int
        get() = R.string.language_and_region_settings

    override val summary: Int
        get() = R.string.languages_setting_summary

    override val icon: Int
        get() = R.drawable.ic_settings_languages

    override fun getMetricsCategory() = SettingsEnums.SETTINGS_LANGUAGES_CATEGORY

    override val highlightMenuKey
        get() = R.string.menu_key_system

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = LanguageAndRegionSettings::class.java

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, LanguageAndRegionSettingsActivity::class.java, metadata?.key)

    companion object {
        const val KEY = "language_and_region_settings"
    }
}
// LINT.ThenChange(LanguageAndRegionSettings.java, LanguageAndRegionPreferenceController.java)
