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
import android.os.UserHandle
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.applications.AppStorageSettings
import com.android.settings.applications.InstalledPackageName
import com.android.settings.applications.getApplicationInfo
import com.android.settings.applications.specialaccess.InteractAcrossProfilesAppDetailScreen
import com.android.settings.contract.TAG_DEVICE_STATE_PREFERENCE
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.spa.app.storage.StorageType
import com.android.settings.utils.highlightPreference
import com.android.settingslib.applications.StorageStatsSource
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.android.settingslib.metadata.KeyParametersSchema
import com.android.settingslib.metadata.ParameterizedPreferenceScreenArgumentsFactory
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.metadata.ValidatedKeyParameters
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_STORAGE
import com.android.settingslib.spaprivileged.model.app.AppListRepositoryImpl
import com.android.settingslib.spaprivileged.model.app.AppStorageRepository
import com.android.settingslib.spaprivileged.model.app.AppStorageRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

// LINT.IfChange
@ProvidePreferenceScreen(AppInfoStorageScreen.KEY, parameterized = true)
open class AppInfoStorageScreen
private constructor(
    val context: Context,
    @Deprecated(
        "This property will be removed once the catalyst framework stops passing the arguments as a bundle. Use the keyParameters instead."
    )
    final override val arguments: Bundle?,
    final override val keyParameters: ValidatedKeyParameters?,
) :
    PreferenceScreenMixin,
    PreferenceSummaryProvider,
    PreferenceTitleProvider,
    PreferenceAvailabilityProvider {
    override fun tags(context: Context) =
        arrayOf(APP_FUNCTION_STORAGE, TAG_DEVICE_STATE_SCREEN, TAG_DEVICE_STATE_PREFERENCE)

    private val packageName: String =
        if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
            keyParameters!!.getRequired(
                InteractAcrossProfilesAppDetailScreen.Companion.KEY_APP_PACKAGE_NAME
            )
        } else {
            arguments!!.getString(
                InteractAcrossProfilesAppDetailScreen.Companion.KEY_APP_PACKAGE_NAME
            )!!
        }

    private val appInfo = context.getApplicationInfo(packageName)

    private val repo = AppStorageRepositoryImpl(context)

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

    override val purpose: Int
        get() = R.string.device_state_app_info_storage_purpose

    override val screenTitle: Int
        get() = R.string.storage_label

    override val highlightMenuKey: Int
        get() = R.string.menu_key_apps

    override fun getMetricsCategory() = SettingsEnums.APPLICATIONS_APP_STORAGE

    override fun getTitle(context: Context): CharSequence? =
        appInfo?.loadLabel(context.packageManager)

    override fun getSummary(context: Context): CharSequence? = appInfo?.let { repo.formatSize(it) }

    override fun isFlagEnabled(context: Context) = Flags.catalystAppList()

    override val availabilityDescription = "The app must be installed."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context) = appInfo != null

    override fun extras(context: Context): Bundle? =
        Bundle(1).apply { putString(KEY_EXTRA_PACKAGE_NAME, packageName) }

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = AppStorageSettings::class.java

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        Intent("com.android.settings.APP_STORAGE_SETTINGS").apply {
            data = "package:$packageName".toUri()
            if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
                highlightPreference(keyParameters!!, metadata?.key)
            } else {
                highlightPreference(arguments!!, metadata?.key)
            }
        }

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            val stats = context.getStatsForPackage() ?: return@preferenceHierarchy
            +AppSizePreference(stats, repo)
            +AppUserDataSizePreference(stats, repo)
            +AppCacheSizePreference(stats, repo)
            +AppTotalSizePreference(stats, repo)
        }

    private fun Context.getStatsForPackage() =
        try {
            appInfo?.let {
                StorageStatsSource(this)
                    .getStatsForPackage(it.volumeUuid, it.packageName, UserHandle.of(userId))
            }
        } catch (_: Exception) {
            // error during lookup, return no result
            null
        }

    companion object : ParameterizedPreferenceScreenArgumentsFactory {
        const val KEY = "device_state_app_info_storage"
        const val KEY_EXTRA_PACKAGE_NAME = "package_name"
        const val KEY_APP_PACKAGE_NAME = "app"

        @JvmStatic
        override val parametersSchema = KeyParametersSchema {
            parameter(
                KEY_APP_PACKAGE_NAME,
                "The package name of the app",
                required = true,
                type = InstalledPackageName,
            )
        }

        @JvmStatic
        override fun keyParameters(context: Context): Flow<ValidatedKeyParameters> {
            // TODO (b/457649430): when the catalyst framework stops passing the arguments as a
            //  bundle: replace the parameters(context) call to the actual implementation,
            // or make this function the primary implementation and the legacy parameters() should
            // call this one.
            return parameters(context).map { bundle -> parametersSchema.prepare(bundle) }
        }

        @Deprecated(
            "This method will be removed once the catalyst framework stops passing the arguments as a bundle. Use keyParameters instead."
        )
        @JvmStatic
        fun parameters(context: Context): Flow<Bundle> = flow {
            AppListRepositoryImpl(context).loadApps(context.userId).forEach { app ->
                if (StorageType.Apps.filter(app)) {
                    emit(Bundle(1).apply { putString(KEY_APP_PACKAGE_NAME, app.packageName) })
                }
            }
        }
    }
}

private class AppSizePreference(
    private val stats: StorageStatsSource.AppStorageStats,
    private val repo: AppStorageRepository,
) : PreferenceMetadata, PreferenceSummaryProvider {

    override val key: String
        get() = "app_size"

    override val purpose: Int
        get() = R.string.app_size_purpose

    override val title: Int
        get() = R.string.application_size_label

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override fun getSummary(context: Context): CharSequence? = repo.formatSizeBytes(stats.codeBytes)
}

private class AppUserDataSizePreference(
    private val stats: StorageStatsSource.AppStorageStats,
    private val repo: AppStorageRepository,
) : PreferenceMetadata, PreferenceSummaryProvider {

    override val key: String
        get() = "data_size"

    override val purpose: Int
        get() = R.string.data_size_purpose

    override val title: Int
        get() = R.string.data_size_label

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override fun getSummary(context: Context): CharSequence? =
        repo.formatSizeBytes(stats.dataBytes - stats.cacheBytes)
}

private class AppCacheSizePreference(
    private val stats: StorageStatsSource.AppStorageStats,
    private val repo: AppStorageRepository,
) : PreferenceMetadata, PreferenceSummaryProvider {

    override val key: String
        get() = "cache_size"

    override val purpose: Int
        get() = R.string.cache_size_purpose

    override val title: Int
        get() = R.string.cache_size_label

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override fun getSummary(context: Context): CharSequence? =
        repo.formatSizeBytes(stats.cacheBytes)
}

private class AppTotalSizePreference(
    private val stats: StorageStatsSource.AppStorageStats,
    private val repo: AppStorageRepository,
) : PreferenceMetadata, PreferenceSummaryProvider {

    override val key: String
        get() = "total_size"

    override val purpose: Int
        get() = R.string.total_size_purpose

    override val title: Int
        get() = R.string.total_size_label

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override fun getSummary(context: Context): CharSequence? =
        repo.formatSizeBytes(stats.totalBytes)
}
// LINT.ThenChange(AppStorageSettings.java, AppStorageSettingsScreenApi.kt)
