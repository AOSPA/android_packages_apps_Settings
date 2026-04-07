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

package com.android.settings.accessibility.timingcontrols.ui

import android.content.Context
import android.provider.Settings
import android.view.ViewConfiguration
import androidx.preference.ListPreference
import com.android.settings.R
import com.android.settings.accessibility.shared.data.StringToIntDataStoreWrapper
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.DiscreteIntValue
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.preference.PreferenceBinding

/**
 * [ListPreference] for the long press timeout setting.
 *
 * This preference allows the user to select a long press timeout duration from a predefined list.
 */
class LongPressTimeoutPreference(context: Context) :
    PersistentPreference<Int>,
    PreferenceBinding,
    DiscreteIntValue,
    PreferenceSummaryProvider,
    PreferenceLifecycleProvider {
    private val dataStore by lazy {
        StringToIntDataStoreWrapper(
            SettingsSecureStore.get(context).apply {
                setDefaultValue(KEY, DEFAULT_LONG_PRESS_TIMEOUT)
            }
        )
    }

    private val timeOutValueToTitleResId by lazy {
        mapOf(
            context.resources.getInteger(R.integer.long_press_timeout_short) to
                R.string.accessibility_long_press_timeout_option_short,
            context.resources.getInteger(R.integer.long_press_timeout_medium) to
                R.string.accessibility_long_press_timeout_option_medium,
            context.resources.getInteger(R.integer.long_press_timeout_long) to
                R.string.accessibility_long_press_timeout_option_long,
        )
    }

    override val valueType: Class<Int>
        get() = Int::class.javaObjectType

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.a11y_touch_hold_delay_purpose

    override val values: Int
        get() = R.array.long_press_timeout_selector_int_values

    override val valuesDescription: Int
        get() = R.array.long_press_timeout_selector_list_titles

    override fun getUnitOfMeasurement() = "milliseconds"

    override fun storage(context: Context): KeyValueStore = dataStore

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val supportsWrite = true

    override val title: Int
        get() = R.string.accessibility_long_press_timeout_preference_title

    override fun getSummary(context: Context): CharSequence? {
        val timeoutValue = storage(context).getInt(KEY) ?: DEFAULT_LONG_PRESS_TIMEOUT
        val summaryResId = timeOutValueToTitleResId[timeoutValue] ?: 0
        return context.getString(summaryResId)
    }

    override fun createWidget(context: Context) = ListPreference(context)

    override val sensitivityLevel: Int
        get() = SensitivityLevel.DEEP_LINK_ONLY

    companion object {
        const val KEY = Settings.Secure.LONG_PRESS_TIMEOUT
        private const val DEFAULT_LONG_PRESS_TIMEOUT = ViewConfiguration.DEFAULT_LONG_PRESS_TIMEOUT
    }
}
