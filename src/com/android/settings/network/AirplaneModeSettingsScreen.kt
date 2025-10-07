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

package com.android.settings.network

import android.app.settings.SettingsEnums.AIRPLANE_MODE_SETTINGS
import android.content.Context
import androidx.annotation.StringRes
import com.android.server.connectivity.Flags
import com.android.settings.R
import com.android.settings.Settings.AirplaneModeSettingsActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.utils.makeLaunchIntent
import com.android.settings.widget.FooterPreferenceBinding
import com.android.settings.widget.FooterPreferenceMetadata
import com.android.settingslib.PrimarySwitchPreferenceBinding
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.metadata.PreferenceIndexableProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceBindingPlaceholder
import kotlinx.coroutines.CoroutineScope

/** Preference for the Airplane Mode two-target toggle in the Network & Internet screen. */
@ProvidePreferenceScreen(AirplaneModeSettingsScreen.KEY)
open class AirplaneModeSettingsScreen(context: Context) :
    AirplaneModePreference(),
    PreferenceScreenMixin,
    PreferenceIndexableProvider,
    PrimarySwitchPreferenceBinding,
    // Placeholder not needed once NetworkDashboardScreen provides the complete preferenceHierarchy.
    PreferenceBindingPlaceholder {

    private val storage = createDataStore(context)
    private var airplaneModeObserver: KeyedObserver<String?>? = null

    override val key: String
        get() = KEY

    override fun isFlagEnabled(context: Context) = Flags.syncAirplaneModeWithWatches()

    override fun isAvailable(context: Context) =
        context.isAirplaneModeEligible() && context.hasPairedWatchForAirplaneModeSync()

    override fun isIndexable(context: Context) = isAvailable(context)

    override fun storage(context: Context) = storage

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, AirplaneModeSettingsActivity::class.java, metadata?.key)

    override val highlightMenuKey: Int
        @StringRes get() = R.string.menu_key_network

    override fun getMetricsCategory() = AIRPLANE_MODE_SETTINGS

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        if (isEntryPoint(context)) {
            val observer =
                KeyedObserver<String?> { key, _ ->
                    if (key == AirplaneModePreference.KEY) context.notifyPreferenceChange(KEY)
                }
            airplaneModeObserver = observer
            storage.addObserver(AirplaneModePreference.KEY, observer, HandlerExecutor.main)
        }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        if (isEntryPoint(context)) {
            airplaneModeObserver?.let {
                storage.removeObserver(AirplaneModePreference.KEY, it)
                airplaneModeObserver = null
            }
        }
    }

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +AirplaneModeDetailsPreference()
            +AirplaneModeSyncPreference()
            +AirplaneModeSettingsFooter()
        }

    companion object {
        const val KEY = "airplane_mode_settings"
    }
}

class AirplaneModeSettingsFooter : FooterPreferenceMetadata, FooterPreferenceBinding {
    override val key: String
        get() = KEY

    override val title: Int
        @StringRes get() = R.string.airplane_mode_sync_description

    companion object {
        const val KEY = "airplane_mode_footer"
    }
}
