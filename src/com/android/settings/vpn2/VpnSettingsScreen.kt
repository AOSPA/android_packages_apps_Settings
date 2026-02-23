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

package com.android.settings.vpn2

import android.app.settings.SettingsEnums
import android.content.Context
import android.os.UserManager
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.VpnSettingsActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.restriction.PreferenceRestrictionMixin
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_MOBILE_DATA

// LINT.IfChange
@ProvidePreferenceScreen(VpnSettingsScreen.KEY)
open class VpnSettingsScreen : PreferenceScreenMixin, PreferenceRestrictionMixin {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_MOBILE_DATA)

    override val key: String
        get() = KEY

    // TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.vpn_settings_purpose

    override val title: Int
        get() = R.string.vpn_title

    override val icon: Int
        get() = R.drawable.ic_vpn_key

    override val useAdminDisabledSummary
        get() = true

    override val restrictionKeys
        get() = arrayOf(UserManager.DISALLOW_CONFIG_VPN)

    override fun isEnabled(context: Context) = super<PreferenceRestrictionMixin>.isEnabled(context)

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    override fun fragmentClass(): Class<out Fragment>? = VpnSettings::class.java

    override fun hasCompleteHierarchy() = false

    override val highlightMenuKey
        get() = R.string.menu_key_network

    override fun getMetricsCategory(): Int = SettingsEnums.VPN

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, VpnSettingsActivity::class.java, metadata?.key)

    companion object {
        const val KEY = "vpn_settings"
    }
}
// LINT.ThenChange(VpnSettings.java)
