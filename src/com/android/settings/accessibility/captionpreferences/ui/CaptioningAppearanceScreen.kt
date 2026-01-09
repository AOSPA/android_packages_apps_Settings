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

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.accessibility.CaptioningAppearanceFragment
import com.android.settings.accessibility.Flags
import com.android.settings.core.PreferenceScreenMixin
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

/** Screens where the user can customize the size and style of the caption. */
@ProvidePreferenceScreen(CaptioningAppearanceScreen.KEY)
open class CaptioningAppearanceScreen : PreferenceScreenMixin, PreferenceSummaryProvider {
    override val highlightMenuKey: Int
        get() = R.string.menu_key_accessibility

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.captioning_appearance_title

    override val indexable: Boolean
        get() = true

    override val purpose: Int
        get() = R.string.caption_preferences_appearance_purpose

    override fun isFlagEnabled(context: Context): Boolean = Flags.catalystCaptionPreferencesScreen()

    override fun fragmentClass(): Class<out Fragment>? = CaptioningAppearanceFragment::class.java

    override fun getMetricsCategory(): Int = SettingsEnums.ACCESSIBILITY_CAPTION_APPEARANCE

    override fun getSummary(context: Context): CharSequence? {
        // Secure.ACCESSIBILITY_CAPTIONING_FONT_SCALE -- onFontScaleChanged
        // Secure.ACCESSIBILITY_CAPTIONING_PRESET -- onUserStyleChanged
        // TODO: Update summary based on the selected caption size and style
        return null
    }

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    companion object {
        const val KEY = "captioning_appearance"
    }
}
