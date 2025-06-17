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
package com.android.settings.accessibility.textreading.ui

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.accessibility.TextReadingPreferenceFragment
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

// @ProvidePreferenceScreen(TextReadingScreen.KEY)
open class TextReadingScreen : PreferenceScreenMixin {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.accessibility_text_reading_options_title

    override fun getMetricsCategory() = SettingsEnums.ACCESSIBILITY_TEXT_READING_OPTIONS

    override val highlightMenuKey
        get() = R.string.menu_key_accessibility

    override fun isFlagEnabled(context: Context) = Flags.catalystTextReadingScreen()

    override fun fragmentClass(): Class<out Fragment>? = TextReadingPreferenceFragment::class.java

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +TextReadingPreview()
            +TextReadingFontSizePreference()
            +TextReadingDisplaySizePreference()
        }

    companion object {
        const val KEY = "text_reading_screen"
    }
}
