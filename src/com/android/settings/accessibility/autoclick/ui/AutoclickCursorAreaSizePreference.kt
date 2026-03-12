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

package com.android.settings.accessibility.autoclick.ui

import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.accessibility.autoclick.dialogs.AutoclickCursorAreaSizeDialogFragment
import com.android.settings.accessibility.shared.utils.SummaryMap
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.DiscreteIntValue
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ReadWritePermit

class AutoclickCursorAreaSizePreference :
    PersistentPreference<Int>,
    DiscreteIntValue,
    PreferenceSummaryProvider,
    PreferenceLifecycleProvider {
    override val key: String
        get() = SETTING_KEY

    override val title: Int
        get() = R.string.autoclick_cursor_area_size_title

    override val purpose: Int
        get() = R.string.a11y_autoclick_cursor_area_size_purpose

    override val valueType: Class<Int>
        get() = Int::class.javaObjectType

    override val values: Int
        get() = R.array.autoclick_cursor_area_size_selector_values

    override val valuesDescription: Int
        get() = R.array.autoclick_cursor_area_size_selector_titles

    override fun getUnitOfMeasurement() = "pixels"

    private val summaryMap by lazy {
        SummaryMap(values, valuesDescription, useIntValues = true) { _, v -> v as Int }
    }

    override fun getSummary(context: Context): CharSequence? {
        return summaryMap.getSummary(context, storage(context).getInt(key))
    }

    override fun storage(context: Context): KeyValueStore =
        SettingsSecureStore.get(context).apply {
            setDefaultValue(SETTING_KEY, AccessibilityManager.AUTOCLICK_CURSOR_AREA_SIZE_DEFAULT)
        }

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val supportsWrite = true

    override fun onStart(context: PreferenceLifecycleContext) {
        super.onStart(context)
        val cursorAreaSizePreference = context.findPreference<Preference>(bindingKey) ?: return
        cursorAreaSizePreference.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                AutoclickCursorAreaSizeDialogFragment()
                    .show(
                        context.childFragmentManager,
                        AutoclickCursorAreaSizeDialogFragment::class.java.name,
                    )
                true
            }
    }

    companion object {
        const val SETTING_KEY = Settings.Secure.ACCESSIBILITY_AUTOCLICK_CURSOR_AREA_SIZE
    }
}
