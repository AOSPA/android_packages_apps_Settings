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

import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import android.Manifest.permission.READ_SYSTEM_PREFERENCES
import android.Manifest.permission.WRITE_SYSTEM_PREFERENCES
import android.app.AppOpsManager
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.core.net.toUri
import com.android.settings.R
import com.android.settings.applications.CatalystAppListFragment.Companion.DEFAULT_SHOW_SYSTEM
import com.android.settings.applications.getPackageInfoWithPermissions
import com.android.settings.applications.isPermissionGranted
import com.android.settings.applications.isPermissionRequested
import com.android.settingslib.metadata.KeyParametersSchema
import com.android.settingslib.metadata.ParameterizedPreferenceScreenArgumentsFactory
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.ValidatedKeyParameters

@ProvidePreferenceScreen(WriteSystemPreferencesAppDetailScreen.KEY, parameterized = true)
open class WriteSystemPreferencesAppDetailScreen : SpecialAccessAppDetailScreen {

    @Deprecated(
        "This constructor will be removed once the catalyst framework stops passing the arguments as a bundle. Use the other constructor instead."
    )
    constructor(context: Context, arguments: Bundle) : super(context, arguments)

    constructor(
        context: Context,
        keyArguments: ValidatedKeyParameters,
    ) : super(context, keyArguments)

    override val key
        get() = KEY

    override val keyParametersSchema: KeyParametersSchema
        get() = parametersSchema

    //TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.special_access_write_system_preferences_app_detail_purpose

    override val bindingKey
        get() = "$KEY-$packageName"

    override val screenTitle
        get() = R.string.write_system_preferences_page_title

    override val op
        get() = AppOpsManager.OP_WRITE_SYSTEM_PREFERENCES

    override val permission: String?
        get() = PERMISSION

    override val switchPreferenceTitle
        get() = R.string.write_system_preferences_switch_title

    override val footerPreferenceTitle
        get() = R.string.write_system_preferences_footer_description

    override val availabilityDescription =
        "The app must be enabled, and must have requested write system preferences permission."

    // Edge case: what if the app's read permission is revoked/granted

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE
    override fun isAvailable(context: Context) =
        super.isAvailable(context) &&
            writeSystemPreferencesFilter(context, packageInfo?.applicationInfo)

    override fun getMetricsCategory() = SettingsEnums.PAGE_UNKNOWN

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        Intent("android.settings.action.WRITE_SYSTEM_PREFERENCES").apply {
            data = "package:$packageName".toUri()
        }

    companion object :
        ParameterizedPreferenceScreenArgumentsFactory by SpecialAccessAppDetailScreen.Companion {
        const val KEY = "special_access_write_system_preferences_app_detail"
        const val PERMISSION = WRITE_SYSTEM_PREFERENCES

        @JvmStatic
        override fun keyParameters(context: Context) = keyParameters(context, DEFAULT_SHOW_SYSTEM)

        fun keyParameters(context: Context, showSystemApp: Boolean) =
            keyParameters(context, showSystemApp, ::writeSystemPreferencesFilter)

        @JvmStatic
        @Deprecated(
            "This method will be removed once the catalyst framework stops passing the arguments as a bundle. Use keyParameters instead."
        )
        fun parameters(context: Context) = parameters(context, DEFAULT_SHOW_SYSTEM)

        @JvmStatic
        @Deprecated(
            "This method will be removed once the catalyst framework stops passing the arguments as a bundle. Use keyParameters instead."
        )
        fun parameters(context: Context, showSystemApp: Boolean) =
            parameters(context, showSystemApp, ::writeSystemPreferencesFilter)

        fun writeSystemPreferencesFilter(context: Context, appInfo: ApplicationInfo?): Boolean {
            if (appInfo == null) return false
            val packageInfo =
                context.getPackageInfoWithPermissions(appInfo.packageName) ?: return false

            return isPermissionGranted(packageInfo, READ_SYSTEM_PREFERENCES) &&
                isPermissionRequested(packageInfo, PERMISSION)
        }
    }
}
