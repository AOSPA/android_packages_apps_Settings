/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may not use this file except in compliance with the License.
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

package com.android.settings.accessibility.buttonshortcutsetting.ui

import android.content.Context
import android.provider.Settings
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityUtil
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.SwitchPreference
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability


/**
 * [SwitchPreference] that controls whether the floating accessibility button fades when inactive.
 */
// TODO(b/426597986): Update permissions, permit and sensitivity level
class FloatingMenuFadePreference :
    SwitchPreference(
        key = KEY,
        purpose = R.string.a11y_floating_menu_shortcut_fade_enabled_purpose,
        title = R.string.accessibility_button_fade_title,
    ),
    PreferenceSummaryProvider,
    PreferenceLifecycleProvider {

    private var buttonModeObserver: KeyedObserver<String>? = null

    override fun getSummary(context: Context): CharSequence? {
        return if (isEnabled(context)) {
            context.getString(R.string.accessibility_button_fade_summary)
        } else {
            context.getString(R.string.accessibility_button_disabled_button_mode_summary)
        }
    }

    override fun getEnabledDescription(): String = "The accessibility floating menu must be enabled."

    override fun getEnabledStability() = PreconditionStability.UNSTABLE

    override fun isEnabled(context: Context): Boolean {
        return AccessibilityUtil.isFloatingMenuEnabled(context)
    }

    override fun storage(context: Context): KeyValueStore {
        return SettingsSecureStore.get(context).apply { setDefaultValue(KEY, FADE_ENABLED_DEFAULT) }
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        buttonModeObserver = KeyedObserver { _, _ -> context.notifyPreferenceChange(key) }

        buttonModeObserver?.let {
            SettingsSecureStore.get(context)
                .addObserver(
                    ACCESSIBILITY_BUTTON_MODE_SETTING,
                    observer = it,
                    executor = HandlerExecutor.main,
                )
        }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        super.onDestroy(context)
        buttonModeObserver?.let {
            SettingsSecureStore.get(context)
                .removeObserver(ACCESSIBILITY_BUTTON_MODE_SETTING, observer = it)
        }

        buttonModeObserver = null
    }

    companion object {
        const val KEY = Settings.Secure.ACCESSIBILITY_FLOATING_MENU_FADE_ENABLED
        const val FADE_ENABLED_DEFAULT = true
        const val ACCESSIBILITY_BUTTON_MODE_SETTING = Settings.Secure.ACCESSIBILITY_BUTTON_MODE
    }
}
