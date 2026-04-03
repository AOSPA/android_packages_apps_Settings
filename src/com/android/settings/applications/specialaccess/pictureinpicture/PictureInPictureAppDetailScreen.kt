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

package com.android.settings.applications.specialaccess.pictureinpicture

import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import android.Manifest.permission.USE_PINNED_WINDOWING_LAYER
import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings.ACTION_PICTURE_IN_PICTURE_SETTINGS
import androidx.core.net.toUri
import com.android.settings.CatalystSettingsActivity
import com.android.settings.R
import com.android.settings.applications.CatalystAppListFragment.Companion.DEFAULT_SHOW_SYSTEM
import com.android.settings.applications.getPackageInfoWithActivitiesAndPermissions
import com.android.settings.applications.specialaccess.SpecialAccessAppDetailScreen
import com.android.settings.contract.TAG_DEVICE_STATE_PREFERENCE
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.utils.highlightPreference
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.android.settingslib.metadata.KeyParametersSchema
import com.android.settingslib.metadata.ParameterizedPreferenceScreenArgumentsFactory
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.ValidatedKeyParameters

@ProvidePreferenceScreen(PictureInPictureAppDetailScreen.KEY, parameterized = true)
open class PictureInPictureAppDetailScreen : SpecialAccessAppDetailScreen {

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
        get() = R.string.special_access_picture_in_picture_app_detail_purpose

    override val bindingKey
        get() = "$KEY-$packageName"

    override val screenTitle
        get() = R.string.picture_in_picture_app_detail_title

    override val op
        get() = AppOpsManager.OP_PICTURE_IN_PICTURE

    override val permission: String?
        get() = null

    override val setModeByUid: Boolean?
        get() = false // set op mode by package

    override val switchPreferenceTitle
        get() = R.string.picture_in_picture_app_detail_switch

    override val footerPreferenceTitle
        get() = R.string.picture_in_picture_app_detail_summary

    override fun tags(context: Context) =
        arrayOf(TAG_DEVICE_STATE_SCREEN, TAG_DEVICE_STATE_PREFERENCE)

    override fun isFlagEnabled(context: Context) = context.isPictureInPictureEnabled()

    override val availabilityDescription =
        "The app must be enabled, and must have requested picture in picture permission."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context) =
        super.isAvailable(context) && pictureInPictureFilter(context, packageInfo?.applicationInfo)

    override fun getMetricsCategory() = SettingsEnums.SETTINGS_MANAGE_PICTURE_IN_PICTURE_DETAIL

    override fun getAccessChangeActionMetrics(allowed: Boolean) =
        when (allowed) {
            true -> SettingsEnums.APP_PICTURE_IN_PICTURE_ALLOW
            else -> SettingsEnums.APP_PICTURE_IN_PICTURE_DENY
        }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        Intent(ACTION_PICTURE_IN_PICTURE_SETTINGS).apply {
            data = "package:$packageName".toUri()

            if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
                highlightPreference(keyParameters!!, metadata?.bindingKey)
            } else {
                highlightPreference(arguments!!, metadata?.bindingKey)
            }
        }

    companion object :
        ParameterizedPreferenceScreenArgumentsFactory by SpecialAccessAppDetailScreen.Companion {
        const val KEY = "special_access_picture_in_picture_app_detail"

        @JvmStatic
        override fun keyParameters(context: Context) = keyParameters(context, DEFAULT_SHOW_SYSTEM)

        fun keyParameters(context: Context, showSystemApp: Boolean) =
            keyParameters(context, showSystemApp, ::pictureInPictureFilter)

        @JvmStatic
        @Deprecated(
            "This method will be removed once the catalyst framework stops passing the arguments as a bundle. Use keyParameters instead."
        )
        fun parameters(context: Context) = parameters(context, DEFAULT_SHOW_SYSTEM)

        @Deprecated(
            "This method will be removed once the catalyst framework stops passing the arguments as a bundle. Use keyParameters instead."
        )
        fun parameters(context: Context, showSystemApp: Boolean) =
            parameters(context, showSystemApp, ::pictureInPictureFilter)

        fun pictureInPictureFilter(context: Context, appInfo: ApplicationInfo?): Boolean {
            if (appInfo == null) return false
            val packageInfo =
                context.getPackageInfoWithActivitiesAndPermissions(appInfo.packageName)
                    ?: return false

            return (packageInfo.activities?.any(ActivityInfo::supportsPictureInPicture) ?: false) ||
                (packageInfo.requestedPermissions?.contains(USE_PINNED_WINDOWING_LAYER) ?: false)
        }
    }
}

class PictureInPictureAppDetailActivity :
    CatalystSettingsActivity(PictureInPictureAppDetailScreen.KEY)

internal fun Context.isPictureInPictureEnabled() =
    packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) &&
        !ActivityManager.isLowRamDeviceStatic()
