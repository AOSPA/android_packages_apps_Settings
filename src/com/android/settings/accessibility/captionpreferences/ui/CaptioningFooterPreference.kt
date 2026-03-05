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
import com.android.settings.accessibility.shared.ui.AccessibilityFooterPreferenceBinding
import com.android.settings.accessibility.shared.ui.AccessibilityFooterPreferenceMetadata
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE

class CaptioningFooterPreference(override val key: String) :
    AccessibilityFooterPreferenceMetadata, AccessibilityFooterPreferenceBinding {
    override val purpose: Int
        get() = R.string.caption_preferences_footer_purpose
    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override val title: Int
        get() = R.string.accessibility_captioning_preference_summary

    override val introductionTitle: Int
        get() = R.string.accessibility_captioning_about_title

    override val learnMoreText: Int
        get() = R.string.accessibility_captioning_footer_learn_more_content_description

    override val helpResource: Int
        get() = R.string.help_url_caption
}
