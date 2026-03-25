/*
 * Copyright (C) 2026 The Android Open Source Project
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

import android.util.Log
import com.android.settings.R
import com.android.settings.applications.AppInfoBase
import com.android.settings.applications.InstalledPackageName
import com.android.settings.flags.Flags
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * The [PreferencesApiScreen] for the App Data Usage screen.
 *
 * This screen allows users to view and manage mobile data usage for a specific app.
 */
@ProvidePreferenceScreen(AppDataUsageScreenApi.KEY, parameterized = true)
class AppDataUsageScreenApi :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.APPS,
        fragment = AppDataUsage::class,
        purpose = R.string.app_data_usage_screen_purpose,
        alreadyPartiallyMigrated = DataUsageAppDetailScreen::class,
    ) {

    private val repository = featureFactory.wifiFeatureProvider.dataUsageRepository

    private fun getPackageName(): String? = keyParameters?.get(KEY_APP_PACKAGE_NAME)

    init {
        flag { Flags.catalystMigration26q2() }

        parameters {
            parameter(
                name = KEY_APP_PACKAGE_NAME,
                purpose = R.string.app_data_usage_parameter_purpose,
                type = InstalledPackageName,
            )

            prepareScreenExtras { keyParameters, extras ->
                val packageName: String =
                    keyParameters[KEY_APP_PACKAGE_NAME] ?: return@prepareScreenExtras
                extras.putString(KEY_APP_PACKAGE_NAME, packageName)

                val uid: Int = runBlocking(Dispatchers.IO) { repository.getPackageUid(packageName) }
                extras.putInt(AppInfoBase.ARG_PACKAGE_UID, uid)
            }
        }

        preference(
            key = KEY_APP_BACKGROUND_DATA_SWITCH,
            purpose = R.string.app_background_data_switch_purpose,
            type = AnyBoolean,
        ) {
            //not reviewed by security & privacy
            get {
                execute {
                    val packageName = getPackageName() ?: return@execute false
                    !repository.isPolicyReject(packageName)
                }
            }
            set {
                execute { value: Boolean ->
                    val packageName = getPackageName() ?: return@execute
                    repository.setPolicyReject(packageName, !value)
                }
            }
        }

        preference(
            key = KEY_APP_UNRESTRICTED_MOBILE_DATA_USAGE_SWITCH,
            purpose = R.string.app_unrestricted_mobile_data_usage_switch_purpose,
            type = AnyBoolean,
        ) {
            //not reviewed by security & privacy
            preconditions(R.string.app_unrestricted_mobile_data_usage_switch_preconditions) {
                val packageName =
                    getPackageName()
                        ?: return@preconditions Custom(R.string.app_package_name_unavailable, stability = PreconditionStability.UNSTABLE)

                runBlocking(Dispatchers.IO) {
                    if (repository.isPolicyAllowAvailable(packageName)) {
                        Allowed
                    } else {
                        Log.w(TAG, "Unrestricted Mobile Data is unavailable for $packageName")

                        // TODO(b/474027987) Catalyst: migrate the Disallowed to InvalidPreference
                        Custom(
                            R.string.app_unrestricted_mobile_data_usage_switch_unavailable,
                            stability = PreconditionStability.UNSTABLE,
                        )
                    }
                }
            }
            get {
                execute {
                    val packageName = getPackageName() ?: return@execute false
                    repository.isPolicyAllow(packageName)
                }
            }
            set {
                execute { value: Boolean ->
                    val packageName = getPackageName() ?: return@execute
                    repository.setPolicyAllow(packageName, value)
                }
            }
        }
    }

    companion object {
        private const val TAG = "AppDataUsageScreenApi"
        const val KEY = "api_app_data_usage_screen"
        const val KEY_APP_PACKAGE_NAME = "app"
        const val KEY_APP_BACKGROUND_DATA_SWITCH = "app_background_data_switch"
        const val KEY_APP_UNRESTRICTED_MOBILE_DATA_USAGE_SWITCH =
            "app_unrestricted_mobile_data_usage_switch"
    }
}
