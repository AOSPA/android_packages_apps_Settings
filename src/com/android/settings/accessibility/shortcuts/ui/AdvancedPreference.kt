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
import androidx.preference.TwoStatePreference
import com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A preference that controls the visibility of advanced accessibility shortcut options.
 *
 * This is used for features like magnification to show or hide secondary shortcut options, such as
 * the triple-tap shortcut. When a user taps this preference, it is hidden and the advanced options
 * are displayed in its place.
 *
 * Note: This preference could be replaced by ExpandablePreference in the future, if we don't need
 * to hide this advanced preference UI when it is expanded.
 */
class AdvancedPreference(private val targets: Set<String>) :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceAvailabilityProvider,
    PreferenceLifecycleProvider {

    private val _expandableState by lazy { MutableStateFlow(false) }

    /** The current expandable state of the advanced preference. */
    val expandableState by lazy { _expandableState.asStateFlow() }

    override val key: String
        get() = KEY

    override val icon: Int
        get() = R.drawable.ic_keyboard_arrow_down

    override val title: Int
        get() = R.string.accessibility_shortcut_edit_dialog_title_advance

    override fun isAvailable(context: Context): Boolean {
        return !_expandableState.value &&
            targets.size == 1 &&
            targets.contains(MAGNIFICATION_CONTROLLER_NAME)
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.setOnPreferenceClickListener {
            _expandableState.value = !_expandableState.value
            true
        }
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        val tripleTapOptionChecked =
            context.findPreference<TwoStatePreference>(TripleTapShortcutPreference.KEY)?.isChecked
                ?: false
        context.findPreference<Preference>(key)?.let { widget ->
            // 1. Expand to show the Triple Tap shortcut if the user already has that shortcut
            // enabled. This shortcut affects touch responsiveness so we want to ensure that the
            // user sees that the shortcut is enabled without requiring them to manually expand it.
            // 2. Updates the expandable state based on the initial visibility of the preference
            // which is updated based on the savedInstanceState in
            // PreferenceFragment#onCreatePreferences
            _expandableState.value = !widget.isVisible || tripleTapOptionChecked
        }
        context.lifecycleScope.launch {
            expandableState.collect { context.notifyPreferenceChange(key) }
        }
    }

    companion object {
        const val KEY = "advanced_shortcuts_collapsed"
    }
}
