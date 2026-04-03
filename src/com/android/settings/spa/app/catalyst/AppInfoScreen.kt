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
import com.android.settings.applications.InstalledPackageName
import com.android.settings.applications.appinfo.AppInfoDashboardFragment
import com.android.settings.applications.applicationInfoComparator
import com.android.settings.applications.getApplicationInfo
import com.android.settings.applications.packageName
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.android.settingslib.metadata.KEY_PACKAGE_NAME
import com.android.settingslib.metadata.KeyParametersSchema
import com.android.settingslib.metadata.ParameterizedPreferenceScreenArgumentsFactory
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.metadata.ValidatedKeyParameters
import com.android.settingslib.metadata.packageName
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.spaprivileged.model.app.AppListRepositoryImpl
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
    override fun tags(context: Context) = arrayOf(
        APP_FUNCTION_UNCATEGORIZED,
        // exclude this screen from api result since we have the data in api_installed_app_detail_settings_screen
        UI_ONLY_PREFERENCE,
    )

    private val packageName: String? =
        if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
            keyParameters!!.packageName
        } else {
            arguments!!.packageName
        }

    private val appInfo = packageName?.let { context.getApplicationInfo(it) }

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

    override val purpose: Int
        get() = R.string.installed_app_detail_settings_screen_purpose

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

    override val availabilityDescription = "The app must be installed."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context) = appInfo != null

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = AppInfoDashboardFragment::class.java

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            // TODO (b/484948332)
        }

    companion object : ParameterizedPreferenceScreenArgumentsFactory {
        const val KEY = "installed_app_detail_settings_screen"
        const val SOURCE = "appinfo"

        @JvmStatic
        override val parametersSchema = KeyParametersSchema {
            parameter(
                KEY_PACKAGE_NAME,
                "The package name of the app",
                required = true,
                type = InstalledPackageName
            )
        }

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
