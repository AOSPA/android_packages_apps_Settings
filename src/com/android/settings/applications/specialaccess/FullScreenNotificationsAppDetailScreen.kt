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
import android.Manifest.permission.USE_FULL_SCREEN_INTENT
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
import com.android.settings.applications.isPermissionRequested
import com.android.settings.contract.TAG_DEVICE_STATE_PREFERENCE
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.flags.Flags
import com.android.settings.utils.highlightPreference
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.android.settingslib.metadata.KeyParametersSchema
import com.android.settingslib.metadata.ParameterizedPreferenceScreenArgumentsFactory
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.ValidatedKeyParameters

/**
 * The app detail catalyst screen for "Full-screen notifications" special app access.
 *
 * This screen is accessible from: Settings > Apps > Special app access > Full-screen
 * notifications > [app name]
 */
@ProvidePreferenceScreen(FullScreenNotificationsAppDetailScreen.KEY, parameterized = true)
open class FullScreenNotificationsAppDetailScreen : SpecialAccessAppDetailScreen {

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
        get() = R.string.special_access_full_screen_notifications_app_detail_purpose

    override val bindingKey
        get() = "$KEY-$packageName"

    override val screenTitle: Int
        get() = R.string.full_screen_intent_title

    override val op
        get() = AppOpsManager.OP_USE_FULL_SCREEN_INTENT

    override val permission: String?
        get() = PERMISSION

    override val setModeByUid: Boolean?
        get() = true

    override val switchPreferenceTitle
        get() = R.string.permit_full_screen_intent

    override val footerPreferenceTitle
        get() = R.string.footer_description_full_screen_intent

    override fun tags(context: Context) =
        arrayOf(TAG_DEVICE_STATE_SCREEN, TAG_DEVICE_STATE_PREFERENCE)

    override fun getMetricsCategory() = SettingsEnums.PAGE_UNKNOWN // TODO: correct page id

    override fun isFlagEnabled(context: Context) = Flags.deeplinkApps25q4()

    override val availabilityDescription =
        "The app must be enabled, and must have requested full screen intent permission."

    // Edge case: what if the app's read permission is revoked/granted

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE
    override fun isAvailable(context: Context) =
        super.isAvailable(context) && fullScreenIntentFilter(context, packageInfo?.applicationInfo)

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        Intent("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT").apply {
            // TODO: b/444137482 - Launch Catalyst page when UI is ready
            data = "package:$packageName".toUri()

            if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
                highlightPreference(keyParameters!!, metadata?.bindingKey)
            } else {
                highlightPreference(arguments!!, metadata?.bindingKey)
            }
        }

    companion object :
        ParameterizedPreferenceScreenArgumentsFactory by SpecialAccessAppDetailScreen.Companion {
        const val KEY = "special_access_full_screen_notifications_app_detail"
        const val PERMISSION = USE_FULL_SCREEN_INTENT

        @JvmStatic
        override fun keyParameters(context: Context) = keyParameters(context, DEFAULT_SHOW_SYSTEM)

        fun keyParameters(context: Context, showSystemApp: Boolean) =
            keyParameters(context, showSystemApp, ::fullScreenIntentFilter)

        @JvmStatic
        @Deprecated(
            "This method will be removed once the catalyst framework stops passing the arguments as a bundle. Use keyParameters instead."
        )
        fun parameters(context: Context) = parameters(context, DEFAULT_SHOW_SYSTEM)

        @Deprecated(
            "This method will be removed once the catalyst framework stops passing the arguments as a bundle. Use keyParameters instead."
        )
        fun parameters(context: Context, showSystemApp: Boolean) =
            parameters(context, showSystemApp, ::fullScreenIntentFilter)

        private fun fullScreenIntentFilter(context: Context, appInfo: ApplicationInfo?): Boolean {
            if (appInfo == null) return false
            val packageInfo =
                context.getPackageInfoWithPermissions(appInfo.packageName) ?: return false

            return isPermissionRequested(packageInfo, PERMISSION)
        }
    }
}
