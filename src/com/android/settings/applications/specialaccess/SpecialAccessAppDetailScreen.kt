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

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.util.IconDrawableFactory
import androidx.annotation.CallSuper
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.applications.AppInfoHeaderPreference
import com.android.settings.applications.CatalystAppListFragment.Companion.DEFAULT_SHOW_SYSTEM
import com.android.settings.applications.InstalledPackageName
import com.android.settings.applications.PackageInfoProvider
import com.android.settings.applications.applicationInfoComparator
import com.android.settings.applications.appops.AppOpsModeDataStore
import com.android.settings.applications.getPackageInfo
import com.android.settings.applications.packageName
import com.android.settings.applications.source
import com.android.settings.applications.toArguments
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settings.spa.app.catalyst.AppInfoScreen
import com.android.settings.widget.FooterPreferenceBinding
import com.android.settings.widget.FooterPreferenceMetadata
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.android.settingslib.metadata.KEY_PACKAGE_NAME
import com.android.settingslib.metadata.KeyParametersSchema
import com.android.settingslib.metadata.ParameterizedPreferenceScreenArgumentsFactory
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.metadata.ValidatedKeyParameters
import com.android.settingslib.metadata.packageName
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.metadata.preferencesapi.types.CustomEnum
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.spaprivileged.model.app.AppListRepositoryImpl
import com.android.settingslib.utils.applications.PackageObservable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Abstract screen to display app details for special access. */
abstract class SpecialAccessAppDetailScreen
private constructor(
    val context: Context,
    @Deprecated(
        "This property will be removed once the catalyst framework stops passing the arguments as a bundle. Use the keyParameters instead."
    )
    final override val arguments: Bundle?,
    final override val keyParameters: ValidatedKeyParameters?,
) :
    PreferenceScreenMixin,
    PreferenceBinding,
    PreferenceAvailabilityProvider,
    PreferenceLifecycleProvider,
    PreferenceTitleProvider,
    PreferenceSummaryProvider,
    Preference.OnPreferenceChangeListener,
    PackageInfoProvider {

    private lateinit var keyedObserver: KeyedObserver<String>

    private val dataStore: KeyValueStore =
        AppOpsModeDataStore(
            SpecialAccessSwitchPreference.KEY,
            context,
            this,
            op,
            permission,
            setModeByUid,
        )

    @Deprecated(
        "This constructor will be removed once the catalyst framework stops passing the arguments as a bundle. Use the other constructor instead."
    )
    constructor(context: Context, args: Bundle) : this(context, args, null)

    constructor(
        context: Context,
        keyParameters: ValidatedKeyParameters,
    ) : this(context, null, keyParameters)

    /** App ops to control. */
    abstract val op: Int

    /** Special access permission needed to be requested. */
    abstract val permission: String?

    /**
     * Indicates how to set op mode.
     *
     * Possible values:
     * - `true`: set op mode by uid
     * - `false`: set op mode by package
     * - `null` (default): detect automatically by
     *   [android.app.AppOpsManager.opIsUidAppOpPermission]
     */
    protected open val setModeByUid: Boolean?
        get() = null

    override val packageName
        by lazy {
            if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
                keyParameters!!.packageName ?: "null"
            } else {
                arguments!!.packageName
            }
        }

    override var packageInfo: PackageInfo? = if(packageName != "null") context.getPackageInfo(packageName) else null

    override val highlightMenuKey
        get() = R.string.menu_key_apps

    @CallSuper
    override fun isAvailable(context: Context) = packageInfo?.applicationInfo?.isAvailable() == true

    private fun ApplicationInfo.isAvailable() =
        enabled || enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER

    override fun getTitle(context: Context) =
        if (isFromAppInfo()) {
            context.getString(screenTitle)
        } else {
            packageInfo?.applicationInfo?.loadLabel(context.packageManager)
        }

    override fun getSummary(context: Context): CharSequence? =
        context.getString(
            when (dataStore.getBoolean(SpecialAccessSwitchPreference.KEY)) {
                true -> R.string.app_permission_summary_allowed
                else -> R.string.app_permission_summary_not_allowed
            }
        )

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        val appInfo = packageInfo?.applicationInfo
        if (preference !is PreferenceScreen) {
            preference.icon =
                if (appInfo != null && !isFromAppInfo()) {
                    IconDrawableFactory.newInstance(preference.context).getBadgedIcon(appInfo)
                } else {
                    null
                }
        }
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        // observer to detect package changes (disabled/enabled/uninstall)
        val observer =
            KeyedObserver<String> { key, _ ->
                if (key != SpecialAccessSwitchPreference.KEY) { // package change
                    packageInfo = context.getPackageInfo(packageName)
                }
                context.notifyPreferenceChange(bindingKey)
            }
        keyedObserver = observer
        val executor = HandlerExecutor.main
        if (isContainer(context)) {
            PackageObservable.get(context).addObserver(packageName, observer, executor)
        }
        dataStore.addObserver(SpecialAccessSwitchPreference.KEY, observer, executor)
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        if (isContainer(context)) {
            PackageObservable.get(context).removeObserver(packageName, keyedObserver)
        }
        dataStore.removeObserver(SpecialAccessSwitchPreference.KEY, keyedObserver)
    }

    override fun hasCompleteHierarchy() = true

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            val packageInfoProvider = this@SpecialAccessAppDetailScreen
            +AppInfoHeaderPreference(packageInfoProvider)
            +SpecialAccessSwitchPreference(
                switchPreferenceTitle,
                dataStore,
                packageInfoProvider,
                key,
            )
            +FooterPreference(footerPreferenceTitle)
        }

    abstract val switchPreferenceTitle: Int

    abstract val footerPreferenceTitle: Int

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        val action = getAccessChangeActionMetrics(newValue == true)
        if (action > 0) {
            featureFactory.metricsFeatureProvider.action(
                metricsCategory,
                action,
                metricsCategory,
                packageName,
                0,
            )
        }
        return true
    }

    /** Returns the action metrics when the access is changed. */
    open fun getAccessChangeActionMetrics(allowed: Boolean): Int = 0

    companion object : ParameterizedPreferenceScreenArgumentsFactory {
        const val KEY_INTENT_SOURCE = "source"

        @JvmStatic
        override val parametersSchema = KeyParametersSchema {
            parameter(KEY_PACKAGE_NAME,  "The package name of the app", required = false, type = InstalledPackageName)
        }

        /**
         * Returns the parameters for the special access app detail parameterized screen with the
         * default show system value and no custom filter.
         */
        @JvmStatic
        override fun keyParameters(context: Context) =
            parameters(context, DEFAULT_SHOW_SYSTEM, { _, _ -> true }).map { bundle ->
                parametersSchema.prepare(bundle)
            }

        /**
         * Returns the parameters for the special access app detail parameterized screen.
         *
         * The [filter] MUST be as quick as possible, otherwise the app list UI will flicker.
         */
        fun keyParameters(
            context: Context,
            showSystemApp: Boolean,
            customFilter: (Context, ApplicationInfo?) -> Boolean,
        ): Flow<ValidatedKeyParameters> {
            // TODO (b/457649430): when the catalyst framework stops passing the arguments as a
            // bundle: replace the parameters(context) call to the actual implementation,
            // or make this function the primary implementation and the legacy parameters() should
            // call this one.
            return parameters(context, showSystemApp, customFilter).map { bundle ->
                parametersSchema.prepare(bundle)
            }
        }

        /**
         * Returns the parameters for the special access app detail parameterized screen.
         *
         * The [filter] MUST be as quick as possible, otherwise the app list UI will flicker.
         */
        @Deprecated(
            "This method will be removed once the catalyst framework stops passing the arguments as a bundle. Use keyParameters instead."
        )
        fun parameters(
            context: Context,
            showSystemApp: Boolean,
            customFilter: (Context, ApplicationInfo?) -> Boolean,
        ): Flow<Bundle> = flow {
            val appInfos =
                withContext(Dispatchers.IO) {
                        AppListRepositoryImpl(context)
                            .loadAndMaybeExcludeSystemApps(context.userId, !showSystemApp)
                    }
                    .filter { hasSpecialAccessPermission(context, it, customFilter) }
                    .sortedWith(context.applicationInfoComparator)
            for (appInfo in appInfos) emit(appInfo.packageName.toArguments())
        }

        fun hasSpecialAccessPermission(
            context: Context,
            appInfo: ApplicationInfo,
            customFilter: (Context, ApplicationInfo?) -> Boolean,
        ) =
            customFilter(context, appInfo) &&
                isNotInChangeablePackages(appInfo) &&
                !isSystemOrRootUid(appInfo)

        private fun isSystemOrRootUid(appInfo: ApplicationInfo): Boolean =
            UserHandle.getAppId(appInfo.uid) in listOf(Process.SYSTEM_UID, Process.ROOT_UID)

        private fun isNotInChangeablePackages(appInfo: ApplicationInfo): Boolean =
            appInfo.packageName !in setOf("com.android.systemui")
    }

    /**
     * Returns whether this detail page is opened from AppInfo or not.
     *
     * If it's from the AppInfo page, we will remove the icon and also update the entry title.
     */
    private fun isFromAppInfo(): Boolean =
        if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
            // Since we removed this from the parameterSchema it will always
            // return false, but screens never launch this via the api anyway.
            //keyParameters!![KEY_INTENT_SOURCE] == AppInfoScreen.SOURCE
            false
        } else {
            arguments!!.source == AppInfoScreen.SOURCE
        }
}

private class FooterPreference(override val title: Int) :
    FooterPreferenceMetadata, FooterPreferenceBinding {

    override val key: String
        get() = "footer"

    override val purpose: Int
        get() = R.string.footer_purpose

    override val sensitivityLevel = SensitivityLevel.DO_NOT_EXPOSE

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)
}
