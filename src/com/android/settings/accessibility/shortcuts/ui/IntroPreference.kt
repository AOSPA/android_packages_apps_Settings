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

package com.android.settings.accessibility.shortcuts.ui

import android.content.Context
import android.icu.text.ListFormatter
import com.android.settings.R
import com.android.settings.accessibility.shared.utils.getAccessibilityFeatureName
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.TopIntroPreference

class IntroPreference(private val targets: Set<String>) :
    PreferenceMetadata, PreferenceBinding, PreferenceTitleProvider, PreferenceAvailabilityProvider {
    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.shortcut_description_purpose

    override val indexable = false

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override fun createWidget(context: Context) = TopIntroPreference(context)

    override fun getTitle(context: Context): CharSequence? =
        if (isAvailable(context)) {
            context.getString(
                R.string.accessibility_shortcut_edit_screen_prompt,
                getFormattedFeatureList(context),
            )
        } else {
            null
        }

    override val availabilityDescription = UI_ONLY_PREFERENCE

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context): Boolean = targets.size > 1

    private fun getFormattedFeatureList(context: Context): String {
        val featureNames = targets.mapNotNull { getAccessibilityFeatureName(context, it) }
        return ListFormatter.getInstance().format(featureNames)
    }

    companion object {
        const val KEY = "shortcut_description"
    }
}
