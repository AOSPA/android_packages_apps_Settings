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

package com.android.settings.accessibility.shortcutssettings.ui

import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.HARDWARE
import com.android.settings.R
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.SwitchPreference
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability


class VolumeKeysShortcutLockScreenPreference() :
    SwitchPreference(
        key = KEY,
        purpose = R.string.a11y_volume_keys_shortcut_setting_on_lock_screen_purpose,
        title = R.string.accessibility_shortcut_service_on_lock_screen_title,
    ),
    PreferenceSummaryProvider {

    override fun storage(context: Context): KeyValueStore {

        return SettingsSecureStore.get(context).apply {
            // The shortcut is enabled by default on the lock screen as long as the user has
            // enabled the shortcut with the warning dialog
            val default = getBoolean(Settings.Secure.ACCESSIBILITY_SHORTCUT_DIALOG_SHOWN) ?: false
            setDefaultValue(KEY, default)
        }
    }

    override fun getEnabledDescription(): String = "At least one accessibility shortcut must be assigned to the volume keys."

    override fun getEnabledStability() = PreconditionStability.UNSTABLE

    override fun isEnabled(context: Context): Boolean {
        return context
            .getSystemService(AccessibilityManager::class.java)
            ?.getAccessibilityShortcutTargets(HARDWARE)
            ?.isNotEmpty() ?: false
    }

    override fun getSummary(context: Context): CharSequence? {
        val stringResId =
            if (isEnabled(context)) {
                R.string.accessibility_shortcut_description
            } else {
                R.string.a11y_volume_keys_shortcut_unassigned_setting_unavailable_summary
            }
        return context.getString(stringResId)
    }

    override val sensitivityLevel: Int
        get() = SensitivityLevel.REQUIRES_CONFIRMATION

    companion object {
        const val KEY = Settings.Secure.ACCESSIBILITY_SHORTCUT_ON_LOCK_SCREEN
    }
}
