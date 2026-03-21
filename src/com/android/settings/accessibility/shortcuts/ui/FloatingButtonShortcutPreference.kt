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
import android.provider.Settings
import android.text.SpannableStringBuilder
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

/** Preference for the floating button shortcut. */
class FloatingButtonShortcutPreference(context: Context, targets: Set<String>) :
    ShortcutOptionPreference(context, UserShortcutType.SOFTWARE, targets),
    PreferenceSummaryProvider,
    PreferenceAvailabilityProvider,
    PreferenceLifecycleProvider {
    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.accessibility_shortcut_fab_pref_purpose

    override val title: Int
        get() = R.string.accessibility_shortcut_edit_dialog_title_software

    private var observer: KeyedObserver<String>? = null

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        if (preference is ShortcutOptionWidget) {
            preference.setIntroImageRawResId(R.raw.accessibility_shortcut_type_fab)
        }
    }

    override fun getSummary(context: Context): CharSequence? {
        val sb = SpannableStringBuilder()
        sb.append(
            context.getText(R.string.accessibility_shortcut_edit_dialog_summary_floating_button)
        )
        if (!context.isInSetupWizard()) {
            sb.append("\n\n").append(getCustomizeAccessibilityButtonLink(context))
        }
        return sb
    }

    override val availabilityDescription =
        "The device must be in gesture navigation mode, or in the 'floating menu' mode."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context): Boolean {
        // FAB should be available when in gesture navigation mode,
        // or if we're in the FAB button mode while in navbar navigation mode.
        return AccessibilityUtil.isGestureNavigateEnabled(context) ||
            AccessibilityUtil.isFloatingMenuEnabled(context)
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
        private const val KEY = "shortcut_fab_pref"
        private const val SOFTWARE_SHORTCUT_MODE_SETTING = Settings.Secure.ACCESSIBILITY_BUTTON_MODE
    }
}
