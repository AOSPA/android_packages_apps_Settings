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

package com.android.settings.accessibility.buttonshortcutsetting.ui

import android.content.Context
import android.provider.Settings
import android.provider.Settings.Secure.ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU
import androidx.annotation.StringRes
import androidx.preference.ListPreference
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityUtil
import com.android.settings.accessibility.shared.data.StringToIntDataStoreWrapper
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.DiscreteIntValue
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.preference.PreferenceBinding

// TODO(b/426597986): Update permissions, permit and sensitivity level
class ButtonLocationPreference(context: Context) :
    PersistentPreference<Int>,
    PreferenceBinding,
    DiscreteIntValue,
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider,
    PreferenceLifecycleProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.accessibility_button_location_title

    private val dataStore by lazy {
        StringToIntDataStoreWrapper(
            SettingsSecureStore.get(context).apply {
                setDefaultValue(KEY, ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU)
            }
        )
    }

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val supportsWrite = true

    override fun getSummary(context: Context): CharSequence? {
        val selectedMode = storage(context).getInt(KEY)

        @StringRes
        val stringRes =
            if (selectedMode == ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU) {
                R.string.accessibility_button_location_selector_floating
            } else {
                R.string.accessibility_button_location_selector_navigation_bar
            }

        return context.getString(stringRes)
    }

    override val purpose: Int
        get() = R.string.a11y_button_shortcut_location_purpose

    override val values: Int
        get() = R.array.accessibility_button_location_selector_int_values

    override val valuesDescription: Int
        get() = R.array.accessibility_button_location_selector_titles

    private var navigationModeObserver: KeyedObserver<String>? = null

    override fun createWidget(context: Context) = ListPreference(context)

    override val valueType: Class<Int>
        get() = Int::class.javaObjectType

    override fun storage(context: Context): KeyValueStore = dataStore

    override val availabilityDescription =
        "The device must be using 3-button navigation, or the operating system must be configured to enable customising the button location."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context): Boolean {
        return AccessibilityUtil.isAccessibilityButtonLocationConfigurable(context)
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        navigationModeObserver = KeyedObserver { _, _ -> context.notifyPreferenceChange(key) }

        navigationModeObserver?.let {
            SettingsSecureStore.get(context)
                .addObserver(NAVIGATION_MODE_KEY, observer = it, executor = HandlerExecutor.main)
        }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        super.onDestroy(context)
        navigationModeObserver?.let {
            SettingsSecureStore.get(context).removeObserver(NAVIGATION_MODE_KEY, observer = it)
        }
    }

    companion object {
        const val KEY = Settings.Secure.ACCESSIBILITY_BUTTON_MODE
        private const val NAVIGATION_MODE_KEY = Settings.Secure.NAVIGATION_MODE
    }
}
