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

package com.android.settings.system

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.ResetDashboardActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

// LINT.IfChange
@ProvidePreferenceScreen(ResetDashboardScreen.KEY)
open class ResetDashboardScreen : PreferenceScreenMixin, PreferenceAvailabilityProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.reset_dashboard_title

    override val icon: Int
        get() = R.drawable.ic_restore

    override val highlightMenuKey: Int
        get() = R.string.menu_key_system

    override fun getMetricsCategory() = SettingsEnums.RESET_DASHBOARD

    override fun isFlagEnabled(context: Context) = Flags.deeplinkSystem25q4()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = ResetDashboardFragment::class.java

    override fun isAvailable(context: Context) =
        context.resources.getBoolean(R.bool.config_show_reset_dashboard)

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, ResetDashboardActivity::class.java, metadata?.key)

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    companion object {
        const val KEY = "reset_dashboard"
    }
}
// LINT.ThenChange(
//     ResetDashboardFragment.java,
//     ResetPreferenceController.java,
// )
