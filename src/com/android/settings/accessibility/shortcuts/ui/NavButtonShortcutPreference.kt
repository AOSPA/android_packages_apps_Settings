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
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.preference.Preference
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityUtil
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settings.accessibility.shortcuts.ShortcutOptionPreference as ShortcutOptionWidget
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider

/** Preference for the navigation button shortcut. */
class NavButtonShortcutPreference(context: Context, targets: Set<String>) :
    ShortcutOptionPreference(context, UserShortcutType.SOFTWARE, targets),
    PreferenceSummaryProvider,
    PreferenceAvailabilityProvider,
    PreferenceLifecycleProvider {
    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.shortcut_nav_button_pref_purpose

    override val title: Int
        get() = R.string.accessibility_shortcut_edit_dialog_title_software

    private var observer: KeyedObserver<String>? = null
    private var textLineHeight = 0

    override fun createWidget(context: Context): ShortcutOptionWidget {
        return super.createWidget(context).apply {
            setOnBindListener { v ->
                // Get the correct line height for the summary view when the preference view is
                // bound, so that the drawable icon in the summary text matches the text's height.
                val summaryView = v.findViewById(android.R.id.summary) as? TextView
                textLineHeight = summaryView?.lineHeight ?: 0
                summaryView?.run { text = getSummary(context) }
            }
        }
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        if (preference is ShortcutOptionWidget) {
            preference.setIntroImageResId(R.drawable.accessibility_shortcut_type_navbar)
        }
    }

    override fun getSummary(context: Context): CharSequence? {
        val sb = SpannableStringBuilder()
        sb.append(getSummaryStringWithIcon(context, textLineHeight))

        if (!context.isInSetupWizard()) {
            sb.append("\n\n")
            sb.append(getCustomizeAccessibilityButtonLink(context))
        }

        return sb
    }

    private fun getSummaryStringWithIcon(context: Context, lineHeight: Int): SpannableString {
        val summary: String =
            context.getString(R.string.accessibility_shortcut_edit_dialog_summary_software)
        val spannableMessage = SpannableString.valueOf(summary)

        // Icon
        val indexIconStart = summary.indexOf("%s")
        val indexIconEnd = indexIconStart + 2
        val icon: Drawable? =
            AppCompatResources.getDrawable(context, R.drawable.ic_accessibility_new)?.also { icon ->
                icon.setBounds(
                    /* left= */ 0,
                    /* top= */ 0,
                    /* right= */ lineHeight,
                    /* bottom= */ lineHeight,
                )
            }

        icon?.let {
            val imageSpan = ImageSpan(it)
            imageSpan.contentDescription = ""
            spannableMessage.setSpan(
                imageSpan,
                indexIconStart,
                indexIconEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }

        return spannableMessage
    }

    override val availabilityDescription =
        "The device must not be in gesture navigation mode and the floating menu must be disabled."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context): Boolean {
        return !AccessibilityUtil.isFloatingMenuEnabled(context) &&
            !AccessibilityUtil.isGestureNavigateEnabled(context)
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        observer =
            KeyedObserver<String> { _, _ -> context.notifyPreferenceChange(key) }
                .also {
                    SettingsSecureStore.get(context)
                        .addObserver(
                            key = SOFTWARE_SHORTCUT_MODE_SETTING,
                            observer = it,
                            executor = HandlerExecutor.main,
                        )
                }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        super.onDestroy(context)
        observer?.let {
            SettingsSecureStore.get(context).removeObserver(SOFTWARE_SHORTCUT_MODE_SETTING, it)
        }
    }

    companion object {
        private const val KEY = "shortcut_nav_button_pref"
        private const val SOFTWARE_SHORTCUT_MODE_SETTING = Settings.Secure.ACCESSIBILITY_BUTTON_MODE
    }
}
