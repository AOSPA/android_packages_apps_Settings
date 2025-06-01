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

import android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
import android.app.Application
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.net.toUri
import com.android.settings.R
import com.android.settings.applications.AppStateManageExternalStorageBridge
import com.android.settings.contract.TAG_DEVICE_STATE_PREFERENCE
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settingslib.applications.ApplicationsState
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.NoOpKeyedObservable
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.spaprivileged.model.app.AppListRepositoryImpl
import com.android.settingslib.widget.MainSwitchPreferenceBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// Note: This page is for DeviceState usages.
@ProvidePreferenceScreen(AppInfoAllFilesAccessScreen.KEY, parameterized = true)
open class AppInfoAllFilesAccessScreen(context: Context, override val arguments: Bundle) :
    PreferenceScreenMixin, PreferenceSummaryProvider, PreferenceTitleProvider {

    private val packageName = arguments.getString("app")!!

    private val appInfo = context.packageManager.getApplicationInfo(packageName, 0)

    private val storage: KeyValueStore = AllFilesAccessStorage(context, appInfo, packageName)

    override val key: String
        get() = KEY

    override val screenTitle: Int
        get() = R.string.manage_external_storage_title

    override val highlightMenuKey: Int
        get() = R.string.menu_key_apps

    override fun getMetricsCategory() = SettingsEnums.PAGE_UNKNOWN // TODO: correct page id

    override fun tags(context: Context) =
        arrayOf(TAG_DEVICE_STATE_SCREEN, TAG_DEVICE_STATE_PREFERENCE)

    override fun getTitle(context: Context): CharSequence =
        appInfo.loadLabel(context.packageManager)

    override fun getSummary(context: Context): CharSequence =
        context.getString(
            when (storage.getBoolean(AllFilesAccessMainSwitch.KEY)) {
                true -> R.string.app_permission_summary_allowed
                else -> R.string.app_permission_summary_not_allowed
            }
        )

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        Intent("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION").apply {
            data = "package:${appInfo.packageName}".toUri()
        }

    override fun isFlagEnabled(context: Context) = Flags.deviceState()

    override fun hasCompleteHierarchy() = false

    override fun getPreferenceHierarchy(context: Context) =
        preferenceHierarchy(context, this) { +AllFilesAccessMainSwitch(storage) }

    companion object {
        const val KEY = "device_state_app_info_all_files_access"

        @JvmStatic
        fun parameters(context: Context): Flow<Bundle> = flow {
            AppListRepositoryImpl(context).loadAndFilterApps(context.userId, true).forEach {
                if (it.hasExternalStoragePermission(context)) {
                    emit(Bundle(1).apply { putString("app", it.packageName) })
                }
            }
        }

        fun ApplicationInfo.hasExternalStoragePermission(context: Context): Boolean =
            try {
                val packageInfo: PackageInfo =
                    context.packageManager.getPackageInfo(
                        this.packageName,
                        PackageManager.GET_PERMISSIONS,
                    )
                val requestedPermissions = packageInfo.requestedPermissions
                requestedPermissions?.contains(MANAGE_EXTERNAL_STORAGE) == true
            } catch (e: Exception) {
                false
            }
    }
}

private class AllFilesAccessMainSwitch(private val storage: KeyValueStore) :
    BooleanValuePreference, MainSwitchPreferenceBinding {

    override val key
        get() = KEY

    override val title
        get() = R.string.permit_manage_external_storage

    override fun storage(context: Context) = storage

    companion object {
        const val KEY = "device_state_app_ops_settings_switch"
    }
}

private class AllFilesAccessStorage(
    context: Context,
    private val appInfo: ApplicationInfo,
    private val packageName: String,
) : NoOpKeyedObservable<String>(), KeyValueStore {

    val state: ApplicationsState =
        ApplicationsState.getInstance(context.applicationContext as Application)
    val bridge = AppStateManageExternalStorageBridge(context, state, null)

    override fun contains(key: String): Boolean = true

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(key: String, valueType: Class<T>): T =
        (bridge.getManageExternalStoragePermState(packageName, appInfo.uid).isPermissible) as T

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {}
}
