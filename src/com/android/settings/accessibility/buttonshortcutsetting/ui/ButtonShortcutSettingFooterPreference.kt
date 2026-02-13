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

package com.android.settings.accessibility.buttonshortcutsetting.ui

import android.content.Context
import android.icu.text.MessageFormat
import android.text.Html
import com.android.settings.R
import com.android.settings.accessibility.shared.ui.AccessibilityFooterPreferenceBinding
import com.android.settings.accessibility.shared.ui.AccessibilityFooterPreferenceMetadata
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE

/** Footer preference for the accessibility button shortcut setting. */
class ButtonShortcutSettingFooterPreference :
    AccessibilityFooterPreferenceMetadata,
    AccessibilityFooterPreferenceBinding,
    PreferenceTitleProvider {
    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.a11y_button_shortcut_setting_footer_purpose

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override fun getTitle(context: Context): CharSequence? =
        Html.fromHtml(
            MessageFormat.format(
                context.getString(R.string.accessibility_button_description),
                1,
                2,
                3,
            ),
            Html.FROM_HTML_MODE_COMPACT,
            /* imageGetter= */ null,
            /* tagHandler= */ null,
        )

    override val introductionTitle: Int
        get() = R.string.accessibility_button_about_title

    companion object {
        const val KEY = "accessibility_button_footer"
    }
}
