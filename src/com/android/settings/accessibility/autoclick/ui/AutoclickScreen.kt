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

import android.app.settings.SettingsEnums
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.fragment.app.Fragment
import com.android.internal.accessibility.AccessibilityShortcutController
import com.android.settings.R
import com.android.settings.accessibility.AutoclickUtils
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.ToggleAutoclickPreferenceFragment
import com.android.settings.accessibility.shared.ui.AccessibilityShortcutPreference
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.inputmethod.InputPeripheralsSettingsUtils
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.METADATA_IN_UI
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

@ProvidePreferenceScreen(AutoclickScreen.KEY)
open class AutoclickScreen :
    PreferenceScreenMixin,
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider,
    PreferenceLifecycleProvider {
    override val highlightMenuKey: Int
        get() = R.string.menu_key_accessibility

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.a11y_autoclick_screen_purpose

    override val keywords: Int
        get() = R.string.keywords_auto_click

    override val title: Int
        get() = R.string.accessibility_autoclick_preference_title

    override val indexable: Boolean
        get() = true

    override fun isFlagEnabled(context: Context) = Flags.catalystAutoclickScreen()

    override fun fragmentClass(): Class<out Fragment> =
        ToggleAutoclickPreferenceFragment::class.java

    private var settingsKeyedObserver: KeyedObserver<String>? = null

    override fun getMetricsCategory(): Int = SettingsEnums.ACCESSIBILITY_TOGGLE_AUTOCLICK

    override val availabilityDescription = "The device must have a mouse connected."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context): Boolean =
        InputPeripheralsSettingsUtils.isMouse() || InputPeripheralsSettingsUtils.isTouchpad()

    override fun getSummary(context: Context): CharSequence? {
        val storage = SettingsSecureStore.get(context)

        val enabled = storage.getBoolean(Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED) ?: false
        if (!enabled) {
            return context.getText(R.string.autoclick_disabled)
        }

        val delayMillis =
            storage.getInt(Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY) ?: DEFAULT_DELAY

        return AutoclickUtils.getAutoclickDelaySummary(
            context,
            R.string.accessibility_autoclick_delay_unit_second,
            delayMillis,
        )
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        if (isEntryPoint(context)) {
            val observer =
                KeyedObserver<String> { _, _ -> context.notifyPreferenceChange(bindingKey) }
            val storage = SettingsSecureStore.get(context)
            SETTING_KEYS.forEach { settingKey ->
                storage.addObserver(settingKey, observer, HandlerExecutor.main)
            }
            settingsKeyedObserver = observer
        }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        if (isEntryPoint(context)) {
            settingsKeyedObserver?.let { observer ->
                val storage = SettingsSecureStore.get(context)
                SETTING_KEYS.forEach { settingKey -> storage.removeObserver(settingKey, observer) }
                settingsKeyedObserver = null
            }
        }
    }

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +AutoclickIntroPreference()
            +AutoclickIllustrationPreference()
            +AutoclickMainSwitchPreference()
            +AutoClickShortcutPreference(context, metricsCategory)
            +AutoclickDelayPreference(context)
            +AutoclickCursorAreaSizePreference()
            +AutoclickIgnoreMinorCursorMovementPreference()
            +AutoclickRevertToLeftClickPreference()
            +AutoclickFooterPreference()
        }

    class AutoClickShortcutPreference(context: Context, metricsCategory: Int) :
        AccessibilityShortcutPreference(
            context = context,
            key = "autoclick_shortcut_preference",
            purpose = R.string.a11y_autoclick_shortcut_purpose,
            title = R.string.accessibility_autoclick_shortcut_title,
            componentName = AccessibilityShortcutController.AUTOCLICK_COMPONENT_NAME,
            featureName = R.string.accessibility_autoclick_preference_title,
            metricsCategory = metricsCategory,
        ) {
        //not reviewed by security & privacy
        override val sensitivityLevel = SensitivityLevel.DO_NOT_EXPOSE
    }

    class AutoclickScreenPreference(
        private val screenMetadata : AutoclickScreen
    ) : PreferenceMetadata, PreferenceAvailabilityProvider, PreferenceSummaryProvider {

        override val key : String
            get() = "autoclick_preference_preference"

        override val purpose : Int
            get() = screenMetadata.purpose

        override fun tags(context: Context) = arrayOf(METADATA_IN_UI)

        override val indexable = false

        override fun isEnabled(context: Context) : Boolean = screenMetadata.isEnabled(context)

        override fun getSummary(context: Context) : CharSequence? = screenMetadata.getSummary(context)

        override val availabilityDescription = screenMetadata.availabilityDescription

        override fun getAvailabilityStability() = screenMetadata.getAvailabilityStability()

        override fun isAvailable(context: Context) : Boolean = screenMetadata.isAvailable(context)

        override val sensitivityLevel
            get() = SensitivityLevel.DEEP_LINK_ONLY
    }

    companion object {
        const val KEY = "autoclick_preference"
        private const val DEFAULT_DELAY =
            AccessibilityManager.AUTOCLICK_DELAY_WITH_INDICATOR_DEFAULT
        private val SETTING_KEYS =
            arrayOf(
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED,
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY,
            )
    }
}
