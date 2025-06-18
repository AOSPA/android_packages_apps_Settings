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

import android.Manifest.permission.WRITE_SETTINGS
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS
import androidx.core.net.toUri
import com.android.settings.R
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.utils.highlightPreference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.spaprivileged.model.app.AppListRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@ProvidePreferenceScreen(AppInfoManageWriteSettingsScreen.KEY, parameterized = true)
open class AppInfoManageWriteSettingsScreen(context: Context, override val arguments: Bundle) :
    PreferenceScreenMixin, PreferenceTitleProvider {

    private val packageName = arguments.getString("app")!!

    private val appInfo = context.packageManager.getApplicationInfo(packageName, 0)

    override val key: String
        get() = KEY

    override val screenTitle: Int
        get() = R.string.write_system_settings

    override val highlightMenuKey: Int
        get() = R.string.menu_key_apps

    override fun getTitle(context: Context): CharSequence =
        appInfo.loadLabel(context.packageManager)

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        Intent(ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = "package:${appInfo.packageName}".toUri()
            highlightPreference(arguments, metadata?.key)
        }

    override fun isFlagEnabled(context: Context) = Flags.deeplinkApps25q4()

    override fun getMetricsCategory(): Int = SettingsEnums.PAGE_UNKNOWN // TODO: correct page id

    override fun hasCompleteHierarchy() = false

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    companion object {
        const val KEY = "app_info_manage_write_settings"

        @JvmStatic
        fun parameters(context: Context): Flow<Bundle> = flow {
            val repo = AppListRepositoryImpl(context)
            repo.loadApps(context.userId).forEach { app ->
                if (app.hasWriteSettingsPermission(context)) {
                    emit(Bundle(1).apply { putString("app", app.packageName) })
                }
            }
        }

        private fun ApplicationInfo.hasWriteSettingsPermission(context: Context): Boolean =
            try {
                val packageInfo: PackageInfo =
                    context.packageManager.getPackageInfo(
                        this.packageName,
                        PackageManager.GET_PERMISSIONS,
                    )
                val requestedPermissions = packageInfo.requestedPermissions
                requestedPermissions?.contains(WRITE_SETTINGS) == true
            } catch (e: Exception) {
                false
            }
    }
}
