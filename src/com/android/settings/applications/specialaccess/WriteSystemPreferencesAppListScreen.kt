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
import com.android.settings.R
import com.android.settingslib.flags.Flags
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED

@ProvidePreferenceScreen(WriteSystemPreferencesAppListScreen.KEY)
open class WriteSystemPreferencesAppListScreen : SpecialAccessAppListScreen() {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_UNCATEGORIZED)

    override val key: String
        get() = KEY

    //TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.special_access_write_system_preferences_app_list_purpose

    override val title: Int
        get() = R.string.write_system_preferences_page_title

    override fun isFlagEnabled(context: Context) = Flags.writeSystemPreferencePermissionEnabled()

    override fun getMetricsCategory() = SettingsEnums.PAGE_UNKNOWN

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        if (metadata == null) {
            Intent("android.settings.action.WRITE_SYSTEM_PREFERENCES")
        } else {
            null
        }

    override val appDetailScreenKey: String
        get() = WriteSystemPreferencesAppDetailScreen.KEY

    @Deprecated(
        message =
            "This method will be removed once the catalyst framework stops passing the arguments as a bundle. Use appDetailKeyParameters instead."
    )
    override fun appDetailParameters(context: Context, hierarchyType: Boolean) =
        WriteSystemPreferencesAppDetailScreen.parameters(context, hierarchyType)

    override fun appDetailKeyParameters(context: Context, hierarchyType: Boolean) =
        WriteSystemPreferencesAppDetailScreen.keyParameters(context, hierarchyType)

    companion object {
        const val KEY = "special_access_write_system_preferences_app_list"
    }
}
