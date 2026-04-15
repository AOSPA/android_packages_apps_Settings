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
package com.android.settings.gestures

import android.app.settings.SettingsEnums
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_2BUTTON
import android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL
import com.android.internal.R as InternalR
import com.android.settings.R
import com.android.settings.Settings.NavigationModeSettingsActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.METADATA_IN_UI
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED

// LINT.IfChange
@ProvidePreferenceScreen(SystemNavigationGestureScreen.KEY)
class SystemNavigationGestureScreen :
    PreferenceScreenMixin, PreferenceAvailabilityProvider, PreferenceSummaryProvider {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_UNCATEGORIZED)


    override val key: String
        get() = KEY

    // TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.gesture_system_navigation_input_summary_purpose

    override val title: Int
        get() = R.string.system_navigation_title

    override val highlightMenuKey: Int
        get() = R.string.menu_key_system

    override fun getMetricsCategory() = SettingsEnums.SETTINGS_GESTURE_SWIPE_UP

    override fun fragmentClass() = SystemNavigationGestureSettings::class.java

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) { +SystemNavigationGestureScreenPreference(this@SystemNavigationGestureScreen) }

    override fun hasCompleteHierarchy() = false

    override val keywords: Int
        get() = R.string.keywords_system_navigation

    override val availabilityDescription =
        "The device must support the swipe up gesture."

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context) = context.isGestureAvailable()

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, NavigationModeSettingsActivity::class.java, metadata?.key)

    override fun getSummary(context: Context): CharSequence? =
        when {
            context.isGestureNavigationEnabled() ->
                context.getText(R.string.edge_to_edge_navigation_title)
            context.is2ButtonNavigationEnabled() ->
                context.getText(R.string.swipe_up_to_switch_apps_title)
            else -> context.getText(R.string.legacy_navigation_title)
        }

    fun Context.isGestureAvailable(): Boolean {
        // Skip if the swipe up settings are not available
        if (!resources.getBoolean(InternalR.bool.config_swipe_up_gesture_setting_available)) {
            return false
        }

        // Skip if the recents component is not defined
        val recentsComponentName =
            ComponentName.unflattenFromString(
                getString(InternalR.string.config_recentsComponentName)
            ) ?: return false

        // Skip if the overview proxy service exists
        val quickStepIntent = Intent(ACTION_QUICKSTEP).setPackage(recentsComponentName.packageName)
        return packageManager.resolveService(quickStepIntent, PackageManager.MATCH_SYSTEM_ONLY) !=
            null
    }

    fun Context.isGestureNavigationEnabled(): Boolean =
        NAV_BAR_MODE_GESTURAL ==
            resources.getInteger(InternalR.integer.config_navBarInteractionMode)

    fun Context.is2ButtonNavigationEnabled(): Boolean =
        NAV_BAR_MODE_2BUTTON == resources.getInteger(InternalR.integer.config_navBarInteractionMode)

    class SystemNavigationGestureScreenPreference(
        private val screenMetadata : SystemNavigationGestureScreen
    ) : PreferenceMetadata, PreferenceSummaryProvider, PreferenceAvailabilityProvider, PersistentPreference<String> {
        override val key : String
            get() = "gesture_system_navigation_input_summary_preference"

        override val purpose : Int
            get() = screenMetadata.purpose

        override val supportsWrite: Boolean
            get() = false

        override val valueType = String::class.javaObjectType

        override fun storage(context: Context): KeyValueStore = createSummaryStorage(context, key)

        override fun tags(context: Context) = arrayOf(METADATA_IN_UI)

        override val indexable = false

        override fun isEnabled(context: Context) : Boolean = screenMetadata.isEnabled(context)

        override fun getSummary(context: Context) : CharSequence? = screenMetadata.getSummary(context)

        override val availabilityDescription = screenMetadata.availabilityDescription

        override fun getAvailabilityStability() = screenMetadata.getAvailabilityStability()

        override fun isAvailable(context: Context) : Boolean = screenMetadata.isAvailable(context)
    }

    companion object {
        const val KEY = "gesture_system_navigation_input_summary"

        private const val ACTION_QUICKSTEP = "android.intent.action.QUICKSTEP_SERVICE"
    }
}
// LINT.ThenChange(SystemNavigationGestureSettings.java, SystemNavigationPreferenceController.java)
