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

package com.android.settings.accessibility.screenmagnification.ui

import android.content.Context
import com.android.settings.R
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.TopIntroPreference

internal class MagnificationTopIntroPreference :
    PreferenceMetadata, PreferenceBinding, PreferenceAvailabilityProvider {

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.magnification_preference_screen_top_intro_purpose

    override val title: Int
        get() = R.string.accessibility_screen_magnification_intro_text

    override val indexable
        get() = false

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override fun createWidget(context: Context) = TopIntroPreference(context)

    override val availabilityDescription = UI_ONLY_PREFERENCE

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context): Boolean = !context.isInSetupWizard()

    companion object {
        const val KEY = "top_intro"
    }
}
