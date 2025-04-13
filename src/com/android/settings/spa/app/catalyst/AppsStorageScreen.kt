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
import android.os.Bundle
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceFragment
import com.android.settingslib.preference.PreferenceScreenCreator
import com.android.settingslib.spaprivileged.model.app.AppListRepositoryImpl
import com.android.settings.flags.Flags
import com.android.settings.spa.app.storage.StorageType
import com.android.settingslib.metadata.PreferenceMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking

// future improvement = parameterize this to support apps (default) and games
@ProvidePreferenceScreen(AppStorageAppListScreen.KEY, parameterized = true)
class AppStorageAppListScreen(
    override val arguments: Bundle
) : PreferenceScreenCreator {
    override val key: String
        get() = KEY

    override val title: Int
        get() = StorageType.Apps.titleResource

    override fun isFlagEnabled(context: Context) = Flags.catalystAppList()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass() = PreferenceFragment::class.java

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent? {
        return Intent("com.android.settings.APP_STORAGE_SETTINGS")
            .setPackage(context.packageName)
    }

    override fun getPreferenceHierarchy(context: Context) = preferenceHierarchy(context, this) {
        val repo = AppListRepositoryImpl(context)
        val apps = runBlocking {
            repo.loadAndFilterApps(
                context.userId,
                arguments.getBoolean(KEY_INCLUDE_SYSTEM_APPS)
            )
        }.filter { app -> StorageType.Apps.filter(app) }
        for (app in apps) {
            addParameterizedScreen(
                AppInfoStorageScreen.KEY,
                Bundle(1).apply { putString("app", app.packageName) }
            )
        }
    }

    companion object {
        const val KEY = "app_storage_app_list"

        const val KEY_INCLUDE_SYSTEM_APPS = "include_system"

        @JvmStatic
        fun parameters(context: Context): Flow<Bundle> {
            return flowOf(Bundle(1).apply { putBoolean(KEY_INCLUDE_SYSTEM_APPS, true) })
        }
    }
}
