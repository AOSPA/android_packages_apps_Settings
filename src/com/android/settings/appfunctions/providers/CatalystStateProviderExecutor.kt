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

package com.android.settings.appfunctions.providers

import android.app.appsearch.GenericDocument
import android.content.Context
import android.content.Intent
import android.util.Log
import com.android.settings.appfunctions.CatalystConfig
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.appfunctions.DeviceStateProviderExecutorResult
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.getPreferenceScreenTitle
import com.android.settingslib.metadata.getPreferenceSummary
import com.android.settingslib.metadata.getPreferenceTitle
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItem
import com.google.android.appfunctions.schema.common.v1.devicestate.LocalizedString
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenDeviceStates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/* A [DeviceStateProvider] that provides device state information for Settings that are
exposed using Catalyst framework. Configured in [CatalystStateProviderConfig]. */
class CatalystStateProviderExecutor(
    val config: CatalystConfig,
    private val context: Context,
    private val englishContext: Context,
) : DeviceStateExecutor {
    private val settingConfigMap = config.deviceStateItems.associateBy { it.settingKey }
    private val perScreenConfigMap = config.screenConfigs.associateBy { it.screenKey }
    private val screenKeyList = perScreenConfigMap.keys.toList()

    override suspend fun execute(
        appFunctionType: DeviceStateAppFunctionType,
        params: GenericDocument?,
    ): DeviceStateProviderExecutorResult {
        val perScreenDeviceStatesList = mutableListOf<PerScreenDeviceStates>()
        coroutineScope {
            val deferredList =
                screenKeyList.map { screenKey ->
                    async {
                        try {
                            buildPerScreenDeviceStates(screenKey, appFunctionType)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error building per screen device states for $screenKey", e)
                            null
                        }
                    }
                }
            val results = deferredList.awaitAll()
            perScreenDeviceStatesList.addAll(results.filterNotNull())
        }
        return DeviceStateProviderExecutorResult(states = perScreenDeviceStatesList)
    }

    private suspend fun CoroutineScope.buildPerScreenDeviceStates(
        screenKey: String,
        appFunctionType: DeviceStateAppFunctionType,
    ): PerScreenDeviceStates? {
        Log.v(TAG, "Building per screen device states for $screenKey")
        val (screenMetaData, preferencesHierarchy) =
            getEnabledPreferencesHierarchy(config, context, appFunctionType, screenKey)
                ?: return null
        val deviceStateItemList = mutableListOf<DeviceStateItem>()
        preferencesHierarchy.forEach {
            val metadata = it.metadata
            val config = settingConfigMap[metadata.key]
            // skip over explicitly disabled preferences
            val jsonValue =
                when (metadata) {
                    is PersistentPreference<*> ->
                        metadata
                            .storage(context)
                            .getValue(metadata.key, metadata.valueType as Class<Any>)
                            .toString()
                    else -> metadata.getPreferenceSummary(context)?.toString()
                }
            jsonValue?.let {
                deviceStateItemList.add(
                    DeviceStateItem(
                        key = metadata.key,
                        purpose = metadata.key,
                        name =
                            LocalizedString(
                                english = metadata.getPreferenceTitle(englishContext).toString(),
                                localized = metadata.getPreferenceTitle(context).toString(),
                            ),
                        jsonValue = it,
                        hintText = config?.hintText(englishContext, metadata),
                    )
                )
            }
        }

        val launchingIntent = screenMetaData.getLaunchIntent(context, null)
        val states =
            PerScreenDeviceStates(
                description = screenMetaData.getPreferenceScreenTitle(context)?.toString() ?: "",
                deviceStateItems = deviceStateItemList,
                intentUri = launchingIntent?.toUri(Intent.URI_INTENT_SCHEME),
            )
        Log.v(TAG, "Built per screen device states for $screenKey")
        return states
    }

    companion object {
        private const val TAG = "CatalystStateProviderExecutor"
    }
}
