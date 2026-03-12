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
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.preference.PreferenceBinding

// TODO(b/426597986): Update permissions, permit and sensitivity level
class ButtonSizePreference(context: Context) :
    PersistentPreference<Int>,
    PreferenceBinding,
    DiscreteIntValue,
    PreferenceSummaryProvider,
    PreferenceLifecycleProvider {
    override val valueType: Class<Int>
        get() = Int::class.javaObjectType

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.a11y_button_shortcut_size_purpose

    override val title: Int
        get() = R.string.accessibility_button_size_title

    private val dataStore by lazy {
        StringToIntDataStoreWrapper(
            SettingsSecureStore.get(context).apply { setDefaultValue(KEY, SIZE_SMALL) }
        )
    }

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val supportsWrite = true

    private var buttonModeObserver: KeyedObserver<String>? = null

    override val values: Int
        get() = R.array.accessibility_button_size_selector_int_values

    override val valuesDescription: Int
        get() = R.array.accessibility_button_size_selector_titles

    override fun getSummary(context: Context): CharSequence? {
        val resId =
            if (isEnabled(context)) {
                when (storage(context).getInt(KEY)) {
                    SIZE_SMALL -> R.string.accessibility_button_size_selector_small
                    else -> R.string.accessibility_button_size_selector_large
                }
            } else {
                R.string.accessibility_button_disabled_button_mode_summary
            }
        return context.getString(resId)
    }

    override fun isEnabled(context: Context): Boolean {
        return AccessibilityUtil.isFloatingMenuEnabled(context)
    }

    override fun createWidget(context: Context) = ListPreference(context)

    override fun storage(context: Context): KeyValueStore = dataStore

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        buttonModeObserver = KeyedObserver { _, _ -> context.notifyPreferenceChange(key) }

        buttonModeObserver?.let {
            SettingsSecureStore.get(context)
                .addObserver(
                    Settings.Secure.ACCESSIBILITY_BUTTON_MODE,
                    observer = it,
                    executor = HandlerExecutor.main,
                )
        }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        super.onDestroy(context)
        buttonModeObserver?.let {
            SettingsSecureStore.get(context)
                .removeObserver(Settings.Secure.ACCESSIBILITY_BUTTON_MODE, observer = it)
        }
    }

    companion object {
        const val KEY = Settings.Secure.ACCESSIBILITY_FLOATING_MENU_SIZE
        const val SIZE_SMALL = 0
        const val SIZE_LARGE = 1
    }
}
