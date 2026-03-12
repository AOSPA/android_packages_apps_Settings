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

import android.content.Context
import android.content.pm.PackageManager
import android.net.NetworkPolicyManager
import android.util.Log
import com.android.settings.R
import com.android.settings.applications.AppInfoBase
import com.android.settings.applications.InstalledPackageName
import com.android.settings.flags.Flags
import com.android.settings.overlay.FeatureFactory.Companion.appContext
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean

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

    init {
        flag { Flags.catalystMigration26q2() }

        parameters {
            parameter(
                name = KEY_APP_PACKAGE_NAME,
                purpose = R.string.app_data_usage_parameter_purpose,
                type = InstalledPackageName,
            )

            prepareScreenExtras { keyParameters, extras ->
                val packageName = keyParameters[KEY_APP_PACKAGE_NAME] ?: return@prepareScreenExtras
                extras.putString(KEY_APP_PACKAGE_NAME, packageName)
                extras.putInt(AppInfoBase.ARG_PACKAGE_UID, appContext.getPackageUid(packageName))
            }
        }

        preference(
            key = APP_BACKGROUND_DATA_SWITCH_KEY,
            purpose = APP_BACKGROUND_DATA_SWITCH_PURPOSE,
            type = AnyBoolean,
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)
            get {
                execute {
                    keyParameters?.get(KEY_APP_PACKAGE_NAME)?.let {
                        context.getBackgroundDataEnabled(it)
                    } ?: false
                }
            }
            set {
                execute { value ->
                    keyParameters?.get(KEY_APP_PACKAGE_NAME)?.let {
                        context.setBackgroundDataEnabled(it, value)
                    } ?: false
                }
            }
        }
    }

    companion object {
        private const val TAG = "AppDataUsageScreenApi"
        const val KEY = "api_app_data_usage_screen"
        const val KEY_APP_PACKAGE_NAME = "app"

        const val APP_BACKGROUND_DATA_SWITCH_KEY = "app_background_data_switch"
        private val APP_BACKGROUND_DATA_SWITCH_PURPOSE = R.string.app_background_data_switch_purpose

        /**
         * Gets the UID for a given package name.
         *
         * @param packageName The target package name.
         * @return The UID of the package, or -1 if the package is not found.
         */
        fun Context.getPackageUid(packageName: String): Int {
            return try {
                packageManager.getPackageUid(packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e(TAG, "Package not found: $packageName", e)
                -1
            }
        }

        /**
         * Checks if background data is enabled for the specified package.
         *
         * @param packageName The target package name to check.
         * @return True if background data is enabled, false otherwise.
         */
        fun Context.getBackgroundDataEnabled(
            packageName: String,
            policyManager: NetworkPolicyManager? =
                getSystemService(NetworkPolicyManager::class.java),
        ): Boolean {
            val uid = getPackageUid(packageName)
            if (uid == -1 || policyManager == null) {
                return true
            }
            val uidPolicy = policyManager.getUidPolicy(uid)
            // POLICY_REJECT_METERED_BACKGROUND means background data is restricted.
            // If the bit is not set (result is 0), background data is enabled.
            return (uidPolicy and NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND) == 0
        }

        /**
         * Sets whether background data is enabled for the specified package.
         *
         * @param packageName The target package name.
         * @param enabled True to enable background data, false to disable.
         * @param dataSaverBackend Optional DataSaverBackend instance.
         */
        fun Context.setBackgroundDataEnabled(
            packageName: String,
            enabled: Boolean,
            dataSaverBackend: DataSaverBackend = DataSaverBackend(this),
        ) {
            val uid = getPackageUid(packageName)
            if (uid == -1) return
            dataSaverBackend.setIsDenylisted(uid, packageName, !enabled)
        }
    }
}
