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

package com.android.settings.accessibility.extradim.ui

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.fragment.app.Fragment
import com.android.internal.accessibility.AccessibilityShortcutController.REDUCE_BRIGHT_COLORS_COMPONENT_NAME
import com.android.settings.R
import com.android.settings.accessibility.ToggleReduceBrightColorsPreferenceFragment
import com.android.settings.accessibility.extradim.data.ExtraDimDataStore
import com.android.settings.accessibility.reduceBrightColorsAvailabilityStatus
import com.android.settings.accessibility.shared.ui.AccessibilityShortcutPreference
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.utils.highlightPreference
import com.android.settingslib.PrimarySwitchPreferenceBinding
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceCategory
import com.android.settingslib.metadata.PreferenceHierarchy
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED
import kotlinx.coroutines.CoroutineScope

@ProvidePreferenceScreen(ExtraDimScreen.KEY)
open class ExtraDimScreen(private val context: Context) :
    PreferenceScreenMixin,
    PrimarySwitchPreferenceBinding,
    BooleanValuePreference,
    PreferenceAvailabilityProvider {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_UNCATEGORIZED)

    private val extraDimStorage by lazy { ExtraDimDataStore(context) }
    override val highlightMenuKey: Int
        get() = R.string.menu_key_accessibility

    override val key: String
        get() = KEY

    // TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.reduce_bright_colors_preference_purpose

    override val indexable
        get() = isAvailable(context)

    override val keywords: Int
        get() = R.string.keywords_reduce_bright_colors

    override val title: Int
        get() = R.string.reduce_bright_colors_preference_title

    override val icon: Int
        get() = R.drawable.ic_reduce_bright_colors

    override val summary: Int
        get() = R.string.reduce_bright_colors_preference_summary

    override fun fragmentClass(): Class<out Fragment> =
        ToggleReduceBrightColorsPreferenceFragment::class.java

    override fun getMetricsCategory(): Int = SettingsEnums.REDUCE_BRIGHT_COLORS_SETTINGS

    override val availabilityDescription =
        "The device must support the reduce bright colors feature."

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context) =
        context.reduceBrightColorsAvailabilityStatus == AVAILABLE

    override fun storage(context: Context): KeyValueStore = extraDimStorage

    override fun getReadPermissions(context: Context) = ExtraDimDataStore.getReadPermissions()

    override fun getReadPermit(
        context: Context,
        callingPid: Int,
        callingUid: Int,
    ): @ReadWritePermit Int = ReadWritePermit.ALLOW

    override fun getWritePermissions(context: Context) = ExtraDimDataStore.getWritePermissions()

    override fun getWritePermit(
        context: Context,
        callingPid: Int,
        callingUid: Int,
    ): @ReadWritePermit Int = ReadWritePermit.ALLOW

    override val supportsWrite = true
    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        Intent(Settings.ACTION_REDUCE_BRIGHT_COLORS_SETTINGS).apply {
            highlightPreference(metadata?.key)
        }

    override fun getPreferenceHierarchy(
        context: Context,
        coroutineScope: CoroutineScope,
    ): PreferenceHierarchy =
        preferenceHierarchy(context) {
            val extraDimStorage = ExtraDimDataStore(context)
            +IntroPreference()
            +ExtraDimIllustrationPreference()
            +ExtraDimMainSwitchPreference(context, extraDimStorage)
            +PreferenceCategory(
                key = "general_categories",
                purpose = R.string.general_categories_purpose,
                title = R.string.accessibility_screen_option,
            ) +=
                {
                    +IntensityPreference(context, extraDimStorage)
                    +PersistentAfterRestartsPreference(context, extraDimStorage)
                    +AccessibilityShortcutPreference(
                        context = context,
                        key = "reduce_bright_colors_shortcut",
                        purpose = R.string.reduce_bright_colors_shortcut_purpose,
                        title = R.string.reduce_bright_colors_shortcut_title,
                        componentName = REDUCE_BRIGHT_COLORS_COMPONENT_NAME,
                        featureName = R.string.reduce_bright_colors_preference_title,
                        metricsCategory = metricsCategory,
                    )
                }
            +FooterPreference()
        }

    companion object {
        const val KEY = "reduce_bright_colors_preference"
    }
}
