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
package com.android.settings.supervision.shared.widget

import android.content.Context
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.preference.PreferenceBinding

/** A [PreferenceCategory] that is visible only if at least one of its children is visible. */
class AutoHidingPreferenceCategory(key: String, purpose: Int, title: Int) :
    com.android.settingslib.metadata.PreferenceCategory(key, purpose, title), PreferenceBinding {

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override fun createWidget(context: Context) = PreferenceCategory(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        val category = preference as? PreferenceCategory ?: return

        category.isVisible =
            (0 until category.preferenceCount).any { category.getPreference(it).isVisible }
    }
}
