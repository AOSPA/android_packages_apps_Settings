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

package com.android.settings.applications.specialaccess

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION
import com.android.settings.CatalystSettingsActivity
import com.android.settings.R
import com.android.settings.Utils.isSystemAlertWindowEnabled
import com.android.settings.applications.CatalystAppListFragment
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED

@ProvidePreferenceScreen(DisplayOverOtherAppsAppListScreen.KEY)
open class DisplayOverOtherAppsAppListScreen :
    SpecialAccessAppListScreen(), PreferenceAvailabilityProvider {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_UNCATEGORIZED, TAG_DEVICE_STATE_SCREEN)

    override val key: String
        get() = KEY

    //TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.special_access_draw_overlay_app_list_purpose

    override val title: Int
        get() = R.string.system_alert_window_settings

    override val keywords: Int
        get() = R.string.keywords_draw_overlay

    override val sensitivityLevel = SensitivityLevel.DO_NOT_EXPOSE

    override fun isFlagEnabled(context: Context) = Flags.deeplinkApps25q4()

    override fun getMetricsCategory() = SettingsEnums.SYSTEM_ALERT_WINDOW_APPS


    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        if (metadata == null) Intent(ACTION_MANAGE_OVERLAY_PERMISSION) else null

    override val appDetailScreenKey
        get() = DisplayOverOtherAppsAppDetailScreen.KEY

    @Deprecated(
        message =
            "This method will be removed once the catalyst framework stops passing the arguments as a bundle. Use appDetailKeyParameters instead."
    )
    override fun appDetailParameters(context: Context, hierarchyType: Boolean) =
        DisplayOverOtherAppsAppDetailScreen.parameters(context, hierarchyType)

    override fun appDetailKeyParameters(context: Context, hierarchyType: Boolean) =
        DisplayOverOtherAppsAppDetailScreen.keyParameters(context, hierarchyType)

    companion object {
        const val KEY = "special_access_draw_overlay_app_list"
    }

    override val availabilityDescription =
        "This must not be a low ram device OR Android must be version P or below."

    override fun getAvailabilityStability() = PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun isAvailable(context: Context) = isSystemAlertWindowEnabled(context)
}

class DisplayOverOtherAppsAppListActivity :
    CatalystSettingsActivity(
        DisplayOverOtherAppsAppListScreen.KEY,
        CatalystAppListFragment::class.java,
    )
