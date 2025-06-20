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

import android.Manifest
import android.app.AppGlobals
import android.app.AppOpsManager
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
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

@ProvidePreferenceScreen(AppInfoInstallUnknownAppsScreen.KEY, parameterized = true)
open class AppInfoInstallUnknownAppsScreen(context: Context, override val arguments: Bundle) :
    PreferenceScreenMixin, PreferenceTitleProvider {

    private val packageName = arguments.getString("app")!!

    private val appInfo = context.packageManager.getApplicationInfo(packageName, 0)

    override val key: String
        get() = KEY

    override val screenTitle: Int
        get() = com.android.settingslib.R.string.install_other_apps

    override val highlightMenuKey: Int
        get() = R.string.menu_key_apps

    override fun getTitle(context: Context): CharSequence =
        appInfo.loadLabel(context.packageManager)

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        Intent(ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = "package:${appInfo.packageName}".toUri()
            highlightPreference(arguments, metadata?.key)
        }

    override fun isFlagEnabled(context: Context) = Flags.deeplinkApps25q4()

    override fun getMetricsCategory(): Int = SettingsEnums.PAGE_UNKNOWN // TODO: correct page id

    override fun hasCompleteHierarchy() = false

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    companion object {
        const val KEY = "app_info_install_unknown_apps"

        @JvmStatic
        fun parameters(context: Context): Flow<Bundle> = flow {
            val repo = AppListRepositoryImpl(context)
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val permissionRequestedPackages =
                AppGlobals.getPackageManager()
                    .getAppOpPermissionPackages(
                        Manifest.permission.REQUEST_INSTALL_PACKAGES,
                        context.userId,
                    )
                    .toSet()
            repo.loadApps(context.userId).forEach { app ->
                if (
                    app.hasInstallPackagesPermission(
                        context,
                        appOpsManager,
                        permissionRequestedPackages,
                    )
                ) {
                    emit(Bundle(1).apply { putString("app", app.packageName) })
                }
            }
        }

        // Check both the Manifest permission and the AppOps permission
        private fun ApplicationInfo.hasInstallPackagesPermission(
            context: Context,
            appOpsManager: AppOpsManager,
            permissionRequestedPackages: Set<String>,
        ): Boolean =
            permissionRequestedPackages.contains(packageName) ||
                appOpsManager.checkOpNoThrow(
                    AppOpsManager.OP_REQUEST_INSTALL_PACKAGES,
                    context.userId,
                    packageName,
                ) != AppOpsManager.MODE_DEFAULT
    }
}
