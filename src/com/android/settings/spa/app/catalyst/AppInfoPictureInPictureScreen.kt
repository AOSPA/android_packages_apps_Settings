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
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings.ACTION_PICTURE_IN_PICTURE_SETTINGS
import androidx.core.net.toUri
import com.android.settings.R
import com.android.settings.applications.specialaccess.pictureinpicture.PictureInPictureDetails
import com.android.settings.applications.specialaccess.pictureinpicture.PictureInPictureSettings
import com.android.settings.contract.TAG_DEVICE_STATE_PREFERENCE
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.flags.Flags
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.NoOpKeyedObservable
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceFragment
import com.android.settingslib.preference.PreferenceScreenCreator
import com.android.settingslib.spaprivileged.model.app.AppListRepositoryImpl
import com.android.settingslib.widget.MainSwitchPreferenceBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// Note: This page is for DeviceState usages.
@ProvidePreferenceScreen(AppInfoPictureInPictureScreen.KEY, parameterized = true)
class AppInfoPictureInPictureScreen(context: Context, override val arguments: Bundle) :
    PreferenceScreenCreator, PreferenceSummaryProvider, PreferenceTitleProvider {

    private val packageName = arguments.getString("app")!!

    private val appInfo = context.packageManager.getApplicationInfo(packageName, 0)

    private val storage: KeyValueStore = PictureInPictureStorage(context, appInfo, packageName)

    override val key: String
        get() = KEY

    override val screenTitle: Int
        get() = R.string.picture_in_picture_app_detail_title

    override fun tags(context: Context) =
        arrayOf(TAG_DEVICE_STATE_SCREEN, TAG_DEVICE_STATE_PREFERENCE)

    override fun getTitle(context: Context): CharSequence =
        appInfo.loadLabel(context.packageManager)

    override fun getSummary(context: Context): CharSequence =
        context.getString(
            when (storage.getBoolean(PictureInPictureMainSwitch.KEY)) {
                true -> R.string.app_permission_summary_allowed
                else -> R.string.app_permission_summary_not_allowed
            }
        )

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        Intent(ACTION_PICTURE_IN_PICTURE_SETTINGS).apply {
            data = "package:${appInfo.packageName}".toUri()
            // Only one switch so no need to highlight it with [IntentUtils.highlightPreference].
        }

    override fun isFlagEnabled(context: Context) = Flags.deviceState()

    override fun extras(context: Context): Bundle? =
        Bundle(1).apply { putString(KEY_EXTRA_PACKAGE_NAME, arguments.getString("app")) }

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass() = PreferenceFragment::class.java

    override fun getPreferenceHierarchy(context: Context) =
        preferenceHierarchy(context, this) { +PictureInPictureMainSwitch(storage) }

    companion object {
        const val KEY = "device_state_app_info_picture_in_picture"

        const val KEY_EXTRA_PACKAGE_NAME = "package_name"

        @JvmStatic
        fun parameters(context: Context): Flow<Bundle> = flow {
            val repo = AppListRepositoryImpl(context)
            repo.loadAndFilterApps(context.userId, true).forEach { app ->
                if (app.supportsPictureInPicture(context)) {
                    emit(Bundle(1).apply { putString("app", app.packageName) })
                }
            }
        }

        fun ApplicationInfo.supportsPictureInPicture(context: Context): Boolean {
            val packageInfo: PackageInfo =
                context.packageManager.getPackageInfo(
                    this.packageName,
                    PackageManager.GET_ACTIVITIES,
                )
            return PictureInPictureSettings.checkPackageHasPictureInPictureActivities(
                packageName,
                packageInfo.activities,
            )
        }
    }
}

private class PictureInPictureMainSwitch(private val storage: KeyValueStore) :
    BooleanValuePreference, MainSwitchPreferenceBinding {

    override val key
        get() = KEY

    override val title
        get() = R.string.picture_in_picture_app_detail_switch

    override fun storage(context: Context) = storage

    companion object {
        const val KEY = "device_state_picture_in_picture_settings_switch"
    }
}

private class PictureInPictureStorage(
    private val context: Context,
    private val appInfo: ApplicationInfo,
    private val packageName: String,
) : NoOpKeyedObservable<String>(), KeyValueStore {

    override fun contains(key: String): Boolean {
        return true
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(key: String, valueType: Class<T>): T {
        return PictureInPictureDetails.getEnterPipStateForPackage(context, appInfo.uid, packageName)
            as T
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {}
}
