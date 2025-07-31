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

package com.android.settings.display.darkmode

import android.content.Context
import com.android.settings.R
import com.android.settings.accessibility.Flags
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceIndexableTitleProvider
import com.android.settingslib.preference.BooleanValuePreferenceBinding
import com.android.settingslib.widget.SelectorWithWidgetPreference

// LINT.IfChange
sealed class DarkModeSelectorPreference(private val dataStore: DarkThemeModeStorage) :
    BooleanValuePreference,
    BooleanValuePreferenceBinding,
    SelectorWithWidgetPreference.OnClickListener,
    PreferenceAvailabilityProvider,
    PreferenceIndexableTitleProvider {

    override fun storage(context: Context): KeyValueStore = dataStore

    override fun getReadPermissions(context: Context) = DarkThemeModeStorage.getReadPermissions()

    override fun getWritePermissions(context: Context) = DarkThemeModeStorage.getWritePermissions()

    override fun isAvailable(context: Context) = Flags.catalystDarkUiMode()

    override fun isIndexable(context: Context) = Flags.catalystDarkUiMode()

    override fun createWidget(context: Context) =
        SelectorWithWidgetPreference(context).apply {
            setOnClickListener(this@DarkModeSelectorPreference)
        }

    override fun onRadioButtonClicked(emiter: SelectorWithWidgetPreference) {
        emiter.isChecked = true
    }
}

/** The "Standard Dark Theme" preference. */
class StandardDarkModeSelectorPreference(dataStore: DarkThemeModeStorage) :
    DarkModeSelectorPreference(dataStore) {

    override val key
        get() = KEY

    override val title
        get() = R.string.accessibility_standard_dark_theme_title

    override val summary
        get() = R.string.accessibility_standard_dark_theme_summary

    override fun getIndexableTitle(context: Context): CharSequence? =
        context.getText(R.string.accessibility_standard_dark_theme_title_in_search)

    companion object {
        const val KEY = "standard_dark_theme"
    }
}

/** The "Expanded Dark Theme" preference. */
class ExpandedDarkModeSelectorPreference(dataStore: DarkThemeModeStorage) :
    DarkModeSelectorPreference(dataStore) {

    override val key
        get() = KEY

    override val title
        get() = R.string.accessibility_expanded_dark_theme_title

    override val summary
        get() = R.string.accessibility_expanded_dark_theme_summary

    override fun getIndexableTitle(context: Context): CharSequence? =
        context.getText(R.string.accessibility_expanded_dark_theme_title_in_search)

    companion object {
        const val KEY = "expanded_dark_theme"
    }
}
// LINT.ThenChange(/src/com/android/settings/accessibility/ForceInvertPreferenceController.java)
