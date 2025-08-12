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

import android.Manifest.permission.USE_FULL_SCREEN_INTENT
import android.app.AppOpsManager
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.net.toUri
import com.android.settings.R
import com.android.settings.contract.TAG_DEVICE_STATE_PREFERENCE
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.core.PreferenceScreenMixin
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@ProvidePreferenceScreen(AppInfoFullScreenIntentScreen.KEY, parameterized = true)
open class AppInfoFullScreenIntentScreen(context: Context, override val arguments: Bundle) :
    PreferenceScreenMixin, PreferenceSummaryProvider, PreferenceTitleProvider {

    private val packageName = arguments.getString("app")!!

    private val appInfo = context.packageManager.getApplicationInfo(packageName, 0)

    private val storage: KeyValueStore = FullScreenIntentStorage(context, appInfo, packageName)

    override val key: String
        get() = KEY

    override val screenTitle: Int
        get() = R.string.full_screen_intent_title

    override val highlightMenuKey: Int
        get() = R.string.menu_key_apps

    override fun getMetricsCategory() = SettingsEnums.PAGE_UNKNOWN // TODO: correct page id

    override fun tags(context: Context) =
        arrayOf(TAG_DEVICE_STATE_SCREEN, TAG_DEVICE_STATE_PREFERENCE)

    override fun getTitle(context: Context): CharSequence =
        appInfo.loadLabel(context.packageManager)

    override fun getSummary(context: Context): CharSequence =
        context.getString(
            when (storage.getBoolean(FullScreenIntentMainSwitch.KEY)) {
                true -> R.string.app_permission_summary_allowed
                else -> R.string.app_permission_summary_not_allowed
            }
        )

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        Intent("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT").apply {
            data = "package:${appInfo.packageName}".toUri()
        }

    override fun isFlagEnabled(context: Context) = false

    override fun hasCompleteHierarchy() = false

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) { +FullScreenIntentMainSwitch(storage) }

    companion object {
        const val KEY = "device_state_app_info_full_screen_intent"

        @JvmStatic
        fun parameters(context: Context): Flow<Bundle> = flow {
            val repo = AppListRepositoryImpl(context)
            repo.loadApps(context.userId).forEach { app ->
                if (app.hasFullScreenPermission(context)) {
                    emit(Bundle(1).apply { putString("app", app.packageName) })
                }
            }
        }

        fun ApplicationInfo.hasFullScreenPermission(context: Context): Boolean {
            return try {
                val packageInfo: PackageInfo =
                    context.packageManager.getPackageInfo(
                        this.packageName,
                        PackageManager.GET_PERMISSIONS,
                    )
                val requestedPermissions = packageInfo.requestedPermissions
                requestedPermissions?.contains(USE_FULL_SCREEN_INTENT) == true
            } catch (e: Exception) {
                false
            }
        }
    }
}

private class FullScreenIntentMainSwitch(private val storage: KeyValueStore) :
    BooleanValuePreference, MainSwitchPreferenceBinding {

    override val key
        get() = KEY

    override val title
        get() = R.string.permit_full_screen_intent

    override fun storage(context: Context) = storage

    companion object {
        const val KEY = "device_state_app_ops_settings_switch"
    }
}

private class FullScreenIntentStorage(
    private val context: Context,
    private val appInfo: ApplicationInfo,
    private val packageName: String,
) : NoOpKeyedObservable<String>(), KeyValueStore {

    override fun contains(key: String): Boolean {
        return true
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(key: String, valueType: Class<T>): T {
        return hasGrantedPermission(context, packageName, appInfo) as T
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {}

    companion object {
        fun hasGrantedPermission(
            context: Context,
            packageName: String,
            appInfo: ApplicationInfo,
        ): Boolean {
            val appOpsManager =
                context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            val mode =
                appOpsManager.checkOpNoThrow(
                    AppOpsManager.OPSTR_USE_FULL_SCREEN_INTENT,
                    appInfo.uid,
                    packageName,
                )
            return mode == AppOpsManager.MODE_ALLOWED || mode == AppOpsManager.MODE_FOREGROUND
        }
    }
}
