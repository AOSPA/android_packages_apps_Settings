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

package com.android.settings.display

import android.app.settings.SettingsEnums
import android.content.Context
import android.hardware.display.ColorDisplayManager
import android.provider.Settings.System.DISPLAY_COLOR_MODE
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.ColorModeActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.display.ColorModeUtils.getActiveColorModeName
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.metadata.METADATA_IN_UI
import com.android.settingslib.metadata.PersistentPreference
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
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED

// LINT.IfChange
@ProvidePreferenceScreen(ColorModeScreen.KEY)
open class ColorModeScreen :
    PreferenceScreenMixin,
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider,
    PreferenceLifecycleProvider {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_UNCATEGORIZED)

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.color_mode_purpose

    override val title: Int
        get() = R.string.color_mode_title

    override val keywords
        get() = R.string.keywords_color_mode

    override fun getMetricsCategory() = SettingsEnums.COLOR_MODE_SETTINGS

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = ColorModePreferenceFragment::class.java

    override val highlightMenuKey: Int
        get() = R.string.menu_key_display

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) { +ColorModeScreenPreference(this@ColorModeScreen) }

    private var settingsKeyedObserver: KeyedObserver<String>? = null

    override val availabilityDescription =
        "The device must be color managed and not have accessibility transforms enabled."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context): Boolean {
        val colorManager = context.getSystemService(ColorDisplayManager::class.java) ?: return false
        return colorManager.isDeviceColorManaged &&
            !ColorDisplayManager.areAccessibilityTransformsEnabled(context)
    }

    override fun getSummary(context: Context): CharSequence? = getActiveColorModeName(context)

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, ColorModeActivity::class.java, metadata?.key)

    override fun onStart(context: PreferenceLifecycleContext) {
        if (isEntryPoint(context)) {
            val observer = KeyedObserver<String> { _, _ -> context.notifyPreferenceChange(KEY) }
            settingsKeyedObserver = observer
            val storage = SettingsSystemStore.get(context)
            storage.addObserver(DISPLAY_COLOR_MODE, observer, HandlerExecutor.main)
        }
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        if (isEntryPoint(context)) {
            settingsKeyedObserver?.let {
                val storage = SettingsSystemStore.get(context)
                storage.removeObserver(DISPLAY_COLOR_MODE, it)
                settingsKeyedObserver = null
            }
        }
    }

    class ColorModeScreenPreference(
        private val screenMetadata : ColorModeScreen
    ) : PreferenceMetadata, PreferenceSummaryProvider, PreferenceAvailabilityProvider, PersistentPreference<String> {
        override val key : String
            get() = "color_mode_preference"

        override val purpose : Int
            get() = screenMetadata.purpose

        override fun tags(context: Context) = arrayOf(METADATA_IN_UI)

        override val supportsWrite: Boolean
            get() = false

        override val valueType = String::class.javaObjectType

        override fun storage(context: Context): KeyValueStore = createSummaryStorage(context, key)

        override val indexable = false

        override fun isEnabled(context: Context) : Boolean = screenMetadata.isEnabled(context)

        override fun getSummary(context: Context) : CharSequence? = screenMetadata.getSummary(context)

        override val availabilityDescription = screenMetadata.availabilityDescription

        override fun getAvailabilityStability() = screenMetadata.getAvailabilityStability()

        override fun isAvailable(context: Context) : Boolean = screenMetadata.isAvailable(context)

        override val sensitivityLevel
            get() = SensitivityLevel.NO_SENSITIVITY
    }

    companion object {
        const val KEY = "color_mode"
    }
}
// LINT.ThenChange(ColorModePreferenceFragment.java, ColorModePreferenceController.java)
