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
import android.content.pm.CrossProfileApps
import android.os.Bundle
import android.os.UserManager
import android.provider.Settings.ACTION_MANAGE_CROSS_PROFILE_ACCESS
import androidx.core.net.toUri
import com.android.settings.R
import com.android.settings.applications.InstalledPackageName
import com.android.settings.applications.getApplicationInfo
import com.android.settings.applications.specialaccess.interactacrossprofiles.InteractAcrossProfilesDetails
import com.android.settings.applications.specialaccess.interactacrossprofiles.InteractAcrossProfilesSettings
import com.android.settings.contract.TAG_DEVICE_STATE_PREFERENCE
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.NoOpKeyedObservable
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.android.settingslib.metadata.KeyParametersSchema
import com.android.settingslib.metadata.ParameterizedPreferenceScreenArgumentsFactory
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.ValidatedKeyParameters
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.widget.MainSwitchPreferenceBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@ProvidePreferenceScreen(InteractAcrossProfilesAppDetailScreen.KEY, parameterized = true)
open class InteractAcrossProfilesAppDetailScreen
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

    private val packageName: String? =
        if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
            keyParameters!!.get(KEY_APP_PACKAGE_NAME)
        } else {
            arguments!!.getString(KEY_APP_PACKAGE_NAME)!!
        }

    private val appInfo = packageName?.let { context.getApplicationInfo(it) }

    private val storage: KeyValueStore = InteractAcrossProfilesStorage(context, packageName)

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
        get() = R.string.special_access_interact_across_profiles_app_detail_purpose

    override val screenTitle: Int
        get() = R.string.interact_across_profiles_title

    override val highlightMenuKey: Int
        get() = R.string.menu_key_apps

    override fun getMetricsCategory() = SettingsEnums.PAGE_UNKNOWN // TODO: correct page id

    override fun tags(context: Context) =
        arrayOf(TAG_DEVICE_STATE_SCREEN, TAG_DEVICE_STATE_PREFERENCE)

    override fun getTitle(context: Context): CharSequence? =
        appInfo?.loadLabel(context.packageManager)

    override fun getSummary(context: Context): CharSequence =
        context.getString(
            when (storage.getBoolean(InteractAcrossProfilesMainSwitch.KEY)) {
                true -> R.string.interact_across_profiles_summary_allowed
                else -> R.string.interact_across_profiles_summary_not_allowed
            }
        )

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        Intent(ACTION_MANAGE_CROSS_PROFILE_ACCESS).apply {
            data = "package:$packageName".toUri()
            // Only one switch so no need to highlight it with [IntentUtils.highlightPreference].
        }

    override fun isFlagEnabled(context: Context) = Flags.deeplinkApps25q4()

    override val availabilityDescription = "The app must be enabled."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context) = appInfo != null

    override fun extras(context: Context): Bundle? =
        Bundle(1).apply {
            if (CatalystFlagProviderFactory.catalystUseKeyParameters()) {
                putString(KEY_EXTRA_PACKAGE_NAME, keyParameters!!.get(KEY_APP_PACKAGE_NAME) ?: "")
            } else {
                putString(KEY_EXTRA_PACKAGE_NAME, arguments!!.getString(KEY_APP_PACKAGE_NAME))
            }
        }

    override fun hasCompleteHierarchy() = false

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) { +InteractAcrossProfilesMainSwitch(storage) }

    companion object : ParameterizedPreferenceScreenArgumentsFactory {
        const val KEY = "special_access_interact_across_profiles_app_detail"

        const val KEY_EXTRA_PACKAGE_NAME = "package_name"
        const val KEY_APP_PACKAGE_NAME = "app"

        @JvmStatic
        override val parametersSchema = KeyParametersSchema {
            parameter(KEY_APP_PACKAGE_NAME, "The package name of the app", required = false, type = InstalledPackageName)
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
            val packageManager = context.packageManager
            val userManager = context.getSystemService(UserManager::class.java)
            val crossProfileApps = context.getSystemService(CrossProfileApps::class.java)

            InteractAcrossProfilesSettings.collectConfigurableApps(
                    packageManager,
                    userManager,
                    crossProfileApps,
                )
                .forEach { appUser ->
                    emit(
                        Bundle(1).apply {
                            putString(KEY_APP_PACKAGE_NAME, appUser.first.packageName)
                        }
                    )
                }
        }
    }
}

private class InteractAcrossProfilesMainSwitch(private val storage: KeyValueStore) :
    BooleanValuePreference, MainSwitchPreferenceBinding {

    override val key
        get() = KEY

    override val purpose: Int
        get() = R.string.device_state_interact_across_profiles_settings_switch_purpose

    override val title
        get() = R.string.interact_across_profiles_title

    override fun storage(context: Context) = storage

    override val supportsWrite = false

    companion object {
        const val KEY = "device_state_interact_across_profiles_settings_switch"
    }
}

private class InteractAcrossProfilesStorage(
    private val context: Context,
    private val packageName: String?,
) : NoOpKeyedObservable<String>(), KeyValueStore {

    override fun contains(key: String): Boolean {
        return true
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(key: String, valueType: Class<T>): T {
        if (packageName == null) error("No package name provided")
        return InteractAcrossProfilesDetails.isInteractAcrossProfilesEnabled(context, packageName)
            as T
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {}
}
