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

package com.android.settings.accessibility.actiontimeout.ui

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityControlTimeoutPreferenceFragment
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.actiontimeout.data.ActionTimeoutOptionDataStore
import com.android.settings.accessibility.actiontimeout.data.ActionTimeoutOptionDataStore.Companion.DISCRETE_TIMEOUT_OPTIONS
import com.android.settings.core.PreferenceScreenMixin
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.metadata.METADATA_IN_UI
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED

/** Preference screen for "Time to take action (Accessibility timeout)" settings. */
@ProvidePreferenceScreen(ActionTimeoutSettingsScreen.KEY)
open class ActionTimeoutSettingsScreen(context: Context) :
    PreferenceScreenMixin, PreferenceSummaryProvider, PreferenceLifecycleProvider {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_UNCATEGORIZED)

    override val highlightMenuKey: Int
        get() = R.string.menu_key_accessibility

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.accessibility_setting_item_control_timeout_title

    override val screenTitle: Int
        get() = R.string.accessibility_control_timeout_preference_title

    override val keywords: Int
        get() = R.string.keywords_accessibility_timeout

    override val purpose: Int
        get() = R.string.a11y_action_timeout_setting_screen_purpose

    private val timeoutOptionDataStore by lazy { ActionTimeoutOptionDataStore(context) }
    private var keyedObserver: KeyedObserver<String>? = null

    override fun fragmentClass(): Class<out Fragment> =
        AccessibilityControlTimeoutPreferenceFragment::class.java

    override fun isFlagEnabled(context: Context): Boolean = Flags.catalystTimeToTakeActionScreen()

    override val indexable: Boolean
        get() = true

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +ActionTimeoutIntroPreference()
            +ActionTimeoutIllustrationPreference()
            DISCRETE_TIMEOUT_OPTIONS.keys.sorted().forEach { timeoutOption ->
                val actionTimeoutOption = DISCRETE_TIMEOUT_OPTIONS[timeoutOption]!!
                +ActionTimeoutOptionPreference(
                    key = actionTimeoutOption.preferenceKey,
                    title = actionTimeoutOption.optionDescriptionRes,
                    dataStoreProvider = { timeoutOptionDataStore },
                )
            }
            +ActionTimeoutFooterPreference()
        }

    override fun getMetricsCategory(): Int = SettingsEnums.ACCESSIBILITY_TIMEOUT

    override fun getSummary(context: Context): CharSequence? {
        val timeoutValue = timeoutOptionDataStore.getInt(bindingKey)
        val stringResId = DISCRETE_TIMEOUT_OPTIONS[timeoutValue]?.optionDescriptionRes ?: 0
        return if (stringResId != 0) {
            context.getString(stringResId)
        } else {
            null
        }
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        if (!isEntryPoint(context)) return
        if (keyedObserver == null) {
            keyedObserver =
                KeyedObserver<String> { _, _ -> context.notifyPreferenceChange(bindingKey) }
        }
        keyedObserver?.let {
            timeoutOptionDataStore.addObserver(bindingKey, it, HandlerExecutor.main)
        }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        super.onDestroy(context)
        keyedObserver?.let {
            timeoutOptionDataStore.removeObserver(bindingKey, it)
            keyedObserver = null
        }
    }

    companion object {
        const val KEY = "accessibility_control_timeout_preference_fragment"
    }
}
