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

package com.android.settings.datausage

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.applications.InstalledPackageName
import com.android.settings.applications.getApplicationInfo
import com.android.settings.contract.TAG_DEVICE_STATE_PREFERENCE
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.android.settingslib.metadata.KeyParametersSchema
import com.android.settingslib.metadata.ParameterizedPreferenceScreenArgumentsFactory
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.metadata.ValidatedKeyParameters
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.spaprivileged.model.app.AppListRepositoryImpl
import com.android.settingslib.utils.applications.PackageObservable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_MOBILE_DATA

/** Preference screen for Apps -> Individual App Info -> Mobile data usage. */
@ProvidePreferenceScreen(DataUsageAppDetailScreen.KEY, parameterized = true)
open class DataUsageAppDetailScreen
private constructor(
    val context: Context,
    @Deprecated(
        "This property will be removed once the catalyst framework stops passing the arguments as a bundle. Use the keyParameters instead."
    )
    final override val arguments: Bundle?,
    final override val keyParameters: ValidatedKeyParameters?,
) :
    PreferenceScreenMixin,
    PreferenceTitleProvider,
    PreferenceLifecycleProvider,
    PreferenceAvailabilityProvider {

    override fun tags(context: Context) = arrayOf(
        APP_FUNCTION_MOBILE_DATA,
        TAG_DEVICE_STATE_SCREEN,
        TAG_DEVICE_STATE_PREFERENCE,
        // exclude this screen from api result since we have the same data in api_app_data_usage_screen
        UI_ONLY_PREFERENCE
    )

    private lateinit var keyedObserver: KeyedObserver<String>

    private val packageName: String =
        if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
            keyParameters!!.getRequired(KEY_APP_PACKAGE_NAME)
        } else {
            arguments!!.getString(KEY_APP_PACKAGE_NAME)!!
        }

    private var appInfo = context.getApplicationInfo(packageName)

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

    override val keyParametersSchema: KeyParametersSchema
        get() = parametersSchema

    //TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.app_data_usage_screen_purpose

    override val bindingKey
        get() = "$KEY-$packageName"

    override val screenTitle: Int
        get() = R.string.data_usage_app_summary_title

    override val highlightMenuKey: Int
        get() = R.string.menu_key_apps

    override fun getMetricsCategory() = SettingsEnums.APP_DATA_USAGE


    override fun getTitle(context: Context): CharSequence? =
        appInfo?.loadLabel(context.packageManager)

    override fun isFlagEnabled(context: Context) = Flags.deeplinkApps25q4()

    override val availabilityDescription =
        "The app must be enabled."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context) = appInfo != null

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = AppDataUsage::class.java

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent {
        val intent =
            if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
                makeLaunchIntent(
                    context,
                    AppDataUsageActivity::class.java,
                    keyParameters!!,
                    metadata?.bindingKey,
                )
            } else {
                makeLaunchIntent(
                    context,
                    AppDataUsageActivity::class.java,
                    arguments!!,
                    metadata?.bindingKey,
                )
            }
        intent.apply { data = "package:$packageName".toUri() }

        return intent
    }

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    override fun onCreate(context: PreferenceLifecycleContext) {
        // observer to detect package changes (disabled/enabled/uninstall)
        val observer =
            KeyedObserver<String> { _, _ ->
                appInfo = context.getApplicationInfo(packageName)
                context.notifyPreferenceChange(bindingKey)
            }
        keyedObserver = observer
        val executor = HandlerExecutor.main
        if (isContainer(context)) {
            PackageObservable.get(context).addObserver(packageName, observer, executor)
        }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        if (isContainer(context)) {
            PackageObservable.get(context).removeObserver(packageName, keyedObserver)
        }
    }

    companion object : ParameterizedPreferenceScreenArgumentsFactory {
        const val KEY = "app_data_usage_screen"
        const val KEY_APP_PACKAGE_NAME = "app"

        @JvmStatic
        override val parametersSchema = KeyParametersSchema {
            parameter(KEY_APP_PACKAGE_NAME, "The package name of the app", required = true, type = InstalledPackageName)
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
            val repo = AppListRepositoryImpl(context)
            // Make sure to exclude system apps
            repo.loadAndMaybeExcludeSystemApps(context.userId, true).forEach { app ->
                emit(Bundle(1).apply { putString(KEY_APP_PACKAGE_NAME, app.packageName) })
            }
        }
    }
}
