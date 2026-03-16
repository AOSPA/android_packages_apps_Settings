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
import android.content.Intent
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings
import com.android.settings.accessibility.FeedbackManager
import com.android.settings.accessibility.TextReadingPreferenceFragment
import com.android.settings.accessibility.TextReadingPreferenceFragment.EntryPoint
import com.android.settings.accessibility.TextReadingPreferenceFragmentForSetupWizard
import com.android.settings.accessibility.shared.ui.FeedbackButtonPreference
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceCategory
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED
import kotlinx.coroutines.CoroutineScope

/**
 * Base screen for Text Reading options.
 *
 * This screen has multiple entry points (Display and touch, Accessibility, etc.). To prevent
 * duplicate nodes in the global PreferenceMetadata graph, secondary entry points are flagged as
 * [isUiOnly] true. This flag propagates to child preferences to exclude them from the graph where
 * necessary.
 */
abstract class BaseTextReadingScreen(val isUiOnly: Boolean) : PreferenceScreenMixin {
    @EntryPoint abstract val entryPoint: Int
    override val title: Int
        get() = R.string.accessibility_text_reading_options_title

    override val summary: Int
        get() = R.string.accessibility_text_reading_options_subtext

    override fun getMetricsCategory() = SettingsEnums.ACCESSIBILITY_TEXT_READING_OPTIONS

    override val highlightMenuKey
        get() = R.string.menu_key_display

    // There are multi-entrypoint to this screen. We only want the [TextReadingScreen] searchable to
    // prevent showing duplicate entries in the search results.
    override val indexable
        get() = false

    override fun fragmentClass(): Class<out Fragment>? = TextReadingPreferenceFragment::class.java

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            val fontSizePreference = FontSizePreference(context, entryPoint, isUiOnly)
            val displaySizePreference = DisplaySizePreference(context, entryPoint, isUiOnly)
            +TextReadingPreview(
                displaySizeProvider = { displaySizePreference.displaySizePreview },
                fontSizeProvider = { fontSizePreference.fontSizePreview },
            )

            +PreferenceCategory(
                key = "display_text_size",
                purpose = R.string.display_text_size_purpose,
                title = R.string.category_title_display_text_size,
            ) +=
                {
                    +fontSizePreference
                    +displaySizePreference
                }
            +PreferenceCategory(
                key = "text_style",
                purpose = R.string.text_style_purpose,
                title = R.string.category_title_text_style,
            ) +=
                {
                    +BoldTextPreference(context, entryPoint, isUiOnly)
                    +OutlineTextPreference(context, entryPoint, isUiOnly)
                }
            +ResetPreference(entryPoint)
            +FeedbackButtonPreference({ FeedbackManager(context, metricsCategory) })
        }
}

/**
 * Screen for Text Reading options.
 *
 * This screen has multiple entry points (Display and touch, Accessibility, etc.). To prevent
 * duplicate nodes in the global PreferenceMetadata graph, secondary entry points are flagged as
 * [isUiOnly] true. This flag propagates to child preferences to exclude them from the graph where
 * necessary.
 */
@ProvidePreferenceScreen(TextReadingScreen.KEY)
open class TextReadingScreen : BaseTextReadingScreen(false) {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_UNCATEGORIZED)

    override val entryPoint: Int
        get() = EntryPoint.DISPLAY_SETTINGS

    override val key: String = KEY

    // TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.text_reading_options_purpose

    override val indexable
        get() = true

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent? {
        return makeLaunchIntent(
            context,
            Settings.TextReadingSettingsActivity::class.java,
            metadata?.key,
        )
    }

    companion object {
        const val KEY = "text_reading_options"
    }
}

/**
 * Screen for Text Reading options.
 *
 * This screen has multiple entry points (Display and touch, Accessibility, etc.). To prevent
 * duplicate nodes in the global PreferenceMetadata graph, secondary entry points are flagged as
 * [isUiOnly] true. This flag propagates to child preferences to exclude them from the graph where
 * necessary.
 */
@ProvidePreferenceScreen(TextReadingScreenOnAccessibility.KEY)
open class TextReadingScreenOnAccessibility : BaseTextReadingScreen(true) {
    override val entryPoint: Int
        get() = EntryPoint.ACCESSIBILITY_SETTINGS

    override val highlightMenuKey: Int
        get() = R.string.menu_key_accessibility

    override val key: String = KEY

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    // TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.text_reading_options_in_a11y_purpose

    override val icon: Int
        get() = R.drawable.ic_adaptive_font_download

    companion object {
        const val KEY = "text_reading_options_in_a11y"
    }
}

// TODO(b/407080818): Remove this catalyst screen once we decouple SUW and Settings
/**
 * Screen for Text Reading options.
 *
 * This screen has multiple entry points (Display and touch, Accessibility, etc.). To prevent
 * duplicate nodes in the global PreferenceMetadata graph, secondary entry points are flagged as
 * [isUiOnly] true. This flag propagates to child preferences to exclude them from the graph where
 * necessary.
 */
@ProvidePreferenceScreen(TextReadingScreenInSuw.KEY)
open class TextReadingScreenInSuw : BaseTextReadingScreen(true) {
    override val entryPoint: Int
        get() = EntryPoint.SUW_VISION_SETTINGS

    override val highlightMenuKey: Int
        get() = R.string.menu_key_accessibility

    override val key: String = KEY

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    // TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.text_reading_options_in_suw_purpose

    override val icon: Int
        get() = R.drawable.ic_adaptive_font_download

    override fun getMetricsCategory(): Int = SettingsEnums.SUW_ACCESSIBILITY_TEXT_READING_OPTIONS

    override fun fragmentClass(): Class<out Fragment>? {
        return TextReadingPreferenceFragmentForSetupWizard::class.java
    }

    companion object {
        const val KEY = "text_reading_options_in_suw"
    }
}

// TODO(b/407080818): Remove this catalyst screen once we decouple SUW and Settings
/**
 * Screen for Text Reading options.
 *
 * This screen has multiple entry points (Display and touch, Accessibility, etc.). To prevent
 * duplicate nodes in the global PreferenceMetadata graph, secondary entry points are flagged as
 * [isUiOnly] true. This flag propagates to child preferences to exclude them from the graph where
 * necessary.
 */
@ProvidePreferenceScreen(TextReadingScreenInAnythingElse.KEY)
open class TextReadingScreenInAnythingElse : BaseTextReadingScreen(true) {
    override val entryPoint: Int
        get() = EntryPoint.SUW_ANYTHING_ELSE

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override val key: String = KEY

    // TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.text_reading_options_in_anything_else_purpose

    override val icon: Int
        get() = R.drawable.ic_font_download

    override val title: Int
        get() = R.string.accessibility_text_reading_options_suggestion_title

    override fun getMetricsCategory(): Int = SettingsEnums.SUW_ACCESSIBILITY_TEXT_READING_OPTIONS

    override fun fragmentClass(): Class<out Fragment>? {
        return TextReadingPreferenceFragmentForSetupWizard::class.java
    }

    companion object {
        const val KEY = "text_reading_options_in_anything_else"
    }
}

/**
 * Screen for Text Reading options.
 *
 * This screen has multiple entry points (Display and touch, Accessibility, etc.). To prevent
 * duplicate nodes in the global PreferenceMetadata graph, secondary entry points are flagged as
 * [isUiOnly] true. This flag propagates to child preferences to exclude them from the graph where
 * necessary.
 */
@ProvidePreferenceScreen(TextReadingScreenFromNotification.KEY)
open class TextReadingScreenFromNotification : BaseTextReadingScreen(true) {
    override val entryPoint: Int
        get() = EntryPoint.HIGH_CONTRAST_TEXT_NOTIFICATION

    override val key: String = KEY

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    // TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.text_reading_options_in_outline_text_notification_purpose

    companion object {
        const val KEY = "text_reading_options_in_outline_text_notification"
    }
}
