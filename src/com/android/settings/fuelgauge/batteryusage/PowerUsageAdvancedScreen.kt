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

package com.android.settings.fuelgauge.batteryusage

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.PowerUsageAdvancedActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_BATTERY
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

// LINT.IfChange
@ProvidePreferenceScreen(PowerUsageAdvancedScreen.KEY)
open class PowerUsageAdvancedScreen : PreferenceScreenMixin, PreferenceAvailabilityProvider {
    override fun tags(context: Context) = arrayOf(
        APP_FUNCTION_BATTERY,
        // exclude this screen from api result since we have the same data in api_battery_usage_summary
        UI_ONLY_PREFERENCE
    )

    override val key: String
        get() = KEY

    // TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.battery_usage_summary_purpose

    override val title: Int
        get() = R.string.advanced_battery_preference_title

    override val summary: Int
        get() = R.string.advanced_battery_preference_summary

    override val screenTitle: Int
        get() = R.string.advanced_battery_title

    override val keywords: Int
        get() = R.string.keywords_battery_usage

    override fun getMetricsCategory() = SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL

    override val highlightMenuKey
        get() = R.string.menu_key_battery

    override fun fragmentClass(): Class<out Fragment>? = PowerUsageAdvanced::class.java

    override fun hasCompleteHierarchy() = false

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, PowerUsageAdvancedActivity::class.java, metadata?.key)

    override val availabilityDescription =
        "The device must support showing battery usage in settings."

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context) =
        featureFactory.powerUsageFeatureProvider.isBatteryUsageEnabled()

    companion object {
        const val KEY = "battery_usage_summary"
    }
}
// LINT.ThenChange(PowerUsageAdvanced.java)
