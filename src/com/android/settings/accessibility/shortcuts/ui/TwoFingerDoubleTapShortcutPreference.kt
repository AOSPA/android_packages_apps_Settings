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
import com.android.internal.accessibility.AccessibilityShortcutController
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType
import com.android.server.accessibility.Flags
import com.android.settings.R
import com.android.settings.accessibility.shortcuts.ShortcutOptionPreference as ShortcutOptionWidget
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider

class TwoFingerDoubleTapShortcutPreference(context: Context, targets: Set<String>) :
    ShortcutOptionPreference(context, UserShortcutType.TWOFINGER_DOUBLETAP, targets),
    PreferenceAvailabilityProvider,
    PreferenceTitleProvider,
    PreferenceSummaryProvider {
    override val key: String
        get() = KEY

    override fun getTitle(context: Context): CharSequence? {
        return context.getString(
            R.string.accessibility_shortcut_edit_screen_title_two_finger_double_tap,
            2,
        )
    }

    override fun getSummary(context: Context): CharSequence? {
        return context.getString(
            R.string.accessibility_shortcut_edit_screen_summary_two_finger_double_tap,
            2,
        )
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        if (preference is ShortcutOptionWidget) {
            preference.setIntroImageRawResId(R.raw.accessibility_shortcut_type_2finger_doubletap)
        }
    }

    override fun isAvailable(context: Context): Boolean {
        if (!Flags.enableMagnificationMultipleFingerMultipleTapGesture()) return false

        // Only Magnification has two fingers triple tap shortcut option.
        return targets.size == 1 &&
            targets.contains(AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME)
    }

    companion object {
        private const val KEY = "shortcut_two_finger_double_tap_pref"
    }
}
