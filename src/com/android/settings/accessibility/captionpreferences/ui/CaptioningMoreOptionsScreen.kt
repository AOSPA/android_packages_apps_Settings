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
import android.provider.Settings
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.accessibility.CaptioningMoreOptionsFragment
import com.android.settings.accessibility.Flags
import com.android.settings.core.PreferenceScreenMixin
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceIndexableProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

/** Displays "More options" in captioning settings. */
@ProvidePreferenceScreen(CaptioningMoreOptionsScreen.KEY)
open class CaptioningMoreOptionsScreen : PreferenceScreenMixin, PreferenceIndexableProvider {
    override val highlightMenuKey: Int
        get() = R.string.menu_key_accessibility

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.captioning_more_options_title

    override val purpose: Int
        get() = R.string.caption_preferences_more_options_screen_purpose

    override fun isFlagEnabled(context: Context): Boolean = Flags.catalystCaptionPreferencesScreen()

    override fun fragmentClass(): Class<out Fragment>? = CaptioningMoreOptionsFragment::class.java

    override fun getMetricsCategory(): Int = SettingsEnums.ACCESSIBILITY_CAPTION_MORE_OPTIONS

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            // LINT.IfChange(ui_hierarchy)
            +CaptionLocalePreference(context)
            +CaptioningFooterPreference("captioning_more_options_footer")
            // LINT.ThenChange()
        }

    override fun isIndexable(context: Context) =
        SettingsSecureStore.get(context)
            .getBoolean(Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED) ?: false

    companion object {
        const val KEY = "captioning_more_options"
    }
}
