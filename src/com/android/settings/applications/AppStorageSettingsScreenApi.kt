/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.applications

import android.content.Context
import android.os.UserHandle
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.applications.StorageStatsSource
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_APPS
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.multiusers.ManagementScope
import com.android.settingslib.metadata.preferencesapi.multiusers.PreferenceTarget
import com.android.settingslib.metadata.preferencesapi.types.AnyInt

@ProvidePreferenceScreen(AppStorageSettingsScreenApi.KEY, parameterized = true)
open class AppStorageSettingsScreenApi :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.APPS,
        fragment = AppStorageSettings::class,
        purpose = R.string.app_storage_screen_purpose,
        canManage = ManagementScope.PROFILE_GROUP,
    ) {

    init {
        flag { Flags.catalystMigration26q2() }

        tags(APP_FUNCTION_APPS)

        parameters {
            parameter(
                name = PARAM_PACKAGE,
                purpose = R.string.app_storage_package_purpose,
                required = true,
                type = InstalledPackageName,
            )

            prepareScreenExtras { keyParameters, extras ->
                val pkg = keyParameters[PARAM_PACKAGE]
                if (pkg != null) {
                    extras.putString("package", pkg)
                }
            }
        }

        preference(
            key = KEY_APP_SIZE,
            purpose = R.string.app_storage_app_size_purpose,
            type = AnyInt(unitOfMeasurement = "kilobytes"),
            appliesTo = PreferenceTarget.USER(canManage = ManagementScope.PROFILE_GROUP),
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)
            get {
                execute {
                    val packageName = parameters[PARAM_PACKAGE]
                    val stats = getAppStorageStats(context, packageName, userId) ?: return@execute 0
                    (stats.codeBytes / 1024L).toInt()
                }
            }
        }

        preference(
            key = KEY_DATA_SIZE,
            purpose = R.string.app_storage_data_size_purpose,
            type = AnyInt(unitOfMeasurement = "kilobytes"),
            appliesTo = PreferenceTarget.USER(canManage = ManagementScope.PROFILE_GROUP),
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)
            get {
                execute {
                    val packageName = parameters[PARAM_PACKAGE]
                    val stats = getAppStorageStats(context, packageName, userId) ?: return@execute 0

                    val dataSize = stats.dataBytes - stats.cacheBytes
                    (dataSize / 1024L).toInt()
                }
            }
        }

        preference(
            key = KEY_CACHE_SIZE,
            purpose = R.string.app_storage_cache_size_purpose,
            type = AnyInt(unitOfMeasurement = "kilobytes"),
            appliesTo = PreferenceTarget.USER(canManage = ManagementScope.PROFILE_GROUP),
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)
            get {
                execute {
                    val packageName = parameters[PARAM_PACKAGE]
                    val stats = getAppStorageStats(context, packageName, userId) ?: return@execute 0
                    (stats.cacheBytes / 1024L).toInt()
                }
            }
        }

        preference(
            key = KEY_TOTAL_SIZE,
            purpose = R.string.app_storage_total_size_purpose,
            type = AnyInt(unitOfMeasurement = "kilobytes"),
            appliesTo = PreferenceTarget.USER(canManage = ManagementScope.PROFILE_GROUP),
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)
            get {
                execute {
                    val packageName = parameters[PARAM_PACKAGE]
                    val stats = getAppStorageStats(context, packageName, userId) ?: return@execute 0
                    (stats.totalBytes / 1024L).toInt()
                }
            }
        }
    }

    /**
     * Retrieves the storage statistics for a given package.
     *
     * This method is left open and visible for testing to allow overriding in unit tests via
     * anonymous subclasses, effectively bypassing the need for complex framework mocks.
     * * Exceptions (e.g., NameNotFoundException) are intentionally not caught here, delegating
     *   error handling to the higher-level framework.
     */
    @androidx.annotation.VisibleForTesting
    open fun getAppStorageStats(
        context: Context,
        packageName: String?,
        userId: Int,
    ): StorageStatsSource.AppStorageStats? {
        if (packageName == null) return null

        val pm = context.packageManager
        val info = pm.getApplicationInfoAsUser(packageName, 0, userId)

        val source = StorageStatsSource(context)
        return source.getStatsForPackage(
            info.storageUuid.toString(),
            packageName,
            UserHandle.of(userId),
        )
    }

    companion object {
        const val KEY = "api_app_storage_settings"
        const val PARAM_PACKAGE = "package"

        const val KEY_APP_SIZE = "app_size"
        const val KEY_DATA_SIZE = "data_size"
        const val KEY_CACHE_SIZE = "cache_size"
        const val KEY_TOTAL_SIZE = "total_size"
    }
}
