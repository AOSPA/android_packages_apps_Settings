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
import androidx.preference.Preference
import com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType
import com.android.settings.R
import com.android.settings.accessibility.shortcuts.ShortcutOptionPreference as ShortcutOptionWidget
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Preference for the triple tap shortcut.
 *
 * This shortcut option is only available for magnification.
 */
class TripleTapShortcutPreference(
    context: Context,
    targets: Set<String>,
    expandableStateProvider: () -> StateFlow<Boolean>,
) :
    ShortcutOptionPreference(context, UserShortcutType.TRIPLETAP, targets),
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider,
    PreferenceLifecycleProvider {

    /** The flow that indicates whether the preference is expanded. */
    private val expandableState: StateFlow<Boolean> by lazy { expandableStateProvider.invoke() }

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.accessibility_shortcut_edit_screen_title_triple_tap

    override fun getSummary(context: Context): CharSequence? {
        return context.getString(R.string.accessibility_shortcut_edit_screen_summary_triple_tap, 3)
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        if (preference is ShortcutOptionWidget) {
            preference.setIntroImageRawResId(R.raw.accessibility_shortcut_type_tripletap)
        }
        preference.isVisible = expandableState.value && isAvailable(preference.context)
    }

    override fun isAvailable(context: Context): Boolean {
        // This preference is always available from a data perspective if the target is
        // magnification.
        // Its UI visibility, however, depends on the expandable state of the shortcut options.
        // We handle the UI visibility in #bind to reflect this, ensuring the preference is only
        // shown when expanded and applicable.
        return targets.size == 1 && targets.contains(MAGNIFICATION_CONTROLLER_NAME)
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        context.lifecycleScope.launch {
            expandableState.collect { context.notifyPreferenceChange(key) }
        }
    }

    companion object {
        const val KEY = "shortcut_triple_tap_pref"
    }
}
