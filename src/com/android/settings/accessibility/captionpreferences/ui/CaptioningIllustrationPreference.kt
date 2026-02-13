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

package com.android.settings.accessibility.captionpreferences.ui

import android.content.Context
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.IllustrationPreference

/** Preference to display the illustration for captioning settings. */
class CaptioningIllustrationPreference : PreferenceMetadata, PreferenceBinding {
    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.caption_preferences_illustration_purpose

    override val indexable
        get() = false

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override fun createWidget(context: Context) =
        IllustrationPreference(context).apply {
            lottieAnimationResId = R.drawable.accessibility_captioning_banner
            isSelectable = false
        }

    companion object {
        const val KEY = "captioning_preview"
    }
}
