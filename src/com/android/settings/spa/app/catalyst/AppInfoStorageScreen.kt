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

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.os.UserHandle
import androidx.core.net.toUri
import com.android.settings.R
import com.android.settings.contract.TAG_DEVICE_STATE_PREFERENCE
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.flags.Flags
import com.android.settings.spa.app.storage.StorageType
import com.android.settingslib.applications.StorageStatsSource
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.NoOpKeyedObservable
import com.android.settingslib.metadata.LongValuePreference
import com.android.settingslib.metadata.PreferenceHierarchy
import com.android.settingslib.metadata.PreferenceHierarchyGenerator
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.asyncPreferenceHierarchy
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceFragment
import com.android.settingslib.preference.PreferenceScreenCreator
import com.android.settingslib.spaprivileged.model.app.AppListRepositoryImpl
import com.android.settingslib.spaprivileged.model.app.AppStorageRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@ProvidePreferenceScreen(AppInfoStorageScreen.KEY, parameterized = true)
class AppInfoStorageScreen(
    private val context: Context,
    override val arguments: Bundle
) : PreferenceScreenCreator,
    LongValuePreference,
    PreferenceHierarchyGenerator<Int>,
    PreferenceSummaryProvider,
    PreferenceTitleProvider {


    private val appInfo by lazy {
        context.packageManager.getApplicationInfo(arguments.getString("app")!!, 0)
    }

    override val key: String
        get() = KEY

    override val sensitivityLevel: @SensitivityLevel Int
        get() = SensitivityLevel.NO_SENSITIVITY

    override val screenTitle: Int
        get() = R.string.storage_label

    override fun tags(context: Context) =
        arrayOf(TAG_DEVICE_STATE_SCREEN, TAG_DEVICE_STATE_PREFERENCE)

    override fun getTitle(context: Context): CharSequence? {
        return appInfo.loadLabel(context.packageManager).toString()
    }

    override fun getSummary(context: Context): CharSequence? {
        return AppStorageRepositoryImpl(context).formatSize(appInfo)
    }

    override fun isFlagEnabled(context: Context) = Flags.catalystAppList() || Flags.deviceState()

    override fun extras(context: Context): Bundle? {
        return Bundle(1).apply {
            putString(KEY_EXTRA_PACKAGE_NAME, arguments.getString("app"))
        }
    }

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass() = PreferenceFragment::class.java

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent? {
        return Intent("com.android.settings.APP_STORAGE_SETTINGS").apply {
            data = "package:${appInfo.packageName}".toUri()
        }
    }

    override fun getPreferenceHierarchy(context: Context) = preferenceHierarchy(context, this) {}

    override val defaultType: Int
        get() = context.userId

    override suspend fun generatePreferenceHierarchy(
        context: Context,
        type: Int // userId
    ): PreferenceHierarchy = asyncPreferenceHierarchy(context, this) {
        val source = StorageStatsSource(context)
        val repo = AppStorageRepositoryImpl(context)
        try {
            val stats = source.getStatsForPackage(
                appInfo.volumeUuid,
                appInfo.packageName,
                UserHandle.of(type)
            )
            +AppSizePreference(stats, repo)
            +AppUserDataSizePreference(stats, repo)
            +AppCacheSizePreference(stats, repo)
            +AppTotalSizePreference(stats, repo)
        } catch (_: Exception) {
            // error during lookup, return no result
        }
    }

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.DISALLOW

    override fun storage(context: Context): KeyValueStore {
        return AppStorageStore(context, appInfo)
    }

    companion object {
        const val KEY = "app_info_storage_screen"

        const val KEY_EXTRA_PACKAGE_NAME = "package_name"

        @JvmStatic
        fun parameters(context: Context): Flow<Bundle> {
            val repo = AppListRepositoryImpl(context)
            val apps = flow {
                repo.loadAndFilterApps(context.userId, true)
                    .filter { app -> StorageType.Apps.filter(app) }.forEach { emit(it) }
            }
            return apps.map { app ->
                Bundle(1).apply {
                    putString("app", app.packageName)
                }
            }
        }
    }
}

private class AppStorageStore(
    private val context: Context,
    private val appInfo: ApplicationInfo
) :
    NoOpKeyedObservable<String>(),
    KeyValueStore {

    override fun contains(key: String): Boolean {
        return true
    }

    override fun <T : Any> getValue(
        key: String,
        valueType: Class<T>
    ): T? {
        return (AppStorageRepositoryImpl(context).calculateSizeBytes(appInfo) ?: 0L) as T
    }

    override fun <T : Any> setValue(
        key: String,
        valueType: Class<T>,
        value: T?
    ) {
    }
}

private class AppSizePreference(
    private val stats: StorageStatsSource.AppStorageStats,
    private val repo: AppStorageRepositoryImpl
) : PreferenceMetadata,
    PreferenceSummaryProvider {

    override val key: String
        get() = "app_size"

    override val title: Int
        get() = R.string.application_size_label

    override fun getSummary(context: Context): CharSequence? {
        return repo.formatSizeBytes(stats.codeBytes)
    }
}

private class AppUserDataSizePreference(
    private val stats: StorageStatsSource.AppStorageStats,
    private val repo: AppStorageRepositoryImpl
) : PreferenceMetadata,
    PreferenceSummaryProvider {

    override val key: String
        get() = "data_size"

    override val title: Int
        get() = R.string.data_size_label

    override fun getSummary(context: Context): CharSequence? {
        return repo.formatSizeBytes(stats.dataBytes - stats.cacheBytes)
    }
}

private class AppCacheSizePreference(
    private val stats: StorageStatsSource.AppStorageStats,
    private val repo: AppStorageRepositoryImpl
) : PreferenceMetadata,
    PreferenceSummaryProvider {

    override val key: String
        get() = "cache_size"

    override val title: Int
        get() = R.string.cache_size_label

    override fun getSummary(context: Context): CharSequence? {
        return repo.formatSizeBytes(stats.cacheBytes)
    }
}

private class AppTotalSizePreference(
    private val stats: StorageStatsSource.AppStorageStats,
    private val repo: AppStorageRepositoryImpl
) : PreferenceMetadata,
    PreferenceSummaryProvider {

    override val key: String
        get() = "total_size"

    override val title: Int
        get() = R.string.total_size_label

    override fun getSummary(context: Context): CharSequence? {
        return repo.formatSizeBytes(stats.totalBytes)
    }
}
