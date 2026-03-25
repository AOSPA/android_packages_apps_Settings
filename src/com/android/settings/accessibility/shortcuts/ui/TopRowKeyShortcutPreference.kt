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

package com.android.settings.accessibility.shortcuts.ui

import android.content.Context
import android.hardware.input.InputManager
import android.view.KeyEvent
import android.view.accessibility.Flags
import androidx.preference.Preference
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType
import com.android.settings.R
import com.android.settings.accessibility.shortcuts.ShortcutOptionPreference as ShortcutOptionWidget
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata

class TopRowKeyShortcutPreference(context: Context, targets: Set<String>) :
    ShortcutOptionPreference(context, UserShortcutType.TOP_ROW_KEY, targets),
    PreferenceAvailabilityProvider,
    PreferenceLifecycleProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.accessibility_shortcut_edit_dialog_title_top_row_key

    override val purpose: Int
        get() = R.string.shortcut_top_row_key_pref_purpose

    override val summary: Int
        get() = R.string.accessibility_shortcut_edit_dialog_summary_top_row_key

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        if (preference is ShortcutOptionWidget) {
            preference.setIntroImageResId(R.drawable.accessibility_shortcut_type_top_row)
        }
    }

    override val availabilityDescription =
        "The device must support Top Row key shortcut settings. There must be an attached keyboard. The keyboard must have a Accessibility key."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context): Boolean {
        // Should only be available if feature flag is available,
        // and if there is an attached keyboard
        val inputManager = context.getSystemService(InputManager::class.java)

        return Flags.enableA11yTopRowShortcut() &&
            com.android.hardware.input.Flags.enableNew26q2Keycodes() &&
            inputManager.deviceHasKeys(intArrayOf(KeyEvent.KEYCODE_ACCESSIBILITY))[0]
    }

    companion object {
        private const val KEY = "shortcut_top_row_key_pref"
    }
}
