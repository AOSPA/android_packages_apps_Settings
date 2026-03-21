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
import com.android.settings.accessibility.AutoclickDelayDialogFragment
import com.android.settings.accessibility.AutoclickUtils
import com.android.settings.accessibility.AutoclickUtils.AUTOCLICK_DELAY_STEP
import com.android.settings.accessibility.AutoclickUtils.MAX_AUTOCLICK_DELAY_MS
import com.android.settings.accessibility.AutoclickUtils.MIN_AUTOCLICK_DELAY_MS
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.IntRangeValuePreference
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.ReadWritePermit

/** Preference for the delay before an automatic click is performed. */
class AutoclickDelayPreference(context: Context) :
    IntRangeValuePreference, PreferenceSummaryProvider, PreferenceLifecycleProvider {
    init {
        // b/413144145: Sanitize delay time
        // Before autoclick ui improvement, the delay time is set to 0 when the autoclick is turned
        // off.
        val storage = storage(context.applicationContext)
        val delay = storage.getInt(SETTING_KEY) ?: DEFAULT_DELAY
        if (delay < getMinValue(context)) {
            storage(context).setInt(SETTING_KEY, DEFAULT_DELAY)
        }
    }

    override fun getMinValue(context: Context): Int = MIN_AUTOCLICK_DELAY_MS

    override fun getMaxValue(context: Context): Int = MAX_AUTOCLICK_DELAY_MS

    override fun getIncrementStep(context: Context): Int = AUTOCLICK_DELAY_STEP

    override fun getUnitOfMeasurement() = "milliseconds"

    override val key: String
        get() = SETTING_KEY

    override val title: Int
        get() = R.string.autoclick_delay_before_click_title

    override val purpose: Int
        get() = R.string.a11y_autoclick_delay_purpose

    override fun storage(context: Context): KeyValueStore =
        SettingsSecureStore.get(context).apply { setDefaultValue(SETTING_KEY, DEFAULT_DELAY) }

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val supportsWrite = true

    override fun getSummary(context: Context): CharSequence {
        val autoclickDelay = storage(context).getInt(SETTING_KEY) ?: DEFAULT_DELAY
        return AutoclickUtils.getAutoclickDelaySummary(
            context,
            R.string.accessibility_autoclick_delay_unit_second,
            autoclickDelay,
        )
    }

    override fun onStart(context: PreferenceLifecycleContext) {
        super.onStart(context)
        val delayPreference = context.findPreference<Preference>(bindingKey) ?: return
        delayPreference.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                AutoclickDelayDialogFragment.newInstance()
                    .show(
                        context.childFragmentManager,
                        AutoclickDelayDialogFragment::class.java.name,
                    )
                true
            }
    }

    override val sensitivityLevel
        get() = SensitivityLevel.DEEP_LINK_ONLY

    companion object {
        const val SETTING_KEY = Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY
        private const val DEFAULT_DELAY =
            AccessibilityManager.AUTOCLICK_DELAY_WITH_INDICATOR_DEFAULT
    }
}
