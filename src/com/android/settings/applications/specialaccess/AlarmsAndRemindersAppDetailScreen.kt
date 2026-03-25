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
import android.Manifest.permission.SCHEDULE_EXACT_ALARM
import android.Manifest.permission.USE_EXACT_ALARM
import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.compat.CompatChanges
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.os.PowerExemptionManager
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
import androidx.core.net.toUri
import com.android.settings.applications.CatalystAppListFragment.Companion.DEFAULT_SHOW_SYSTEM
import com.android.settings.applications.getPackageInfoWithPermissions
import com.android.settings.applications.isPermissionRequested
import com.android.settings.flags.Flags
import com.android.settings.utils.highlightPreference
import com.android.settingslib.R
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.android.settingslib.metadata.KeyParametersSchema
import com.android.settingslib.metadata.ParameterizedPreferenceScreenArgumentsFactory
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.ValidatedKeyParameters
import com.android.settingslib.spaprivileged.model.app.userHandle

/**
 * The app detail catalyst screen for "Alarms & reminders" special app access.
 *
 * This screen is accessible from: Settings > Apps > Special app access > Alarms &
 * reminders > [app name]
 */
@ProvidePreferenceScreen(AlarmsAndRemindersAppDetailScreen.KEY, parameterized = true)
open class AlarmsAndRemindersAppDetailScreen : SpecialAccessAppDetailScreen {

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
        get() = R.string.special_access_alarms_and_reminders_app_detail_purpose

    override val bindingKey
        get() = "$KEY-$packageName"

    override val screenTitle
        get() = R.string.alarms_and_reminders_title

    override val op
        get() = AppOpsManager.OP_SCHEDULE_EXACT_ALARM

    override val permission: String?
        get() = PERMISSION

    override val setModeByUid: Boolean?
        get() = true

    override val switchPreferenceTitle
        get() = R.string.alarms_and_reminders_switch_title

    override val footerPreferenceTitle
        get() = R.string.alarms_and_reminders_footer_title

    override val availabilityDescription =
        "The app must be enabled, and must have requested exact alarm permission."

    // Edge case: what if the app's read permission is revoked/granted

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE
    override fun isAvailable(context: Context) =
        super.isAvailable(context) &&
            alarmsAndRemindersFilter(context, packageInfo?.applicationInfo)

    override fun getMetricsCategory() = SettingsEnums.ALARMS_AND_REMINDERS

    override fun isFlagEnabled(context: Context) = Flags.deeplinkApps25q4()

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        Intent(ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = "package:$packageName".toUri()

            if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
                highlightPreference(keyParameters!!, metadata?.bindingKey)
            } else {
                highlightPreference(arguments!!, metadata?.bindingKey)
            }
        }

    companion object :
        ParameterizedPreferenceScreenArgumentsFactory by SpecialAccessAppDetailScreen.Companion {
        const val KEY = "special_access_alarms_and_reminders_app_detail"
        const val BROADER_PERMISSION = USE_EXACT_ALARM
        const val PERMISSION = SCHEDULE_EXACT_ALARM

        @JvmStatic
        override fun keyParameters(context: Context) = keyParameters(context, DEFAULT_SHOW_SYSTEM)

        fun keyParameters(context: Context, showSystemApp: Boolean) =
            keyParameters(context, showSystemApp, ::alarmsAndRemindersFilter)

        @JvmStatic
        @Deprecated(
            "This method will be removed once the catalyst framework stops passing the arguments as a bundle. Use keyParameters instead."
        )
        fun parameters(context: Context) = parameters(context, DEFAULT_SHOW_SYSTEM)

        @Deprecated(
            "This method will be removed once the catalyst framework stops passing the arguments as a bundle. Use keyParameters instead."
        )
        fun parameters(context: Context, showSystemApp: Boolean) =
            parameters(context, showSystemApp, ::alarmsAndRemindersFilter)

        fun alarmsAndRemindersFilter(context: Context, appInfo: ApplicationInfo?): Boolean {
            if (appInfo == null) return false
            val packageInfo =
                context.getPackageInfoWithPermissions(appInfo.packageName) ?: return false

            val hasRequestScheduleExactAlarmPermission =
                isPermissionRequested(packageInfo, PERMISSION) &&
                    CompatChanges.isChangeEnabled(
                        AlarmManager.REQUIRE_EXACT_ALARM_PERMISSION,
                        appInfo.packageName,
                        appInfo.userHandle,
                    )
            val hasRequestUseExactAlarm =
                isPermissionRequested(packageInfo, BROADER_PERMISSION) &&
                    CompatChanges.isChangeEnabled(
                        AlarmManager.ENABLE_USE_EXACT_ALARM,
                        appInfo.packageName,
                        appInfo.userHandle,
                    )
            val isPowerAllowListed =
                context
                    .getSystemService(PowerExemptionManager::class.java)
                    ?.isAllowListed(appInfo.packageName, true) ?: false
            val isTrumped =
                hasRequestScheduleExactAlarmPermission &&
                    (hasRequestUseExactAlarm || isPowerAllowListed)

            return hasRequestScheduleExactAlarmPermission && !isTrumped
        }
    }
}
