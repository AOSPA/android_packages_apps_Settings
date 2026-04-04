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
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.accessibility.Flags
import com.android.settings.spa.SpaActivity.Companion.startSpaActivity
import com.android.settings.spa.accessibility.ForceDarkAppExceptionsPageProvider
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceIndexableTitleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
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

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override val supportsWrite = true
    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    override val availabilityDescription = "The device must support dark mode in the API."

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context) = Flags.catalystDarkUiMode()

    override fun createWidget(context: Context) = SelectorWithWidgetPreference(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        (preference as SelectorWithWidgetPreference).setOnClickListener(this)
    }

    override fun onRadioButtonClicked(emiter: SelectorWithWidgetPreference) {
        emiter.isChecked = true
    }
}

/** The "Standard Dark Theme" preference. */
class StandardDarkModeSelectorPreference(dataStore: DarkThemeModeStorage, val isUiOnly: Boolean) :
    DarkModeSelectorPreference(dataStore) {

    override val key
        get() = KEY

    override val purpose: Int
        get() = R.string.standard_dark_theme_purpose

    override val title
        get() = R.string.accessibility_standard_dark_theme_title

    override val summary
        get() = R.string.accessibility_standard_dark_theme_summary

    override fun tags(context: Context): Array<String> {
        if (isUiOnly) {
            return arrayOf(UI_ONLY_PREFERENCE)
        }
        return super.tags(context)
    }

    override fun getIndexableTitle(context: Context): CharSequence? =
        context.getText(R.string.accessibility_standard_dark_theme_title_in_search)

    override val sensitivityLevel: Int
        get() = SensitivityLevel.NO_SENSITIVITY

    companion object {
        const val KEY = "standard_dark_theme"
    }
}

/** The "Expanded Dark Theme" preference. */
class ExpandedDarkModeSelectorPreference(dataStore: DarkThemeModeStorage, val isUiOnly: Boolean) :
    DarkModeSelectorPreference(dataStore), View.OnClickListener {

    override val key
        get() = KEY

    override val purpose: Int
        get() = R.string.expanded_dark_theme_purpose

    override val title
        get() = R.string.accessibility_expanded_dark_theme_title

    override val summary
        get() = R.string.accessibility_expanded_dark_theme_summary

    override val keywords: Int
        get() = R.string.keywords_expanded_dark_theme

    override fun getIndexableTitle(context: Context): CharSequence? =
        context.getText(R.string.accessibility_expanded_dark_theme_title_in_search)

    override fun onClick(v: View?) {
        if (v != null) {
            v.context.startSpaActivity(ForceDarkAppExceptionsPageProvider.name)
        }
    }

    override fun tags(context: Context): Array<String> {
        if (isUiOnly) {
            return arrayOf(UI_ONLY_PREFERENCE)
        }
        return super.tags(context)
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        if (Flags.enableEdtAppExceptions()) {
            val selectorWithWidgetPreference = preference as SelectorWithWidgetPreference
            selectorWithWidgetPreference.setExtraWidgetOnClickListener(this)
            selectorWithWidgetPreference.setExtraWidgetContentDescription(
                preference.context.getString(
                    R.string.accessibility_expanded_dark_theme_exceptions_gear_content_description
                )
            )
            selectorWithWidgetPreference.setExtraWidgetOnBindConsumer({ widget ->
                ViewCompat.replaceAccessibilityAction(
                    widget,
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
                    preference.context.getString(
                        R.string.accessibility_expanded_dark_theme_exceptions_gear_hint
                    ),
                    null,
                )
            })
        }
    }

    override val sensitivityLevel: Int
        get() = SensitivityLevel.NO_SENSITIVITY

    companion object {
        const val KEY = "expanded_dark_theme"
    }
}
// LINT.ThenChange(/src/com/android/settings/accessibility/ForceInvertPreferenceController.java)
