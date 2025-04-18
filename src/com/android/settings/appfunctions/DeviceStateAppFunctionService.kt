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

import android.app.appsearch.GenericDocument
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import android.util.Log
import com.android.extensions.appfunctions.AppFunctionException
import com.android.extensions.appfunctions.AppFunctionException.ERROR_FUNCTION_NOT_FOUND
import com.android.extensions.appfunctions.AppFunctionService
import com.android.extensions.appfunctions.ExecuteAppFunctionRequest
import com.android.extensions.appfunctions.ExecuteAppFunctionResponse
import com.android.settings.utils.getLocale
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceScreenCoordinate
import com.android.settingslib.metadata.PreferenceScreenRegistry
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
        return DeviceStateResponse(
            perScreenDeviceStates = perScreenDeviceStatesList,
            deviceLocale = applicationContext.getLocale().toString()
        )
    }

    private fun buildPerScreenDeviceStates(
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
                PreferenceScreenCoordinate(screenKey, null)
            )
        if (screenMetaData == null) {
            return null
        }
        val deviceStateItemList: MutableList<DeviceStateItem> = ArrayList()
        // TODO(b/405344827): support PreferenceHierarchyGenerator
        val hierarchy = screenMetaData.getPreferenceHierarchy(applicationContext)
        hierarchy.forEach {
            val metadata = it.metadata as? PersistentPreference<*> ?: return@forEach
            val config = settingConfigMap[metadata.key]
            if (config == null || !config.enabled) {
                return@forEach
            }
            val valueType = metadata.valueType
            var jasonValue: String? = when (valueType) {
                Int::class.javaObjectType -> metadata.storage(applicationContext)
                    ?.getInt("")
                    .toString()

                Boolean::class.javaObjectType -> metadata.storage(applicationContext)
                    ?.getBoolean("").toString()

                Long::class.javaObjectType -> metadata.storage(applicationContext)
                    ?.getLong("")
                    .toString()

                Float::class.javaObjectType -> metadata.storage(applicationContext)
                    ?.getLong("")
                    .toString()

                String::class.javaObjectType -> metadata.storage(applicationContext)?.getString("")
                else -> null
            }
            if (jasonValue == null) {
                jasonValue = tryGetStringRes(metadata.summary)
            }
            deviceStateItemList.add(
                DeviceStateItem(
                    key = metadata.key,
                    name = getLocalizedString(metadata.title),
                    jsonValue = jasonValue,
                    hintText = config.hintText
                )
            )
        }

        val launchingIntent = screenMetaData.getLaunchIntent(applicationContext, null)
        return PerScreenDeviceStates(
            description = tryGetStringRes(screenMetaData.title),
            deviceStateItems = deviceStateItemList,
            intentUri = launchingIntent?.toUri(Intent.URI_INTENT_SCHEME)
        )
    }

    private fun tryGetStringRes(resId: Int): String {
        return try {
            applicationContext.getString(resId)
        } catch (_: Resources.NotFoundException) {
            ""
        }
    }

    private fun getLocalizedString(resId: Int): LocalizedString? {
        return try {
            LocalizedString(
                english = englishContext.getString(resId),
                localized = applicationContext.getString(resId)
            )
        } catch (_: Resources.NotFoundException) {
            null
        }
    }

    private fun createEnglishContext(): Context {
        val configuration = Configuration(applicationContext.resources.configuration)
        configuration.setLocale(Locale.US)
        return applicationContext.createConfigurationContext(configuration)
    }

    companion object {
        private const val TAG = "DeviceStateService"
    }
}
