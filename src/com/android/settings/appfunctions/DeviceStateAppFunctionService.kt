/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.appfunctions

import android.app.INotificationManager
import android.app.appsearch.GenericDocument
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.ApplicationInfoFlags
import android.content.res.Configuration
import android.content.res.Resources
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import android.os.ServiceManager
import android.util.Log
import com.android.extensions.appfunctions.AppFunctionException
import com.android.extensions.appfunctions.AppFunctionException.ERROR_FUNCTION_NOT_FOUND
import com.android.extensions.appfunctions.AppFunctionService
import com.android.extensions.appfunctions.ExecuteAppFunctionRequest
import com.android.extensions.appfunctions.ExecuteAppFunctionResponse
import com.android.settings.utils.getLocale
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceHierarchy
import com.android.settingslib.metadata.PreferenceScreenCoordinate
import com.android.settingslib.metadata.PreferenceHierarchyGenerator
import com.android.settingslib.metadata.PreferenceScreenMetadata
import com.android.settingslib.metadata.PreferenceScreenRegistry
import com.android.settingslib.metadata.getPreferenceScreenTitle
import com.android.settingslib.metadata.getPreferenceSummary
import com.android.settingslib.metadata.getPreferenceTitle
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItem
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateResponse
import com.google.android.appfunctions.schema.common.v1.devicestate.LocalizedString
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenDeviceStates
import java.util.Locale
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

class DeviceStateAppFunctionService : AppFunctionService() {
    private val settingConfigMap = getDeviceStateItemList().associateBy { it.settingKey }
    private val perScreenConfigMap = getScreenConfigs().associateBy { it.screenKey }
    private lateinit var englishContext: Context

    override fun onCreate() {
        super.onCreate()
        englishContext = createEnglishContext()
    }

    override fun onExecuteFunction(
        request: ExecuteAppFunctionRequest,
        callingPackage: String, cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<ExecuteAppFunctionResponse, AppFunctionException>
    ) {
        val requestCategory = DeviceStateCategory.fromId(request.functionIdentifier)
        if (requestCategory == null) {
            callback.onError(
                AppFunctionException(
                    ERROR_FUNCTION_NOT_FOUND,
                    "${request.functionIdentifier} not supported."
                )
            )
            return
        }
        runBlocking {
            Log.d(TAG, "device state app function ${request.functionIdentifier} called.")
            val jetpackDocument =
                androidx.appsearch.app.GenericDocument.fromDocumentClass(
                    buildResponseFromCatalyst(
                        requestCategory
                    )
                )
            val platformDocument =
                GenericDocumentToPlatformConverter.toPlatformGenericDocument(jetpackDocument)
            val result =
                GenericDocument.Builder<GenericDocument.Builder<*>>("", "", "")
                    .setPropertyDocument(
                        ExecuteAppFunctionResponse.PROPERTY_RETURN_VALUE,
                        platformDocument
                    )
                    .build()
            val response = ExecuteAppFunctionResponse(result)
            callback.onResult(response)
            Log.d(TAG, "app function ${request.functionIdentifier} fulfilled.")
        }
    }

    private suspend fun buildResponseFromCatalyst(
        requestCategory: DeviceStateCategory
    ): DeviceStateResponse {
        val screenKeyList = perScreenConfigMap.keys.toList()
        val perScreenDeviceStatesList: MutableList<PerScreenDeviceStates> = ArrayList()
        coroutineScope {
            val deferredList = screenKeyList.map { screenKey ->
                async { buildPerScreenDeviceStates(screenKey, requestCategory) }
            }
            deferredList.awaitAll().forEach {
                if (it != null) {
                    perScreenDeviceStatesList.add(it)
                }
            }
        }

        if (requestCategory in setOf(DeviceStateCategory.UNCATEGORIZED)) {
            perScreenDeviceStatesList.add(buildNotificationsScreenStates())
        }

        return DeviceStateResponse(
            perScreenDeviceStates = perScreenDeviceStatesList,
            deviceLocale = applicationContext.getLocale().toString()
        )
    }

    private suspend fun buildPerScreenDeviceStates(
        screenKey: String,
        requestCategory: DeviceStateCategory,
    ): PerScreenDeviceStates? {
        val perScreenConfig = perScreenConfigMap[screenKey]
        if (perScreenConfig == null || !perScreenConfig.enabled || requestCategory !in perScreenConfig.category) {
            return null
        }
        val screenMetaData =
            PreferenceScreenRegistry.create(
                applicationContext,
                PreferenceScreenCoordinate(screenKey, null),
            ) ?: return null
        val deviceStateItemList: MutableList<DeviceStateItem> = ArrayList()
        // TODO if child node is PreferenceScreen, recursively process it
        screenMetaData.getPreferenceHierarchy().forEachRecursively {
            val metadata = it.metadata
            val config = settingConfigMap[metadata.key]
            // skip over explicitly disabled preferences
            if (config?.enabled == false) return@forEachRecursively
            val jsonValue =
                when (metadata) {
                    is PersistentPreference<*> ->
                        metadata
                            .storage(applicationContext)
                            .getValue(metadata.key, metadata.valueType as Class<Any>)
                            .toString()
                    else -> metadata.getPreferenceSummary(applicationContext)?.toString()
                }
            deviceStateItemList.add(
                DeviceStateItem(
                    key = metadata.key,
                    name = LocalizedString(
                        english = metadata.getPreferenceTitle(englishContext).toString(),
                        localized = metadata.getPreferenceTitle(applicationContext).toString()
                    ),
                    jsonValue = jsonValue,
                    hintText = config?.hintText(englishContext, metadata)
                )
            )
        }

        val launchingIntent = screenMetaData.getLaunchIntent(applicationContext, null)
        return PerScreenDeviceStates(
            description = screenMetaData.getPreferenceScreenTitle(applicationContext)?.toString()
                ?: "",
            deviceStateItems = deviceStateItemList,
            intentUri = launchingIntent?.toUri(Intent.URI_INTENT_SCHEME)
        )
    }

    private suspend fun PreferenceScreenMetadata.getPreferenceHierarchy(): PreferenceHierarchy =
        when (this) {
            is PreferenceHierarchyGenerator<*> ->
                generatePreferenceHierarchy(applicationContext, defaultType)
            else -> getPreferenceHierarchy(applicationContext)
        }

    private fun createEnglishContext(): Context {
        val configuration = Configuration(applicationContext.resources.configuration)
        configuration.setLocale(Locale.US)
        return applicationContext.createConfigurationContext(configuration)
    }

    /**
     * Build a PerScreenDeviceStates for the notifications screen.
     *
     * This is temporary solution to unblock CUJ 6 for Teamfood.
     */
    private fun buildNotificationsScreenStates(): PerScreenDeviceStates {

        val packageManager = applicationContext.packageManager
        val notificationManager = INotificationManager.Stub.asInterface(
            ServiceManager.getService(Context.NOTIFICATION_SERVICE)
        )
        val disabledComponentsFlag =
            (PackageManager.MATCH_DISABLED_COMPONENTS or
                    PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS)
                .toLong()
        val regularFlags = ApplicationInfoFlags.of(disabledComponentsFlag)
        val installedPackages = packageManager.getInstalledApplications(regularFlags)
        val deviceStateItems = ArrayList<DeviceStateItem>(installedPackages.size)
        for (info in installedPackages) {
            val packageName = info.packageName
            val appName = packageManager.getApplicationLabel(info.applicationInfo)
            val uid = info.applicationInfo.uid
            val areNotificationsEnabled =
                notificationManager?.areNotificationsEnabledForPackage(packageName, uid) ?: false
            deviceStateItems.add(
                DeviceStateItem(
                    key = "notifications_enabled_package_$packageName",
                    hintText = "App: $appName",
                    jsonValue = areNotificationsEnabled.toString(),
                )
            )
        }

        return PerScreenDeviceStates(
            description =
                "Notifications Settings Screen. Note that to get to the notification settings for a given package, the intent uri is intent:#Intent;action=android.settings.APP_NOTIFICATION_SETTINGS;S.android.provider.extra.APP_PACKAGE=\$packageName;end",
            intentUri = "intent:#Intent;action=android.settings.NOTIFICATION_SETTINGS;end",
            deviceStateItems = deviceStateItems,
        )
    }

    companion object {
        private const val TAG = "DeviceStateService"
    }
}
