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

package com.android.settings.spa.app.catalyst

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.applications.CatalystAppListFragment.Companion.DEFAULT_SHOW_SYSTEM
import com.android.settings.applications.appinfo.AppInfoDashboardFragment
import com.android.settings.applications.applicationInfoComparator
import com.android.settings.applications.getApplicationInfo
import com.android.settings.applications.packageName
import com.android.settings.applications.specialaccess.AlarmsAndRemindersAppDetailScreen
import com.android.settings.applications.specialaccess.AlarmsAndRemindersAppDetailScreen.Companion.alarmsAndRemindersFilter
import com.android.settings.applications.specialaccess.DisplayOverOtherAppsAppDetailScreen
import com.android.settings.applications.specialaccess.DisplayOverOtherAppsAppDetailScreen.Companion.displayOverOtherAppsFilter
import com.android.settings.applications.specialaccess.InstallUnknownAppsAppDetailScreen
import com.android.settings.applications.specialaccess.InstallUnknownAppsAppDetailScreen.Companion.installUnknownAppsFilter
import com.android.settings.applications.specialaccess.ManageWriteSettingsAppDetailScreen
import com.android.settings.applications.specialaccess.ManageWriteSettingsAppDetailScreen.Companion.manageWriteSettingsFilter
import com.android.settings.applications.specialaccess.SpecialAccessAppDetailScreen
import com.android.settings.applications.specialaccess.SpecialAccessAppDetailScreen.Companion.hasSpecialAccessPermission
import com.android.settings.applications.specialaccess.WriteSystemPreferencesAppDetailScreen
import com.android.settings.applications.specialaccess.WriteSystemPreferencesAppDetailScreen.Companion.writeSystemPreferencesFilter
import com.android.settings.applications.specialaccess.pictureinpicture.PictureInPictureAppDetailScreen
import com.android.settings.applications.specialaccess.pictureinpicture.PictureInPictureAppDetailScreen.Companion.pictureInPictureFilter
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settingslib.catalyst.flags.Flags as CatalystFlags
import com.android.settingslib.metadata.KeyParametersSchema
import com.android.settingslib.metadata.ParameterizedPreferenceScreenArgumentsFactory
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceCategory
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.ValidatedKeyParameters
import com.android.settingslib.metadata.packageName
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.metadata.withAppPackageName
import com.android.settingslib.spaprivileged.model.app.AppListRepositoryImpl
import kotlin.let
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@ProvidePreferenceScreen(AppInfoScreen.KEY, parameterized = true)
open class AppInfoScreen
private constructor(
    val context: Context,
    @Deprecated(
        "This property will be removed once the catalyst framework stops passing the arguments as a bundle. Use the keyParameters instead."
    )
    final override val arguments: Bundle?,
    final override val keyParameters: ValidatedKeyParameters?,
) : PreferenceScreenMixin, PreferenceTitleProvider, PreferenceAvailabilityProvider {

    private val packageName: String =
        if (CatalystFlags.catalystUseKeyParameters()) {
            keyParameters!!.packageName
        } else {
            arguments!!.packageName
        }

    private val appInfo = context.getApplicationInfo(packageName)

    @Deprecated(
        "This constructor will be removed once the catalyst framework stops passing the arguments as a bundle. Use the other constructor instead."
    )
    constructor(context: Context, args: Bundle) : this(context, args, null)

    constructor(
        context: Context,
        keyParameters: ValidatedKeyParameters,
    ) : this(context, null, keyParameters)

    override val key: String
        get() = KEY

    override fun getMetricsCategory() = SettingsEnums.APPLICATIONS_INSTALLED_APP_DETAILS

    override val screenTitle: Int
        get() = R.string.application_info_label

    override fun getTitle(context: Context): CharSequence? =
        appInfo?.loadLabel(context.packageManager)

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        Intent("android.settings.APPLICATION_DETAILS_SETTINGS").apply {
            data = "package:$packageName".toUri()
            // TODO: create highlight intent for SpaActivity.
        }

    override val highlightMenuKey: Int
        get() = R.string.menu_key_apps

    override fun isFlagEnabled(context: Context) = Flags.deeplinkApps25q4()

    override fun isAvailable(context: Context) = appInfo != null

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = AppInfoDashboardFragment::class.java

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +PreferenceCategory("advanced_app_info", R.string.advanced_apps) += {
                var newKeyParameters: ValidatedKeyParameters? = null
                if (CatalystFlags.catalystUseKeyParameters()) {
                    newKeyParameters =
                        SpecialAccessAppDetailScreen.Companion.parametersSchema.prepareWith(
                            keyParameters,
                            SpecialAccessAppDetailScreen.KEY_INTENT_SOURCE to SOURCE,
                        )
                } else {
                    arguments!!.putString("source", SOURCE)
                }

                appInfo?.let {
                    if (hasSpecialAccessPermission(context, it, ::displayOverOtherAppsFilter)) {
                        if (CatalystFlags.catalystUseKeyParameters()) {
                            +(DisplayOverOtherAppsAppDetailScreen.KEY withParameters
                                newKeyParameters!!)
                        } else {
                            +(DisplayOverOtherAppsAppDetailScreen.KEY args arguments!!)
                        }
                    }
                    if (hasSpecialAccessPermission(context, it, ::manageWriteSettingsFilter)) {
                        if (CatalystFlags.catalystUseKeyParameters()) {
                            +(ManageWriteSettingsAppDetailScreen.KEY withParameters
                                newKeyParameters!!)
                        } else {
                            +(ManageWriteSettingsAppDetailScreen.KEY args arguments!!)
                        }
                    }
                    if (hasSpecialAccessPermission(context, it, ::pictureInPictureFilter)) {
                        if (CatalystFlags.catalystUseKeyParameters()) {
                            +(PictureInPictureAppDetailScreen.KEY withParameters newKeyParameters!!)
                        } else {
                            +(PictureInPictureAppDetailScreen.KEY args arguments!!)
                        }
                    }
                    if (hasSpecialAccessPermission(context, it, ::installUnknownAppsFilter)) {
                        if (CatalystFlags.catalystUseKeyParameters()) {
                            +(InstallUnknownAppsAppDetailScreen.KEY withParameters
                                newKeyParameters!!)
                        } else {
                            +(InstallUnknownAppsAppDetailScreen.KEY args arguments!!)
                        }
                    }
                    if (hasSpecialAccessPermission(context, it, ::alarmsAndRemindersFilter)) {
                        if (CatalystFlags.catalystUseKeyParameters()) {
                            +(AlarmsAndRemindersAppDetailScreen.KEY withParameters
                                newKeyParameters!!)
                        } else {
                            +(AlarmsAndRemindersAppDetailScreen.KEY args arguments!!)
                        }
                    }
                    if (hasSpecialAccessPermission(context, it, ::writeSystemPreferencesFilter)) {
                        if (CatalystFlags.catalystUseKeyParameters()) {
                            +(WriteSystemPreferencesAppDetailScreen.KEY withParameters
                                newKeyParameters!!)
                        } else {
                            +(WriteSystemPreferencesAppDetailScreen.KEY args arguments!!)
                        }
                    }
                }
            }
        }

    companion object : ParameterizedPreferenceScreenArgumentsFactory {
        const val KEY = "installed_app_detail_settings_screen"
        const val SOURCE = "appinfo"

        @JvmStatic override val parametersSchema = KeyParametersSchema { withAppPackageName() }

        @JvmStatic
        override fun keyParameters(context: Context): Flow<ValidatedKeyParameters> {
            // TODO (b/457649430): when the catalyst framework stops passing the arguments as a
            // bundle: replace the parameters(context) call to the actual implementation,
            // or make this function the primary implementation and the legacy parameters() should
            // call this one.
            return parameters(context).map { bundle -> parametersSchema.prepare(bundle) }
        }

        @Deprecated(
            "This method will be removed once the catalyst framework stops passing the arguments as a bundle. Use keyParameters instead."
        )
        @JvmStatic
        fun parameters(context: Context): Flow<Bundle> = flow {
            AppListRepositoryImpl(context)
                .loadAndMaybeExcludeSystemApps(context.userId, !DEFAULT_SHOW_SYSTEM)
                .sortedWith(context.applicationInfoComparator)
                .forEach { emit(Bundle(1).apply { putString("pkg", it.packageName) }) }
        }
    }
}
