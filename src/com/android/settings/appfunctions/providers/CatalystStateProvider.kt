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

import android.content.Context
import android.content.Intent
import android.util.Log
import com.android.settings.appfunctions.DeviceStateCategory
import com.android.settings.appfunctions.getDeviceStateItemList
import com.android.settings.appfunctions.getCatalystScreenConfigs
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceScreenCoordinate
import com.android.settingslib.metadata.PreferenceScreenRegistry
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
class CatalystStateProvider(
    private val context: Context,
    private val englishContext: Context,
) : DeviceStateProvider {
    private val settingConfigMap = getDeviceStateItemList().associateBy { it.settingKey }
    private val perScreenConfigMap = getCatalystScreenConfigs().associateBy { it.screenKey }
    private val screenKeyList = perScreenConfigMap.keys.toList()

    override suspend fun provide(
        requestCategory: DeviceStateCategory
    ): DeviceStateProviderResult {
        val perScreenDeviceStatesList = mutableListOf<PerScreenDeviceStates>()
        coroutineScope {
            val deferredList = screenKeyList.map { screenKey ->
                async {
                    try {
                        buildPerScreenDeviceStates(screenKey, requestCategory)
                    } catch (e: Exception) {
                        Log.e(TAG, "error building $screenKey", e)
                        null
                    }
                }
            }
            val results = deferredList.awaitAll()
            perScreenDeviceStatesList.addAll(results.filterNotNull())
        }
        return DeviceStateProviderResult(states = perScreenDeviceStatesList)
    }

    private suspend fun CoroutineScope.buildPerScreenDeviceStates(
        screenKey: String,
        requestCategory: DeviceStateCategory,
    ): PerScreenDeviceStates? {
        val perScreenConfig = perScreenConfigMap[screenKey]
        if (perScreenConfig == null || !perScreenConfig.enabled || requestCategory !in perScreenConfig.category) {
            return null
        }
        val screenMetaData =
            PreferenceScreenRegistry.create(
                context,
                PreferenceScreenCoordinate(screenKey, null),
            ) ?: return null
        if (screenMetaData is PreferenceAvailabilityProvider &&
            !screenMetaData.isAvailable(context)) {
            return null
        }
        val deviceStateItemList = mutableListOf<DeviceStateItem>()
        // TODO if child node is PreferenceScreen, recursively process it
        screenMetaData.getPreferenceHierarchy(context, this).forEachRecursivelyAsync {
            val metadata = it.metadata
            val config = settingConfigMap[metadata.key]
            // skip over explicitly disabled preferences
            if (config?.enabled == false) return@forEachRecursivelyAsync
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
                        name = LocalizedString(
                            english = metadata.getPreferenceTitle(englishContext).toString(),
                            localized = metadata.getPreferenceTitle(context).toString()
                        ),
                        jsonValue = it,
                        hintText = config?.hintText(englishContext, metadata)
                    )
                )
            }
        }

        val launchingIntent = screenMetaData.getLaunchIntent(context, null)
        return PerScreenDeviceStates(
            description = screenMetaData.getPreferenceScreenTitle(context)?.toString()
                ?: "",
            deviceStateItems = deviceStateItemList,
            intentUri = launchingIntent?.toUri(Intent.URI_INTENT_SCHEME)
        )
    }

    companion object {
        private const val TAG = "CatalystStateProvider"
    }
}